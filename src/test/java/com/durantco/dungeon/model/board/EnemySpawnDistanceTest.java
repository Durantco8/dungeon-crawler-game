package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * No game should be lost before the player has had a turn.
 *
 * <p>The simulation harness found this: a report flagged a game that ended on turn one, and measuring across
 * two thousand seeds showed an enemy spawning next to the hero in roughly one game in thirty.
 */
class EnemySpawnDistanceTest {

  private static final int WIDTH = GameSetup.standard(0L).width();
  private static final int HEIGHT = GameSetup.standard(0L).height();

  /** The margin BoardImpl promises, restated here so a change to it has to be deliberate. */
  private static final int REQUIRED_MARGIN = 4;

  private static BoardImpl board(long seed) {
    return new BoardImpl(WIDTH, HEIGHT, new Random(seed), new BspLevelGenerator());
  }

  private static Posn find(Board board, PieceType type) {
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == type) {
          return new Posn(row, col);
        }
      }
    }
    return null;
  }

  private static List<Posn> findAll(Board board, PieceType type) {
    List<Posn> found = new ArrayList<>();
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == type) {
          found.add(new Posn(row, col));
        }
      }
    }
    return found;
  }

  @Test
  @DisplayName("no enemy spawns within reach of the hero, across many seeds and levels")
  void enemiesKeepTheirDistanceAtSpawn() {
    for (int seed = 0; seed < 300; seed++) {
      BoardImpl board = board(seed);
      for (int level = 1; level <= 4; level++) {
        board.init(LevelSpec.forLevel(level));
        Posn hero = find(board, PieceType.HERO);
        assertNotNull(hero);

        Map<Posn, Integer> fromHero = Reachability.distancesFrom(hero, board::heroCanEnter);
        for (Posn enemy : findAll(board, PieceType.ENEMY)) {
          Integer steps = fromHero.get(enemy);
          if (steps == null) {
            continue; // unreachable from the hero, which is no threat at all
          }
          assertTrue(
              steps >= REQUIRED_MARGIN,
              "Seed " + seed + " level " + level + " spawned an enemy " + steps + " steps away");
        }
      }
    }
  }

  @Test
  @DisplayName("the rest of the level is unaffected: counts and solvability still hold")
  void theLevelIsOtherwiseUnchanged() {
    for (int seed = 0; seed < 60; seed++) {
      BoardImpl board = board(seed);
      LevelSpec spec = LevelSpec.forLevel(3);
      board.init(spec);

      assertEquals(spec.enemies(), findAll(board, PieceType.ENEMY).size());
      assertEquals(1, findAll(board, PieceType.HERO).size());
      assertEquals(1, findAll(board, PieceType.EXIT).size());
      assertEquals(spec.treasures(), findAll(board, PieceType.TREASURE).size());
      assertTrue(board.isSolvable(), "Seed " + seed + " became unsolvable");
    }
  }

  @Test
  @DisplayName("spawning is still decided only by the seed")
  void spawningStaysReproducible() {
    BoardImpl first = board(77L);
    first.init(LevelSpec.forLevel(5));
    BoardImpl second = board(77L);
    second.init(LevelSpec.forLevel(5));

    assertEquals(findAll(first, PieceType.ENEMY), findAll(second, PieceType.ENEMY));
    assertEquals(find(first, PieceType.HERO), find(second, PieceType.HERO));
  }

  @Test
  @DisplayName("a dungeon too cramped for the margin still builds, rather than refusing or hanging")
  void acrampedLevelFallsBackToTheFarthestCell() {
    // Every cell is within four steps of every other, so the margin cannot be honoured. The level must
    // still be built: an unfair spawn beats no level at all.
    BoardImpl tiny = new BoardImpl(3, 3, new Random(1L), new RandomScatterGenerator(0));
    tiny.init(new LevelSpec(2, 1, 1));

    assertEquals(2, findAll(tiny, PieceType.ENEMY).size());
    assertNotNull(find(tiny, PieceType.HERO));
  }

  @Test
  @DisplayName("the hero always gets at least one turn before anything can reach it")
  void theHeroAlwaysGetsATurn() {
    // An enemy four steps away cannot reach the hero on the opening move even at the fastest pace.
    for (int seed = 0; seed < 120; seed++) {
      BoardImpl board = board(seed);
      board.setHardMode(true);
      board.init(LevelSpec.forLevel(2));

      assertEquals(
          com.durantco.dungeon.model.pieces.CollisionResult.Result.CONTINUE,
          board.moveHero(0, 0).getResults(),
          "Seed " + seed + " lost the hero before it acted");
    }
  }
}
