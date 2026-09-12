package com.durantco.dungeon.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordingControllerTest {

  /** A controller that only notes what it was asked to do. */
  private static final class SpyController implements Controller {
    private final List<String> calls = new ArrayList<>();

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
    public void setHardMode(boolean hardMode) {
      calls.add("hard:" + hardMode);
    }
  }

  private static final GameSetup SETUP = GameSetup.standard(3L);

  @Test
  void recordsNothingBeforeAnythingHappens() {
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    assertTrue(controller.recording().isAtStart());
    assertEquals(SETUP, controller.recording().setup());
  }

  @Test
  void recordsEachMoveInOrder() {
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    controller.moveRight();
    controller.moveUp();
    controller.moveLeft();
    controller.moveDown();

    assertEquals(
        List.of(PlayerMove.RIGHT, PlayerMove.UP, PlayerMove.LEFT, PlayerMove.DOWN),
        controller.recording().moves());
  }

  @Test
  @DisplayName("every input still reaches the real controller")
  void passesEveryInputThrough() {
    SpyController spy = new SpyController();
    RecordingController controller = new RecordingController(spy, SETUP);
    controller.moveRight();
    controller.moveUp();
    controller.startGame();
    controller.setHardMode(true);

    assertEquals(List.of("right", "up", "start", "hard:true"), spy.calls);
  }

  @Test
  @DisplayName("starting a game clears the previous play but keeps the setup")
  void startingAGameRewindsTheRecording() {
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    controller.moveRight();
    controller.moveRight();
    controller.startGame();

    assertTrue(controller.recording().isAtStart());
    assertEquals(SETUP, controller.recording().setup());
  }

  @Test
  @DisplayName("difficulty is recorded as part of the setup, not as an input")
  void difficultyChangesTheSetup() {
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    controller.setHardMode(true);

    assertEquals(Difficulty.HARD, controller.recording().setup().difficulty());
    assertEquals(0, controller.recording().moveCount(), "Difficulty is not a move");

    controller.setHardMode(false);
    assertEquals(Difficulty.EASY, controller.recording().setup().difficulty());
  }

  @Test
  @DisplayName("a refused move is recorded like any other, since replay must match move for move")
  void recordsMovesWithoutJudgingThem() {
    // The spy accepts everything, so this stands in for a move the board would refuse: the recorder
    // has no idea either way, which is the point.
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    for (int i = 0; i < 5; i++) {
      controller.moveUp();
    }
    assertEquals(5, controller.recording().moveCount());
  }

  @Test
  void theRecordingCanBeReplacedForLoadingOrUndo() {
    RecordingController controller = new RecordingController(new SpyController(), SETUP);
    controller.moveUp();
    controller.moveDown();

    controller.setRecording(controller.recording().withoutLastMove());
    assertEquals(List.of(PlayerMove.UP), controller.recording().moves());

    GameRecording loaded = GameRecording.startingFrom(SETUP.withSeed(99L)).plus(PlayerMove.LEFT);
    controller.setRecording(loaded);
    assertEquals(loaded, controller.recording());
  }
}
