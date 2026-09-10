package com.durantco.dungeon.model.pieces;

public class CollisionResult {
  private final int points;
  private final Result res;

  public CollisionResult(int points, Result res) {
    this.points = points;
    this.res = res;
  }

  public int getPoints() {
    return points;
  }

  public Result getResults() {
    return res;
  }

  public enum Result {
    CONTINUE,
    GAME_OVER,
    NEXT_LEVEL
  }
}
