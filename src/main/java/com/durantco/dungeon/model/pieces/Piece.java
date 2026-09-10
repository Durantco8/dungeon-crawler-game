package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.Posn;

public interface Piece {
  String getName();

  Posn getPosn();

  void setPosn(Posn posn);

  String getResourcePath();
}
