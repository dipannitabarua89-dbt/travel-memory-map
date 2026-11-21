package com.travelvault;

import com.travelvault.service.TravelVaultService;
import com.travelvault.strategy.CategoryFilterStrategy;
import com.travelvault.strategy.MinRatingFilterStrategy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TravelVaultTest {

    @Test
    void testPolymorphismAndInheritance() {
        Memory landmark = new LandmarkMemory(
                "Eiffel Tower", LocalDate.of(2025, 6, 12), 5,
                "Visited at sunset", "Iron Lattice", 29.40
        );
        Memory culinary = new CulinaryMemory(
                "Le Petit Bistro", LocalDate.of(2025, 6, 13), 4,
                "Crispy crust", "Crème Brûlée", 12.50
        );

        assertEquals("LANDMARK", landmark.getCategory());
        assertEquals("CULINARY", culinary.getCategory());
        assertTrue(landmark.getSummary().contains("Iron Lattice"));
        assertTrue(culinary.getSummary().contains("Crème Brûlée"));
    }

    @Test
    void testCityCompositionAndAverageRating() {
        City city = new City("Budapest");
        assertEquals(0.0, city.getAverageRating());

        city.addMemory(new LandmarkMemory("Buda Castle", LocalDate.now(), 5, "Historic", "Baroque", 15.0));
        city.addMemory(new CulinaryMemory("Central Market", LocalDate.now(), 3, "Busy", "Lángos", 6.0));

        assertEquals(2, city.getMemories().size());
        assertEquals(4.0, city.getAverageRating(), 0.001);
        assertTrue(city.removeMemoryByTitle("buda castle"));
        assertEquals(1, city.getMemories().size());
    }

    @Test
    void testStrategyPatternFiltering(@TempDir Path tempDir) {
        TravelVaultService service = new TravelVaultService(tempDir.resolve("test.csv"));
        service.addMemory("Italy", "Europe", "Rome",
                new LandmarkMemory("Colosseum", LocalDate.now(), 5, "Amazing", "Roman", 18.0));
        service.addMemory("Italy", "Europe", "Rome",
                new CulinaryMemory("Trattoria", LocalDate.now(), 3, "Good pasta", "Carbonara", 14.0));

        service.setFilterStrategy(new CategoryFilterStrategy("LANDMARK"));
        List<Memory> landmarks = service.getFilteredMemories();
        assertEquals(1, landmarks.size());
        assertEquals("Colosseum", landmarks.get(0).getTitle());

        service.setFilterStrategy(new MinRatingFilterStrategy(4));
        List<Memory> topRated = service.getFilteredMemories();
        assertEquals(1, topRated.size());
        assertEquals(5, topRated.get(0).getRating());
    }

    @Test
    void testCsvPersistenceSaveAndLoad(@TempDir Path tempDir) throws IOException {
        Path file = tempDir.resolve("vault.csv");
        TravelVaultService writerService = new TravelVaultService(file);
        writerService.addMemory("Japan", "Asia", "Kyoto",
                new LandmarkMemory("Fushimi Inari", LocalDate.of(2025, 4, 2), 5, "Thousands of gates", "Shinto Shrine", 0.0));
        writerService.save();

        TravelVaultService readerService = new TravelVaultService(file);
        readerService.load();

        assertEquals(1, readerService.getTotalMemoriesCount());
        assertEquals("Japan", readerService.getCountries().get(0).getName());
        assertEquals("Kyoto", readerService.getCountries().get(0).getCities().get(0).getName());
    }
}