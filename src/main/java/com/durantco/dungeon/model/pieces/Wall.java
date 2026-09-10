package com.durantco.dungeon.model.pieces;

/** An impassable obstacle. */
public class Wall extends APiece {

  public Wall() {
    super("Wall", "wall.png");
  }

  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.blocked();
  }

  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.blocked();
  }
}
