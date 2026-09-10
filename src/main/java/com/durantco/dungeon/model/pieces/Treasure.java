package com.durantco.dungeon.model.pieces;

/** A collectible worth points to the hero. */
public class Treasure extends APiece {

  public Treasure() {
    super("Treasure", "treasure.png");
  }

  public int getValue() {
    return 5;
  }

  /** The hero banks the treasure's value. */
  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.scoring(getValue());
  }

  /** An enemy may step onto treasure, which destroys it without scoring. */
  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.free();
  }
}
