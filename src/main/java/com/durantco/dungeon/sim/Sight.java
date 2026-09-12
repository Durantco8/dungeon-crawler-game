package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.AStarPathfinder;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * What an agent can work out by looking at the board.
 *
 * <p>Finding pieces by type is identification, not dispatch: an agent asking where the exit is does not care
 * how an exit behaves, and nothing here decides anything a piece should decide for itself.
 */
final class Sight {

  private static final AStarPathfinder PATHFINDER = new AStarPathfinder();

  private Sight() {}

  /**
   * @return where the given kind of piece sits, in row-major order
   */
  static List<Posn> locate(Model model, PieceType type) {
    List<Posn> found = new ArrayList<>();
    for (int row = 0; row < model.getHeight(); row++) {
      for (int col = 0; col < model.getWidth(); col++) {
        Piece piece = model.get(new Posn(row, col));
        if (piece != null && piece.getType() == type) {
          found.add(new Posn(row, col));
        }
      }
    }
    return found;
  }

  /**
   * @return where the hero is, or empty if there is no hero on the board
   */
  static Optional<Posn> hero(Model model) {
    List<Posn> found = locate(model, PieceType.HERO);
    if (found.isEmpty()) {
      return Optional.empty();
    }
    return Optional.of(found.get(0));
  }

  /**
   * The move that starts a shortest route from the hero to a cell.
   *
   * @return the move to make, or empty if the hero or the route is missing
   */
  static Optional<PlayerMove> stepTowards(Model model, Posn goal) {
    Optional<Posn> from = hero(model);
    if (from.isEmpty()) {
      return Optional.empty();
    }
    Optional<Posn> step = PATHFINDER.nextStep(from.get(), goal, model::heroCanEnter);
    if (step.isEmpty()) {
      return Optional.empty();
    }
    return moveBetween(from.get(), step.get());
  }

  /** Translates one step between neighbouring cells into the input that would cause it. */
  static Optional<PlayerMove> moveBetween(Posn from, Posn to) {
    int drow = to.row() - from.row();
    int dcol = to.col() - from.col();
    for (PlayerMove move : PlayerMove.values()) {
      if (move.drow() == drow && move.dcol() == dcol) {
        return Optional.of(move);
      }
    }
    return Optional.empty();
  }

  /**
   * The nearest of several cells by route length, so an agent chases what it can actually reach soonest
   * rather than what is closest in a straight line through a wall.
   */
  static Optional<Posn> nearestReachable(Model model, List<Posn> targets) {
    Optional<Posn> from = hero(model);
    if (from.isEmpty()) {
      return Optional.empty();
    }
    Posn nearest = null;
    int shortest = Integer.MAX_VALUE;
    for (Posn target : targets) {
      int length = PATHFINDER.findPath(from.get(), target, model::heroCanEnter).size();
      if (length > 0 && length < shortest) {
        shortest = length;
        nearest = target;
      }
    }
    return Optional.ofNullable(nearest);
  }
}
