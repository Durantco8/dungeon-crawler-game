package com.durantco.dungeon.model.pieces;

public interface MovablePiece extends Piece {
  CollisionResult collide(Piece other);
}
