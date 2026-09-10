package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DungeonLayoutTest {

  private static boolean[][] grid(String... rows) {
    boolean[][] walkable = new boolean[rows.length][rows[0].length()];
    for (int row = 0; row < rows.length; row++) {
      for (int col = 0; col < rows[row].length(); col++) {
        walkable[row][col] = rows[row].charAt(col) == '.';
      }
    }
    return walkable;
  }

  @Test
  void reportsItsDimensions() {
    DungeonLayout layout = new DungeonLayout(grid("...", "..."));
    assertEquals(3, layout.width());
    assertEquals(2, layout.height());
  }

  @Test
  void countsWalkableCells() {
    assertEquals(4, new DungeonLayout(grid("..#", ".#.")).walkableCount());
  }

  @Test
  void reportsWhichCellsAreWalkable() {
    DungeonLayout layout = new DungeonLayout(grid(".#", "#."));
    assertTrue(layout.isWalkable(new Posn(0, 0)));
    assertFalse(layout.isWalkable(new Posn(0, 1)));
    assertFalse(layout.isWalkable(new Posn(1, 0)));
    assertTrue(layout.isWalkable(new Posn(1, 1)));
  }

  @Test
  @DisplayName("cells outside the layout are not walkable, so callers need no bounds check")
  void treatsOutOfBoundsAsSolid() {
    DungeonLayout layout = new DungeonLayout(grid("..", ".."));
    assertFalse(layout.isWalkable(new Posn(-1, 0)));
    assertFalse(layout.isWalkable(new Posn(0, -1)));
    assertFalse(layout.isWalkable(new Posn(2, 0)));
    assertFalse(layout.isWalkable(new Posn(0, 2)));
  }

  @Test
  void listsWalkableCellsInRowMajorOrder() {
    DungeonLayout layout = new DungeonLayout(grid(".#", ".."));
    assertEquals(
        List.of(new Posn(0, 0), new Posn(1, 0), new Posn(1, 1)), layout.walkableCells());
  }

  @Test
  @DisplayName("the layout copies the grid, so a later change to the caller's array cannot alter it")
  void isImmutableAgainstTheSourceArray() {
    boolean[][] source = grid("..", "..");
    DungeonLayout layout = new DungeonLayout(source);
    source[0][0] = false;

    assertTrue(layout.isWalkable(new Posn(0, 0)));
    assertEquals(4, layout.walkableCount());
  }

  @Test
  void rejectsAnEmptyGrid() {
    assertThrows(IllegalArgumentException.class, () -> new DungeonLayout(new boolean[0][0]));
    assertThrows(IllegalArgumentException.class, () -> new DungeonLayout(new boolean[1][0]));
  }

  @Test
  void rejectsARaggedGrid() {
    boolean[][] ragged = {new boolean[3], new boolean[2]};
    assertThrows(IllegalArgumentException.class, () -> new DungeonLayout(ragged));
  }
}
