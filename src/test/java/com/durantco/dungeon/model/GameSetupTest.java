package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.board.BspLevelGenerator;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.GeneratorSpec;
import com.durantco.dungeon.model.board.LevelSpec;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.board.RandomScatterGenerator;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.GameStates;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class GameSetupTest {

  @Test
  void theStandardGameIsAPartitionedDungeonOnEasy() {
    GameSetup setup = GameSetup.standard(1L);
    assertEquals(28, setup.width());
    assertEquals(18, setup.height());
    assertEquals(Difficulty.EASY, setup.difficulty());
    assertInstanceOf(GeneratorSpec.Bsp.class, setup.generator());
  }

  @Test
  void carriesItsSeed() {
    assertEquals(4242L, GameSetup.standard(4242L).seed());
  }

  @Test
  @DisplayName("setups with the same values are equal, so a recording can be compared")
  void isAValueType() {
    assertEquals(GameSetup.standard(7L), GameSetup.standard(7L));
    assertNotEquals(GameSetup.standard(7L), GameSetup.standard(8L));
  }

  @Test
  void canBeRederivedWithADifferentSeedOrDifficulty() {
    GameSetup base = GameSetup.standard(1L);
    assertEquals(9L, base.withSeed(9L).seed());
    assertEquals(Difficulty.HARD, base.withDifficulty(Difficulty.HARD).difficulty());
    assertEquals(base.generator(), base.withSeed(9L).generator());
    assertEquals(1L, base.seed(), "The original should be untouched");
  }

  @Test
  void rejectsANonsenseBoard() {
    GeneratorSpec bsp = new GeneratorSpec.Bsp(3, 4);
    assertThrows(
        IllegalArgumentException.class, () -> new GameSetup(1L, 0, 10, Difficulty.EASY, bsp));
    assertThrows(
        IllegalArgumentException.class, () -> new GameSetup(1L, 10, -1, Difficulty.EASY, bsp));
  }

  @Test
  void rejectsAnIncompleteSetup() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new GameSetup(1L, 10, 10, Difficulty.EASY, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> new GameSetup(1L, 10, 10, null, new GeneratorSpec.Bsp(3, 4)));
  }

  @Test
  void aSpecBuildsTheGeneratorItDescribes() {
    assertInstanceOf(BspLevelGenerator.class, new GeneratorSpec.Bsp(3, 4).create());
    assertInstanceOf(RandomScatterGenerator.class, new GeneratorSpec.Scatter(2).create());
  }

  @Test
  @DisplayName("a spec's settings reach the generator it builds")
  void aSpecPassesItsSettingsOn() {
    // A five-cell minimum needs a bigger leaf, so a board that fits three-cell rooms is refused.
    assertThrows(
        IllegalArgumentException.class,
        () -> new GeneratorSpec.Bsp(5, 4).create().generate(5, 5, new java.util.Random(1L)));
    assertEquals(20, new GeneratorSpec.Scatter(5).create().guaranteedWalkableCells(5, 5));
  }

  @Test
  @DisplayName("two games built from one setup are identical, which is what replay rests on")
  void oneSetupBuildsIdenticalGames() {
    assertEquals(transcript(GameSetup.standard(99L)), transcript(GameSetup.standard(99L)));
  }

  @Test
  void differentSeedsBuildDifferentGames() {
    assertNotEquals(transcript(GameSetup.standard(1L)), transcript(GameSetup.standard(2L)));
  }

  @Test
  @DisplayName("the factory honours the difficulty the setup asks for")
  void theFactoryAppliesTheDifficulty() {
    Model hard = GameFactory.create(GameSetup.standard(5L).withDifficulty(Difficulty.HARD));
    hard.startGame();
    assertTrue(hard.isHardMode() || countEnemies(hard) > 0);
    assertNotEquals(
        transcript(GameSetup.standard(5L)),
        transcript(GameSetup.standard(5L).withDifficulty(Difficulty.HARD)),
        "Hunting enemies should play differently from drifting ones");
  }

  @Test
  void theFactoryBuildsABoardOfTheRequestedSize() {
    Model model = GameFactory.create(GameSetup.standard(1L));
    assertEquals(28, model.getWidth());
    assertEquals(18, model.getHeight());
  }

  @Test
  @DisplayName("the standard setup can build every level it claims to fit")
  void theStandardBoardFitsTheLevelsItPlays() {
    Model model = GameFactory.create(GameSetup.standard(3L));
    model.startGame();
    assertEquals(1, model.getLevel());
    for (int level = 1; level <= 20; level++) {
      assertTrue(LevelSpec.forLevel(level).cellsRequired() < 28 * 18);
    }
  }

  private static int countEnemies(Model model) {
    int found = 0;
    for (int row = 0; row < model.getHeight(); row++) {
      for (int col = 0; col < model.getWidth(); col++) {
        Piece piece = model.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.ENEMY) {
          found++;
        }
      }
    }
    return found;
  }

  /** Plays a fixed sequence of moves and renders everything that happened. */
  private static String transcript(GameSetup setup) {
    Model model = GameFactory.create(setup);
    model.startGame();
    StringBuilder log = new StringBuilder();
    for (int turn = 0; turn < 40; turn++) {
      switch (turn % 4) {
        case 0 -> model.moveRight();
        case 1 -> model.moveDown();
        case 2 -> model.moveLeft();
        default -> model.moveUp();
      }
      log.append(GameStates.render(model)).append('\n');
    }
    return log.toString();
  }
}
