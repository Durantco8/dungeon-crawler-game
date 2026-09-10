package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ActionMeterTest {

  /** Counts how many actions a meter affords over a run of turns. */
  private static int actionsOver(ActionRate rate, int turns) {
    ActionMeter meter = new ActionMeter(rate);
    int actions = 0;
    for (int turn = 0; turn < turns; turn++) {
      meter.grantTurn();
      while (meter.canAct()) {
        meter.spendAction();
        actions++;
      }
    }
    return actions;
  }

  @Test
  void startsWithNothingBanked() {
    ActionMeter meter = new ActionMeter(ActionRate.NORMAL);
    assertEquals(0, meter.stored());
    assertFalse(meter.canAct());
  }

  @Test
  @DisplayName("a normal pace is exactly one action per turn, as every enemy used to have")
  void normalPaceActsOncePerTurn() {
    assertEquals(10, actionsOver(ActionRate.NORMAL, 10));
  }

  @Test
  void slowPaceActsOnEveryOtherTurn() {
    assertEquals(5, actionsOver(ActionRate.SLOW, 10));
  }

  @Test
  void fastPaceActsHalfAgainAsOften() {
    assertEquals(15, actionsOver(ActionRate.FAST, 10));
  }

  @Test
  @DisplayName("a slow piece really does sit out its first turn rather than acting late")
  void slowPaceSitsOutAlternateTurns() {
    ActionMeter meter = new ActionMeter(ActionRate.SLOW);
    meter.grantTurn();
    assertFalse(meter.canAct(), "Half a turn's energy is not enough to act");
    meter.grantTurn();
    assertTrue(meter.canAct());
  }

  @Test
  @DisplayName("a fast piece takes a second step on alternate turns, not two every turn")
  void fastPaceEarnsAnOccasionalSecondAction() {
    ActionMeter meter = new ActionMeter(ActionRate.FAST);

    meter.grantTurn();
    assertTrue(meter.canAct());
    meter.spendAction();
    assertFalse(meter.canAct(), "One and a half turns' energy affords one action, not two");

    meter.grantTurn();
    assertTrue(meter.canAct());
    meter.spendAction();
    assertTrue(meter.canAct(), "The saved half now pays for a second action");
    meter.spendAction();
    assertFalse(meter.canAct());
  }

  @Test
  void unspentEnergyCarriesOver() {
    ActionMeter meter = new ActionMeter(ActionRate.SLOW);
    meter.grantTurn();
    assertEquals(50, meter.stored());
    meter.grantTurn();
    assertEquals(100, meter.stored());
  }

  @Test
  void spendingLeavesTheRemainder() {
    ActionMeter meter = new ActionMeter(ActionRate.FAST);
    meter.grantTurn();
    meter.spendAction();
    assertEquals(50, meter.stored());
  }

  @Test
  @DisplayName("acting without the energy to pay for it is a programming error, not a free move")
  void refusesToActWithoutEnoughEnergy() {
    ActionMeter meter = new ActionMeter(ActionRate.NORMAL);
    assertThrows(IllegalStateException.class, meter::spendAction);
  }

  @Test
  void remembersItsRate() {
    assertEquals(ActionRate.FAST, new ActionMeter(ActionRate.FAST).rate());
  }

  @Test
  void ratesAreOrderedSlowestFirst() {
    assertTrue(ActionRate.SLOW.energyPerTurn() < ActionRate.NORMAL.energyPerTurn());
    assertTrue(ActionRate.NORMAL.energyPerTurn() < ActionRate.FAST.energyPerTurn());
    assertEquals(ActionMeter.COST_PER_ACTION, ActionRate.NORMAL.energyPerTurn());
  }
}
