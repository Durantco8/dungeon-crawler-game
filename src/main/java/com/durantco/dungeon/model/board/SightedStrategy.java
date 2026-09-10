package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;

/**
 * Gates hunting behaviour on actually being able to see the hero.
 *
 * <p>Wraps two behaviours: one for when the hero is in view, one for when it is not. Between the two
 * sits a short memory. An enemy that loses sight of the hero heads for where it last saw it, and only
 * gives up once it arrives and finds nothing. Forgetting instantly would make enemies feel blind, and
 * never forgetting would make line of sight pointless, since the first sighting would commit them
 * forever.
 */
public final class SightedStrategy implements MovementStrategy {

  private static final int DEFAULT_SIGHT_RANGE = 8;

  private final MovementStrategy whenSeen;
  private final MovementStrategy whenUnseen;
  private final int sightRange;

  private Posn lastSeenAt;

  /**
   * @param whenSeen how to behave while the hero is in view
   * @param whenUnseen how to behave once the trail has gone cold
   * @param sightRange how many steps away the hero can be seen from
   */
  public SightedStrategy(MovementStrategy whenSeen, MovementStrategy whenUnseen, int sightRange) {
    if (sightRange < 0) {
      throw new IllegalArgumentException("Sight range cannot be negative");
    }
    this.whenSeen = whenSeen;
    this.whenUnseen = whenUnseen;
    this.sightRange = sightRange;
  }

  /**
   * @param whenSeen how to behave while the hero is in view
   * @param whenUnseen how to behave once the trail has gone cold
   */
  public SightedStrategy(MovementStrategy whenSeen, MovementStrategy whenUnseen) {
    this(whenSeen, whenUnseen, DEFAULT_SIGHT_RANGE);
  }

  @Override
  public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
    if (canSeeHero(enemy, context)) {
      lastSeenAt = context.heroPosition();
      return whenSeen.chooseStep(enemy, context);
    }

    if (lastSeenAt != null) {
      if (enemy.getPosn().equals(lastSeenAt)) {
        lastSeenAt = null; // arrived, and the hero is not here
        return whenUnseen.chooseStep(enemy, context);
      }
      Optional<Posn> towardsMemory = context.stepTowards(enemy, lastSeenAt);
      if (towardsMemory.isPresent()) {
        return towardsMemory;
      }
      lastSeenAt = null; // the trail is unreachable, so stop following it
    }

    return whenUnseen.chooseStep(enemy, context);
  }

  /**
   * @return whether this enemy has seen the hero at least once and has not yet given up on it
   */
  public boolean isTrackingHero() {
    return lastSeenAt != null;
  }

  /**
   * @return the behaviour used while the hero is in view
   */
  public MovementStrategy whenSeen() {
    return whenSeen;
  }

  /**
   * @return the behaviour used once the trail has gone cold
   */
  public MovementStrategy whenUnseen() {
    return whenUnseen;
  }

  /**
   * @return how many steps away the hero can be seen from
   */
  public int sightRange() {
    return sightRange;
  }

  private boolean canSeeHero(Enemy enemy, MovementContext context) {
    Posn hero = context.heroPosition();
    if (LineOfSight.sightDistance(enemy.getPosn(), hero) > sightRange) {
      return false;
    }
    return context.hasLineOfSight(enemy.getPosn(), hero);
  }
}
