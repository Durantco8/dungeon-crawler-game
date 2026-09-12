package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.PlayerMove;
import com.durantco.dungeon.persistence.InMemoryHighScoreStore;
import com.durantco.dungeon.support.CountingObserver;
import com.durantco.dungeon.support.GameStates;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class GameSessionTest {

  private static GameSession started(long seed) {
    GameSession session = new GameSession(GameSetup.standard(seed));
    session.startGame();
    return session;
  }

  /** Plays a seeded run of inputs, returning the state after each one. */
  private static List<String> playRecording(GameSession session, int moves, long inputSeed) {
    Random inputs = new Random(inputSeed);
    List<String> states = new ArrayList<>();
    for (int i = 0; i < moves; i++) {
      switch (inputs.nextInt(4)) {
        case 0 -> session.moveUp();
        case 1 -> session.moveDown();
        case 2 -> session.moveLeft();
        default -> session.moveRight();
      }
      states.add(GameStates.render(session));
    }
    return states;
  }

  @Nested
  class Recording {

    @Test
    void recordsEachMoveInOrder() {
      GameSession session = started(1L);
      session.moveRight();
      session.moveUp();
      assertEquals(List.of(PlayerMove.RIGHT, PlayerMove.UP), session.recording().moves());
    }

    @Test
    @DisplayName("difficulty is part of the setup, not an input")
    void difficultyChangesTheSetup() {
      GameSession session = new GameSession(GameSetup.standard(1L));
      session.setHardMode(true);

      assertEquals(Difficulty.HARD, session.recording().setup().difficulty());
      assertEquals(0, session.recording().moveCount());
      assertTrue(session.isHardMode());
    }

    @Test
    @DisplayName("starting a game clears the play and rebuilds from the seed")
    void startingAGameRewinds() {
      GameSession session = started(3L);
      playRecording(session, 10, 1L);
      String afterPlay = GameStates.render(session);

      session.startGame();
      assertTrue(session.recording().isAtStart());
      assertNotEquals(afterPlay, GameStates.render(session));
    }

    @Test
    @DisplayName("a second game on one session matches a fresh session with the same seed")
    void restartingIsNotAffectedByThePreviousGame() {
      GameSession reused = started(21L);
      playRecording(reused, 25, 4L);
      reused.startGame();

      GameSession fresh = started(21L);
      assertEquals(GameStates.render(fresh), GameStates.render(reused));
    }
  }

  @Nested
  class Undoing {

    @Test
    void thereIsNothingToUndoAtTheStart() {
      GameSession session = started(1L);
      assertFalse(session.canUndo());
    }

    @Test
    void undoingAtTheStartChangesNothing() {
      GameSession session = started(1L);
      String before = GameStates.render(session);
      session.undo();
      assertEquals(before, GameStates.render(session));
    }

    @Test
    @DisplayName("undo returns the game to exactly the state before the last move")
    void undoRestoresThePreviousState() {
      GameSession session = started(7L);
      List<String> states = playRecording(session, 20, 9L);

      session.undo();
      assertEquals(states.get(18), GameStates.render(session));
      assertEquals(19, session.recording().moveCount());
    }

    @Test
    @DisplayName("undo can be repeated all the way back to the opening position")
    void undoWalksBackThroughTheWholeGame() {
      GameSession session = started(11L);
      List<String> states = playRecording(session, 30, 2L);

      for (int remaining = 29; remaining >= 0; remaining--) {
        session.undo();
        if (remaining > 0) {
          assertEquals(
              states.get(remaining - 1),
              GameStates.render(session),
              "Undoing to " + remaining + " moves gave the wrong state");
        }
      }
      assertFalse(session.canUndo(), "Everything has been taken back");
    }

    @Test
    @DisplayName("undo restores enemy state a snapshot would have had to remember")
    void undoRestoresHiddenEnemyState() {
      // Hard mode enemies remember where they last saw the hero, hold a patrol position, and bank
      // energy. None of that is on the board, so this is the state an inverse-operation undo would
      // most easily get wrong. Replaying rebuilds it because it rebuilds everything.
      GameSession session =
          new GameSession(GameSetup.standard(404L).withDifficulty(Difficulty.HARD));
      session.startGame();
      List<String> states = playRecording(session, 40, 6L);

      session.undo();
      String afterUndo = GameStates.render(session);
      assertEquals(states.get(38), afterUndo);

      // Redoing the same move must land back where it did the first time, which it only can if the
      // enemies' memories and meters were restored too.
      session.moveRight();
      GameSession comparison =
          new GameSession(GameSetup.standard(404L).withDifficulty(Difficulty.HARD));
      comparison.startGame();
      playRecording(comparison, 39, 6L);
      comparison.moveRight();
      assertEquals(GameStates.render(comparison), GameStates.render(session));
    }

    @Test
    void undoingThenPlayingOnRecordsTheNewMove() {
      GameSession session = started(5L);
      session.moveRight();
      session.moveDown();
      session.undo();
      session.moveLeft();

      assertEquals(List.of(PlayerMove.RIGHT, PlayerMove.LEFT), session.recording().moves());
    }

    @Test
    void undoTellsObserversTheGameChanged() {
      GameSession session = started(1L);
      session.moveRight();

      CountingObserver observer = new CountingObserver();
      session.addObserver(observer);
      session.undo();
      assertEquals(1, observer.updates());
    }

    @Test
    @DisplayName("undoing fires one update, not one per replayed move")
    void undoDoesNotNotifyOncePerReplayedMove() {
      GameSession session = started(1L);
      playRecording(session, 50, 1L);

      CountingObserver observer = new CountingObserver();
      session.addObserver(observer);
      session.undo();
      assertEquals(1, observer.updates(), "A replay of fifty moves must not redraw fifty times");
    }
  }

  @Nested
  class Observing {

    @Test
    void aMoveReachesObserversThroughTheSession() {
      GameSession session = started(1L);
      CountingObserver observer = new CountingObserver();
      session.addObserver(observer);

      session.moveRight();
      assertEquals(1, observer.updates());
    }

    @Test
    @DisplayName("observers keep working after undo replaces the model underneath")
    void observersSurviveARebuild() {
      GameSession session = started(1L);
      session.moveRight();
      session.moveDown();

      CountingObserver observer = new CountingObserver();
      session.addObserver(observer);
      session.undo();
      int afterUndo = observer.updates();

      session.moveUp();
      assertEquals(afterUndo + 1, observer.updates(), "The session must still forward updates");
    }
  }

  @Nested
  class Resuming {

    @Test
    @DisplayName("a loaded game carries on from where it was saved")
    void resumingARecordingRestoresTheGame() {
      GameSession original = started(88L);
      List<String> states = playRecording(original, 30, 3L);
      GameRecording saved = original.recording();

      GameSession resumed = new GameSession(GameSetup.standard(1L), new InMemoryHighScoreStore());
      resumed.resume(saved);

      assertEquals(states.get(29), GameStates.render(resumed));
      assertEquals(saved, resumed.recording());
      assertTrue(resumed.canUndo());
    }

    @Test
    void aResumedGameCanBeUndoneAndPlayedOn() {
      GameSession original = started(12L);
      List<String> states = playRecording(original, 15, 8L);

      GameSession resumed = new GameSession(GameSetup.standard(0L));
      resumed.resume(original.recording());
      resumed.undo();

      assertEquals(states.get(13), GameStates.render(resumed));
    }
  }
}
