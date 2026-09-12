package com.durantco.dungeon.persistence;

/**
 * Keeps the best score for as long as the process lives, and no longer.
 *
 * <p>The behaviour the game had before high scores were persisted, kept as the default so a model built
 * without a store behaves exactly as it used to, and so tests need no filesystem.
 */
public final class InMemoryHighScoreStore implements HighScoreStore {

  private int highest;

  @Override
  public int highest() {
    return highest;
  }

  @Override
  public void record(int score) {
    if (score > highest) {
      highest = score;
    }
  }
}
