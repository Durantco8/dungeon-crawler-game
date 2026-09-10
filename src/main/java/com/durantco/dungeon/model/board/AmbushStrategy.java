package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;

/**
 * Cuts the hero off by aiming where it is going rather than where it is.
 *
 * <p>Targets a cell a few steps ahead along the hero's current heading, so a hero running down a
 * corridor finds something waiting at the far end. If nothing ahead can be reached, or the hero has
 * not moved yet and so has no heading, it falls back to a direct chase; an ambusher that stood still
 * whenever its plan failed would simply look broken.
 */
public final class AmbushStrategy implements MovementStrategy {

  private static final int LOOK_AHEAD = 3;

  private final MovementStrategy fallback = new ChaseStrategy();

  @Override
  public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
    Posn heading = context.heroHeading();
    if (heading.row() == 0 && heading.col() == 0) {
      return fallback.chooseStep(enemy, context);
    }

    // Aim as far ahead as can actually be reached, shortening the lead until something works.
    for (int lead = LOOK_AHEAD; lead >= 1; lead--) {
      Posn ambush =
          context.heroPosition().offset(heading.row() * lead, heading.col() * lead);
      Optional<Posn> step = context.stepTowards(enemy, ambush);
      if (step.isPresent()) {
        return step;
      }
    }
    return fallback.chooseStep(enemy, context);
  }
}
