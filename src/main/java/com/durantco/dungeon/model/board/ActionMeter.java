package com.durantco.dungeon.model.board;

/**
 * Tracks whether a piece has banked enough energy to act.
 *
 * <p>Every turn a piece is granted its rate's worth of energy, and spends a fixed cost per action. A
 * piece slower than the cost therefore sits out some turns, and one faster than it occasionally gets a
 * second action, without either needing a special case.
 *
 * <p>The hero has no meter. It acts when the player presses a key, because a game that sometimes
 * ignored a keypress would read as broken rather than as tactical.
 */
public final class ActionMeter {

  /** What one action costs. Rates are expressed against this. */
  public static final int COST_PER_ACTION = 100;

  private final ActionRate rate;
  private int stored;

  /**
   * @param rate how much energy this piece banks per turn
   */
  public ActionMeter(ActionRate rate) {
    this.rate = rate;
  }

  /** Banks this piece's energy for one turn. */
  public void grantTurn() {
    stored += rate.energyPerTurn();
  }

  /**
   * @return true if enough energy is banked for another action this turn
   */
  public boolean canAct() {
    return stored >= COST_PER_ACTION;
  }

  /**
   * Pays for one action.
   *
   * @throws IllegalStateException if there is not enough energy banked, which would mean a caller
   *     acted without checking
   */
  public void spendAction() {
    if (!canAct()) {
      throw new IllegalStateException("Acted with only " + stored + " energy banked");
    }
    stored -= COST_PER_ACTION;
  }

  /**
   * @return the energy banked but not yet spent
   */
  public int stored() {
    return stored;
  }

  /**
   * @return the rate this meter fills at
   */
  public ActionRate rate() {
    return rate;
  }
}
