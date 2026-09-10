package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;

/** Walks a shortest route straight at the hero, rounding corners and going around walls. */
public final class ChaseStrategy implements MovementStrategy {

  @Override
  public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
    return context.stepTowards(enemy, context.heroPosition());
  }
}
