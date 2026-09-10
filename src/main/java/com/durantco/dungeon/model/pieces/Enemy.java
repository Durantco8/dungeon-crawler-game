package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.MovementStrategy;
import com.durantco.dungeon.model.board.WanderStrategy;

/**
 * A hostile piece that hunts the hero.
 *
 * <p>How it hunts is composed in rather than subclassed, so every enemy stays one piece type for
 * collision and rendering purposes while behaving differently.
 */
public class Enemy extends APiece implements MovablePiece {

  private MovementStrategy movement;

  /** An enemy that drifts at random. */
  public Enemy() {
    this(new WanderStrategy());
  }

  /**
   * @param movement how this enemy decides where to go
   */
  public Enemy(MovementStrategy movement) {
    super(PieceType.ENEMY);
    this.movement = movement;
  }

  /**
   * @return how this enemy decides where to go
   */
  public MovementStrategy movement() {
    return movement;
  }

  /**
   * Re-arms this enemy with different behaviour, as happens when the difficulty changes.
   *
   * @param movement the new behaviour
   */
  public void setMovement(MovementStrategy movement) {
    this.movement = movement;
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
