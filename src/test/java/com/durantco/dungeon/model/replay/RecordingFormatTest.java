package com.durantco.dungeon.model.replay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.GeneratorSpec;
import com.durantco.dungeon.support.GameStates;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RecordingFormatTest {

  private static GameRecording sample() {
    return GameRecording.startingFrom(GameSetup.standard(20260912L).withDifficulty(Difficulty.HARD))
        .plus(PlayerMove.UP)
        .plus(PlayerMove.RIGHT)
        .plus(PlayerMove.RIGHT)
        .plus(PlayerMove.DOWN);
  }

  @Test
  @DisplayName("a recording survives a round trip through text")
  void roundTripsExactly() {
    GameRecording original = sample();
    assertEquals(original, RecordingFormat.read(RecordingFormat.write(original)));
  }

  @Test
  void roundTripsAGameWithNoMovesYet() {
    GameRecording fresh = GameRecording.startingFrom(GameSetup.standard(5L));
    assertEquals(fresh, RecordingFormat.read(RecordingFormat.write(fresh)));
  }

  @Test
  void roundTripsEveryGenerator() {
    for (GeneratorSpec spec :
        List.of(new GeneratorSpec.Bsp(4, 5), new GeneratorSpec.Scatter(7))) {
      GameRecording recording =
          GameRecording.startingFrom(
              new GameSetup(1L, 20, 12, Difficulty.EASY, spec));
      assertEquals(recording, RecordingFormat.read(RecordingFormat.write(recording)));
    }
  }

  @Test
  void roundTripsEveryDifficulty() {
    for (Difficulty difficulty : Difficulty.values()) {
      GameRecording recording =
          GameRecording.startingFrom(GameSetup.standard(1L).withDifficulty(difficulty));
      assertEquals(difficulty, RecordingFormat.read(RecordingFormat.write(recording)).setup().difficulty());
    }
  }

  @Test
  void roundTripsANegativeSeed() {
    GameRecording recording = GameRecording.startingFrom(GameSetup.standard(-8_675_309L));
    assertEquals(-8_675_309L, RecordingFormat.read(RecordingFormat.write(recording)).setup().seed());
  }

  @Test
  @DisplayName("a long session is still small and readable")
  void staysCompact() {
    GameRecording recording = GameRecording.startingFrom(GameSetup.standard(1L));
    for (int i = 0; i < 1000; i++) {
      recording = recording.plus(PlayerMove.values()[i % 4]);
    }
    String text = RecordingFormat.write(recording);

    assertEquals(recording, RecordingFormat.read(text));
    assertTrue(text.length() < 1200, "A thousand-move game should be about a kilobyte");
  }

  @Test
  @DisplayName("the written form is human readable, because a save file is also a replay file")
  void isReadableByEye() {
    String text = RecordingFormat.write(sample());
    assertTrue(text.startsWith("dungeon-crawler-save 1\n"));
    assertTrue(text.contains("\nseed 20260912\n"));
    assertTrue(text.contains("\nboard 28 18\n"));
    assertTrue(text.contains("\ndifficulty HARD\n"));
    assertTrue(text.contains("\ngenerator bsp 3 4\n"));
    assertTrue(text.contains("\nmoves URRD\n"));
  }

  @Test
  @DisplayName("a loaded game replays to the same state as the one that was saved")
  void aLoadedGamePlaysOutTheSame() {
    GameRecording original =
        GameRecording.startingFrom(GameSetup.standard(77L).withDifficulty(Difficulty.HARD));
    for (int i = 0; i < 60; i++) {
      original = original.plus(PlayerMove.values()[(i * 3) % 4]);
    }
    GameRecording loaded = RecordingFormat.read(RecordingFormat.write(original));

    assertEquals(
        GameStates.render(GameReplay.replay(original)),
        GameStates.render(GameReplay.replay(loaded)));
  }

  @Test
  void rejectsTextThatIsNotASavedGame() {
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(""));
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read("hello"));
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read("some-other-game 1\n"));
  }

  @Test
  @DisplayName("a save from a future version is refused rather than half understood")
  void rejectsAnUnknownVersion() {
    String text = RecordingFormat.write(sample()).replace("dungeon-crawler-save 1", "dungeon-crawler-save 2");
    IllegalArgumentException failure =
        assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(text));
    assertTrue(failure.getMessage().contains("version"));
  }

  @Test
  void rejectsAnIncompleteSave() {
    assertThrows(
        IllegalArgumentException.class,
        () -> RecordingFormat.read("dungeon-crawler-save 1\nseed 1\n"));
  }

  @Test
  void rejectsUnreadableNumbers() {
    String text = RecordingFormat.write(sample()).replace("seed 20260912", "seed banana");
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(text));
  }

  @Test
  void rejectsAnUnknownGeneratorOrDifficulty() {
    String generator = RecordingFormat.write(sample()).replace("generator bsp 3 4", "generator spiral 3");
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(generator));

    String difficulty = RecordingFormat.write(sample()).replace("difficulty HARD", "difficulty BRUTAL");
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(difficulty));
  }

  @Test
  void rejectsAnUnknownEntry() {
    String text = RecordingFormat.write(sample()) + "cheat true\n";
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(text));
  }

  @Test
  void rejectsAnUnknownMove() {
    String text = RecordingFormat.write(sample()).replace("moves URRD", "moves URXD");
    assertThrows(IllegalArgumentException.class, () -> RecordingFormat.read(text));
  }

  @Test
  @DisplayName("trailing blank lines and stray whitespace do not break loading")
  void toleratesUntidyWhitespace() {
    String text = RecordingFormat.write(sample()) + "\n\n";
    assertEquals(sample(), RecordingFormat.read(text));
  }
}
