package com.durantco.dungeon.persistence;

import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.RecordingFormat;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/** Keeps a saved game in a text file. */
public final class FileGameStore implements GameStore {

  private static final String DIRECTORY = ".dungeon-crawler";
  private static final String SAVE_FILE = "save.txt";

  private final Path file;

  /**
   * @param file where the saved game lives; its directory is created on save if needed
   */
  public FileGameStore(Path file) {
    this.file = file;
  }

  /**
   * The location used by the installed game, under the user's home directory.
   *
   * @return a store pointing at the standard save file
   */
  public static FileGameStore inUserHome() {
    return new FileGameStore(
        Path.of(System.getProperty("user.home"), DIRECTORY, SAVE_FILE));
  }

  @Override
  public void save(GameRecording recording) throws IOException {
    Path parent = file.getParent();
    if (parent != null) {
      Files.createDirectories(parent);
    }
    Files.writeString(file, RecordingFormat.write(recording), StandardCharsets.UTF_8);
  }

  @Override
  public Optional<GameRecording> load() throws IOException {
    if (!Files.exists(file)) {
      return Optional.empty();
    }
    return Optional.of(RecordingFormat.read(Files.readString(file, StandardCharsets.UTF_8)));
  }

  /**
   * @return where this store keeps its saved game
   */
  public Path file() {
    return file;
  }
}
