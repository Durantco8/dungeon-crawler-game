package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReachabilityTest {

  private static DungeonLayout layout(String... rows) {
    boolean[][] walkable = new boolean[rows.length][rows[0].length()];
    for (int row = 0; row < rows.length; row++) {
      for (int col = 0; col < rows[row].length(); col++) {
        walkable[row][col] = rows[row].charAt(col) == '.';
      }
    }
    return new DungeonLayout(walkable);
  }

  @Test
  void reachesEveryCellOfAnOpenGrid() {
    DungeonLayout open = layout("...", "...", "...");
    assertEquals(9, Reachability.floodFrom(new Posn(0, 0), open::isWalkable).size());
  }

  @Test
  void stopsAtWalls() {
    DungeonLayout split = layout("..#..", "..#..", "..#..");
    Set<Posn> reached = Reachability.floodFrom(new Posn(0, 0), split::isWalkable);
    assertEquals(6, reached.size());
    assertTrue(reached.contains(new Posn(2, 1)));
    assertFalse(reached.contains(new Posn(0, 3)));
  }

  @Test
  @DisplayName("movement is four-way, so diagonals are not a shortcut")
  void doesNotMoveDiagonally() {
    DungeonLayout diagonal = layout(".#", "#.");
    assertEquals(1, Reachability.floodFrom(new Posn(0, 0), diagonal::isWalkable).size());
  }

  @Test
  void followsACorridorAroundAWall() {
    DungeonLayout maze = layout(".....", "####.", ".....");
    Set<Posn> reached = Reachability.floodFrom(new Posn(0, 0), maze::isWalkable);
    assertTrue(reached.contains(new Posn(2, 0)), "The far corner is reachable the long way round");
    assertEquals(11, reached.size());
  }

  @Test
  @DisplayName("the starting cell counts even when the predicate would refuse it")
  void includesTheStartRegardless() {
    DungeonLayout solid = layout("##", "##");
    assertEquals(Set.of(new Posn(0, 0)), Reachability.floodFrom(new Posn(0, 0), solid::isWalkable));
  }

  @Test
  void recognisesAConnectedLayout() {
    assertTrue(Reachability.isConnected(layout(".....", "####.", ".....")));
  }

  @Test
  void recognisesASplitLayout() {
    assertFalse(Reachability.isConnected(layout("..#..", "..#..", "..#..")));
  }

  @Test
  void treatsALayoutWithNoFloorAsConnected() {
    assertTrue(Reachability.isConnected(layout("##", "##")));
  }

  @Test
  @DisplayName("an isolated single cell makes a layout disconnected")
  void recognisesAStrandedCell() {
    assertFalse(Reachability.isConnected(layout("...", "###", "..#")));
  }

  @Test
  void measuresDistanceInStepsFromTheStart() {
    DungeonLayout open = layout("...", "...", "...");
    Map<Posn, Integer> distances = Reachability.distancesFrom(new Posn(0, 0), open::isWalkable);

    assertEquals(0, distances.get(new Posn(0, 0)));
    assertEquals(1, distances.get(new Posn(0, 1)));
    assertEquals(1, distances.get(new Posn(1, 0)));
    assertEquals(2, distances.get(new Posn(1, 1)));
    assertEquals(4, distances.get(new Posn(2, 2)));
  }

  @Test
  @DisplayName("distance is how far you must walk, not how far apart two cells look")
  void measuresAroundWallsRatherThanThroughThem() {
    // (0, 0) and (2, 0) are two cells apart in a straight line, but the wall between them means the
    // only route is the long way round. That gap is exactly why enemy spawn margins count steps: two
    // cells either side of a wall are close on the board and far apart in play.
    DungeonLayout blocked = layout("....", "###.", "....");
    Map<Posn, Integer> distances = Reachability.distancesFrom(new Posn(0, 0), blocked::isWalkable);

    // Round the right-hand end of the wall: three steps across the top, two down, three back.
    int straightLine = 2;
    assertEquals(8, distances.get(new Posn(2, 0)));
    assertTrue(distances.get(new Posn(2, 0)) > straightLine);
  }

  @Test
  void leavesOutCellsItCannotReach() {
    DungeonLayout split = layout("..#..", "..#..");
    Map<Posn, Integer> distances = Reachability.distancesFrom(new Posn(0, 0), split::isWalkable);

    assertTrue(distances.containsKey(new Posn(1, 1)));
    assertFalse(distances.containsKey(new Posn(0, 3)), "The far side is unreachable, not distance zero");
    assertEquals(4, distances.size());
  }

  @Test
  @DisplayName("every cell is recorded at its shortest distance, not the first route tried")
  void recordsShortestDistances() {
    DungeonLayout open = layout(".....", ".....", ".....");
    Map<Posn, Integer> distances = Reachability.distancesFrom(new Posn(1, 2), open::isWalkable);

    for (Map.Entry<Posn, Integer> cell : distances.entrySet()) {
      int manhattan =
          Math.abs(cell.getKey().row() - 1) + Math.abs(cell.getKey().col() - 2);
      assertEquals(manhattan, cell.getValue().intValue(), "Wrong distance to " + cell.getKey());
    }
  }

  @Test
  void reachesTheSameCellsAsAPlainFloodFill() {
    DungeonLayout maze = layout(".....", "####.", ".....", ".####", ".....");
    assertEquals(
        Reachability.floodFrom(new Posn(0, 0), maze::isWalkable),
        Reachability.distancesFrom(new Posn(0, 0), maze::isWalkable).keySet());
  }
}
