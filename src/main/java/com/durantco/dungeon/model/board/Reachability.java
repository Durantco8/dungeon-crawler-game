package com.durantco.dungeon.model.board;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Breadth-first flood fill over the four-way grid.
 *
 * <p>Takes a predicate rather than a concrete grid so the same search serves both a bare {@link
 * DungeonLayout}, before any pieces exist, and a populated board where passability depends on what
 * occupies a cell.
 */
public final class Reachability {

  private static final int[][] STEPS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private Reachability() {}

  /**
   * Every cell reachable from a starting cell by repeated single steps.
   *
   * @param start where to search from; included in the result even if the predicate rejects it, since
   *     an occupant standing there has already got there
   * @param passable whether a cell may be entered; must reject cells outside the grid
   * @return the set of reachable cells
   */
  public static Set<Posn> floodFrom(Posn start, Predicate<Posn> passable) {
    Set<Posn> seen = new HashSet<>();
    Deque<Posn> frontier = new ArrayDeque<>();
    seen.add(start);
    frontier.add(start);
    while (!frontier.isEmpty()) {
      Posn at = frontier.removeFirst();
      for (int[] step : STEPS) {
        Posn next = at.offset(step[0], step[1]);
        if (passable.test(next) && seen.add(next)) {
          frontier.add(next);
        }
      }
    }
    return seen;
  }

  /**
   * Whether every walkable cell of a layout can be reached from every other.
   *
   * <p>This is the invariant the partitioning generator is supposed to guarantee by construction, so a
   * false answer means the generator has a bug rather than that this level was unlucky.
   *
   * @param layout the structure to check
   * @return true if the walkable cells form a single connected region, or if there are none
   */
  public static boolean isConnected(DungeonLayout layout) {
    List<Posn> floor = layout.walkableCells();
    if (floor.isEmpty()) {
      return true;
    }
    return floodFrom(floor.get(0), layout::isWalkable).size() == layout.walkableCount();
  }
}
