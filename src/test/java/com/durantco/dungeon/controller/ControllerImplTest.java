package com.durantco.dungeon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.model.GameSession;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.Observer;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The controller is a pass-through, and its whole job is to pass the right thing through. These tests exist to
 * catch a method wired to the wrong one, which is the only mistake a delegating class can make.
 */
class ControllerImplTest {

  /** A model that records which of its methods were called. */
  private static final class RecordingModel implements Model {
    private final List<String> calls = new ArrayList<>();

    @Override
    public void setHardMode(boolean hardMode) {
      calls.add("hard:" + hardMode);
    }

    @Override
    public void moveUp() {
      calls.add("up");
    }

    @Override
    public void moveDown() {
      calls.add("down");
    }

    @Override
    public void moveLeft() {
      calls.add("left");
    }

    @Override
    public void moveRight() {
      calls.add("right");
    }

    @Override
    public void startGame() {
      calls.add("start");
    }

    @Override
    public void endGame() {
      calls.add("end");
    }

    @Override
    public void undo() {
      calls.add("undo");
    }

    @Override
    public boolean isHardMode() {
      return false;
    }

    @Override
    public int getWidth() {
      return 0;
    }

    @Override
    public int getHeight() {
      return 0;
    }

    @Override
    public Piece get(Posn p) {
      return null;
    }

    @Override
    public boolean heroCanEnter(Posn p) {
      return true;
    }

    @Override
    public int getCurScore() {
      return 0;
    }

    @Override
    public int getHighScore() {
      return 0;
    }

    @Override
    public int getLevel() {
      return 0;
    }

    @Override
    public STATUS getStatus() {
      return STATUS.END_GAME;
    }

    @Override
    public void addObserver(Observer o) {}
  }

  @Test
  @DisplayName("each input reaches the matching model method, and only that one")
  void everyInputReachesTheRightMethod() {
    RecordingModel model = new RecordingModel();
    Controller controller = new ControllerImpl(model);

    controller.moveUp();
    controller.moveDown();
    controller.moveLeft();
    controller.moveRight();
    controller.startGame();
    controller.undo();
    controller.setHardMode(true);
    controller.setHardMode(false);

    assertEquals(
        List.of("up", "down", "left", "right", "start", "undo", "hard:true", "hard:false"),
        model.calls);
  }

  @Test
  @DisplayName("the controller holds no state of its own, so repeated input just repeats")
  void repeatedInputRepeats() {
    RecordingModel model = new RecordingModel();
    Controller controller = new ControllerImpl(model);
    controller.moveUp();
    controller.moveUp();

    assertEquals(List.of("up", "up"), model.calls);
  }

  @Test
  @DisplayName("driving a real session through the controller takes back a move")
  void undoWorksThroughTheController() {
    GameSession session = new GameSession(GameSetup.standard(6L));
    Controller controller = new ControllerImpl(session);
    controller.startGame();
    controller.moveRight();
    controller.moveDown();
    assertEquals(2, session.recording().moveCount());

    controller.undo();
    assertEquals(1, session.recording().moveCount());
  }
}
