package com.durantco.dungeon.model.board;

import java.util.Random;

/**
 * A strategy for laying out a level's walls and floor.
 *
 * <p>Implementations decide structure only. Placing the hero, exit and other pieces onto that
 * structure is the board's job, which keeps generation testable without game objects and makes the
 * strategy swappable.
 */
public interface LevelGenerator {

  /**
   * Lays out one level.
   *
   * @param width board width in cells
   * @param height board height in cells
   * @param rng the randomness to draw on; the same seed must produce the same layout
   * @return the generated structure
   */
  DungeonLayout generate(int width, int height, Random rng);

  /**
   * A lower bound on the walkable cells this generator will produce, so the board can decide whether
   * a level's pieces will fit before generating anything.
   *
   * @param width board width in cells
   * @param height board height in cells
   * @return the fewest walkable cells any layout of this size will contain
   */
  int guaranteedWalkableCells(int width, int height);
}
