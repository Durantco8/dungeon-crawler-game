package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** How an enemy's pace shows up in the number of cells it covers on a real board. */
class EnemyPaceTest {

  private static Posn enemyPosition(Board board) {
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

  /**
   * An open two-row corridor with the hero at one end and a single chaser of the given pace at the
   * other, close enough to see it.
   */
  private static BoardImpl boardWithPacedChaser(ActionRate rate) {
    Piece[][] grid = Boards.parse("H.......", "........");
    grid[1][7] = new Enemy(new ChaseStrategy(), rate);
    return new BoardImpl(grid, new Random(1L));
  }

  @Test
  @DisplayName("a normal enemy covers one cell per hero move, as before")
  void normalEnemiesTakeOneStepPerTurn() {
    BoardImpl board = boardWithPacedChaser(ActionRate.NORMAL);
    Posn before = enemyPosition(board);
    board.moveHero(1, 0);
    Posn after = enemyPosition(board);

    assertEquals(1, Math.abs(after.row() - before.row()) + Math.abs(after.col() - before.col()));
  }

  @Test
  @DisplayName("a slow enemy stands still on its first turn")
  void slowEnemiesSitOutAlternateTurns() {
    BoardImpl board = boardWithPacedChaser(ActionRate.SLOW);
    Posn before = enemyPosition(board);
    board.moveHero(1, 0);

    assertEquals(before, enemyPosition(board), "Half a turn's energy should not buy a step");
  }

  @Test
  @DisplayName("a fast enemy covers ground quicker over several turns")
  void fastEnemiesOutpaceNormalOnes() {
    assertTrue(
        cellsCoveredIn(ActionRate.FAST, 4) > cellsCoveredIn(ActionRate.NORMAL, 4),
        "A fast enemy should get further in the same number of turns");
    assertTrue(
        cellsCoveredIn(ActionRate.NORMAL, 4) > cellsCoveredIn(ActionRate.SLOW, 4),
        "A slow enemy should fall behind a normal one");
  }

  @Test
  @DisplayName("paces are consistent across a longer run, not just the first turn")
  void pacesHoldOverManyTurns() {
    assertEquals(6, cellsCoveredIn(ActionRate.NORMAL, 6));
    assertEquals(3, cellsCoveredIn(ActionRate.SLOW, 6));
  }

  /**
   * How many cells the enemy covers over a number of turns, chasing along an open corridor.
   *
   * <p>The hero steps back and forth so that every turn is a real one. A refused move returns before
   * the enemies act, so nudging the hero into a wall would not spend a turn at all.
   */
  private static int cellsCoveredIn(ActionRate rate, int turns) {
    Enemy paced = new Enemy(new ChaseStrategy(), rate);
    Piece[][] grid = Boards.parse("H..........", "...........");
    grid[1][10] = paced;
    BoardImpl board = new BoardImpl(grid, new Random(1L));

    Posn start = paced.getPosn();
    for (int turn = 0; turn < turns; turn++) {
      int sideways = 1;
      if (turn % 2 == 1) {
        sideways = -1;
      }
      board.moveHero(0, sideways);
    }
    Posn end = paced.getPosn();
    return Math.abs(end.row() - start.row()) + Math.abs(end.col() - start.col());
  }
}
