package com.durantco.dungeon.support;

import com.durantco.dungeon.model.board.Board;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.LevelSpec;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * A scripted board, so ModelImpl can be tested without a real grid.
 *
 * <p>The model's job is to translate collision outcomes into score, level and status changes. Handing
 * it canned outcomes tests that translation directly, instead of trying to manoeuvre a real board
 * into each situation.
 */
public final class FakeBoard implements Board {

  private final Deque<CollisionResult> scriptedResults = new ArrayDeque<>();
  private final List<LevelSpec> initCalls = new ArrayList<>();
  private final List<Move> moves = new ArrayList<>();
  private boolean canFit = true;

  /**
   * One hero move as the model requested it.
   *
   * @param drow rows requested, positive is downward
   * @param dcol columns requested, positive is rightward
   */
  public record Move(int drow, int dcol) {}

  /**
   * Queues the outcomes that successive moveHero calls will return. Once the queue is empty, further
   * moves report an uneventful move.
   */
  public FakeBoard script(CollisionResult... results) {
    for (CollisionResult result : results) {
      scriptedResults.add(result);
    }
    return this;
  }

  /** Makes canFit report false, as it would for a level too big for the board. */
  public FakeBoard rejectingLevels() {
    this.canFit = false;
    return this;
  }

  /** The specs passed to init, in order, so level progression can be asserted. */
  public List<LevelSpec> initCalls() {
    return List.copyOf(initCalls);
  }

  /** The deltas passed to moveHero, in order. */
  public List<Move> moves() {
    return List.copyOf(moves);
  }

  public int moveHeroCalls() {
    return moves.size();
  }

  @Override
  public void init(LevelSpec spec) {
    initCalls.add(spec);
  }

  @Override
  public boolean canFit(LevelSpec spec) {
    return canFit;
  }

  @Override
  public CollisionResult moveHero(int drow, int dcol) {
    moves.add(new Move(drow, dcol));
    CollisionResult next = scriptedResults.poll();
    if (next == null) {
      return CollisionResult.free();
    }
    return next;
  }

  @Override
  public void setHardMode(boolean hardMode) {}

  @Override
  public void setDifficulty(Difficulty difficulty) {}

  @Override
  public int getWidth() {
    return 8;
  }

  @Override
  public int getHeight() {
    return 8;
  }

  @Override
  public Piece get(Posn posn) {
    return null;
  }

  @Override
  public void set(Piece p, Posn newPos) {}
}
