package com.durantco.dungeon.model.board;

/**
 * A rectangular room, addressed by its top-left cell.
 *
 * <p>Rooms are part of a level's structure rather than decoration: enemy patrol routes and ambush
 * points are derived from them, so the generator hands them to the layout instead of discarding them
 * once the floor is carved.
 *
 * @param row top-most row of the room
 * @param col left-most column of the room
 * @param width room width in cells
 * @param height room height in cells
 */
public record Room(int row, int col, int width, int height) {

  public Room {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("A room must have a positive width and height");
    }
    if (row < 0 || col < 0) {
      throw new IllegalArgumentException("A room cannot start outside the board");
    }
  }

  /**
   * @return the room's middle cell, biased down and right for even dimensions
   */
  public Posn center() {
    return new Posn(row + height / 2, col + width / 2);
  }

  /**
   * @param p the cell to test
   * @return true if the cell lies inside this room
   */
  public boolean contains(Posn p) {
    return p.row() >= row && p.row() < row + height && p.col() >= col && p.col() < col + width;
  }

  /**
   * @return how many cells the room covers
   */
  public int area() {
    return width * height;
  }
}
