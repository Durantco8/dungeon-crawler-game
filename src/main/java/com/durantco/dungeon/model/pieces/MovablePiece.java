package com.durantco.dungeon.model.pieces;

/** A piece that can initiate a move into another cell. */
public interface MovablePiece extends Piece {

  /**
   * Asks the destination cell what happens when this piece enters it.
   *
   * @param other the occupant of the destination cell, or null if the cell is empty
   * @return the outcome of the move
   */
  CollisionResult collide(Piece other);
}
