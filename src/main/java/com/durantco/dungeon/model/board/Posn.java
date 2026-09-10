package com.durantco.dungeon.model.board;

/**
 * An immutable (row, column) coordinate on the board.
 *
 * <p>Value equality matters here: positions are used as keys when reasoning about the grid, so two
 * separately constructed coordinates naming the same cell must be equal and hash alike.
 *
 * @param row the zero-based row index, increasing downward
 * @param col the zero-based column index, increasing rightward
 */
public record Posn(int row, int col) {

  /**
   * Returns the position reached by offsetting this one.
   *
   * @param drow rows to move, positive is downward
   * @param dcol columns to move, positive is rightward
   * @return the offset position
   */
  public Posn offset(int drow, int dcol) {
    return new Posn(row + drow, col + dcol);
  }

  @Override
  public String toString() {
    return row + "," + col;
  }
}
