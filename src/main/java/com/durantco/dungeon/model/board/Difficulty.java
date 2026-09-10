package com.durantco.dungeon.model.board;

import java.util.Random;

/**
 * What kind of enemies a level spawns.
 *
 * <p>This replaces a bare "do enemies chase you" flag. Once enemies can behave in several ways, the
 * interesting question is the mix, not whether hunting is switched on. The board's public API still
 * speaks in terms of hard mode, because that is the vocabulary the interface offers the player.
 */
public enum Difficulty {

  /** Every enemy drifts at random. */
  EASY {
    @Override
    public MovementStrategy strategyFor(int enemyIndex, Random rng) {
      return new WanderStrategy();
    }
  },

  /**
   * A mix of hunters, each of which has to actually see the hero before it pursues: one that comes
   * straight at you and drifts once you break away, one that tries to cut you off, and one walking a
   * beat that gives chase on sight.
   */
  HARD {
    @Override
    public MovementStrategy strategyFor(int enemyIndex, Random rng) {
      switch (enemyIndex % 3) {
        case 0:
          return new SightedStrategy(new ChaseStrategy(), new WanderStrategy());
        case 1:
          return new SightedStrategy(new AmbushStrategy(), new WanderStrategy());
        default:
          return new SightedStrategy(new ChaseStrategy(), new PatrolStrategy());
      }
    }
  };

  /**
   * The behaviour for one spawned enemy. Chosen by position in the spawn order rather than at random,
   * so a level's mix is fixed by its seed.
   *
   * @param enemyIndex the enemy's position in the spawn order
   * @param rng randomness for strategies that need it
   * @return the behaviour to give that enemy
   */
  public abstract MovementStrategy strategyFor(int enemyIndex, Random rng);
}
