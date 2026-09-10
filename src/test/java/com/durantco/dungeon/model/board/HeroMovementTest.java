package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Movement is asserted against exact layouts, with no enemies unless a test needs them. */
class HeroMovementTest {

  private static final long SEED = 7L;

  private static BoardImpl boardOf(String... rows) {
    return new BoardImpl(Boards.parse(rows), new Random(SEED));
  }

  private static Piece at(Board board, int row, int col) {
    return board.get(new Posn(row, col));
  }

  private static Posn heroPosition(Board board) {
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = at(board, row, col);
        if (piece != null && piece.getType() == PieceType.HERO) {
          return new Posn(row, col);
        }
      }
    }
    return null;
  }

  @Nested
  class IntoEmptySpace {

    @Test
    void movesUp() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(-1, 0);
      assertEquals(new Posn(0, 1), heroPosition(board));
    }

    @Test
    void movesDown() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(1, 0);
      assertEquals(new Posn(2, 1), heroPosition(board));
    }

    @Test
    void movesLeft() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(0, -1);
      assertEquals(new Posn(1, 0), heroPosition(board));
    }

    @Test
    void movesRight() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(0, 1);
      assertEquals(new Posn(1, 2), heroPosition(board));
    }

    @Test
    void clearsTheCellItLeft() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(-1, 0);
      assertNull(at(board, 1, 1), "The vacated cell should be empty");
      assertNotNull(at(board, 0, 1));
    }

    @Test
    @DisplayName("the hero piece itself is moved, not replaced")
    void keepsTheSameHeroInstance() {
      BoardImpl board = boardOf("...", ".H.", "...");
      Piece before = at(board, 1, 1);
      board.moveHero(-1, 0);
      assertSame(before, at(board, 0, 1));
    }

    @Test
    void updatesTheHerosOwnPosition() {
      BoardImpl board = boardOf("...", ".H.", "...");
      board.moveHero(0, 1);
      assertEquals(new Posn(1, 2), at(board, 1, 2).getPosn());
    }

    @Test
    void reportsAnUneventfulMove() {
      BoardImpl board = boardOf("...", ".H.", "...");
      CollisionResult result = board.moveHero(-1, 0);
      assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
      assertEquals(0, result.getPoints());
    }
  }

  @Nested
  class BlockedByAWall {

    @Test
    void doesNotMoveThroughAWallAbove() {
      BoardImpl board = boardOf(".W.", ".H.", "...");
      board.moveHero(-1, 0);
      assertEquals(new Posn(1, 1), heroPosition(board));
    }

    @Test
    void doesNotMoveThroughAWallBelow() {
      BoardImpl board = boardOf("...", ".H.", ".W.");
      board.moveHero(1, 0);
      assertEquals(new Posn(1, 1), heroPosition(board));
    }

    @Test
    void doesNotMoveThroughAWallToTheLeft() {
      BoardImpl board = boardOf("...", "WH.", "...");
      board.moveHero(0, -1);
      assertEquals(new Posn(1, 1), heroPosition(board));
    }

    @Test
    void doesNotMoveThroughAWallToTheRight() {
      BoardImpl board = boardOf("...", ".HW", "...");
      board.moveHero(0, 1);
      assertEquals(new Posn(1, 1), heroPosition(board));
    }

    @Test
    @DisplayName("the wall survives the attempt")
    void leavesTheWallInPlace() {
      BoardImpl board = boardOf(".W.", ".H.", "...");
      board.moveHero(-1, 0);
      assertEquals(PieceType.WALL, at(board, 0, 1).getType());
    }

    @Test
    @DisplayName("a refused move is reported as uneventful, not as a distinct outcome")
    void reportsContinueWithNoPoints() {
      BoardImpl board = boardOf(".W.", ".H.", "...");
      CollisionResult result = board.moveHero(-1, 0);
      assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
      assertEquals(0, result.getPoints());
    }

    @Test
    @DisplayName("enemies do not get a turn when the hero's move is refused")
    void spendsTheTurnWithoutMovingEnemies() {
      BoardImpl board = boardOf(".W.", ".H.", "E..");
      board.moveHero(-1, 0);
      assertEquals(PieceType.ENEMY, at(board, 2, 0).getType());
    }
  }

  @Nested
  class BlockedByTheBoardEdge {

    @Test
    void cannotLeaveTheTopEdge() {
      BoardImpl board = boardOf("H..", "...", "...");
      board.moveHero(-1, 0);
      assertEquals(new Posn(0, 0), heroPosition(board));
    }

    @Test
    void cannotLeaveTheLeftEdge() {
      BoardImpl board = boardOf("H..", "...", "...");
      board.moveHero(0, -1);
      assertEquals(new Posn(0, 0), heroPosition(board));
    }

    @Test
    void cannotLeaveTheBottomEdge() {
      BoardImpl board = boardOf("...", "...", "..H");
      board.moveHero(1, 0);
      assertEquals(new Posn(2, 2), heroPosition(board));
    }

    @Test
    void cannotLeaveTheRightEdge() {
      BoardImpl board = boardOf("...", "...", "..H");
      board.moveHero(0, 1);
      assertEquals(new Posn(2, 2), heroPosition(board));
    }

    @Test
    void reportsContinueWithNoPoints() {
      BoardImpl board = boardOf("H..", "...", "...");
      CollisionResult result = board.moveHero(-1, 0);
      assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
      assertEquals(0, result.getPoints());
    }

    @Test
    void spendsTheTurnWithoutMovingEnemies() {
      BoardImpl board = boardOf("H..", "...", "E..");
      board.moveHero(-1, 0);
      assertEquals(PieceType.ENEMY, at(board, 2, 0).getType());
    }
  }

  @Nested
  class WithoutAHero {

    @Test
    @DisplayName("a board with no hero reports an uneventful move rather than failing")
    void reportsAnUneventfulMove() {
      BoardImpl board = boardOf("...", ".W.", "...");
      CollisionResult result = board.moveHero(-1, 0);
      assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
      assertEquals(0, result.getPoints());
    }
  }
}
