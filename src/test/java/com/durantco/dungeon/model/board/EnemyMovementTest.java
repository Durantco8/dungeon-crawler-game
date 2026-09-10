package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Enemy movement in both difficulty modes.
 *
 * <p>Hard mode is a greedy chase along whichever axis is further from the hero. Easy mode shuffles
 * the four directions, so it is only assertable because the randomness is now injected.
 */
class EnemyMovementTest {

  private static BoardImpl boardOf(long seed, String... rows) {
    return new BoardImpl(Boards.parse(rows), new Random(seed));
  }

  private static Posn firstEnemy(Board board) {
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.ENEMY) {
          return new Posn(row, col);
        }
      }
    }
    return null;
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
    }
    return out.toString();
  }

  @Nested
  class HardMode {

    @Test
    @DisplayName("closes the row gap when the hero is further away vertically")
    void chasesAlongRowsWhenRowsDominate() {
      BoardImpl board = boardOf(1L, ".H.", "...", ".E.");
      board.setHardMode(true);
      board.moveHero(0, 1);

      assertEquals(new Posn(1, 1), firstEnemy(board));
    }

    @Test
    @DisplayName("closes the column gap when the hero is further away horizontally")
    void chasesAlongColumnsWhenColumnsDominate() {
      BoardImpl board = boardOf(1L, "H...", "...E");
      board.setHardMode(true);
      board.moveHero(1, 0);

      assertEquals(new Posn(1, 2), firstEnemy(board));
    }

    @Test
    void movesOnlyOneCellPerHeroMove() {
      BoardImpl board = boardOf(1L, ".H..", "....", "....", ".E..");
      board.setHardMode(true);
      Posn before = firstEnemy(board);
      board.moveHero(0, 1);
      Posn after = firstEnemy(board);

      int distance = Math.abs(after.row() - before.row()) + Math.abs(after.col() - before.col());
      assertEquals(1, distance);
    }

    @Test
    @DisplayName("the chase consults no randomness, so the seed cannot change it")
    void isIndependentOfTheSeed() {
      BoardImpl first = boardOf(1L, ".H..", "....", ".E..");
      first.setHardMode(true);
      first.moveHero(0, 1);

      BoardImpl second = boardOf(999_999L, ".H..", "....", ".E..");
      second.setHardMode(true);
      second.moveHero(0, 1);

      assertEquals(render(first), render(second));
    }

    @Test
    @DisplayName("a walled-off preferred direction falls back rather than standing still")
    void takesAnAlternativeWhenThePreferredDirectionIsBlocked() {
      BoardImpl board = boardOf(1L, "H...", ".W..", ".E..");
      board.setHardMode(true);
      board.moveHero(0, 1);

      // The wall sits on the enemy's preferred upward step, so it must pick another direction.
      assertNotEquals(new Posn(2, 1), firstEnemy(board));
      assertEquals(PieceType.WALL, board.get(new Posn(1, 1)).getType());
    }
  }

  @Nested
  class EasyMode {

    @Test
    @DisplayName("the same seed replays the same enemy wandering")
    void isReproducibleForAGivenSeed() {
      BoardImpl first = boardOf(42L, "H....", ".....", "..E..");
      BoardImpl second = boardOf(42L, "H....", ".....", "..E..");
      for (int move = 0; move < 3; move++) {
        first.moveHero(0, 1);
        second.moveHero(0, 1);
      }

      assertEquals(render(first), render(second));
    }

    @Test
    void differentSeedsDivergeEventually() {
      BoardImpl first = boardOf(1L, "H......", ".......", "...E...");
      BoardImpl second = boardOf(20L, "H......", ".......", "...E...");
      for (int move = 0; move < 4; move++) {
        first.moveHero(0, 1);
        second.moveHero(0, 1);
      }

      assertNotEquals(render(first), render(second));
    }

    @Test
    void movesTheEnemyToAnAdjacentCell() {
      BoardImpl board = boardOf(42L, "H....", ".....", "..E..");
      Posn before = firstEnemy(board);
      board.moveHero(0, 1);
      Posn after = firstEnemy(board);

      int distance = Math.abs(after.row() - before.row()) + Math.abs(after.col() - before.col());
      assertTrue(distance <= 1, "An enemy moved " + distance + " cells in one turn");
    }

    @Test
    @DisplayName("an enemy boxed in on all sides simply stays put")
    void staysPutWhenEveryDirectionIsRefused() {
      BoardImpl board = boardOf(42L, ".W..", "WEW.", ".W.H");
      board.moveHero(0, -1);

      assertEquals(new Posn(1, 1), firstEnemy(board));
    }
  }
}
