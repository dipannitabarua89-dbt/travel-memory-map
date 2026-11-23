# UML Class Diagram

Editable PlantUML source: [uml-diagram.puml](./uml-diagram.puml)

``mermaid
classDiagram
    class Memory {
        <<abstract>>
        -String title
        -LocalDate dateVisited
        -int rating
        -String notes
        +getCategory()* String
        +getSpecificDetails()* String
        +getSummary() String
        +compareTo(Memory) int
    }
    class LandmarkMemory {
        -String architecturalStyle
        -double entryFeeEur
    }
    class CulinaryMemory {
        -String dishName
        -double priceEur
    }
    class City {
        -String name
        -List~Memory~ memories
        +getAverageRating() double
    }
    class Country {
        -String name
        -String continent
        -List~City~ cities
    }
    class InvalidMemoryException
    class MemoryFilterStrategy {
        <<interface>>
        +filter(List~Memory~) List~Memory~
        +getDescription() String
    }
    class AllMemoriesStrategy
    class CategoryFilterStrategy
    class MinRatingFilterStrategy
    class TravelVaultService
    class VaultStorage
    class Main
    class WebMapServer

    Memory <|-- LandmarkMemory
    Memory <|-- CulinaryMemory
    Memory ..> InvalidMemoryException : throws
    Country "1" *-- "0..*" City
    City "1" *-- "0..*" Memory
    MemoryFilterStrategy <|.. AllMemoriesStrategy
    MemoryFilterStrategy <|.. CategoryFilterStrategy
    MemoryFilterStrategy <|.. MinRatingFilterStrategy
    TravelVaultService "1" o-- "0..*" Country
    TravelVaultService "1" --> "1" MemoryFilterStrategy
    TravelVaultService "1" --> "1" VaultStorage
    Main ..> TravelVaultService
    WebMapServer ..> TravelVaultService
