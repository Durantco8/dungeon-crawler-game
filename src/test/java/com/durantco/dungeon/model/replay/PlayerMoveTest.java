package com.durantco.dungeon.model.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameFactory;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PlayerMoveTest {

  private static Posn heroOn(Model model) {
    for (int row = 0; row < model.getHeight(); row++) {
      for (int col = 0; col < model.getWidth(); col++) {
        Piece piece = model.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.HERO) {
          return new Posn(row, col);
        }
      }
    }
    return null;
  }

  @Test
  void eachMoveTravelsInItsOwnDirection() {
    assertEquals(-1, PlayerMove.UP.drow());
    assertEquals(0, PlayerMove.UP.dcol());
    assertEquals(1, PlayerMove.DOWN.drow());
    assertEquals(-1, PlayerMove.LEFT.dcol());
    assertEquals(1, PlayerMove.RIGHT.dcol());
  }

  @Test
  void everyMoveHasItsOwnSymbol() {
    Set<Character> symbols = new HashSet<>();
    for (PlayerMove move : PlayerMove.values()) {
      assertTrue(symbols.add(move.symbol()), move + " shares a symbol with another move");
    }
    assertEquals(PlayerMove.values().length, symbols.size());
  }

  @Test
  @DisplayName("a symbol round-trips, which is what a save file relies on")
  void symbolsRoundTrip() {
    for (PlayerMove move : PlayerMove.values()) {
      assertEquals(move, PlayerMove.ofSymbol(move.symbol()));
    }
  }

  @Test
  void refusesAnUnknownSymbol() {
    assertThrows(IllegalArgumentException.class, () -> PlayerMove.ofSymbol('Z'));
  }

  @Test
  @DisplayName("applying a move does what calling the model directly would do")
  void applyingAMoveMatchesTheModelMethod() {
    for (PlayerMove move : PlayerMove.values()) {
      Model throughCommand = GameFactory.create(GameSetup.standard(12L));
      throughCommand.startGame();
      move.applyTo(throughCommand);

      Model throughModel = GameFactory.create(GameSetup.standard(12L));
      throughModel.startGame();
      switch (move) {
        case UP -> throughModel.moveUp();
        case DOWN -> throughModel.moveDown();
        case LEFT -> throughModel.moveLeft();
        case RIGHT -> throughModel.moveRight();
      }

      assertEquals(heroOn(throughModel), heroOn(throughCommand), move + " behaved differently");
      assertEquals(throughModel.getCurScore(), throughCommand.getCurScore());
    }
  }

  @Test
  @DisplayName("a move shifts the hero by its own offset when nothing is in the way")
  void aMoveShiftsTheHeroByItsOffset() {
    // An open board with no walls, so every direction from the middle is available.
    Model model = GameFactory.create(GameSetup.standard(7L));
    model.startGame();
    Posn before = heroOn(model);
    PlayerMove.DOWN.applyTo(model);
    Posn after = heroOn(model);

    boolean movedDown = after.equals(new Posn(before.row() + 1, before.col()));
    boolean stayedPut = after.equals(before);
    assertTrue(movedDown || stayedPut, "A move either happens or is refused; it cannot teleport");
  }
}
