package com.travelvault.service;

import com.travelvault.City;
import com.travelvault.Country;
import com.travelvault.Memory;
import com.travelvault.strategy.AllMemoriesStrategy;
import com.travelvault.strategy.MemoryFilterStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class TravelVaultService {
    private final List<Country> countries;
    private final VaultStorage storage;
    private final Path storagePath;
    private MemoryFilterStrategy activeStrategy;

    public TravelVaultService(Path storagePath) {
        this.countries = new ArrayList<>();
        this.storage = new VaultStorage();
        this.storagePath = storagePath;
        this.activeStrategy = new AllMemoriesStrategy();
    }

    public void setFilterStrategy(MemoryFilterStrategy strategy) {
        if (strategy == null) {
            throw new IllegalArgumentException("Filter strategy cannot be null.");
        }
        this.activeStrategy = strategy;
    }

    public MemoryFilterStrategy getActiveStrategy() {
        return activeStrategy;
    }

    public Country getOrCreateCountry(String countryName, String continent) {
        return findCountry(countryName).orElseGet(() -> {
            Country created = new Country(countryName, continent);
            countries.add(created);
            return created;
        });
    }

    public Optional<Country> findCountry(String countryName) {
        if (countryName == null) {
            return Optional.empty();
        }
        return countries.stream()
                .filter(c -> c.getName().equalsIgnoreCase(countryName.trim()))
                .findFirst();
    }

    public void addMemory(String countryName, String continent, String cityName, Memory memory) {
        Country country = getOrCreateCountry(countryName, continent);
        City city = country.getOrCreateCity(cityName);
        city.addMemory(memory);
    }

    public List<Country> getCountries() {
        return Collections.unmodifiableList(countries);
    }

    public List<Memory> getFilteredMemories() {
        List<Memory> all = new ArrayList<>();
        for (Country country : countries) {
            for (City city : country.getCities()) {
                all.addAll(city.getMemories());
            }
        }
        return activeStrategy.filter(all);
    }

    public int getTotalMemoriesCount() {
        int count = 0;
        for (Country country : countries) {
            count += country.getTotalMemoriesCount();
        }
        return count;
    }

    public void save() throws IOException {
        storage.saveToFile(countries, storagePath);
    }

    public void load() throws IOException {
        List<Country> loaded = storage.loadFromFile(storagePath);
        countries.clear();
        countries.addAll(loaded);
    }
}