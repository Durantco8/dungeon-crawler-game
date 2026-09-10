package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import com.durantco.dungeon.support.FakeBoard;
import java.util.Random;
import org.junit.jupiter.api.Test;

/** The model is the view's only window onto the board, so its read-through must be faithful. */
class ModelDelegationTest {

  @Test
  void reportsTheBoardDimensions() {
    Model model = new ModelImpl(new BoardImpl(5, 9, new Random(3L)));
    assertEquals(5, model.getWidth());
    assertEquals(9, model.getHeight());
  }

  @Test
  void readsPiecesThroughToTheBoard() {
    BoardImpl board = new BoardImpl(Boards.parse("H.", ".W"), new Random(3L));
    Model model = new ModelImpl(board);

    assertEquals(PieceType.HERO, model.get(new Posn(0, 0)).getType());
    assertEquals(PieceType.WALL, model.get(new Posn(1, 1)).getType());
    assertSame(board.get(new Posn(0, 0)), model.get(new Posn(0, 0)));
  }

  @Test
  void reportsEmptyCellsAsNull() {
    Model model = new ModelImpl(new BoardImpl(Boards.parse("H.", ".."), new Random(3L)));
    assertEquals(null, model.get(new Posn(1, 1)));
  }

  @Test
  void startsInEasyMode() {
    assertFalse(new ModelImpl(new FakeBoard()).isHardMode());
  }

  @Test
  void remembersTheDifficulty() {
    Model model = new ModelImpl(new FakeBoard());
    model.setHardMode(true);
    assertTrue(model.isHardMode());

    model.setHardMode(false);
    assertFalse(model.isHardMode());
  }

  @Test
  void theSizedConstructorBuildsABoardOfThatSize() {
    Model model = new ModelImpl(8, 8, new Random(3L));
    assertEquals(8, model.getWidth());
    assertEquals(8, model.getHeight());
  }

  @Test
  void aSeededModelIsReproducible() {
    Model first = new ModelImpl(8, 8, new Random(123L));
    first.startGame();
    Model second = new ModelImpl(8, 8, new Random(123L));
    second.startGame();

    StringBuilder a = new StringBuilder();
    StringBuilder b = new StringBuilder();
    for (int row = 0; row < 8; row++) {
      for (int col = 0; col < 8; col++) {
        a.append(describe(first, row, col));
        b.append(describe(second, row, col));
      }
    }
    assertEquals(a.toString(), b.toString());
  }

  private static char describe(Model model, int row, int col) {
    if (model.get(new Posn(row, col)) == null) {
      return '.';
    }
    return model.get(new Posn(row, col)).getType().name().charAt(0);
  }
}
