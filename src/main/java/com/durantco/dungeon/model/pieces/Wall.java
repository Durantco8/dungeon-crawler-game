package com.durantco.dungeon.model.pieces;

/** An impassable obstacle. */
public class Wall extends APiece {

  public Wall() {
    super(PieceType.WALL);
  }

  /** You cannot see through a wall, which is what gives the dungeon blind corners. */
  @Override
  public boolean blocksSight() {
    return true;
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
