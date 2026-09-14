package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.AStarPathfinder;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Heads for the exit while keeping its distance from anything that can kill it.
 *
 * <p>Exists to answer a question the other agents cannot. They ignore enemies entirely, so when every run
 * ends with the hero caught, that says the agents are careless and not necessarily that the game is hard.
 * This one has the same goal as {@link ExitRunnerAgent} and differs only in avoiding danger, which isolates
 * the variable: the gap between the two is what caution is worth.
 *
 * <p>Caution is applied in stages. It first looks for a route that stays well clear of enemies, then for one
 * that merely avoids walking into them, then for any route at all. Only when there is no route does it give
 * up on progress and retreat, because an agent that refused to move whenever the ideal path was unavailable
 * would measure its own timidity rather than the game's difficulty.
 */
public final class SurvivorAgent implements Agent {

  /** How much clear ground it prefers between itself and the nearest enemy. */
  private static final int PREFERRED_MARGIN = 3;

  private static final AStarPathfinder PATHFINDER = new AStarPathfinder();

  private final Agent fallback;

  /**
   * @param rng randomness for the turns when nothing better presents itself
   */
  public SurvivorAgent(Random rng) {
    this.fallback = new RandomAgent(rng);
  }

  @Override
  public String name() {
    return "survivor";
  }

  @Override
  public PlayerMove chooseMove(Model model) {
    Optional<Posn> hero = Sight.hero(model);
    List<Posn> enemies = Sight.locate(model, PieceType.ENEMY);
    List<Posn> exits = Sight.locate(model, PieceType.EXIT);

    if (hero.isPresent() && !exits.isEmpty()) {
      // Widest berth first, narrowing until a route exists.
      for (int margin = PREFERRED_MARGIN; margin >= 0; margin--) {
        Set<Posn> danger = within(enemies, margin);
        Optional<PlayerMove> step = routeTo(model, hero.get(), exits.get(0), danger);
        if (step.isPresent()) {
          return step.get();
        }
      }
    }

    if (hero.isPresent() && !enemies.isEmpty()) {
      // Cornered: no way onward, so put ground between itself and the nearest threat.
      Optional<PlayerMove> retreat = retreatFrom(model, hero.get(), enemies);
      if (retreat.isPresent()) {
        return retreat.get();
      }
    }
    return fallback.chooseMove(model);
  }

  /** The first step of a route that never enters a dangerous cell. */
  private static Optional<PlayerMove> routeTo(
      Model model, Posn from, Posn goal, Set<Posn> danger) {
    if (danger.contains(goal)) {
      return Optional.empty(); // the destination itself is not worth reaching this turn
    }
    List<Posn> path =
        PATHFINDER.findPath(from, goal, cell -> model.heroCanEnter(cell) && !danger.contains(cell));
    if (path.isEmpty()) {
      return Optional.empty();
    }
    return Sight.moveBetween(from, path.get(0));
  }

  /** Steps to whichever neighbour is furthest from the nearest enemy. */
  private static Optional<PlayerMove> retreatFrom(Model model, Posn hero, List<Posn> enemies) {
    PlayerMove best = null;
    int bestClearance = nearestEnemy(hero, enemies);
    for (PlayerMove move : PlayerMove.values()) {
      Posn candidate = hero.offset(move.drow(), move.dcol());
      if (!model.heroCanEnter(candidate)) {
        continue;
      }
      int clearance = nearestEnemy(candidate, enemies);
      if (clearance > bestClearance) {
        bestClearance = clearance;
        best = move;
      }
    }
    return Optional.ofNullable(best);
  }

  private static int nearestEnemy(Posn from, List<Posn> enemies) {
    int nearest = Integer.MAX_VALUE;
    for (Posn enemy : enemies) {
      nearest =
          Math.min(nearest, Math.abs(from.row() - enemy.row()) + Math.abs(from.col() - enemy.col()));
    }
    return nearest;
  }

  /**
   * Cells within a given distance of any enemy.
   *
   * <p>Measured in straight lines rather than steps. It overstates danger through a wall, which is the safe
   * direction to be wrong in for an agent deciding where not to walk, and it keeps the cost to a handful of
   * cells per enemy rather than a search per enemy per turn.
   */
  private static Set<Posn> within(List<Posn> enemies, int margin) {
    Set<Posn> danger = new HashSet<>();
    for (Posn enemy : enemies) {
      for (int drow = -margin; drow <= margin; drow++) {
        int span = margin - Math.abs(drow);
        for (int dcol = -span; dcol <= span; dcol++) {
          danger.add(enemy.offset(drow, dcol));
        }
      }
    }
    return danger;
  }
}
