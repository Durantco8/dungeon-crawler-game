package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The unseeded constructors the application itself uses. They cannot be asserted on layout, only on
 * the structural facts that must hold whatever the randomness produces.
 */
class ProductionConstructorTest {

  /** The wall count the default scatter generator uses when no generator is supplied. */
  private static final int DEFAULT_WALLS = 2;

  @Test
  @DisplayName("the unseeded board constructor still builds a complete level")
  void unseededSizedBoardInitialisesALevel() {
    BoardImpl board = new BoardImpl(8, 8);
    LevelSpec spec = LevelSpec.forLevel(1);
    board.init(spec);

    int occupied = 0;
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        if (board.get(new Posn(row, col)) != null) {
          occupied++;
        }
      }
    }
    assertEquals(spec.cellsRequired() + DEFAULT_WALLS, occupied);
  }

  @Test
  void unseededGridBoardAdoptsTheLayout() {
    BoardImpl board = new BoardImpl(Boards.parse("H.", ".W"));

    assertEquals(PieceType.HERO, board.get(new Posn(0, 0)).getType());
    assertEquals(PieceType.WALL, board.get(new Posn(1, 1)).getType());
    assertEquals(new Posn(0, 0), board.get(new Posn(0, 0)).getPosn());
  }

  @Test
  void unseededModelStartsAGame() {
    Model model = new ModelImpl(8, 8);
    model.startGame();

    assertEquals(Model.STATUS.IN_PROGRESS, model.getStatus());
    assertEquals(1, model.getLevel());
  }

  @Test
  @DisplayName("a piece describes itself by name and position, for debugging output")
  void piecesDescribeThemselves() {
    BoardImpl board = new BoardImpl(Boards.parse("H."));
    Piece hero = board.get(new Posn(0, 0));

    assertEquals("Hero@0,0", hero.toString());
    assertTrue(hero.toString().contains(hero.getName()));
  }
}
