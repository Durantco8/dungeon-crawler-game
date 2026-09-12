package com.durantco.dungeon.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.support.FakeBoard;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HighScoreStoreTest {

  /** Both implementations must agree on what a high score means. */
  private static List<HighScoreStore> storesIn(Path dir) {
    return List.of(
        new InMemoryHighScoreStore(), new FileHighScoreStore(dir.resolve("highscore.properties")));
  }

  @Test
  void startsWithNoScore(@TempDir Path dir) {
    for (HighScoreStore store : storesIn(dir)) {
      assertEquals(0, store.highest(), store.getClass().getSimpleName());
    }
  }

  @Test
  void keepsAScore(@TempDir Path dir) {
    for (HighScoreStore store : storesIn(dir)) {
      store.record(40);
      assertEquals(40, store.highest(), store.getClass().getSimpleName());
    }
  }

  @Test
  @DisplayName("only an improvement replaces the record")
  void keepsOnlyTheBest(@TempDir Path dir) {
    for (HighScoreStore store : storesIn(dir)) {
      store.record(40);
      store.record(15);
      assertEquals(40, store.highest(), store.getClass().getSimpleName());
      store.record(55);
      assertEquals(55, store.highest(), store.getClass().getSimpleName());
    }
  }

  @Nested
  class OnDisk {

    @Test
    @DisplayName("a score survives the process it was set in")
    void aScoreOutlivesTheStore(@TempDir Path dir) {
      Path file = dir.resolve("highscore.properties");
      new FileHighScoreStore(file).record(75);

      assertEquals(75, new FileHighScoreStore(file).highest(), "A new store should read the old score");
    }

    @Test
    void createsItsDirectory(@TempDir Path dir) {
      Path nested = dir.resolve("deeper").resolve("highscore.properties");
      new FileHighScoreStore(nested).record(10);
      assertTrue(Files.exists(nested));
    }

    @Test
    @DisplayName("an unreadable file reads as no score rather than failing")
    void degradesOnNonsense(@TempDir Path dir) throws IOException {
      Path file = dir.resolve("highscore.properties");
      Files.writeString(file, "highScore=not-a-number\n");

      assertEquals(0, new FileHighScoreStore(file).highest());
    }

    @Test
    @DisplayName("an empty file reads as no score")
    void degradesOnAnEmptyFile(@TempDir Path dir) throws IOException {
      Path file = dir.resolve("highscore.properties");
      Files.writeString(file, "");
      assertEquals(0, new FileHighScoreStore(file).highest());
    }

    @Test
    @DisplayName("a score that cannot be written is lost, not thrown")
    void degradesWhenItCannotWrite(@TempDir Path dir) throws IOException {
      // A directory where the file should be: writing cannot succeed, and must not end the game.
      Path blocked = dir.resolve("highscore.properties");
      Files.createDirectories(blocked);

      FileHighScoreStore store = new FileHighScoreStore(blocked);
      store.record(99); // must not throw
      assertEquals(0, store.highest());
    }

    @Test
    void reportsWhereItKeepsTheScore(@TempDir Path dir) {
      Path file = dir.resolve("highscore.properties");
      assertEquals(file, new FileHighScoreStore(file).file());
    }

    @Test
    void theDefaultLocationIsUnderTheUserHome() {
      Path file = FileHighScoreStore.inUserHome().file();
      assertTrue(file.startsWith(Path.of(System.getProperty("user.home"))));
      assertEquals(".dungeon-crawler", file.getParent().getFileName().toString());
    }
  }

  @Nested
  class ThroughTheModel {

    @Test
    @DisplayName("the model reports the score the store already holds")
    void anExistingScoreShowsImmediately(@TempDir Path dir) {
      Path file = dir.resolve("highscore.properties");
      new FileHighScoreStore(file).record(120);

      Model model = new ModelImpl(new FakeBoard(), new FileHighScoreStore(file));
      assertEquals(120, model.getHighScore(), "A new session should already know the record");
    }

    @Test
    @DisplayName("ending a game records the score for the next run")
    void endingAGamePersistsTheScore(@TempDir Path dir) {
      Path file = dir.resolve("highscore.properties");
      FakeBoard board = new FakeBoard();
      board.script(CollisionResult.scoring(30));

      Model model = new ModelImpl(board, new FileHighScoreStore(file));
      model.startGame();
      model.moveRight();
      model.endGame();

      assertEquals(30, new FileHighScoreStore(file).highest());
    }

    @Test
    @DisplayName("a weaker game does not overwrite a better record")
    void aWorseGameLeavesTheRecordAlone(@TempDir Path dir) {
      Path file = dir.resolve("highscore.properties");
      new FileHighScoreStore(file).record(200);

      FakeBoard board = new FakeBoard();
      board.script(CollisionResult.scoring(5));
      Model model = new ModelImpl(board, new FileHighScoreStore(file));
      model.startGame();
      model.moveRight();
      model.endGame();

      assertEquals(200, model.getHighScore());
    }

    @Test
    @DisplayName("a model built without a store keeps scores only for the session, as before")
    void theDefaultIsStillInMemory() {
      FakeBoard board = new FakeBoard();
      board.script(CollisionResult.scoring(12));
      Model model = new ModelImpl(board);
      model.startGame();
      model.moveRight();
      model.endGame();

      assertEquals(12, model.getHighScore());
      assertEquals(0, new ModelImpl(new FakeBoard()).getHighScore(), "Nothing carries between models");
    }
  }
}
