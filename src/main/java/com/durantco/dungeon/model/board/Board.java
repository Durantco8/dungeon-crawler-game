package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;

/** Defines the public operations on the game board. */
public interface Board {

  /**
   * Clears the board and populates it according to the spec.
   *
   * @param spec how many of each piece to place
   * @throws IllegalArgumentException if the spec does not fit; callers are expected to consult
   *     {@link #canFit} first, so this signals a programming error rather than a game condition
   */
  void init(LevelSpec spec);

  /**
   * Reports whether a level's pieces fit on this board, so the model can decide what to do when
   * they do not instead of discovering it through a thrown exception.
   *
   * @param spec the level contents to check
   * @return true if every piece can be given its own cell
   */
  boolean canFit(LevelSpec spec);

  void setHardMode(boolean hardMode);

  int getWidth();

  int getHeight();

  Piece get(Posn posn);

  void set(Piece p, Posn newPos);

  CollisionResult moveHero(int drow, int dcol);
}
