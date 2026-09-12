package com.durantco.dungeon.persistence;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Keeps the best score in a properties file between runs.
 *
 * <p>Deliberately forgiving. A missing, unreadable, or nonsensical file reads as no score rather than as
 * an error, and a score that cannot be written is lost rather than thrown: the alternative is a game that
 * dies at the moment it would have congratulated you. The trade is that a broken save is invisible, which
 * is the right way round for a high score and would not be for a saved game.
 */
public final class FileHighScoreStore implements HighScoreStore {

  private static final String DIRECTORY = ".dungeon-crawler";
  private static final String SCORE_FILE = "highscore.properties";
  private static final String KEY = "highScore";

  private final Path file;

  /**
   * @param file where the score lives; its directory is created when a score is recorded
   */
  public FileHighScoreStore(Path file) {
    this.file = file;
  }

  /**
   * The location used by the installed game, under the user's home directory.
   *
   * @return a store pointing at the standard score file
   */
  public static FileHighScoreStore inUserHome() {
    return new FileHighScoreStore(
        Path.of(System.getProperty("user.home"), DIRECTORY, SCORE_FILE));
  }

  @Override
  public int highest() {
    if (!Files.exists(file)) {
      return 0;
    }
    Properties stored = new Properties();
    try (var in = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      stored.load(in);
    } catch (IOException | IllegalArgumentException e) {
      return 0; // an unreadable record is no record
    }
    try {
      return Integer.parseInt(stored.getProperty(KEY, "0").strip());
    } catch (NumberFormatException e) {
      return 0;
    }
  }

  @Override
  public void record(int score) {
    if (score <= highest()) {
      return;
    }
    Properties toStore = new Properties();
    toStore.setProperty(KEY, Integer.toString(score));
    try {
      Path parent = file.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      try (var out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
        toStore.store(out, "Dungeon Crawler high score");
      }
    } catch (IOException e) {
      // Losing a high score is not worth ending a game over. See this class's documentation.
    }
  }

  /**
   * @return where this store keeps the score
   */
  public Path file() {
    return file;
  }
}
