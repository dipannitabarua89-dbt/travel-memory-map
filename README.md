# Travel Memory Map (`travel-memory-map`)

A Java 21 object-oriented digital travel archive and interactive map vault built with Apache Maven and JUnit 5.

## Features
- **Hierarchical Domain Model:** Organizes travel records across `Country` $\rightarrow$ `City` $\rightarrow$ `Memory` (`LandmarkMemory` and `CulinaryMemory`) using abstraction, inheritance, and polymorphism.
- **Dynamic Filtering (Strategy Pattern):** Runtime-switchable filtering via `MemoryFilterStrategy` (`AllMemoriesStrategy`, `CategoryFilterStrategy`, `MinRatingFilterStrategy`).
- **Persistent Storage:** Custom UTF-8 CSV serialization and deserialization (`VaultStorage` & `TravelVaultService`).
- **Dual User Interfaces:**
  - **Console CLI (`com.travelvault.Main`):** Menu-driven terminal interface for adding, viewing, filtering, and saving memories.
  - **Interactive Web Map (`com.travelvault.ui.WebMapServer`):** Zero-dependency local HTTP server (`com.sun.net.httpserver.HttpServer`) serving a Leaflet + OpenStreetMap interface with coordinate pinning, category filters, and local photo uploads.
- **Automated Testing:** JUnit 5 unit test suite (`TravelVaultTest`) verifying polymorphism, aggregation math, strategy filtering, and isolated disk persistence (`@TempDir`).

## Project Structure
```text
src/
├── main/java/com/travelvault/
│   ├── Memory.java                  # Abstract base class with input validation
│   ├── LandmarkMemory.java          # Subclass for architectural landmarks
│   ├── CulinaryMemory.java          # Subclass for culinary experiences
│   ├── City.java                    # Aggregates memories & computes average ratings
│   ├── Country.java                 # Top-level geographic container
│   ├── Main.java                    # Interactive console application
│   ├── strategy/
│   │   ├── MemoryFilterStrategy.java
│   │   ├── AllMemoriesStrategy.java
│   │   ├── CategoryFilterStrategy.java
│   │   └── MinRatingFilterStrategy.java
│   ├── service/
│   │   ├── TravelVaultService.java  # Central application & strategy coordinator
│   │   └── VaultStorage.java        # CSV persistence layer
│   └── ui/
│       └── WebMapServer.java        # Interactive map & photo upload server
└── test/java/com/travelvault/
    └── TravelVaultTest.java         # JUnit 5 test suite