package com.durantco.dungeon.model.pieces;

/** The outcome of one piece moving onto another piece's cell. */
public class CollisionResult {
  private final int points;
  private final Result res;

  /**
   * @param points points awarded to the mover, which may be negative
   * @param res what the collision means for the game
   */
  public CollisionResult(int points, Result res) {
    this.points = points;
    this.res = res;
  }

  /** The move succeeds and changes nothing else. */
  public static CollisionResult free() {
    return new CollisionResult(0, Result.CONTINUE);
  }

  /** The move is not legal; the mover stays put. */
  public static CollisionResult blocked() {
    return new CollisionResult(0, Result.BLOCKED);
  }

  /**
   * The move succeeds and scores.
   *
   * @param points points awarded to the mover, which may be negative
   */
  public static CollisionResult scoring(int points) {
    return new CollisionResult(points, Result.CONTINUE);
  }

  /** The move ends the game. */
  public static CollisionResult gameOver() {
    return new CollisionResult(0, Result.GAME_OVER);
  }

  /** The move completes the level. */
  public static CollisionResult nextLevel() {
    return new CollisionResult(0, Result.NEXT_LEVEL);
  }

  public int getPoints() {
    return points;
  }

  public Result getResults() {
    return res;
  }

  /** What a collision means for the game. */
  public enum Result {
    /** The mover advances and play continues. */
    CONTINUE,
    /**
     * The mover cannot enter the cell and stays where it is. Handled inside the board, which retries
     * another direction or reports the move as a no-op; it never reaches the model.
     */
    BLOCKED,
    /** The hero and an enemy have met. */
    GAME_OVER,
    /** The hero has reached the exit. */
    NEXT_LEVEL
  }
}
