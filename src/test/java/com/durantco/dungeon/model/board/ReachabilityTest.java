package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
}
