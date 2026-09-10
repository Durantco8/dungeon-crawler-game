package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * What an enemy is allowed to know about the board when deciding where to step.
 *
 * <p>Strategies are handed this rather than the board itself, so a strategy can be tested against a
 * few stubbed answers instead of a fully populated dungeon, and so no strategy can quietly reach past
 * its own perception and mutate the board.
 */
public interface MovementContext {

  /**
   * @return where the hero currently stands
   */
  Posn heroPosition();

  /**
   * Whether an enemy may enter a cell. The hero's cell counts as enterable, since reaching it is what
   * an enemy is for.
   *
   * @param enemy the enemy that would move
   * @param target the cell it would move into
   * @return true if the move would not be refused
   */
  boolean canEnter(Enemy enemy, Posn target);

  /**
   * The first step along a shortest route to a cell, routing around walls.
   *
   * @param enemy the enemy that would move
   * @param goal where it wants to end up
   * @return the cell to step into, or empty if the goal cannot be reached
   */
  Optional<Posn> stepTowards(Enemy enemy, Posn goal);

  /**
   * The direction the hero last actually moved, as a one-cell offset. Zero before the hero has moved,
   * or after a move that was refused.
   *
   * @return the hero's heading, for strategies that aim where it is going rather than where it is
   */
  Posn heroHeading();

  /**
   * The rooms of the current level, in the order the generator produced them. Empty for boards built
   * from a fixed layout rather than generated.
   *
   * @return the level's rooms, for strategies that walk a beat between them
   */
  List<Room> rooms();

  /**
   * Whether the view between two cells is unobstructed. Walls block sight; pieces standing in the way
   * do not.
   *
   * @param from where the looking happens
   * @param to what is being looked at
   * @return true if nothing between the two blocks the view
   */
  boolean hasLineOfSight(Posn from, Posn to);

  /**
   * @return the randomness to use for any arbitrary choice, so behaviour stays reproducible
   */
  Random rng();
}
