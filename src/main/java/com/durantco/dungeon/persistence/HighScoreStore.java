package com.durantco.dungeon.persistence;

/**
 * Where the best score so far is kept.
 *
 * <p>Neither method throws. A high score is not state a game can fail over: a read-only home directory
 * should cost the player their record, not their session. Implementations are expected to degrade rather
 * than propagate, and to say so in their own documentation.
 */
public interface HighScoreStore {

  /**
   * @return the best score recorded, or zero if none has been
   */
  int highest();

  /**
   * Records a score, keeping it only if it beats what is already there.
   *
   * @param score the score to offer
   */
  void record(int score);
}
