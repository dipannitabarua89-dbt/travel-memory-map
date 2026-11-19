package com.travelvault;

import com.travelvault.service.TravelVaultService;
import com.travelvault.strategy.AllMemoriesStrategy;
import com.travelvault.strategy.CategoryFilterStrategy;
import com.travelvault.strategy.MinRatingFilterStrategy;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Path DATA_FILE = Paths.get("data", "travel_memories.csv");

    public static void main(String[] args) {
        TravelVaultService service = new TravelVaultService(DATA_FILE);
        Scanner scanner = new Scanner(System.in);

        try {
            service.load();
            if (service.getTotalMemoriesCount() > 0) {
                System.out.println("Loaded " + service.getTotalMemoriesCount() + " saved memories from " + DATA_FILE);
            }
        } catch (IOException e) {
            System.out.println("Starting with an empty vault (could not read previous data: " + e.getMessage() + ")");
        }

        boolean running = true;
        while (running) {
            printMenu(service);
            String choice = scanner.nextLine().trim();

            switch (choice) {
                case "1" -> addLandmarkMemory(scanner, service);
                case "2" -> addCulinaryMemory(scanner, service);
                case "3" -> viewAllLocations(service);
                case "4" -> viewFilteredMemories(service);
                case "5" -> changeFilterStrategy(scanner, service);
                case "6" -> saveData(service);
                case "0" -> {
                    saveData(service);
                    System.out.println("Goodbye! Safe travels.");
                    running = false;
                }
                default -> System.out.println("Invalid option. Please choose 0-6.");
            }
        }
        scanner.close();
    }

    private static void printMenu(TravelVaultService service) {
        System.out.println("\n==========================================");
        System.out.println("          TRAVEL MEMORY MAP VAULT         ");
        System.out.println("==========================================");
        System.out.println("Active Filter: " + service.getActiveStrategy().getDescription());
        System.out.println("Total Memories: " + service.getTotalMemoriesCount());
        System.out.println("------------------------------------------");
        System.out.println("1. Add a Landmark Memory");
        System.out.println("2. Add a Culinary Memory");
        System.out.println("3. View Countries, Cities & Summaries");
        System.out.println("4. View Filtered Memories");
        System.out.println("5. Change Filter Strategy");
        System.out.println("6. Save Memories to File");
        System.out.println("0. Save & Exit");
        System.out.print("Select an option: ");
    }

    private static void addLandmarkMemory(Scanner scanner, TravelVaultService service) {
        try {
            System.out.print("Country name: ");
            String country = scanner.nextLine();
            System.out.print("Continent: ");
            String continent = scanner.nextLine();
            System.out.print("City name: ");
            String city = scanner.nextLine();
            System.out.print("Landmark title: ");
            String title = scanner.nextLine();
            LocalDate date = readDate(scanner);
            int rating = readRating(scanner);
            System.out.print("Notes: ");
            String notes = scanner.nextLine();
            System.out.print("Architectural style: ");
            String style = scanner.nextLine();
            System.out.print("Entry fee (EUR): ");
            double fee = Double.parseDouble(scanner.nextLine().trim());

            LandmarkMemory memory = new LandmarkMemory(title, date, rating, notes, style, fee);
            service.addMemory(country, continent, city, memory);
            System.out.println("Landmark memory added successfully!");
        } catch (Exception e) {
            System.out.println("Error adding landmark memory: " + e.getMessage());
        }
    }

    private static void addCulinaryMemory(Scanner scanner, TravelVaultService service) {
        try {
            System.out.print("Country name: ");
            String country = scanner.nextLine();
            System.out.print("Continent: ");
            String continent = scanner.nextLine();
            System.out.print("City name: ");
            String city = scanner.nextLine();
            System.out.print("Restaurant / Experience title: ");
            String title = scanner.nextLine();
            LocalDate date = readDate(scanner);
            int rating = readRating(scanner);
            System.out.print("Notes: ");
            String notes = scanner.nextLine();
            System.out.print("Dish name: ");
            String dish = scanner.nextLine();
            System.out.print("Meal price (EUR): ");
            double price = Double.parseDouble(scanner.nextLine().trim());

            CulinaryMemory memory = new CulinaryMemory(title, date, rating, notes, dish, price);
            service.addMemory(country, continent, city, memory);
            System.out.println("Culinary memory added successfully!");
        } catch (Exception e) {
            System.out.println("Error adding culinary memory: " + e.getMessage());
        }
    }

    private static void viewAllLocations(TravelVaultService service) {
        List<Country> countries = service.getCountries();
        if (countries.isEmpty()) {
            System.out.println("No travel memories recorded yet.");
            return;
        }
        for (Country country : countries) {
            System.out.println("\n=== " + country.getName() + " (" + country.getContinent() + ") ===");
            for (City city : country.getCities()) {
                System.out.printf("  City: %s | Memories: %d | Avg Rating: %.1f/5%n",
                        city.getName(), city.getMemories().size(), city.getAverageRating());
                for (Memory memory : city.getMemories()) {
                    System.out.println("    - " + memory.getSummary());
                }
            }
        }
    }

    private static void viewFilteredMemories(TravelVaultService service) {
        List<Memory> filtered = service.getFilteredMemories();
        System.out.println("\n--- Filtered Results (" + service.getActiveStrategy().getDescription() + ") ---");
        if (filtered.isEmpty()) {
            System.out.println("No memories match the active filter.");
            return;
        }
        for (Memory memory : filtered) {
            System.out.println("  * " + memory.getSummary());
        }
    }

    private static void changeFilterStrategy(Scanner scanner, TravelVaultService service) {
        System.out.println("\nChoose a filter strategy:");
        System.out.println("1. Show All Memories");
        System.out.println("2. Landmarks Only");
        System.out.println("3. Culinary Experiences Only");
        System.out.println("4. Minimum Rating Filter");
        System.out.print("Choice: ");
        String choice = scanner.nextLine().trim();

        switch (choice) {
            case "1" -> service.setFilterStrategy(new AllMemoriesStrategy());
            case "2" -> service.setFilterStrategy(new CategoryFilterStrategy("LANDMARK"));
            case "3" -> service.setFilterStrategy(new CategoryFilterStrategy("CULINARY"));
            case "4" -> {
                int min = readRating(scanner);
                service.setFilterStrategy(new MinRatingFilterStrategy(min));
            }
            default -> System.out.println("Invalid filter choice.");
        }
    }

    private static void saveData(TravelVaultService service) {
        try {
            service.save();
            System.out.println("All memories saved to " + DATA_FILE);
        } catch (IOException e) {
            System.out.println("Failed to save memories: " + e.getMessage());
        }
    }

    private static LocalDate readDate(Scanner scanner) {
        while (true) {
            System.out.print("Date visited (YYYY-MM-DD): ");
            String input = scanner.nextLine().trim();
            try {
                return LocalDate.parse(input);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid format. Please use YYYY-MM-DD (e.g., 2026-08-15).");
            }
        }
    }

    private static int readRating(Scanner scanner) {
        while (true) {
            System.out.print("Rating (1-5): ");
            try {
                int rating = Integer.parseInt(scanner.nextLine().trim());
                if (rating >= 1 && rating <= 5) {
                    return rating;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Please enter a number between 1 and 5.");
        }
    }
}