package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.support.FakeBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScoringTest {

  @Test
  void startsAtZero() {
    assertEquals(0, new ModelImpl(new FakeBoard()).getCurScore());
  }

  @Test
  void addsThePointsFromAMove() {
    FakeBoard board = new FakeBoard().script(CollisionResult.scoring(5));
    Model model = new ModelImpl(board);
    model.moveUp();

    assertEquals(5, model.getCurScore());
  }

  @Test
  void subtractsNegativePoints() {
    FakeBoard board = new FakeBoard().script(CollisionResult.scoring(-5));
    Model model = new ModelImpl(board);
    model.moveUp();

    assertEquals(-5, model.getCurScore());
  }

  @Test
  void accumulatesAcrossMoves() {
    FakeBoard board =
        new FakeBoard()
            .script(
                CollisionResult.scoring(5),
                CollisionResult.scoring(5),
                CollisionResult.scoring(-5));
    Model model = new ModelImpl(board);
    model.moveUp();
    model.moveUp();
    model.moveUp();

    assertEquals(5, model.getCurScore());
  }

  @Test
  @DisplayName("a score can go negative")
  void allowsANegativeRunningTotal() {
    FakeBoard board =
        new FakeBoard().script(CollisionResult.scoring(5), CollisionResult.scoring(-15));
    Model model = new ModelImpl(board);
    model.moveUp();
    model.moveUp();

    assertEquals(-10, model.getCurScore());
  }

  @Test
  void anUneventfulMoveScoresNothing() {
    FakeBoard board = new FakeBoard().script(CollisionResult.free());
    Model model = new ModelImpl(board);
    model.moveUp();

    assertEquals(0, model.getCurScore());
  }

  @Test
  void startingAGameClearsTheScore() {
    FakeBoard board = new FakeBoard().script(CollisionResult.scoring(25));
    Model model = new ModelImpl(board);
    model.moveUp();
    model.startGame();

    assertEquals(0, model.getCurScore());
  }
}
