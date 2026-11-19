package com.travelvault;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Country {
    private final String name;
    private final String continent;
    private final List<City> cities;

    public Country(String name, String continent) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Country name cannot be empty.");
        }
        this.name = name.trim();
        this.continent = (continent == null || continent.isBlank()) ? "Unknown" : continent.trim();
        this.cities = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public String getContinent() {
        return continent;
    }

    public City getOrCreateCity(String cityName) {
        return findCity(cityName).orElseGet(() -> {
            City newCity = new City(cityName);
            cities.add(newCity);
            return newCity;
        });
    }

    public Optional<City> findCity(String cityName) {
        if (cityName == null) {
            return Optional.empty();
        }
        return cities.stream()
                .filter(c -> c.getName().equalsIgnoreCase(cityName.trim()))
                .findFirst();
    }

    public List<City> getCities() {
        return Collections.unmodifiableList(cities);
    }

    public int getTotalMemoriesCount() {
        int total = 0;
        for (City city : cities) {
            total += city.getMemories().size();
        }
        return total;
    }
}