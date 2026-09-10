package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BoardInitTest {

  private static final long SEED = 20260910L;

  /** A board whose generator scatters an exact, known number of walls. */
  private static BoardImpl boardWith(int width, int height, int walls) {
    return new BoardImpl(width, height, new Random(SEED), new RandomScatterGenerator(walls));
  }

  private static int occupiedCells(Board board) {
    int occupied = 0;
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        if (board.get(new Posn(row, col)) != null) {
          occupied++;
        }
      }
    }
    return occupied;
  }

  private static Map<PieceType, Integer> census(Board board) {
    Map<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
    for (PieceType type : PieceType.values()) {
      counts.put(type, 0);
    }
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null) {
          counts.merge(piece.getType(), 1, Integer::sum);
        }
      }
    }
    return counts;
  }

  private static String render(Board board) {
    StringBuilder out = new StringBuilder();
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece == null) {
          out.append('.');
        } else {
          out.append(piece.getType().name().charAt(0));
        }
      }
      out.append('\n');
    }
    return out.toString();
  }

  @Test
  void placesExactlyTheSpecifiedPieceCounts() {
    BoardImpl board = boardWith(8, 8, 4);
    board.init(new LevelSpec(3, 2, 1));

    Map<PieceType, Integer> counts = census(board);
    assertEquals(3, counts.get(PieceType.ENEMY));
    assertEquals(2, counts.get(PieceType.TREASURE));
    assertEquals(1, counts.get(PieceType.THIEF));
  }

  @Test
  @DisplayName("the generator decides the walls, not the level spec")
  void placesTheWallsTheGeneratorAsksFor() {
    BoardImpl board = boardWith(8, 8, 6);
    board.init(new LevelSpec(3, 2, 1));

    assertEquals(6, census(board).get(PieceType.WALL));
  }

  @Test
  @DisplayName("every level has exactly one hero and one exit")
  void placesOneHeroAndOneExit() {
    BoardImpl board = boardWith(8, 8, 4);
    board.init(LevelSpec.forLevel(1));

    Map<PieceType, Integer> counts = census(board);
    assertEquals(1, counts.get(PieceType.HERO));
    assertEquals(1, counts.get(PieceType.EXIT));
  }

  @Test
  void occupiesExactlyAsManyCellsAsTheSpecAndTheWallsRequire() {
    LevelSpec spec = new LevelSpec(3, 2, 1);
    BoardImpl board = boardWith(8, 8, 4);
    board.init(spec);

    assertEquals(spec.cellsRequired() + 4, occupiedCells(board));
  }

  @Test
  @DisplayName("no two pieces share a cell")
  void givesEveryPieceItsOwnCell() {
    BoardImpl board = boardWith(8, 8, 4);
    board.init(LevelSpec.forLevel(3));

    Set<Posn> occupied = new HashSet<>();
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Posn at = new Posn(row, col);
        if (board.get(at) != null) {
          assertTrue(occupied.add(at), "Two pieces occupy " + at);
        }
      }
    }
  }

  @Test
  void everyPieceKnowsTheCellItOccupies() {
    BoardImpl board = boardWith(8, 8, 4);
    board.init(LevelSpec.forLevel(2));

    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Posn at = new Posn(row, col);
        Piece piece = board.get(at);
        if (piece != null) {
          assertEquals(at, piece.getPosn(), "Piece " + piece.getName() + " has a stale position");
        }
      }
    }
  }

  @Test
  @DisplayName("the same seed produces the same dungeon")
  void isReproducibleForAGivenSeed() {
    BoardImpl first = boardWith(8, 8, 4);
    first.init(LevelSpec.forLevel(4));
    BoardImpl second = boardWith(8, 8, 4);
    second.init(LevelSpec.forLevel(4));

    assertEquals(render(first), render(second));
  }

  @Test
  void differentSeedsProduceDifferentDungeons() {
    BoardImpl first =
        new BoardImpl(8, 8, new Random(1L), new RandomScatterGenerator(4));
    first.init(LevelSpec.forLevel(4));
    BoardImpl second =
        new BoardImpl(8, 8, new Random(2L), new RandomScatterGenerator(4));
    second.init(LevelSpec.forLevel(4));

    assertNotEquals(render(first), render(second));
  }

  @Test
  @DisplayName("re-initialising clears the previous level rather than adding to it")
  void clearsTheBoardBetweenLevels() {
    LevelSpec spec = LevelSpec.forLevel(1);
    BoardImpl board = boardWith(8, 8, 3);
    board.init(spec);
    board.init(spec);

    assertEquals(spec.cellsRequired() + 3, occupiedCells(board));
  }

  @Test
  void reportsDimensions() {
    BoardImpl board = boardWith(5, 9, 2);
    assertEquals(5, board.getWidth());
    assertEquals(9, board.getHeight());
  }

  @Test
  void canFitAcceptsALevelThatExactlyFillsTheBoard() {
    BoardImpl board = boardWith(3, 3, 0);
    LevelSpec exact = new LevelSpec(3, 2, 2); // 7 pieces plus hero and exit is 9
    assertEquals(9, exact.cellsRequired());
    assertTrue(board.canFit(exact));
  }

  @Test
  @DisplayName("capacity is the walkable space, so walls reduce what fits")
  void canFitAccountsForTheGeneratorsWalls() {
    LevelSpec spec = new LevelSpec(3, 2, 2); // needs 9 walkable cells
    assertTrue(boardWith(3, 3, 0).canFit(spec));
    assertFalse(boardWith(3, 3, 1).canFit(spec), "One wall leaves only 8 walkable cells");
  }

  @Test
  void canFitRejectsALevelOneCellTooBig() {
    BoardImpl board = boardWith(3, 3, 0);
    LevelSpec tooBig = new LevelSpec(4, 2, 2); // 10 cells needed, 9 available
    assertEquals(10, tooBig.cellsRequired());
    assertFalse(board.canFit(tooBig));
  }

  @Test
  @DisplayName("init guards its precondition, which callers avoid by asking canFit first")
  void initRejectsALevelThatDoesNotFit() {
    BoardImpl board = boardWith(3, 3, 0);
    assertThrows(IllegalArgumentException.class, () -> board.init(new LevelSpec(4, 2, 2)));
  }

  @Test
  void fillsABoardToCapacityWithoutHanging() {
    BoardImpl board = boardWith(3, 3, 0);
    board.init(new LevelSpec(3, 2, 2));
    assertEquals(9, census(board).values().stream().mapToInt(Integer::intValue).sum());
  }
}
