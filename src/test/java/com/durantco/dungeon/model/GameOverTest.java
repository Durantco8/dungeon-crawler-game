package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.support.FakeBoard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameOverTest {

  @Test
  void aNewModelIsNotInProgress() {
    assertEquals(Model.STATUS.END_GAME, new ModelImpl(new FakeBoard()).getStatus());
  }

  @Test
  void startingAGamePutsItInProgress() {
    Model model = new ModelImpl(new FakeBoard());
    model.startGame();

    assertEquals(Model.STATUS.IN_PROGRESS, model.getStatus());
  }

  @Test
  void aFatalCollisionEndsTheGame() {
    FakeBoard board = new FakeBoard().script(CollisionResult.gameOver());
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(Model.STATUS.END_GAME, model.getStatus());
  }

  @Test
  void endGameEndsTheGame() {
    Model model = new ModelImpl(new FakeBoard());
    model.startGame();
    model.endGame();

    assertEquals(Model.STATUS.END_GAME, model.getStatus());
  }

  @Test
  @DisplayName("the points from the fatal move still count")
  void keepsTheScoreFromTheFinalMove() {
    FakeBoard board = new FakeBoard().script(new CollisionResult(5, CollisionResult.Result.GAME_OVER));
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(5, model.getCurScore());
  }

  @Test
  void highScoreStartsAtZero() {
    assertEquals(0, new ModelImpl(new FakeBoard()).getHighScore());
  }

  @Test
  void endingAGameRecordsANewHighScore() {
    FakeBoard board = new FakeBoard().script(CollisionResult.scoring(30));
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();
    model.endGame();

    assertEquals(30, model.getHighScore());
  }

  @Test
  @DisplayName("a worse game does not overwrite the high score")
  void keepsTheBestScoreAcrossGames() {
    FakeBoard board =
        new FakeBoard().script(CollisionResult.scoring(30), CollisionResult.scoring(10));
    Model model = new ModelImpl(board);

    model.startGame();
    model.moveUp();
    model.endGame();

    model.startGame();
    model.moveUp();
    model.endGame();

    assertEquals(30, model.getHighScore());
    assertEquals(10, model.getCurScore());
  }

  @Test
  void anImprovedScoreReplacesTheHighScore() {
    FakeBoard board =
        new FakeBoard().script(CollisionResult.scoring(10), CollisionResult.scoring(40));
    Model model = new ModelImpl(board);

    model.startGame();
    model.moveUp();
    model.endGame();

    model.startGame();
    model.moveUp();
    model.endGame();

    assertEquals(40, model.getHighScore());
  }

  @Test
  @DisplayName("a fatal move records the high score without a separate endGame call")
  void recordsTheHighScoreOnAFatalMove() {
    FakeBoard board =
        new FakeBoard().script(new CollisionResult(20, CollisionResult.Result.GAME_OVER));
    Model model = new ModelImpl(board);
    model.startGame();
    model.moveUp();

    assertEquals(20, model.getHighScore());
  }
}
