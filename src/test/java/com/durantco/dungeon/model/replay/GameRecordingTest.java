package com.durantco.dungeon.model.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameRecordingTest {

  private static final GameSetup SETUP = GameSetup.standard(1L);

  @Test
  void aNewRecordingHasNoMoves() {
    GameRecording recording = GameRecording.startingFrom(SETUP);
    assertEquals(0, recording.moveCount());
    assertTrue(recording.isAtStart());
    assertEquals(SETUP, recording.setup());
  }

  @Test
  void appendingKeepsTheOrderOfPlay() {
    GameRecording recording =
        GameRecording.startingFrom(SETUP).plus(PlayerMove.UP).plus(PlayerMove.RIGHT);
    assertEquals(List.of(PlayerMove.UP, PlayerMove.RIGHT), recording.moves());
    assertEquals(2, recording.moveCount());
    assertFalse(recording.isAtStart());
  }

  @Test
  @DisplayName("appending leaves the original recording alone, which is what makes undo safe")
  void appendingDoesNotMutate() {
    GameRecording original = GameRecording.startingFrom(SETUP).plus(PlayerMove.UP);
    GameRecording extended = original.plus(PlayerMove.DOWN);

    assertEquals(1, original.moveCount());
    assertEquals(2, extended.moveCount());
    assertNotSame(original, extended);
  }

  @Test
  void droppingTheLastMoveGivesTheEarlierGame() {
    GameRecording recording =
        GameRecording.startingFrom(SETUP).plus(PlayerMove.UP).plus(PlayerMove.DOWN);
    assertEquals(List.of(PlayerMove.UP), recording.withoutLastMove().moves());
  }

  @Test
  @DisplayName("undoing past the start is not an error, there is simply nothing to undo")
  void droppingAMoveFromTheStartChangesNothing() {
    GameRecording start = GameRecording.startingFrom(SETUP);
    assertEquals(start, start.withoutLastMove());
  }

  @Test
  void rewindingKeepsTheSetupAndDropsThePlay() {
    GameRecording recording =
        GameRecording.startingFrom(SETUP).plus(PlayerMove.UP).plus(PlayerMove.LEFT);
    GameRecording rewound = recording.rewound();

    assertEquals(0, rewound.moveCount());
    assertEquals(SETUP, rewound.setup());
  }

  @Test
  void theSetupCanBeChangedWithoutLosingThePlay() {
    GameRecording recording = GameRecording.startingFrom(SETUP).plus(PlayerMove.UP);
    GameRecording harder = recording.withSetup(SETUP.withDifficulty(Difficulty.HARD));

    assertEquals(Difficulty.HARD, harder.setup().difficulty());
    assertEquals(List.of(PlayerMove.UP), harder.moves());
    assertEquals(Difficulty.EASY, recording.setup().difficulty(), "The original is untouched");
  }

  @Test
  @DisplayName("the move list is copied, so a caller cannot alter a recording after the fact")
  void isImmutableAgainstItsSourceList() {
    List<PlayerMove> source = new ArrayList<>(List.of(PlayerMove.UP));
    GameRecording recording = new GameRecording(SETUP, source);
    source.add(PlayerMove.DOWN);

    assertEquals(1, recording.moveCount());
  }

  @Test
  void theMoveListCannotBeModifiedThroughTheRecording() {
    GameRecording recording = GameRecording.startingFrom(SETUP).plus(PlayerMove.UP);
    assertThrows(
        UnsupportedOperationException.class, () -> recording.moves().add(PlayerMove.DOWN));
  }

  @Test
  void recordingsOfTheSamePlayAreEqual() {
    assertEquals(
        GameRecording.startingFrom(SETUP).plus(PlayerMove.UP),
        GameRecording.startingFrom(SETUP).plus(PlayerMove.UP));
  }

  @Test
  void refusesARecordingWithNoSetup() {
    assertThrows(IllegalArgumentException.class, () -> new GameRecording(null, List.of()));
  }
}
