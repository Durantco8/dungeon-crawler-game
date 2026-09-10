package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LineOfSightTest {

  private static Predicate<Posn> transparency(String... rows) {
    boolean[][] clear = new boolean[rows.length][rows[0].length()];
    for (int row = 0; row < rows.length; row++) {
      for (int col = 0; col < rows[row].length(); col++) {
        clear[row][col] = rows[row].charAt(col) == '.';
      }
    }
    DungeonLayout layout = new DungeonLayout(clear);
    return layout::isWalkable;
  }

  @Test
  void aLineToItselfIsJustThatCell() {
    assertEquals(List.of(new Posn(2, 2)), LineOfSight.between(new Posn(2, 2), new Posn(2, 2)));
  }

  @Test
  void walksAHorizontalLine() {
    assertEquals(
        List.of(new Posn(0, 0), new Posn(0, 1), new Posn(0, 2)),
        LineOfSight.between(new Posn(0, 0), new Posn(0, 2)));
  }

  @Test
  void walksAVerticalLine() {
    assertEquals(
        List.of(new Posn(0, 0), new Posn(1, 0), new Posn(2, 0)),
        LineOfSight.between(new Posn(0, 0), new Posn(2, 0)));
  }

  @Test
  void walksAPerfectDiagonal() {
    assertEquals(
        List.of(new Posn(0, 0), new Posn(1, 1), new Posn(2, 2)),
        LineOfSight.between(new Posn(0, 0), new Posn(2, 2)));
  }

  @Test
  void walksBackwardsAsWellAsForwards() {
    assertEquals(
        List.of(new Posn(2, 2), new Posn(1, 1), new Posn(0, 0)),
        LineOfSight.between(new Posn(2, 2), new Posn(0, 0)));
  }

  @Test
  @DisplayName("a line ends on its target and starts on its origin")
  void spansExactlyTheTwoEnds() {
    List<Posn> line = LineOfSight.between(new Posn(1, 0), new Posn(4, 7));
    assertEquals(new Posn(1, 0), line.get(0));
    assertEquals(new Posn(4, 7), line.get(line.size() - 1));
  }

  @Test
  @DisplayName("consecutive cells on a line always touch")
  void neverSkipsACell() {
    List<Posn> line = LineOfSight.between(new Posn(0, 0), new Posn(5, 11));
    for (int i = 1; i < line.size(); i++) {
      int rowStep = Math.abs(line.get(i).row() - line.get(i - 1).row());
      int colStep = Math.abs(line.get(i).col() - line.get(i - 1).col());
      assertTrue(rowStep <= 1 && colStep <= 1, "Line jumped between " + line.get(i - 1) + " and " + line.get(i));
    }
  }

  @Test
  @DisplayName("the same line is produced every time, which replay depends on")
  void isDeterministic() {
    List<Posn> first = LineOfSight.between(new Posn(0, 0), new Posn(7, 3));
    for (int repeat = 0; repeat < 20; repeat++) {
      assertEquals(first, LineOfSight.between(new Posn(0, 0), new Posn(7, 3)));
    }
  }

  @Test
  void seesAcrossAnEmptyRoom() {
    assertTrue(
        LineOfSight.isClear(new Posn(0, 0), new Posn(0, 4), transparency(".....", ".....")));
  }

  @Test
  void cannotSeeThroughAWall() {
    assertFalse(
        LineOfSight.isClear(new Posn(0, 0), new Posn(0, 4), transparency("..#..", ".....")));
  }

  @Test
  @DisplayName("neither end blocks the view, so standing in a doorway does not hide you")
  void ignoresObstructionAtEitherEnd() {
    assertTrue(LineOfSight.isClear(new Posn(0, 0), new Posn(0, 2), transparency("#.#")));
  }

  @Test
  void cannotSeeAroundACorner() {
    assertFalse(
        LineOfSight.isClear(new Posn(0, 0), new Posn(2, 2), transparency("...", ".#.", "...")));
  }

  @Test
  @DisplayName("sight is symmetric for every pair of cells, which a raw Bresenham walk is not")
  void sightIsSymmetric() {
    Predicate<Posn> room = transparency(".....", "..#..", ".....", ".#.#.");
    for (int fromRow = 0; fromRow < 4; fromRow++) {
      for (int fromCol = 0; fromCol < 5; fromCol++) {
        for (int toRow = 0; toRow < 4; toRow++) {
          for (int toCol = 0; toCol < 5; toCol++) {
            Posn from = new Posn(fromRow, fromCol);
            Posn to = new Posn(toRow, toCol);
            assertEquals(
                LineOfSight.isClear(from, to, room),
                LineOfSight.isClear(to, from, room),
                "Sight between " + from + " and " + to + " is one-way");
          }
        }
      }
    }
  }

  @Test
  @DisplayName("distance counts a diagonal as one step, so vision reaches evenly")
  void measuresSightDistanceDiagonally() {
    assertEquals(0, LineOfSight.sightDistance(new Posn(2, 2), new Posn(2, 2)));
    assertEquals(3, LineOfSight.sightDistance(new Posn(0, 0), new Posn(0, 3)));
    assertEquals(3, LineOfSight.sightDistance(new Posn(0, 0), new Posn(3, 3)));
    assertEquals(4, LineOfSight.sightDistance(new Posn(0, 0), new Posn(4, 2)));
  }

  @Test
  void wallsBlockSightAndOtherPiecesDoNot() {
    assertTrue(new com.durantco.dungeon.model.pieces.Wall().blocksSight());
    assertFalse(new com.durantco.dungeon.model.pieces.Enemy().blocksSight());
    assertFalse(new com.durantco.dungeon.model.pieces.Treasure().blocksSight());
    assertFalse(new com.durantco.dungeon.model.pieces.Thief().blocksSight());
    assertFalse(new com.durantco.dungeon.model.pieces.Exit().blocksSight());
    assertFalse(new com.durantco.dungeon.model.pieces.Hero().blocksSight());
  }
}
