package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A fatal turn must leave the grid describing a real game state.
 *
 * <p>Previously the fatal move was completed regardless: the hero was written into the enemy's cell,
 * or an enemy was written into the hero's, so one of the two pieces was erased from the board while
 * still reporting that position. Nothing noticed, because the model ends the game and the view stops
 * drawing the board. The replay and simulation work in the next phase drives the board past this
 * point, so the turn now stops instead of completing.
 */
class GameOverBoardStateTest {

  private static BoardImpl boardOf(String... rows) {
    return new BoardImpl(Boards.parse(rows), new Random(5L));
  }

  private static int countOf(Board board, PieceType type) {
    int found = 0;
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == type) {
          found++;
        }
      }
    }
    return found;
  }

  @Test
  @DisplayName("walking into an enemy leaves both the hero and the enemy on the board")
  void aFatalHeroMoveLosesNoPieces() {
    BoardImpl board = boardOf("HE.");
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.GAME_OVER, result.getResults());
    assertEquals(1, countOf(board, PieceType.HERO));
    assertEquals(1, countOf(board, PieceType.ENEMY));
  }

  @Test
  @DisplayName("the hero does not step onto the enemy that kills it")
  void aFatalHeroMoveDoesNotComplete() {
    BoardImpl board = boardOf("HE.");
    board.moveHero(0, 1);

    assertEquals(PieceType.HERO, board.get(new Posn(0, 0)).getType());
    assertEquals(PieceType.ENEMY, board.get(new Posn(0, 1)).getType());
  }

  @Test
  @DisplayName("being caught leaves both the hero and the enemy on the board")
  void aFatalEnemyMoveLosesNoPieces() {
    BoardImpl board = boardOf(".E.", "H..");
    board.setHardMode(true);
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.GAME_OVER, result.getResults());
    assertEquals(1, countOf(board, PieceType.HERO));
    assertEquals(1, countOf(board, PieceType.ENEMY));
  }

  @Test
  @DisplayName("every piece still agrees with the cell it occupies after a fatal turn")
  void positionsStayConsistentAfterAFatalTurn() {
    BoardImpl board = boardOf(".E.", "H..");
    board.setHardMode(true);
    board.moveHero(0, 1);

    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Posn at = new Posn(row, col);
        Piece piece = board.get(at);
        if (piece != null) {
          assertEquals(at, piece.getPosn(), piece.getName() + " disagrees about where it is");
        }
      }
    }
  }

  @Test
  @DisplayName("a second enemy gets no turn once the hero has been caught")
  void noFurtherEnemyActsAfterTheHeroIsCaught() {
    // The first enemy in scan order reaches the hero, so the second must not move.
    BoardImpl board = boardOf(".E..", "H...", "...E");
    board.setHardMode(true);
    board.moveHero(0, 1);

    assertNotNull(board.get(new Posn(2, 3)), "The trailing enemy should not have moved");
    assertEquals(PieceType.ENEMY, board.get(new Posn(2, 3)).getType());
  }

  @Test
  void theScoreFromEarlierInTheTurnIsStillReported() {
    // The hero collects treasure, then the enemy directly below it closes in and catches it.
    BoardImpl board = boardOf("HT.", ".E.");
    board.setHardMode(true);
    CollisionResult result = board.moveHero(0, 1);

    assertEquals(CollisionResult.Result.GAME_OVER, result.getResults());
    assertEquals(5, result.getPoints());
  }
}
