package com.durantco.dungeon.model.replay;

import com.durantco.dungeon.model.Model;

/**
 * One thing the player can ask the game to do.
 *
 * <p>This is the Command pattern with the commands as enum constants: each knows how to apply itself to
 * a model, and carries the single character it is written as in a saved game. Constants rather than
 * classes because none of the four takes any arguments, so there is no state for an object to hold and
 * a fixed set is exactly what a save file needs to be able to parse.
 *
 * <p>Refused moves are commands too. Walking into a wall is recorded like any other input, because a
 * replay has to reproduce the game move for move, and because whether a move is refused is the board's
 * answer, not something the player knew when they pressed the key.
 */
public enum PlayerMove {

  UP(-1, 0, 'U') {
    @Override
    public void applyTo(Model model) {
      model.moveUp();
    }
  },

  DOWN(1, 0, 'D') {
    @Override
    public void applyTo(Model model) {
      model.moveDown();
    }
  },

  LEFT(0, -1, 'L') {
    @Override
    public void applyTo(Model model) {
      model.moveLeft();
    }
  },

  RIGHT(0, 1, 'R') {
    @Override
    public void applyTo(Model model) {
      model.moveRight();
    }
  };

  private final int drow;
  private final int dcol;
  private final char symbol;

  PlayerMove(int drow, int dcol, char symbol) {
    this.drow = drow;
    this.dcol = dcol;
    this.symbol = symbol;
  }

  /**
   * Carries this move out.
   *
   * @param model the game to apply it to
   */
  public abstract void applyTo(Model model);

  /**
   * @return rows this move travels, negative being upward
   */
  public int drow() {
    return drow;
  }

  /**
   * @return columns this move travels, negative being leftward
   */
  public int dcol() {
    return dcol;
  }

  /**
   * @return the character this move is written as in a saved game
   */
  public char symbol() {
    return symbol;
  }

  /**
   * @param symbol a character written by {@link #symbol()}
   * @return the move it stands for
   * @throws IllegalArgumentException if no move uses that character
   */
  public static PlayerMove ofSymbol(char symbol) {
    for (PlayerMove move : values()) {
      if (move.symbol == symbol) {
        return move;
      }
    }
    throw new IllegalArgumentException("No move is written as '" + symbol + "'");
  }
}
