package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.board.LevelSpec;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.support.FakeBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LevelProgressionTest {

  @Test
  void noLevelBeforeTheGameStarts() {
    assertEquals(0, new ModelImpl(new FakeBoard()).getLevel());
  }

  @Test
  void startingAGameBeginsAtLevelOne() {
    Model model = new ModelImpl(new FakeBoard());
    model.startGame();

    assertEquals(1, model.getLevel());
  }

  @Test
  void startingAGameBuildsTheFirstLevel() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).startGame();

    assertEquals(1, board.initCalls().size());
    assertEquals(LevelSpec.forLevel(1), board.initCalls().get(0));
  }

  @Test
  void reachingTheExitAdvancesTheLevel() {
    FakeBoard board = new FakeBoard().script(CollisionResult.nextLevel());
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(2, model.getLevel());
  }

  @Test
  @DisplayName("advancing rebuilds the board for the new level")
  void reachingTheExitBuildsTheNextLevel() {
    FakeBoard board = new FakeBoard().script(CollisionResult.nextLevel());
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(2, board.initCalls().size());
    assertEquals(LevelSpec.forLevel(2), board.initCalls().get(1));
  }

  @Test
  @DisplayName("each level is built with more enemies than the last")
  void difficultyGrowsWithEachLevel() {
    FakeBoard board =
        new FakeBoard()
            .script(
                CollisionResult.nextLevel(), CollisionResult.nextLevel(), CollisionResult.nextLevel());
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();
    model.moveUp();
    model.moveUp();

    assertEquals(4, model.getLevel());
    assertEquals(4, board.initCalls().size());
    for (int i = 1; i < board.initCalls().size(); i++) {
      assertTrue(
          board.initCalls().get(i).enemies() > board.initCalls().get(i - 1).enemies(),
          "Level " + (i + 1) + " should have more enemies than level " + i);
    }
  }

  @Test
  void anUneventfulMoveLeavesTheLevelAlone() {
    FakeBoard board = new FakeBoard().script(CollisionResult.free());
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(1, model.getLevel());
    assertEquals(1, board.initCalls().size());
  }

  @Test
  @DisplayName("a level too big for the board ends the game instead of throwing")
  void endsTheGameWhenTheNextLevelWillNotFit() {
    FakeBoard board = new FakeBoard().rejectingLevels().script(CollisionResult.nextLevel());
    Model model = new ModelImpl(board);
    model.startGame();

    assertEquals(Model.STATUS.END_GAME, model.getStatus());
    assertEquals(0, board.initCalls().size(), "A level that does not fit should never be built");
  }

  @Test
  void advancingIntoALevelThatWillNotFitEndsTheGame() {
    FakeBoard board = new FakeBoard().script(CollisionResult.nextLevel());
    Model model = new ModelImpl(board);
    model.startGame();
    board.rejectingLevels();
    model.moveUp();

    assertEquals(Model.STATUS.END_GAME, model.getStatus());
  }
}
