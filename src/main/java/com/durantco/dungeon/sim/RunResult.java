package com.durantco.dungeon.sim;

/**
 * What happened in one simulated game.
 *
 * @param seed the game that was played
 * @param agentName which agent played it
 * @param turns how many moves it lasted
 * @param levelReached the level it was on when it finished
 * @param score the score it finished with
 * @param died true if the hero was caught, false if the run hit the turn limit still alive
 */
public record RunResult(
    long seed, String agentName, int turns, int levelReached, int score, boolean died) {

  /**
   * @return how many levels this run finished
   */
  public int levelsCleared() {
    return levelReached - 1;
  }
}
