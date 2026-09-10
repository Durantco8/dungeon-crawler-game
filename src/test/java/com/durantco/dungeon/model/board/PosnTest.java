package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Posn's value semantics are a prerequisite for the graph search work, which uses positions as set
 * and map keys.
 */
class PosnTest {

  @Test
  void exposesRowAndColumn() {
    Posn p = new Posn(3, 7);
    assertEquals(3, p.row());
    assertEquals(7, p.col());
  }

  @Test
  void samePositionsAreEqual() {
    assertEquals(new Posn(2, 5), new Posn(2, 5));
  }

  @Test
  void rowAndColumnAreNotInterchangeable() {
    assertNotEquals(new Posn(2, 5), new Posn(5, 2));
  }

  @Test
  void differentPositionsAreNotEqual() {
    assertNotEquals(new Posn(2, 5), new Posn(2, 6));
  }

  @Test
  void equalPositionsShareAHashCode() {
    assertEquals(new Posn(4, 4).hashCode(), new Posn(4, 4).hashCode());
  }

  @Test
  @DisplayName("a set treats two coordinates for the same cell as one element")
  void deduplicatesInASet() {
    Set<Posn> visited = new HashSet<>();
    visited.add(new Posn(1, 1));
    visited.add(new Posn(1, 1));
    assertEquals(1, visited.size());
    assertTrue(visited.contains(new Posn(1, 1)));
  }

  @Test
  @DisplayName("a map can be keyed by position, as pathfinding needs")
  void worksAsAMapKey() {
    Map<Posn, Integer> distances = new HashMap<>();
    distances.put(new Posn(0, 0), 0);
    distances.put(new Posn(0, 0), 5);
    assertEquals(1, distances.size());
    assertEquals(5, distances.get(new Posn(0, 0)));
  }

  @Test
  void offsetProducesTheNeighbouringPosition() {
    assertEquals(new Posn(1, 2), new Posn(2, 2).offset(-1, 0));
    assertEquals(new Posn(3, 2), new Posn(2, 2).offset(1, 0));
    assertEquals(new Posn(2, 1), new Posn(2, 2).offset(0, -1));
    assertEquals(new Posn(2, 3), new Posn(2, 2).offset(0, 1));
  }

  @Test
  void offsetLeavesTheOriginalUnchanged() {
    Posn original = new Posn(2, 2);
    original.offset(1, 1);
    assertEquals(new Posn(2, 2), original);
  }

  @Test
  void offsetCanLeaveTheGridSoCallersMustBoundsCheck() {
    assertEquals(new Posn(-1, 0), new Posn(0, 0).offset(-1, 0));
  }
}
