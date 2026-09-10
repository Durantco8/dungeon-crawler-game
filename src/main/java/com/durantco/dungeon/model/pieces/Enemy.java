package com.durantco.dungeon.model.pieces;

/** A hostile piece that hunts the hero. */
public class Enemy extends APiece implements MovablePiece {

  public Enemy() {
    super(PieceType.ENEMY);
  }

  @Override
  public CollisionResult collide(Piece other) {
    if (other == null) {
      return CollisionResult.free();
    }
    return other.onEnemyEnter(this);
  }

  /** The hero walking into an enemy ends the game. */
  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.gameOver();
  }

  /** Enemies do not stack. */
  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.blocked();
  }
}
