package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.ActionMeter;
import com.durantco.dungeon.model.board.ActionRate;
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
  private final ActionMeter meter;

  /** An enemy that drifts at random, acting once per turn. */
  public Enemy() {
    this(new WanderStrategy(), ActionRate.NORMAL);
  }

  /**
   * @param movement how this enemy decides where to go
   */
  public Enemy(MovementStrategy movement) {
    this(movement, ActionRate.NORMAL);
  }

  /**
   * @param movement how this enemy decides where to go
   * @param rate how often it gets to go
   */
  public Enemy(MovementStrategy movement, ActionRate rate) {
    super(PieceType.ENEMY);
    this.movement = movement;
    this.meter = new ActionMeter(rate);
  }

  /**
   * @return this enemy's energy meter, which decides how many actions it gets this turn
   */
  public ActionMeter meter() {
    return meter;
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
