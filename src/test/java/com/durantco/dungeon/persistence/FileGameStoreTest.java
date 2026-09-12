package com.durantco.dungeon.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.PlayerMove;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileGameStoreTest {

  private static GameRecording sample() {
    return GameRecording.startingFrom(GameSetup.standard(42L).withDifficulty(Difficulty.HARD))
        .plus(PlayerMove.UP)
        .plus(PlayerMove.LEFT);
  }

  @Test
  void thereIsNothingToLoadBeforeAnythingIsSaved(@TempDir Path dir) throws IOException {
    GameStore store = new FileGameStore(dir.resolve("save.txt"));
    assertEquals(Optional.empty(), store.load());
  }

  @Test
  void aSavedGameComesBackUnchanged(@TempDir Path dir) throws IOException {
    GameStore store = new FileGameStore(dir.resolve("save.txt"));
    store.save(sample());

    assertEquals(Optional.of(sample()), store.load());
  }

  @Test
  @DisplayName("saving twice keeps the newer game")
  void savingReplacesTheEarlierGame(@TempDir Path dir) throws IOException {
    GameStore store = new FileGameStore(dir.resolve("save.txt"));
    store.save(sample());
    GameRecording later = sample().plus(PlayerMove.DOWN);
    store.save(later);

    assertEquals(Optional.of(later), store.load());
  }

  @Test
  @DisplayName("the save directory is created if it is not there yet")
  void createsItsDirectory(@TempDir Path dir) throws IOException {
    Path nested = dir.resolve("deeper").resolve("still").resolve("save.txt");
    new FileGameStore(nested).save(sample());

    assertTrue(Files.exists(nested));
  }

  @Test
  @DisplayName("the file on disk is the readable text form")
  void writesTheReadableFormat(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("save.txt");
    new FileGameStore(file).save(sample());

    String text = Files.readString(file);
    assertTrue(text.startsWith("dungeon-crawler-save 1"));
    assertTrue(text.contains("seed 42"));
    assertTrue(text.contains("moves UL"));
  }

  @Test
  @DisplayName("a corrupt save reports the problem rather than loading a wrong game")
  void refusesACorruptSave(@TempDir Path dir) throws IOException {
    Path file = dir.resolve("save.txt");
    Files.writeString(file, "this is not a saved game");
    GameStore store = new FileGameStore(file);

    assertThrows(IllegalArgumentException.class, store::load);
  }

  @Test
  void reportsWhereItKeepsItsSave(@TempDir Path dir) {
    Path file = dir.resolve("save.txt");
    assertEquals(file, new FileGameStore(file).file());
  }

  @Test
  @DisplayName("the installed game saves under the user's home directory")
  void theDefaultLocationIsUnderTheUserHome() {
    Path file = FileGameStore.inUserHome().file();
    assertTrue(file.startsWith(Path.of(System.getProperty("user.home"))));
    assertEquals("save.txt", file.getFileName().toString());
    assertEquals(".dungeon-crawler", file.getParent().getFileName().toString());
  }
}
