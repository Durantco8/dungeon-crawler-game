package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.support.Boards;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Every level handed to the player must be finishable. */
class SolvabilityTest {

  private static final int WIDTH = 28;
  private static final int HEIGHT = 18;
  private static final int SEEDS = 400;

  private static BoardImpl bspBoard(long seed) {
    return new BoardImpl(WIDTH, HEIGHT, new Random(seed), new BspLevelGenerator());
  }

  @Test
  @DisplayName("every level of every seed is solvable")
  void everyGeneratedLevelIsSolvable() {
    for (int seed = 0; seed < SEEDS; seed++) {
      BoardImpl board = bspBoard(seed);
      for (int level = 1; level <= 5; level++) {
        board.init(LevelSpec.forLevel(level));
        assertTrue(
            board.isSolvable(),
            "Seed " + seed + " level " + level + " produced an unsolvable dungeon");
      }
    }
  }

  @Test
  @DisplayName("the partitioning generator never needs a second attempt")
  void connectivityByConstructionMeansNoRetries() {
    // The retry budget is a safety net. If this ever fails, the generator's connect-on-unwind step is
    // broken, which is a much more useful signal than a level being quietly regenerated.
    for (int seed = 0; seed < SEEDS; seed++) {
      BoardImpl board = bspBoard(seed);
      board.init(LevelSpec.forLevel(3));
      assertEquals(
          1, board.generationAttempts(), "Seed " + seed + " needed more than one layout");
    }
  }

  @Test
  void aLayoutFromTheGeneratorIsAlwaysConnected() {
    BspLevelGenerator generator = new BspLevelGenerator();
    for (int seed = 0; seed < SEEDS; seed++) {
      assertTrue(
          Reachability.isConnected(generator.generate(WIDTH, HEIGHT, new Random(seed))),
          "Seed " + seed + " produced a disconnected floor");
    }
  }

  @Test
  void recognisesAWinnableHandBuiltLevel() {
    BoardImpl board = new BoardImpl(Boards.parse("HTX", "...", "..."), new Random(1L));
    assertTrue(board.isSolvable());
  }

  @Test
  @DisplayName("an exit walled off from the hero is not solvable")
  void recognisesAnUnreachableExit() {
    BoardImpl board = new BoardImpl(Boards.parse("H.W", "..W", "WWX"), new Random(1L));
    assertFalse(board.isSolvable());
  }

  @Test
  @DisplayName("a treasure walled off from the hero is not solvable")
  void recognisesAnUnreachableTreasure() {
    BoardImpl board = new BoardImpl(Boards.parse("H.WT", "..WW", "..X."), new Random(1L));
    assertFalse(board.isSolvable());
  }

  @Test
  @DisplayName("enemies and thieves stand in the way but do not block a route")
  void treatsOnlyWallsAsImpassable() {
    BoardImpl board = new BoardImpl(Boards.parse("HEX", "WWW", "..."), new Random(1L));
    assertTrue(board.isSolvable(), "An enemy in a corridor is a hazard, not a wall");
  }

  @Test
  void aBoardWithNoHeroIsNotSolvable() {
    BoardImpl board = new BoardImpl(Boards.parse("..X", "...", "..."), new Random(1L));
    assertFalse(board.isSolvable());
  }

  @Test
  @DisplayName("a generator that returns a disconnected layout exhausts its retries and fails loudly")
  void givesUpOnAGeneratorThatCannotProduceASolvableLevel() {
    // Two sealed halves: the hero and the exit can never share a region.
    LevelGenerator sealedHalves =
        new LevelGenerator() {
          @Override
          public DungeonLayout generate(int width, int height, Random rng) {
            boolean[][] walkable = new boolean[height][width];
            for (int row = 0; row < height; row++) {
              for (int col = 0; col < width; col++) {
                walkable[row][col] = col != width / 2;
              }
            }
            return new DungeonLayout(walkable, List.of());
          }

          @Override
          public int guaranteedWalkableCells(int width, int height) {
            return width * height - height;
          }
        };

    BoardImpl board = new BoardImpl(9, 9, new Random(1L), sealedHalves);
    // A level with enough pieces that some are certain to land on the far side of the divide.
    IllegalStateException failure =
        assertThrows(IllegalStateException.class, () -> board.init(new LevelSpec(0, 20, 0)));
    assertTrue(failure.getMessage().contains("solvable"));
  }

  @Test
  @DisplayName("the scatter generator's unwinnable levels are now retried rather than played")
  void retriesRescueTheScatterGenerator() {
    // Scattered walls can seal a corner containing the hero or the exit. On a tight board that
    // happens often enough to exercise the retry path.
    for (int seed = 0; seed < 200; seed++) {
      BoardImpl board = new BoardImpl(5, 5, new Random(seed), new RandomScatterGenerator(8));
      board.init(new LevelSpec(1, 2, 1));
      assertTrue(board.isSolvable(), "Seed " + seed + " handed the player an unwinnable level");
    }
  }
}
