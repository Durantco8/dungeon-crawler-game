package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.support.CountingObserver;
import com.durantco.dungeon.support.FakeBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The view repaints from these notifications, so the model must emit one for every state change. */
class ObserverTest {

  @Test
  void startingAGameNotifiesOnce() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.addObserver(observer);
    model.startGame();

    assertEquals(1, observer.updates());
  }

  @Test
  void everyMoveNotifiesOnce() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.addObserver(observer);
    model.moveUp();
    model.moveDown();
    model.moveLeft();
    model.moveRight();

    assertEquals(4, observer.updates());
  }

  @Test
  @DisplayName("a move that changes nothing still notifies, so the view stays authoritative")
  void anUneventfulMoveStillNotifies() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard().script(CollisionResult.free()));
    model.addObserver(observer);
    model.moveUp();

    assertEquals(1, observer.updates());
  }

  @Test
  void advancingALevelNotifiesOnce() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard().script(CollisionResult.nextLevel()));
    model.startGame();
    model.addObserver(observer);
    model.moveUp();

    assertEquals(1, observer.updates());
  }

  @Test
  void changingDifficultyNotifies() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.addObserver(observer);
    model.setHardMode(true);

    assertEquals(1, observer.updates());
  }

  @Test
  void endingAGameNotifies() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.addObserver(observer);
    model.endGame();

    assertEquals(1, observer.updates());
  }

  @Test
  @DisplayName("a fatal move notifies twice, once from endGame and once from the move itself")
  void aFatalMoveNotifiesTwice() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard().script(CollisionResult.gameOver()));
    model.startGame();
    model.addObserver(observer);
    model.moveUp();

    // Redundant but harmless: the view simply repaints the same end state twice.
    assertEquals(2, observer.updates());
  }

  @Test
  void everyRegisteredObserverIsNotified() {
    CountingObserver first = new CountingObserver();
    CountingObserver second = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.addObserver(first);
    model.addObserver(second);
    model.startGame();

    assertEquals(1, first.updates());
    assertEquals(1, second.updates());
  }

  @Test
  void aModelWithNoObserversStillWorks() {
    Model model = new ModelImpl(new FakeBoard().script(CollisionResult.scoring(5)));
    model.startGame();
    model.moveUp();

    assertEquals(5, model.getCurScore());
  }

  @Test
  @DisplayName("an observer added mid-game only hears about later changes")
  void observersOnlyHearAboutChangesAfterTheyRegister() {
    CountingObserver observer = new CountingObserver();
    Model model = new ModelImpl(new FakeBoard());
    model.startGame();
    model.addObserver(observer);
    model.moveUp();

    assertEquals(1, observer.updates());
  }
}
