package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;

/**
 * How one enemy decides where to move.
 *
 * <p>Behaviour is composed onto an enemy rather than subclassed into it, so all enemies remain one
 * piece type as far as collisions and rendering are concerned, and a new archetype is a new strategy
 * with no change to Enemy or to the board.
 */
public interface MovementStrategy {

  /**
   * Chooses this enemy's step for the current turn.
   *
   * @param enemy the enemy to move
   * @param context what the enemy is allowed to know about the board
   * @return the cell to step into, which the context has confirmed is enterable, or empty to stay put
   */
  Optional<Posn> chooseStep(Enemy enemy, MovementContext context);
}
