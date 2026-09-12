package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Heads straight for the exit, ignoring treasure and enemies alike.
 *
 * <p>Measures how survivable the shortest route through a level is. A low win rate here says levels are
 * dangerous even when played efficiently, rather than merely long.
 */
public final class ExitRunnerAgent implements Agent {

  private final Agent fallback;

  /**
   * @param rng randomness for the rare turn when no route to the exit exists
   */
  public ExitRunnerAgent(Random rng) {
    this.fallback = new RandomAgent(rng);
  }

  @Override
  public String name() {
    return "exit-runner";
  }

  @Override
  public PlayerMove chooseMove(Model model) {
    List<Posn> exits = Sight.locate(model, PieceType.EXIT);
    if (!exits.isEmpty()) {
      Optional<PlayerMove> step = Sight.stepTowards(model, exits.get(0));
      if (step.isPresent()) {
        return step.get();
      }
    }
    // No exit in sight, or no way to it this turn: keep moving rather than stalling the run.
    return fallback.chooseMove(model);
  }
}
