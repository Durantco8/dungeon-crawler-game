package com.durantco.dungeon.support;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;

/**
 * Renders a whole game state as text, so two games can be compared in one assertion.
 *
 * <p>Covers everything a player can observe: the board cell by cell, the score, the level, and the
 * status. Comparing renders is how the replay tests assert that a reconstructed game matches the one it
 * came from, and a mismatch prints as a readable diff rather than as a failed reference comparison.
 */
public final class GameStates {

  private GameStates() {}

  /**
   * @param model the game to render
   * @return a single line describing the entire observable state
   */
  public static String render(Model model) {
    StringBuilder out = new StringBuilder();
    out.append("score=").append(model.getCurScore());
    out.append(" level=").append(model.getLevel());
    out.append(" status=").append(model.getStatus());
    out.append(' ');
    for (int row = 0; row < model.getHeight(); row++) {
      for (int col = 0; col < model.getWidth(); col++) {
        Piece piece = model.get(new Posn(row, col));
        if (piece == null) {
          out.append('.');
        } else {
          out.append(piece.getType().name().charAt(0));
        }
      }
      out.append('/');
    }
    return out.toString();
  }
}
