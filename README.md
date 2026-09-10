# Dungeon Crawler Game

A 2D tile-based dungeon crawler built with Java and JavaFX. Navigate through procedurally generated dungeons, collect treasure, avoid enemies, and escape through the exit portal.

## Screenshots

<!-- Add screenshots of your game here -->

## Features

- **Grid-based movement** on an 8x8 dungeon board
- **Multiple game pieces**: Hero, Enemies, Treasure, Thieves, Walls, and Exit Portals
- **Two difficulty modes**: Easy (random enemy movement) and Hard (enemies chase the hero)
- **Scoring system**: Collect treasure (+5 points), avoid thieves (-5 points)
- **Level progression**: Each level increases in difficulty with more enemies
- **Dark and Light themes**: Toggle between visual styles
- **High score tracking** across levels

## Tech Stack

- **Language**: Java 24
- **UI Framework**: JavaFX 21
- **Build Tool**: Maven
- **Architecture**: Model-View-Controller (MVC) with Observer pattern

## How to Run

### Prerequisites
- Java 24+
- Maven 3.6+

### Build and Run

```bash
# Compile the project
mvn compile

# Run the application
mvn javafx:run

# Build a standalone JAR
mvn package
java -jar target/dungeon-crawler-game-1.0-SNAPSHOT.jar
```

## Project Structure

```
src/main/java/com/durantco/dungeon/
├── Main.java                  # Application entry point
├── controller/
│   ├── Controller.java        # Controller interface
│   └── ControllerImpl.java    # Handles user input and game logic
├── model/
│   ├── Model.java             # Game state interface
│   ├── ModelImpl.java         # Game state management and scoring
│   ├── Observer.java          # Observer pattern interface
│   ├── Subject.java           # Subject pattern interface
│   ├── board/
│   │   ├── Board.java         # Board interface
│   │   ├── BoardImpl.java     # 8x8 grid with collision detection
│   │   └── Posn.java          # Position (row, col) representation
│   └── pieces/
│       ├── Piece.java         # Base piece interface
│       ├── APiece.java        # Abstract piece implementation
│       ├── MovablePiece.java  # Movable piece interface
│       ├── CollisionResult.java
│       ├── Hero.java          # Player character
│       ├── Enemy.java         # AI opponents
│       ├── Treasure.java      # Collectible items
│       ├── Thief.java         # Score-reducing hazard
│       ├── Wall.java          # Immovable obstacles
│       └── Exit.java          # Level completion portal
└── view/
    ├── AppLauncher.java       # JavaFX application launcher
    ├── FXComponent.java       # UI component interface
    ├── View.java              # Main view manager
    ├── GameView.java          # In-game UI rendering
    └── TitleScreenView.java   # Title screen and menu
```
