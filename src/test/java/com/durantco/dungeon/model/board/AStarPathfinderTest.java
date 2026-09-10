package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AStarPathfinderTest {

  private final AStarPathfinder pathfinder = new AStarPathfinder();

  private static Predicate<Posn> grid(String... rows) {
    DungeonLayout layout = layoutOf(rows);
    return layout::isWalkable;
  }

  private static DungeonLayout layoutOf(String... rows) {
    boolean[][] walkable = new boolean[rows.length][rows[0].length()];
    for (int row = 0; row < rows.length; row++) {
      for (int col = 0; col < rows[row].length(); col++) {
        walkable[row][col] = rows[row].charAt(col) == '.';
      }
    }
    return new DungeonLayout(walkable);
  }

  @Test
  void walksAStraightCorridor() {
    List<Posn> path = pathfinder.findPath(new Posn(0, 0), new Posn(0, 3), grid("...."));
    assertEquals(List.of(new Posn(0, 1), new Posn(0, 2), new Posn(0, 3)), path);
  }

  @Test
  @DisplayName("the path excludes where the mover already is and ends on the goal")
  void excludesTheStartAndIncludesTheGoal() {
    List<Posn> path = pathfinder.findPath(new Posn(0, 0), new Posn(2, 2), grid("...", "...", "..."));
    assertTrue(!path.contains(new Posn(0, 0)));
    assertEquals(new Posn(2, 2), path.get(path.size() - 1));
  }

  @Test
  void takesTheShortestRouteAcrossOpenGround() {
    List<Posn> path = pathfinder.findPath(new Posn(0, 0), new Posn(2, 3), grid("....", "....", "...."));
    assertEquals(5, path.size(), "Manhattan distance is exact on open four-way ground");
  }

  @Test
  void everyStepIsASingleMove() {
    List<Posn> path = pathfinder.findPath(new Posn(0, 0), new Posn(3, 3), grid("....", "....", "....", "...."));
    Posn previous = new Posn(0, 0);
    for (Posn step : path) {
      int distance = Math.abs(step.row() - previous.row()) + Math.abs(step.col() - previous.col());
      assertEquals(1, distance, "Path jumped from " + previous + " to " + step);
      previous = step;
    }
  }

  @Test
  void everyStepIsPassable() {
    Predicate<Posn> passable = grid(".....", ".###.", ".....");
    List<Posn> path = pathfinder.findPath(new Posn(0, 0), new Posn(2, 4), passable);
    for (Posn step : path) {
      assertTrue(passable.test(step), step + " is not passable");
    }
  }

  @Test
  @DisplayName("routes around a wall rather than pressing against it")
  void goesAroundAnObstacle() {
    // The direct line is blocked, so the route must detour through the one opening on the left.
    List<Posn> path = pathfinder.findPath(new Posn(2, 0), new Posn(0, 3), grid("....", ".###", "...."));

    assertEquals(new Posn(1, 0), path.get(0), "The only way through is upward on the left");
    assertEquals(5, path.size());
  }

  @Test
  @DisplayName("a detour is longer than the straight-line distance, as it must be")
  void aDetourCostsMoreThanTheHeuristic() {
    List<Posn> path = pathfinder.findPath(new Posn(2, 0), new Posn(0, 0), grid("...", "##.", "..."));
    assertTrue(path.size() > 2, "Straight up is walled off, so the route must be longer than two steps");
    assertEquals(6, path.size());
  }

  @Test
  void findsNothingWhenTheGoalIsSealedOff() {
    assertEquals(
        List.of(), pathfinder.findPath(new Posn(0, 0), new Posn(2, 2), grid("..#", "..#", "##.")));
  }

  @Test
  void findsNothingWhenTheGoalIsItselfSolid() {
    assertEquals(List.of(), pathfinder.findPath(new Posn(0, 0), new Posn(0, 2), grid("..#")));
  }

  @Test
  void haveNoPathToWalkWhenAlreadyThere() {
    assertEquals(List.of(), pathfinder.findPath(new Posn(1, 1), new Posn(1, 1), grid("...", "...", "...")));
  }

  @Test
  @DisplayName("the same search repeated returns exactly the same path, which replay depends on")
  void isDeterministicAcrossRepeatedSearches() {
    Predicate<Posn> passable = grid("......", "......", "......", "......");
    List<Posn> first = pathfinder.findPath(new Posn(0, 0), new Posn(3, 5), passable);
    for (int repeat = 0; repeat < 50; repeat++) {
      assertEquals(first, pathfinder.findPath(new Posn(0, 0), new Posn(3, 5), passable));
    }
  }

  @Test
  @DisplayName("two separately built pathfinders agree, so nothing carries over between searches")
  void isDeterministicAcrossInstances() {
    Predicate<Posn> passable = grid(".......", ".#####.", ".......");
    List<Posn> first = new AStarPathfinder().findPath(new Posn(0, 0), new Posn(2, 6), passable);
    List<Posn> second = new AStarPathfinder().findPath(new Posn(0, 0), new Posn(2, 6), passable);
    assertEquals(first, second);
  }

  @Test
  void nextStepIsTheFirstCellOfThePath() {
    Predicate<Posn> passable = grid("....", "....", "....");
    Optional<Posn> step = pathfinder.nextStep(new Posn(0, 0), new Posn(2, 3), passable);
    assertEquals(
        Optional.of(pathfinder.findPath(new Posn(0, 0), new Posn(2, 3), passable).get(0)), step);
  }

  @Test
  void nextStepIsEmptyWhenTheGoalIsUnreachable() {
    assertEquals(
        Optional.empty(),
        pathfinder.nextStep(new Posn(0, 0), new Posn(2, 2), grid("..#", "..#", "##.")));
  }

  @Test
  @DisplayName("a long corridor still finds the one route through")
  void solvesASpiralCorridor() {
    List<Posn> path =
        pathfinder.findPath(
            new Posn(0, 0),
            new Posn(4, 0),
            grid("......", "#####.", "......", ".#####", "......"));
    assertTrue(path.size() >= 14, "The only route snakes across the board three times");
    assertEquals(new Posn(4, 0), path.get(path.size() - 1));
  }
}
