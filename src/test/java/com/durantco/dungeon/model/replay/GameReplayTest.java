package com.durantco.dungeon.model.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.controller.ControllerImpl;
import com.durantco.dungeon.controller.RecordingController;
import com.durantco.dungeon.model.GameFactory;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.support.GameStates;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameReplayTest {

  /** Plays a pseudo-random but seeded session through a recording controller, noting every state. */
  private record Session(GameRecording recording, List<String> statesAfterEachMove) {}

  private static Session play(GameSetup setup, int moves, long inputSeed) {
    Model model = GameFactory.create(setup);
    Controller real = new ControllerImpl(model);
    RecordingController recorder = new RecordingController(real, setup);
    recorder.startGame();

    Random inputs = new Random(inputSeed);
    List<String> states = new ArrayList<>();
    for (int i = 0; i < moves; i++) {
      switch (inputs.nextInt(4)) {
        case 0 -> recorder.moveUp();
        case 1 -> recorder.moveDown();
        case 2 -> recorder.moveLeft();
        default -> recorder.moveRight();
      }
      states.add(GameStates.render(model));
    }
    return new Session(recorder.recording(), states);
  }

  @Test
  @DisplayName("a recorded session replays to exactly the same final state")
  void aRecordedSessionReplaysExactly() {
    Session session = play(GameSetup.standard(2024L), 120, 7L);
    Model replayed = GameReplay.replay(session.recording());

    assertEquals(
        session.statesAfterEachMove().get(session.statesAfterEachMove().size() - 1),
        GameStates.render(replayed));
  }

  @Test
  @DisplayName("a replay matches the original move for move, not just at the end")
  void aReplayMatchesAtEveryStep() {
    // Matching only at the end would pass even if the replay diverged and happened to converge again.
    Session session = play(GameSetup.standard(99L), 80, 3L);

    for (int upTo = 0; upTo <= session.recording().moveCount(); upTo++) {
      Model partial = GameReplay.replay(session.recording(), upTo);
      if (upTo > 0) {
        assertEquals(
            session.statesAfterEachMove().get(upTo - 1),
            GameStates.render(partial),
            "The replay diverged after " + upTo + " moves");
      }
    }
  }

  @Test
  @DisplayName("hard mode replays exactly too, archetypes and paces included")
  void aHardSessionReplaysExactly() {
    Session session =
        play(GameSetup.standard(555L).withDifficulty(Difficulty.HARD), 100, 11L);
    Model replayed = GameReplay.replay(session.recording());

    assertEquals(
        session.statesAfterEachMove().get(session.statesAfterEachMove().size() - 1),
        GameStates.render(replayed));
  }

  @Test
  @DisplayName("many different sessions all replay exactly")
  void everySessionReplaysExactly() {
    for (int seed = 0; seed < 25; seed++) {
      GameSetup setup = GameSetup.standard(seed);
      if (seed % 2 == 0) {
        setup = setup.withDifficulty(Difficulty.HARD);
      }
      Session session = play(setup, 60, seed * 31L);
      assertEquals(
          session.statesAfterEachMove().get(session.statesAfterEachMove().size() - 1),
          GameStates.render(GameReplay.replay(session.recording())),
          "Seed " + seed + " did not replay exactly");
    }
  }

  @Test
  @DisplayName("a replay carries the game past level transitions intact")
  void replaysThroughLevelChanges() {
    // Long enough that at least one session is very likely to finish a level or lose the hero, which is
    // where a replay is most likely to drift if anything outside the seed influenced the game.
    Session session = play(GameSetup.standard(31L), 400, 5L);
    Model replayed = GameReplay.replay(session.recording());

    assertEquals(
        session.statesAfterEachMove().get(session.statesAfterEachMove().size() - 1),
        GameStates.render(replayed));
    assertEquals(400, session.recording().moveCount());
  }

  @Test
  void replayingNoMovesGivesTheOpeningPosition() {
    GameRecording recording = GameRecording.startingFrom(GameSetup.standard(8L));
    Model fromReplay = GameReplay.replay(recording);

    Model fresh = GameFactory.create(GameSetup.standard(8L));
    fresh.startGame();
    assertEquals(GameStates.render(fresh), GameStates.render(fromReplay));
  }

  @Test
  void replayingPartOfAGameStopsWhereAsked() {
    Session session = play(GameSetup.standard(4L), 30, 2L);
    Model partial = GameReplay.replay(session.recording(), 10);

    assertEquals(session.statesAfterEachMove().get(9), GameStates.render(partial));
    assertNotEquals(session.statesAfterEachMove().get(29), GameStates.render(partial));
  }

  @Test
  void refusesToReplayMoreMovesThanWereRecorded() {
    GameRecording recording =
        GameRecording.startingFrom(GameSetup.standard(1L)).plus(PlayerMove.UP);
    assertThrows(IllegalArgumentException.class, () -> GameReplay.replay(recording, 2));
    assertThrows(IllegalArgumentException.class, () -> GameReplay.replay(recording, -1));
  }

  @Test
  @DisplayName("two different seeds do not replay to the same state, so the test could fail")
  void theAssertionIsCapableOfFailing() {
    Session first = play(GameSetup.standard(1L), 50, 1L);
    Session second = play(GameSetup.standard(2L), 50, 1L);
    assertNotEquals(
        GameStates.render(GameReplay.replay(first.recording())),
        GameStates.render(GameReplay.replay(second.recording())));
  }

  @Test
  void theReplayedGameKnowsItsDifficulty() {
    GameRecording hard =
        GameRecording.startingFrom(GameSetup.standard(1L).withDifficulty(Difficulty.HARD));
    assertTrue(GameReplay.replay(hard).isHardMode());

    GameRecording easy = GameRecording.startingFrom(GameSetup.standard(1L));
    assertTrue(!GameReplay.replay(easy).isHardMode());
  }
}
