package com.durantco.dungeon.persistence;

import com.durantco.dungeon.model.replay.GameRecording;
import java.io.IOException;
import java.util.Optional;

/**
 * Somewhere a game can be kept between runs.
 *
 * <p>An interface rather than direct file access so tests can save and load without touching the user's
 * home directory, and so the game does not need to know whether its saves live in a file at all.
 */
public interface GameStore {

  /**
   * Keeps a game, replacing whatever was kept before.
   *
   * @param recording the game to keep
   * @throws IOException if it could not be written
   */
  void save(GameRecording recording) throws IOException;

  /**
   * @return the kept game, or empty if there is none
   * @throws IOException if something is there but could not be read
   */
  Optional<GameRecording> load() throws IOException;
}
