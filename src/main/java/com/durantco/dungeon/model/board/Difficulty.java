package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Random;

/**
 * What kind of enemies a level spawns.
 *
 * <p>This replaces a bare "do enemies chase you" flag. Once enemies can behave in several ways, the
 * interesting question is the mix, not whether hunting is switched on. The board's public API still
 * speaks in terms of hard mode, because that is the vocabulary the interface offers the player.
 */
public enum Difficulty {

  /** Every enemy drifts at random, all at the same unhurried pace. */
  EASY {
    @Override
    public Enemy spawn(int enemyIndex, Random rng) {
      return new Enemy(new WanderStrategy(), ActionRate.NORMAL);
    }
  },

  /**
   * A mix of hunters, each of which has to actually see the hero before it pursues: one that comes
   * straight at you and drifts once you break away, one that tries to cut you off, and one walking a
   * beat that gives chase on sight.
   */
  HARD {
    @Override
    public Enemy spawn(int enemyIndex, Random rng) {
      switch (enemyIndex % 3) {
        case 0:
          // The workhorse: comes straight at you at your own speed.
          return new Enemy(
              new SightedStrategy(new ChaseStrategy(), new WanderStrategy()), ActionRate.NORMAL);
        case 1:
          // Getting ahead of the hero is only frightening if it can outpace it.
          return new Enemy(
              new SightedStrategy(new AmbushStrategy(), new WanderStrategy()), ActionRate.FAST);
        default:
          // A heavy guard on a beat: dangerous to walk into, possible to outrun.
          return new Enemy(
              new SightedStrategy(new ChaseStrategy(), new PatrolStrategy()), ActionRate.SLOW);
      }
    }
  };

  /**
   * Builds one enemy, complete with its behaviour and its pace. Both are chosen by position in the
   * spawn order rather than at random, so a level's mix is fixed by its seed.
   *
   * @param enemyIndex the enemy's position in the spawn order
   * @param rng randomness for strategies that need it
   * @return a newly built enemy, with no position yet
   */
  public abstract Enemy spawn(int enemyIndex, Random rng);
}
