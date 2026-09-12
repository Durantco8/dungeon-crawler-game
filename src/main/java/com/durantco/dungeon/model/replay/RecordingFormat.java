package com.durantco.dungeon.model.replay;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.GeneratorSpec;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads and writes a recording as plain text.
 *
 * <p>A saved game is a setup and a string of moves, so it needs no library to represent: the whole of a
 * long session is a few hundred bytes. Keeping it readable is worth more than compactness here, because
 * a save file is also a replay file and being able to open one, read the seed, and see the inputs makes
 * a divergence something you can inspect rather than only observe.
 *
 * <pre>
 *   dungeon-crawler-save 1
 *   seed 20260912
 *   board 28 18
 *   difficulty HARD
 *   generator bsp 3 4
 *   moves URRDLLU
 * </pre>
 */
public final class RecordingFormat {

  private static final String HEADER = "dungeon-crawler-save";
  private static final int VERSION = 1;

  private RecordingFormat() {}

  /**
   * @param recording the game to write
   * @return the text form of that game
   */
  public static String write(GameRecording recording) {
    GameSetup setup = recording.setup();
    StringBuilder moves = new StringBuilder();
    for (PlayerMove move : recording.moves()) {
      moves.append(move.symbol());
    }
    return HEADER
        + ' '
        + VERSION
        + "\nseed "
        + setup.seed()
        + "\nboard "
        + setup.width()
        + ' '
        + setup.height()
        + "\ndifficulty "
        + setup.difficulty().name()
        + "\ngenerator "
        + writeGenerator(setup.generator())
        + "\nmoves "
        + moves
        + '\n';
  }

  /**
   * @param text a game written by {@link #write}
   * @return the game it describes
   * @throws IllegalArgumentException if the text is not a saved game this version understands
   */
  public static GameRecording read(String text) {
    List<String> lines = new ArrayList<>();
    for (String line : text.split("\n")) {
      if (!line.isBlank()) {
        lines.add(line.strip());
      }
    }
    if (lines.isEmpty()) {
      throw new IllegalArgumentException("A saved game cannot be empty");
    }

    String[] header = lines.get(0).split(" ");
    if (header.length != 2 || !HEADER.equals(header[0])) {
      throw new IllegalArgumentException("Not a dungeon crawler saved game");
    }
    if (parseInt(header[1], "version") != VERSION) {
      throw new IllegalArgumentException(
          "Saved game is version " + header[1] + ", but this build reads version " + VERSION);
    }

    long seed = 0;
    int width = 0;
    int height = 0;
    Difficulty difficulty = null;
    GeneratorSpec generator = null;
    String moves = "";
    for (String line : lines.subList(1, lines.size())) {
      String[] parts = line.split(" ", 2);
      String value = "";
      if (parts.length == 2) {
        value = parts[1].strip();
      }
      switch (parts[0]) {
        case "seed" -> seed = parseLong(value, "seed");
        case "board" -> {
          String[] size = value.split(" ");
          if (size.length != 2) {
            throw new IllegalArgumentException("A board needs a width and a height, got: " + value);
          }
          width = parseInt(size[0], "board width");
          height = parseInt(size[1], "board height");
        }
        case "difficulty" -> difficulty = parseDifficulty(value);
        case "generator" -> generator = readGenerator(value);
        case "moves" -> moves = value;
        default -> throw new IllegalArgumentException("Unknown entry in saved game: " + parts[0]);
      }
    }
    if (difficulty == null || generator == null || width == 0 || height == 0) {
      throw new IllegalArgumentException("Saved game is missing a seed, board, difficulty or generator");
    }

    List<PlayerMove> replayed = new ArrayList<>();
    for (char symbol : moves.toCharArray()) {
      replayed.add(PlayerMove.ofSymbol(symbol));
    }
    return new GameRecording(
        new GameSetup(seed, width, height, difficulty, generator), replayed);
  }

  /**
   * Exhaustive over the sealed GeneratorSpec, so adding a generator fails to compile here rather than
   * silently writing a save file that cannot be read back.
   */
  private static String writeGenerator(GeneratorSpec spec) {
    return switch (spec) {
      case GeneratorSpec.Bsp bsp -> "bsp " + bsp.minRoomSize() + ' ' + bsp.maxSplitDepth();
      case GeneratorSpec.Scatter scatter -> "scatter " + scatter.wallCount();
    };
  }

  private static GeneratorSpec readGenerator(String value) {
    String[] parts = value.split(" ");
    return switch (parts[0]) {
      case "bsp" -> {
        if (parts.length != 3) {
          throw new IllegalArgumentException("A bsp generator needs a room size and a depth");
        }
        yield new GeneratorSpec.Bsp(
            parseInt(parts[1], "minimum room size"), parseInt(parts[2], "split depth"));
      }
      case "scatter" -> {
        if (parts.length != 2) {
          throw new IllegalArgumentException("A scatter generator needs a wall count");
        }
        yield new GeneratorSpec.Scatter(parseInt(parts[1], "wall count"));
      }
      default -> throw new IllegalArgumentException("Unknown generator: " + parts[0]);
    };
  }

  private static Difficulty parseDifficulty(String value) {
    for (Difficulty difficulty : Difficulty.values()) {
      if (difficulty.name().equals(value)) {
        return difficulty;
      }
    }
    throw new IllegalArgumentException("Unknown difficulty: " + value);
  }

  private static int parseInt(String value, String what) {
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Saved game has an unreadable " + what + ": " + value);
    }
  }

  private static long parseLong(String value, String what) {
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Saved game has an unreadable " + what + ": " + value);
    }
  }
}
