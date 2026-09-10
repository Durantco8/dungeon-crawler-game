package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Collision outcomes as they play out on a real board, including what happens to the grid. */
class BoardCollisionTest {

  private static BoardImpl boardOf(String... rows) {
    return new BoardImpl(Boards.parse(rows), new Random(11L));
  }

  private static Piece at(Board board, int row, int col) {
    return board.get(new Posn(row, col));
  }

  private static int countOf(Board board, PieceType type) {
    int found = 0;
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = at(board, row, col);
        if (piece != null && piece.getType() == type) {
          found++;
        }
      }
    }
    return found;
  }

  @Test
  void collectingTreasureScoresAndRemovesIt() {
    BoardImpl board = boardOf("HT.");
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(5, result.getPoints());
    assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
    assertEquals(PieceType.HERO, at(board, 0, 1).getType());
    assertEquals(0, countOf(board, PieceType.TREASURE));
  }

  @Test
  void meetingAThiefCostsPointsAndRemovesIt() {
    BoardImpl board = boardOf("HF.");
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(-5, result.getPoints());
    assertEquals(CollisionResult.Result.CONTINUE, result.getResults());
    assertEquals(0, countOf(board, PieceType.THIEF));
  }

  @Test
  void reachingTheExitCompletesTheLevel() {
    BoardImpl board = boardOf("HX.");
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.NEXT_LEVEL, result.getResults());
    assertEquals(0, result.getPoints());
  }

  @Test
  @DisplayName("enemies do not get a turn on the move that completes the level")
  void reachingTheExitEndsTheTurnImmediately() {
    BoardImpl board = boardOf("HX.", "..E");
    board.moveHero(0, 1);

    assertEquals(PieceType.ENEMY, at(board, 1, 2).getType());
  }

  @Test
  void walkingIntoAnEnemyEndsTheGame() {
    BoardImpl board = boardOf("HE.");
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.GAME_OVER, result.getResults());
  }

  @Test
  @DisplayName("an enemy catching the hero ends the game")
  void beingCaughtByAnEnemyEndsTheGame() {
    BoardImpl board = boardOf(".E.", "H..");
    board.setHardMode(true);
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.GAME_OVER, result.getResults());
  }

  @Test
  @DisplayName("an enemy stepping onto treasure destroys it and scores nothing")
  void enemiesDestroyTreasureTheyStepOn() {
    BoardImpl board = boardOf("H..", ".TE");
    board.setHardMode(true);
    CollisionResult result = board.moveHero(1, 0);

    assertEquals(0, result.getPoints());
    assertEquals(0, countOf(board, PieceType.TREASURE));
    assertEquals(PieceType.ENEMY, at(board, 1, 1).getType());
  }

  @Test
  void enemiesCannotEnterTheExit() {
    BoardImpl board = boardOf("H..", ".XE");
    board.setHardMode(true);
    board.moveHero(1, 0);

    assertEquals(PieceType.EXIT, at(board, 1, 1).getType());
    assertEquals(1, countOf(board, PieceType.EXIT));
  }

  @Test
  void enemiesCannotWalkThroughWalls() {
    BoardImpl board = boardOf("H..", ".WE");
    board.setHardMode(true);
    board.moveHero(1, 0);

    assertEquals(PieceType.WALL, at(board, 1, 1).getType());
  }

  @Test
  @DisplayName("no enemy is lost while the game is still running")
  void enemiesAreNeitherStackedNorLost() {
    BoardImpl board = boardOf("H....", ".....", ".EE..");
    board.setHardMode(true);

    // Play until an enemy catches the hero. Past that point the board is no longer a valid game
    // state, because the model is what stops play on a game over.
    for (int move = 0; move < 12; move++) {
      int drow = 0;
      int dcol = 1;
      if (move % 2 == 1) {
        dcol = -1;
      }
      CollisionResult result = board.moveHero(drow, dcol);
      if (result.getResults() == CollisionResult.Result.GAME_OVER) {
        break;
      }
      assertEquals(2, countOf(board, PieceType.ENEMY), "An enemy went missing on move " + move);
    }
  }

  @Test
  void vacatedCellsAreLeftEmpty() {
    BoardImpl board = boardOf("H..");
    board.moveHero(0, 1);
    assertNull(at(board, 0, 0));
  }
}
