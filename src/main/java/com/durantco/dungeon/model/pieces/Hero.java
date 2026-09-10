package com.durantco.dungeon.model.pieces;

/** The player's character. */
public class Hero extends APiece implements MovablePiece {

  public Hero() {
    super("Hero", "hero.png");
  }

  @Override
  public CollisionResult collide(Piece other) {
    if (other == null) {
      return CollisionResult.free();
    }
    return other.onHeroEnter(this);
  }

  /** There is only ever one hero, so this cannot happen; refuse the move rather than throw. */
  @Override
  public CollisionResult onHeroEnter(Hero hero) {
    return CollisionResult.blocked();
  }

  /** An enemy reaching the hero ends the game. */
  @Override
  public CollisionResult onEnemyEnter(Enemy enemy) {
    return CollisionResult.gameOver();
  }
}
