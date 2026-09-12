package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.replay.PlayerMove;

/**
 * Something that plays the game without a person.
 *
 * <p>An agent sees exactly what a player sees, through the same Model interface, so a run it produces is a
 * run a person could have produced. That is what makes simulation results say anything about the real game
 * rather than about a private testing path.
 */
public interface Agent {

  /**
   * @return a short name for this agent, used to label results
   */
  String name();

  /**
   * Chooses the next input.
   *
   * @param model the game as it stands
   * @return the move to make
   */
  PlayerMove chooseMove(Model model);
}
