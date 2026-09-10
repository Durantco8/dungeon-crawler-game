package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;

/**
 * Drifts at random, taking the first of the four directions, shuffled, that the enemy may enter.
 *
 * <p>Shuffling rather than picking one direction outright means a boxed-in enemy still finds its one
 * legal move instead of standing still by bad luck.
 */
public final class WanderStrategy implements MovementStrategy {

  private static final int[][] STEPS = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

  @Override
  public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
    int[][] directions = STEPS.clone();
    for (int i = 0; i < directions.length; i++) {
      directions[i] = STEPS[i].clone();
    }
    for (int i = directions.length - 1; i > 0; i--) {
      int j = context.rng().nextInt(i + 1);
      int[] swap = directions[i];
      directions[i] = directions[j];
      directions[j] = swap;
    }
    for (int[] direction : directions) {
      Posn target = enemy.getPosn().offset(direction[0], direction[1]);
      if (context.canEnter(enemy, target)) {
        return Optional.of(target);
      }
    }
    return Optional.empty();
  }
}
