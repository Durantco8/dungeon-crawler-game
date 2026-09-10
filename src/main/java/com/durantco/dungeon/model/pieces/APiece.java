package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.Posn;

/** Shared type and position state for pieces. */
public abstract class APiece implements Piece {
  private final PieceType type;
  private Posn position;

  public APiece(PieceType type) {
    this.type = type;
  }

  @Override
  public PieceType getType() {
    return type;
  }

  @Override
  public String getName() {
    return type.displayName();
  }

  @Override
  public Posn getPosn() {
    return position;
  }

  @Override
  public void setPosn(Posn posn) {
    this.position = posn;
  }

  @Override
  public String toString() {
    return getName() + "@" + position;
  }

  /*
   * onHeroEnter and onEnemyEnter are deliberately left abstract rather than given blocking
   * defaults. A new piece type must state both outcomes explicitly, so forgetting one is a
   * compile error instead of a silently impassable cell.
   */
}
