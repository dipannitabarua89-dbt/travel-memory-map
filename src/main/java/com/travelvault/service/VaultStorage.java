package com.travelvault.service;

import com.travelvault.City;
import com.travelvault.Country;
import com.travelvault.CulinaryMemory;
import com.travelvault.LandmarkMemory;
import com.travelvault.Memory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class VaultStorage {
    private static final String DELIMITER = ";";
    private static final String HEADER = "Country;Continent;City;Category;Title;Date;Rating;Notes;SpecificText;AmountEur";

    public void saveToFile(List<Country> countries, Path filePath) throws IOException {
        if (filePath.getParent() != null) {
            Files.createDirectories(filePath.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(filePath, StandardCharsets.UTF_8)) {
            writer.write(HEADER);
            writer.newLine();

            for (Country country : countries) {
                for (City city : country.getCities()) {
                    for (Memory memory : city.getMemories()) {
                        writer.write(formatRow(country, city, memory));
                        writer.newLine();
                    }
                }
            }
        }
    }

    public List<Country> loadFromFile(Path filePath) throws IOException {
        Map<String, Country> countryMap = new LinkedHashMap<>();
        if (!Files.exists(filePath)) {
            return new ArrayList<>();
        }

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line = reader.readLine(); // Skip header
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(DELIMITER, -1);
                if (parts.length < 10) {
                    continue;
                }

                String countryName = unescape(parts[0]);
                String continent = unescape(parts[1]);
                String cityName = unescape(parts[2]);
                String category = unescape(parts[3]);
                String title = unescape(parts[4]);
                LocalDate date = LocalDate.parse(parts[5].trim());
                int rating = Integer.parseInt(parts[6].trim());
                String notes = unescape(parts[7]);
                String specificText = unescape(parts[8]);
                double amountEur = Double.parseDouble(parts[9].trim());

                Country country = countryMap.computeIfAbsent(
                        countryName.toLowerCase(),
                        k -> new Country(countryName, continent)
                );
                City city = country.getOrCreateCity(cityName);

                Memory memory;
                if ("LANDMARK".equalsIgnoreCase(category)) {
                    memory = new LandmarkMemory(title, date, rating, notes, specificText, amountEur);
                } else {
                    memory = new CulinaryMemory(title, date, rating, notes, specificText, amountEur);
                }
                city.addMemory(memory);
            }
        }
        return new ArrayList<>(countryMap.values());
    }

    private String formatRow(Country country, City city, Memory memory) {
        String specificText = "";
        double amountEur = 0.0;

        if (memory instanceof LandmarkMemory lm) {
            specificText = lm.getArchitecturalStyle();
            amountEur = lm.getEntryFeeEur();
        } else if (memory instanceof CulinaryMemory cm) {
            specificText = cm.getDishName();
            amountEur = cm.getPriceEur();
        }

        return String.join(DELIMITER,
                escape(country.getName()),
                escape(country.getContinent()),
                escape(city.getName()),
                escape(memory.getCategory()),
                escape(memory.getTitle()),
                memory.getDateVisited().toString(),
                String.valueOf(memory.getRating()),
                escape(memory.getNotes()),
                escape(specificText),
                String.valueOf(amountEur)
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace(";", ",").replace("\n", " ").replace("\r", " ");
    }

    private String unescape(String value) {
        return value == null ? "" : value.trim();
    }
}