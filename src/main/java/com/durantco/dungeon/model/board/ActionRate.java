package com.durantco.dungeon.model.board;

/**
 * How quickly a piece accumulates the energy an action costs.
 *
 * <p>Rates are expressed against {@link ActionMeter#COST_PER_ACTION} rather than as "acts every N
 * turns", so a rate need not divide evenly into a whole number of turns: a fast piece takes an extra
 * action every other turn rather than two every turn.
 */
public enum ActionRate {

  /** Acts on every other turn. */
  SLOW(50),

  /** Acts once per turn, which is what every enemy used to do. */
  NORMAL(100),

  /** Acts once per turn and again on every other turn. */
  FAST(150);

  private final int energyPerTurn;

  ActionRate(int energyPerTurn) {
    this.energyPerTurn = energyPerTurn;
  }

  /**
   * @return the energy granted each turn
   */
  public int energyPerTurn() {
    return energyPerTurn;
  }
}
