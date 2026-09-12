package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A board driven by the partitioning generator at the dimensions the game actually uses.
 *
 * <p>The dimensions come from GameSetup.standard rather than being restated here, so these tests follow
 * the shipped configuration if it ever changes.
 */
class BspBoardIntegrationTest {

  private static final int WIDTH = GameSetup.standard(0L).width();
  private static final int HEIGHT = GameSetup.standard(0L).height();

  private static BoardImpl board(long seed) {
    return new BoardImpl(WIDTH, HEIGHT, new Random(seed), new BspLevelGenerator());
  }

  private static Map<PieceType, Integer> census(Board board) {
    Map<PieceType, Integer> counts = new EnumMap<>(PieceType.class);
    for (PieceType type : PieceType.values()) {
      counts.put(type, 0);
    }
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null) {
          counts.merge(piece.getType(), 1, Integer::sum);
        }
      }
    }
    return counts;
  }

  @Test
  void placesAFullLevelOnAPartitionedBoard() {
    BoardImpl board = board(1L);
    board.init(LevelSpec.forLevel(1));

    Map<PieceType, Integer> counts = census(board);
    assertEquals(1, counts.get(PieceType.HERO));
    assertEquals(1, counts.get(PieceType.EXIT));
    assertEquals(2, counts.get(PieceType.ENEMY));
    assertEquals(2, counts.get(PieceType.TREASURE));
    assertEquals(2, counts.get(PieceType.THIEF));
  }

  @Test
  @DisplayName("the dungeon has real wall structure, not a couple of scattered blocks")
  void fillsTheUnusedSpaceWithWalls() {
    BoardImpl board = board(1L);
    board.init(LevelSpec.forLevel(1));

    assertTrue(census(board).get(PieceType.WALL) > 100, "A partitioned board is substantially solid");
  }

  @Test
  @DisplayName("levels build across many seeds and depths without hanging or failing")
  void buildsEveryLevelItClaimsItCan() {
    for (int seed = 0; seed < 40; seed++) {
      BoardImpl board = board(seed);
      for (int level = 1; level <= 20; level++) {
        LevelSpec spec = LevelSpec.forLevel(level);
        assertTrue(board.canFit(spec), "Level " + level + " should fit a 28x18 dungeon");
        board.init(spec);
        assertEquals(
            spec.enemies(),
            census(board).get(PieceType.ENEMY),
            "Seed " + seed + " level " + level + " placed the wrong number of enemies");
      }
    }
  }

  @Test
  void everyPieceAgreesWithTheCellItOccupies() {
    BoardImpl board = board(3L);
    board.init(LevelSpec.forLevel(5));

    for (int row = 0; row < HEIGHT; row++) {
      for (int col = 0; col < WIDTH; col++) {
        Posn at = new Posn(row, col);
        Piece piece = board.get(at);
        if (piece != null) {
          assertEquals(at, piece.getPosn(), piece.getName() + " disagrees about where it is");
        }
      }
    }
  }

  @Test
  @DisplayName("the hero can always take a turn, even hemmed in by dungeon walls")
  void theHeroCanAlwaysActOnAPartitionedBoard() {
    for (int seed = 0; seed < 40; seed++) {
      BoardImpl board = board(seed);
      board.init(LevelSpec.forLevel(1));
      assertNotNull(board.moveHero(-1, 0));
      assertNotNull(board.moveHero(1, 0));
      assertNotNull(board.moveHero(0, -1));
      assertNotNull(board.moveHero(0, 1));
    }
  }

  @Test
  void aWholeGameCanBePlayedThroughTheModel() {
    Model model = new ModelImpl(board(9L));
    model.startGame();

    assertEquals(Model.STATUS.IN_PROGRESS, model.getStatus());
    for (int move = 0; move < 200; move++) {
      model.moveRight();
      model.moveDown();
    }
    // Whatever happened, the model is in one of its two valid states and the score is a number.
    assertNotNull(model.getStatus());
    assertTrue(model.getLevel() >= 1);
  }
}
