package com.durantco.dungeon.model.pieces;

public class Enemy extends APiece implements MovablePiece {

  public Enemy() {
    super("Enemy", "enemy.png");
  }

  public CollisionResult collide(Piece other) {
    /*
    if null --> continue the game unaffected
    if Treasure --> it will be reset to zero
    if hero --> the game will end
     */

    if (other == null) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    } else if (other instanceof Treasure) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    } else if (other instanceof Hero) {
      return new CollisionResult(0, CollisionResult.Result.GAME_OVER);
    } else if (other instanceof Thief) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    } else if (other instanceof Wall) {
      throw new IllegalArgumentException();
    } else if (other instanceof Exit) {
      throw new IllegalArgumentException();
    }
    throw new IllegalArgumentException();
  }
}
