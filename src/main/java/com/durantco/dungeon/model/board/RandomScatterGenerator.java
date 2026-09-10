package com.durantco.dungeon.model.board;

import java.util.Random;

/**
 * Scatters a fixed number of walls at random and leaves everything else walkable.
 *
 * <p>This is the layout the game shipped with. It is kept as a named strategy rather than deleted,
 * both because it is the baseline the room-and-corridor generator is measured against and because a
 * second implementation is what makes the strategy demonstrably swappable.
 *
 * <p>It makes no connectivity guarantee: scattered walls can seal off a corner containing the hero
 * or the exit, which is exactly the weakness the room-and-corridor generator addresses.
 */
public final class RandomScatterGenerator implements LevelGenerator {

  private final int wallCount;

  /**
   * @param wallCount how many walls to scatter
   * @throws IllegalArgumentException if the count is negative
   */
  public RandomScatterGenerator(int wallCount) {
    if (wallCount < 0) {
      throw new IllegalArgumentException("Wall count cannot be negative");
    }
    this.wallCount = wallCount;
  }

  @Override
  public DungeonLayout generate(int width, int height, Random rng) {
    if (wallCount > width * height) {
      throw new IllegalArgumentException(
          "Cannot place " + wallCount + " walls on a " + width + "x" + height + " board");
    }
    boolean[][] walkable = new boolean[height][width];
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        walkable[row][col] = true;
      }
    }
    int placed = 0;
    while (placed < wallCount) {
      int row = rng.nextInt(height);
      int col = rng.nextInt(width);
      if (walkable[row][col]) {
        walkable[row][col] = false;
        placed++;
      }
    }
    return new DungeonLayout(walkable);
  }

  @Override
  public int guaranteedWalkableCells(int width, int height) {
    return Math.max(0, width * height - wallCount);
  }
}
