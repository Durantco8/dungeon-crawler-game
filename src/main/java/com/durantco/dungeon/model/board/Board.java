package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;

/** Defines the public operations on the game board. */
public interface Board {

  void init(int enemies, int treasures, int walls);

  void setHardMode(boolean hardMode);

  int getWidth();

  int getHeight();

  Piece get(Posn posn);

  void set(Piece p, Posn newPos);

  CollisionResult moveHero(int drow, int dcol);
}
