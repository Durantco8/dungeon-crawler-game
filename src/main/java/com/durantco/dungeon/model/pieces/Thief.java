package com.durantco.dungeon.model.pieces;

/** A hazard that costs the hero points on contact. */
public class Thief extends APiece {

  public Thief() {
    super(PieceType.THIEF);
  }

  public int getValue() {
    return -5;
  }

  /** The hero loses points to the thief. */
  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.scoring(getValue());
  }

  /** An enemy may step onto a thief, which removes it. */
  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.free();
  }
}
