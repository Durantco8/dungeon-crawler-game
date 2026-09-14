# Architecture

Three diagrams: how the layers fit together, how one turn resolves, and how a level gets built.

## Layers

Model–View–Controller with the Observer pattern carrying changes back to the screen. The arrows are
dependencies, so the absence of an arrow matters as much as its presence: **nothing in the model points at
the view.**

```mermaid
flowchart TB
    subgraph viewLayer["view (JavaFX)"]
        direction LR
        ViewClass["View"]
        GameViewClass["GameView"]
        TitleView["TitleScreenView"]
        Sprites["PieceSprites"]
    end

    subgraph controllerLayer["controller"]
        Ctrl["ControllerImpl"]
    end

    subgraph modelLayer["model (no JavaFX)"]
        direction LR
        Session["GameSession<br/>holds the recording"]
        ModelC["ModelImpl"]
        BoardC["BoardImpl"]
    end

    subgraph boardParts["board internals"]
        direction LR
        Pieces["pieces"]
        Strategies["movement strategies"]
        Generators["level generators"]
    end

    Launcher["AppLauncher<br/>composition root"] --> viewLayer
    Launcher --> Ctrl
    Launcher --> Session

    ViewClass --> GameViewClass
    ViewClass --> TitleView
    GameViewClass --> Sprites
    GameViewClass -->|"input"| Ctrl
    Ctrl -->|"commands"| Session
    Session --> ModelC
    ModelC --> BoardC
    BoardC --> boardParts
    Session -.->|"Observer: update()"| ViewClass
    GameViewClass -.->|"reads state"| Session
```

Everything above the model line knows about JavaFX; nothing below it does. The game logic runs headless,
which is what lets the whole suite finish in a couple of seconds and lets the simulation harness play
hundreds of games on a machine with no display.

Persistence and tooling hang off the same model, never off the view:

```mermaid
flowchart LR
    Session["GameSession"]
    Session --> Recording["GameRecording<br/>setup plus inputs"]
    Recording --> Replay["GameReplay"]
    Recording --> Format["RecordingFormat"]
    Format --> SaveStore["FileGameStore<br/>save.txt"]
    Session --> Scores["FileHighScoreStore"]
    Replay --> Undo["undo"]
    Replay --> Load["load a saved game"]
    Agents["scripted agents"] --> Session
    Agents --> Harness["Simulation<br/>SimulationReport"]
```

A recording is a setup and a string of moves. Because the game is reproducible from its seed, that one
value is the whole of a game, which is why replay, saving, loading, and undo are all the same feature
wearing different hats.


Two things worth pointing out.

**`GameSession` is a `Model`.** It forwards to whichever model is current and can replace it underneath.
That is what makes undo possible: rebuilding a game produces a new `ModelImpl`, and a view holding the old
one directly would be left watching a game nobody is playing.

**The view maps pieces to sprites, not the model.** A `Piece` exposes a `PieceType`, and `PieceSprites` in
the view decides which PNG that is in which theme. The model has never heard of a file.

## One turn

What happens between a keypress and a redraw. The interesting part is that collisions are resolved by
asking the piece being walked into, never by testing its type.

```mermaid
sequenceDiagram
    participant Player
    participant Controller
    participant Session as GameSession
    participant Board as BoardImpl
    participant Target as Piece entered
    participant Enemy
    participant View

    Player->>Controller: moveRight()
    Controller->>Session: moveRight()
    Session->>Session: record the input
    Session->>Board: moveHero(0, 1)

    Board->>Target: onHeroEnter(hero)
    Target-->>Board: CollisionResult
    Note over Board,Target: Wall returns BLOCKED, Treasure returns points,<br/>Exit returns NEXT_LEVEL, Enemy returns GAME_OVER.<br/>The board never asks what type it is.

    alt refused, fatal, or level complete
        Board-->>Session: result, no enemy acts
    else the move stands
        Board->>Board: move the hero, note its heading
        loop each enemy
            Board->>Enemy: grant a turn's energy
            loop while it can afford to act
                Board->>Enemy: chooseStep(context)
                Note over Enemy: Sees the hero only with line of sight.<br/>Chases with A*, ambushes ahead of it,<br/>or walks a patrol.
                Enemy-->>Board: a cell, or nothing
                Board->>Board: resolve that step
            end
        end
        Board-->>Session: points and outcome
    end

    Session->>View: update()
    View->>Session: read the board and score
```

## Building a level

Generation and verification are separate, and both happen before the player sees anything.

```mermaid
flowchart TD
    Setup["GameSetup<br/>seed, size, difficulty, generator"] --> CanFit{"will the pieces fit?"}
    CanFit -->|no| Over["end the game"]
    CanFit -->|yes| Generate["LevelGenerator.generate"]

    Generate --> Layout["DungeonLayout<br/>walkable cells plus rooms"]
    Layout --> Enough{"enough floor?"}
    Enough -->|no| Retry
    Enough -->|yes| Populate["place walls, hero, exit,<br/>enemies, treasure, thieves"]

    Populate --> Margin["enemies spawn at least<br/>4 steps from the hero"]
    Margin --> Verify{"BFS: can the hero reach<br/>the exit and every treasure?"}
    Verify -->|no| Retry["try another layout<br/>up to 10 times"]
    Retry --> Generate
    Verify -->|yes| Play["play"]
```

The retry loop is a safety net, not the mechanism. The partitioning generator joins every node of its tree
to its sibling as the recursion unwinds, so the floor is connected **by construction** and the BFS check is
an assertion about that property. A test asserts the retry counter stays at exactly one across 400 seeds:
if it ever climbs, the generator has a bug, which is a far more useful signal than a level being quietly
regenerated. The same budget does real work for the older scatter generator, which makes no such promise.
