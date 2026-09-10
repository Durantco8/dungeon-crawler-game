package com.durantco.dungeon.model.board;

/**
 * How many of each piece a level should contain.
 *
 * <p>Walls are deliberately absent. Wall layout is structure, decided by the {@link LevelGenerator},
 * not a count the level asks for; a room-and-corridor dungeon has no meaningful "number of walls".
 *
 * <p>Every level also contains exactly one hero and one exit, counted by {@link #cellsRequired()} but
 * not configurable.
 *
 * @param enemies number of enemies to place
 * @param treasures number of treasures to place
 * @param thieves number of thieves to place
 */
public record LevelSpec(int enemies, int treasures, int thieves) {

  private static final int FIXED_PIECES = 2; // the hero and the exit

  /** Treasures and thieves are constant; difficulty scales with enemy count. */
  private static final int TREASURES_PER_LEVEL = 2;

  private static final int THIEVES_PER_LEVEL = 2;

  public LevelSpec {
    if (enemies < 0 || treasures < 0 || thieves < 0) {
      throw new IllegalArgumentException("Piece counts cannot be negative");
    }
  }

  /**
   * The spec for a given level number. Enemy count grows with the level; everything else is fixed.
   *
   * @param level the one-based level number
   * @return the spec describing that level's contents
   */
  public static LevelSpec forLevel(int level) {
    return new LevelSpec(level + 1, TREASURES_PER_LEVEL, THIEVES_PER_LEVEL);
  }

  /**
   * @return the number of walkable cells this level needs, including the hero and the exit
   */
  public int cellsRequired() {
    return enemies + treasures + thieves + FIXED_PIECES;
  }
}
