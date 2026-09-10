package com.durantco.dungeon.model.board;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * Whether one cell can see another, along a Bresenham line.
 *
 * <p>Bresenham walks the straight line between two cells using only integer addition, choosing at each
 * step whichever cell the true line passes closest to. That gives one unambiguous set of cells to test
 * for obstruction, which matters more here than geometric perfection: the answer has to be the same
 * every time for a recorded game to replay.
 */
public final class LineOfSight {

  private LineOfSight() {}

  /**
   * The cells a straight line from one cell to another passes through, including both ends.
   *
   * @param from one end of the line
   * @param to the other end
   * @return the cells along the line, starting at {@code from} and ending at {@code to}
   */
  public static List<Posn> between(Posn from, Posn to) {
    List<Posn> line = new ArrayList<>();
    int row = from.row();
    int col = from.col();
    int rowSpan = Math.abs(to.row() - row);
    int colSpan = Math.abs(to.col() - col);

    int rowStep = -1;
    if (row < to.row()) {
      rowStep = 1;
    }
    int colStep = -1;
    if (col < to.col()) {
      colStep = 1;
    }

    int error = colSpan - rowSpan;
    while (true) {
      line.add(new Posn(row, col));
      if (row == to.row() && col == to.col()) {
        return List.copyOf(line);
      }
      int doubled = 2 * error;
      if (doubled > -rowSpan) {
        error -= rowSpan;
        col += colStep;
      }
      if (doubled < colSpan) {
        error += colSpan;
        row += rowStep;
      }
    }
  }

  /**
   * Whether the view between two cells is unobstructed.
   *
   * <p>The two ends are not themselves tested. A viewer is never blocked by the cell it occupies, and a
   * target standing in a doorway is visible rather than hidden by itself.
   *
   * <p>The answer is symmetric: if one cell can see another, the reverse holds too.
   *
   * @param from where the viewer is
   * @param to what it is looking at
   * @param transparent whether a cell can be seen through
   * @return true if nothing between the two blocks the view
   */
  public static boolean isClear(Posn from, Posn to, Predicate<Posn> transparent) {
    // Bresenham is not symmetric: where the true line passes exactly between two cells, which one it
    // picks depends on which end it started from, so A could see B while B could not see A. Ordering
    // the ends first means the same pair of cells is always walked the same way, which makes sight a
    // symmetric relation for one comparison rather than a second line walk.
    Posn start = from;
    Posn end = to;
    if (comesAfter(from, to)) {
      start = to;
      end = from;
    }

    List<Posn> line = between(start, end);
    for (int i = 1; i < line.size() - 1; i++) {
      if (!transparent.test(line.get(i))) {
        return false;
      }
    }
    return true;
  }

  private static boolean comesAfter(Posn first, Posn second) {
    if (first.row() != second.row()) {
      return first.row() > second.row();
    }
    return first.col() > second.col();
  }

  /**
   * How far apart two cells are for sight purposes, counting a diagonal as one step so that vision
   * reaches roughly as far in every direction.
   *
   * @param from one cell
   * @param to the other
   * @return the number of steps between them
   */
  public static int sightDistance(Posn from, Posn to) {
    return Math.max(Math.abs(from.row() - to.row()), Math.abs(from.col() - to.col()));
  }
}
