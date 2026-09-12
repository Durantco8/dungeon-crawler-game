package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Collects every reachable treasure, then leaves.
 *
 * <p>Plays the game the way a scoring player would, so its results say what a level is worth rather than only
 * whether it can be survived. Comparing it against the exit runner separates two questions that otherwise get
 * conflated: whether a level can be finished, and whether finishing it well is worth the risk.
 */
public final class TreasureHunterAgent implements Agent {

  private final Agent whenNothingLeft;
  private final Agent fallback;

  /**
   * @param rng randomness for turns when nothing can be reached
   */
  public TreasureHunterAgent(Random rng) {
    this.whenNothingLeft = new ExitRunnerAgent(rng);
    this.fallback = new RandomAgent(rng);
  }

  @Override
  public String name() {
    return "treasure-hunter";
  }

  @Override
  public PlayerMove chooseMove(Model model) {
    List<Posn> treasures = Sight.locate(model, PieceType.TREASURE);
    Optional<Posn> nearest = Sight.nearestReachable(model, treasures);
    if (nearest.isPresent()) {
      Optional<PlayerMove> step = Sight.stepTowards(model, nearest.get());
      if (step.isPresent()) {
        return step.get();
      }
    }
    // Nothing worth collecting is reachable, so bank what it has and head for the exit.
    PlayerMove towardsExit = whenNothingLeft.chooseMove(model);
    if (towardsExit != null) {
      return towardsExit;
    }
    return fallback.chooseMove(model);
  }
}
