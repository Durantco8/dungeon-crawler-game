package com.durantco.dungeon.model.board;

import java.util.ArrayList;
import java.util.List;

/**
 * The structure of a level: which cells can be walked on and which are solid.
 *
 * <p>Deliberately free of {@link com.durantco.dungeon.model.pieces.Piece}. Generation decides shape,
 * and populating that shape with pieces is a separate step, so the shape can be generated and
 * verified without constructing a single game object.
 */
public final class DungeonLayout {

  private final boolean[][] walkable;
  private final int width;
  private final int height;
  private final int walkableCount;
  private final List<Room> rooms;

  /**
   * A layout with no room structure, for generators that do not produce rooms.
   *
   * @param walkable row-major grid, true where a piece may stand
   */
  public DungeonLayout(boolean[][] walkable) {
    this(walkable, List.of());
  }

  /**
   * @param walkable row-major grid, true where a piece may stand; copied, so later changes to the
   *     caller's array cannot alter this layout
   * @param rooms the rooms this layout was built from, empty if the generator has no notion of rooms
   * @throws IllegalArgumentException if the grid is empty or ragged
   */
  public DungeonLayout(boolean[][] walkable, List<Room> rooms) {
    if (walkable.length == 0 || walkable[0].length == 0) {
      throw new IllegalArgumentException("A layout needs at least one cell");
    }
    this.height = walkable.length;
    this.width = walkable[0].length;
    this.walkable = new boolean[height][width];
    int open = 0;
    for (int row = 0; row < height; row++) {
      if (walkable[row].length != width) {
        throw new IllegalArgumentException("Layout row " + row + " has an inconsistent width");
      }
      for (int col = 0; col < width; col++) {
        this.walkable[row][col] = walkable[row][col];
        if (walkable[row][col]) {
          open++;
        }
      }
    }
    this.walkableCount = open;
    this.rooms = List.copyOf(rooms);
  }

  /**
   * @return the rooms this layout was built from, in the order the generator produced them; empty for
   *     generators with no notion of rooms
   */
  public List<Room> rooms() {
    return rooms;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  /**
   * @return how many cells can be walked on
   */
  public int walkableCount() {
    return walkableCount;
  }

  /**
   * @param p the cell to test
   * @return true if the cell is inside the layout and walkable
   */
  public boolean isWalkable(Posn p) {
    if (p.row() < 0 || p.row() >= height || p.col() < 0 || p.col() >= width) {
      return false;
    }
    return walkable[p.row()][p.col()];
  }

  /**
   * @return every walkable cell, in row-major order
   */
  public List<Posn> walkableCells() {
    List<Posn> cells = new ArrayList<>(walkableCount);
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        if (walkable[row][col]) {
          cells.add(new Posn(row, col));
        }
      }
    }
    return cells;
  }
}
