package com.durantco.dungeon.model.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Shortest four-way path between two cells, by A* with a Manhattan heuristic.
 *
 * <p>Manhattan distance is exact for four-way movement with uniform cost, so it never overestimates
 * and the first path found is optimal.
 *
 * <p>Ties are broken on a total order: cost, then remaining distance, then row, then column. Ordering
 * only on cost would leave the choice among equal-cost nodes to the queue's internal layout, so two
 * runs of the same seed could return different paths of the same length. Recorded games have to
 * replay exactly, which makes that unacceptable rather than merely untidy.
 */
public final class AStarPathfinder {

  private static final int[][] STEPS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  private static final Comparator<Node> CHEAPEST_THEN_STABLE =
      Comparator.comparingInt(Node::f)
          .thenComparingInt(Node::h)
          .thenComparingInt(node -> node.at().row())
          .thenComparingInt(node -> node.at().col());

  /**
   * Finds a shortest path between two cells.
   *
   * @param start where the path begins; not included in the result
   * @param goal where the path ends; must satisfy the predicate to be reachable
   * @param passable whether a cell may be entered; must reject cells outside the grid
   * @return the cells to walk through in order, ending at the goal, or empty if the goal cannot be
   *     reached or is already occupied by the searcher
   */
  public List<Posn> findPath(Posn start, Posn goal, Predicate<Posn> passable) {
    if (start.equals(goal)) {
      return List.of();
    }

    PriorityQueue<Node> frontier = new PriorityQueue<>(CHEAPEST_THEN_STABLE);
    Map<Posn, Integer> cheapestKnown = new HashMap<>();
    Map<Posn, Posn> arrivedFrom = new HashMap<>();
    Set<Posn> settled = new HashSet<>();

    cheapestKnown.put(start, 0);
    frontier.add(new Node(start, 0, manhattan(start, goal)));

    while (!frontier.isEmpty()) {
      Node current = frontier.poll();
      if (!settled.add(current.at())) {
        continue; // already expanded by a cheaper route
      }
      if (current.at().equals(goal)) {
        return retrace(arrivedFrom, start, goal);
      }
      for (int[] step : STEPS) {
        Posn next = current.at().offset(step[0], step[1]);
        if (settled.contains(next) || !passable.test(next)) {
          continue;
        }
        int costToNext = current.g() + 1;
        Integer known = cheapestKnown.get(next);
        if (known == null || costToNext < known) {
          cheapestKnown.put(next, costToNext);
          arrivedFrom.put(next, current.at());
          frontier.add(new Node(next, costToNext, manhattan(next, goal)));
        }
      }
    }
    return List.of();
  }

  /**
   * The single step to take now in order to follow a shortest path.
   *
   * @param start where the mover currently stands
   * @param goal where it is heading
   * @param passable whether a cell may be entered
   * @return the next cell to move into, or empty if the goal is unreachable
   */
  public Optional<Posn> nextStep(Posn start, Posn goal, Predicate<Posn> passable) {
    List<Posn> path = findPath(start, goal, passable);
    if (path.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(path.get(0));
  }

  private static int manhattan(Posn from, Posn to) {
    return Math.abs(from.row() - to.row()) + Math.abs(from.col() - to.col());
  }

  private static List<Posn> retrace(Map<Posn, Posn> arrivedFrom, Posn start, Posn goal) {
    List<Posn> path = new ArrayList<>();
    Posn at = goal;
    while (!at.equals(start)) {
      path.add(at);
      at = arrivedFrom.get(at);
    }
    Collections.reverse(path);
    return List.copyOf(path);
  }

  /**
   * A cell on the frontier.
   *
   * @param at the cell
   * @param g steps taken to get here
   * @param h optimistic steps still to go
   */
  private record Node(Posn at, int g, int h) {
    int f() {
      return g + h;
    }
  }
}
