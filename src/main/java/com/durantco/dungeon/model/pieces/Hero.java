package com.durantco.dungeon.model.pieces;

public class Hero extends APiece implements MovablePiece {
  public Hero() {
    super("Hero", "hero.png");
  }

  public CollisionResult collide(Piece other) {
    /*
    if NUll --> continue the game as normal
    if treasure --> treat other as treasure and get the points associated
    if Enemy --> the game will end
    if Exit --> then the game will send off to the next level
     */

    if (other == null) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    } else if (other instanceof Treasure) {
      return new CollisionResult(((Treasure) other).getValue(), CollisionResult.Result.CONTINUE);
    } else if (other instanceof Enemy) {
      return new CollisionResult(0, CollisionResult.Result.GAME_OVER);
    } else if (other instanceof Exit) {
      return new CollisionResult(0, CollisionResult.Result.NEXT_LEVEL);
    } else if (other instanceof Thief) {
      return new CollisionResult(((Thief) other).getValue(), CollisionResult.Result.CONTINUE);
    } else if (other instanceof Wall) {
      throw new IllegalArgumentException();
    }
    throw new IllegalArgumentException();
  }
}
