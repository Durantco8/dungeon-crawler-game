package com.durantco.dungeon.model.pieces;

/** The portal that completes a level. */
public class Exit extends APiece {

  public Exit() {
    super("Exit", "exit.png");
  }

  /** Reaching the exit advances the hero to the next level. */
  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.nextLevel();
  }

  /** Enemies cannot use the exit. */
  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.blocked();
  }
}
