package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.Random;

/**
 * Wanders at random.
 *
 * <p>The baseline every other agent is measured against. If a smarter agent cannot beat this, the
 * intelligence is not paying for itself, and if this one wins levels regularly then the levels are too easy.
 */
public final class RandomAgent implements Agent {

  private final Random rng;

  /**
   * @param rng the randomness to draw on, seeded so a run can be repeated
   */
  public RandomAgent(Random rng) {
    this.rng = rng;
  }

  @Override
  public String name() {
    return "random";
  }

  @Override
  public PlayerMove chooseMove(Model model) {
    PlayerMove[] moves = PlayerMove.values();
    return moves[rng.nextInt(moves.length)];
  }
}
