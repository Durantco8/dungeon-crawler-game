package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.GeneratorSpec;

/**
 * Everything needed to build a particular game, as data.
 *
 * <p>These values used to be literals at the composition root, which meant a game could be played but
 * never described, and so never recorded, saved, or replayed. Together with the sequence of moves the
 * player made, a setup is a complete account of a game: the seed fixes the dungeons, the enemy mix, and
 * every arbitrary choice the board makes.
 *
 * @param seed the seed for every random decision in the game
 * @param width board width in cells
 * @param height board height in cells
 * @param difficulty which enemies the levels spawn
 * @param generator how each level is laid out
 */
public record GameSetup(
    long seed, int width, int height, Difficulty difficulty, GeneratorSpec generator) {

  /** Board size the game ships with: big enough for the partitioning generator to show its work. */
  private static final int STANDARD_WIDTH = 28;

  private static final int STANDARD_HEIGHT = 18;

  private static final int STANDARD_MIN_ROOM_SIZE = 3;
  private static final int STANDARD_SPLIT_DEPTH = 4;

  public GameSetup {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("A board needs a positive width and height");
    }
    if (difficulty == null || generator == null) {
      throw new IllegalArgumentException("A setup needs both a difficulty and a generator");
    }
  }

  /**
   * The game as shipped: a partitioned dungeon at the standard size, on easy.
   *
   * @param seed the seed to play
   * @return the standard setup for that seed
   */
  public static GameSetup standard(long seed) {
    return new GameSetup(
        seed,
        STANDARD_WIDTH,
        STANDARD_HEIGHT,
        Difficulty.EASY,
        new GeneratorSpec.Bsp(STANDARD_MIN_ROOM_SIZE, STANDARD_SPLIT_DEPTH));
  }

  /**
   * @param difficulty the difficulty to play at
   * @return the same setup at a different difficulty
   */
  public GameSetup withDifficulty(Difficulty difficulty) {
    return new GameSetup(seed, width, height, difficulty, generator);
  }

  /**
   * @param seed the seed to play
   * @return the same setup with a different seed
   */
  public GameSetup withSeed(long seed) {
    return new GameSetup(seed, width, height, difficulty, generator);
  }
}
