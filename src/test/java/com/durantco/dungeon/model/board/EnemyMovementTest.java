package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.support.Boards;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Enemy movement in both difficulty modes.
 *
 * <p>Hard mode follows a shortest path to the hero, so it rounds corners instead of pressing against
 * walls. Easy mode shuffles the four directions, so it is only assertable because the randomness is
 * now injected.
 */
class EnemyMovementTest {

  private static BoardImpl boardOf(long seed, String... rows) {
    return new BoardImpl(Boards.parse(rows), new Random(seed));
  }

  private static Posn firstEnemy(Board board) {
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.ENEMY) {
          return new Posn(row, col);
        }
      }
    }
    return null;
  }

  private static String render(Board board) {
    StringBuilder out = new StringBuilder();
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece == null) {
          out.append('.');
        } else {
          out.append(piece.getType().name().charAt(0));
        }
      }
    }
    return out.toString();
  }

  @Nested
  class HardMode {

    @Test
    @DisplayName("closes the row gap when the hero is further away vertically")
    void chasesAlongRowsWhenRowsDominate() {
      BoardImpl board = boardOf(1L, ".H.", "...", ".E.");
      Boards.armChasers(board);
      board.moveHero(0, 1);

      assertEquals(new Posn(1, 1), firstEnemy(board));
    }

    @Test
    @DisplayName("closes the column gap when the hero is further away horizontally")
    void chasesAlongColumnsWhenColumnsDominate() {
      BoardImpl board = boardOf(1L, "H...", "...E");
      Boards.armChasers(board);
      board.moveHero(1, 0);

      assertEquals(new Posn(1, 2), firstEnemy(board));
    }

    @Test
    void movesOnlyOneCellPerHeroMove() {
      BoardImpl board = boardOf(1L, ".H..", "....", "....", ".E..");
      Boards.armChasers(board);
      Posn before = firstEnemy(board);
      board.moveHero(0, 1);
      Posn after = firstEnemy(board);

      int distance = Math.abs(after.row() - before.row()) + Math.abs(after.col() - before.col());
      assertEquals(1, distance);
    }

    @Test
    @DisplayName("the chase consults no randomness, so the seed cannot change it")
    void isIndependentOfTheSeed() {
      BoardImpl first = boardOf(1L, ".H..", "....", ".E..");
      Boards.armChasers(first);
      first.moveHero(0, 1);

      BoardImpl second = boardOf(999_999L, ".H..", "....", ".E..");
      Boards.armChasers(second);
      second.moveHero(0, 1);

      assertEquals(render(first), render(second));
    }

    @Test
    @DisplayName("walks away from the hero when that is the only route to it")
    void roundsACornerTheGreedyChaseWouldHaveMissed() {
      // The hero is to the enemy's right, but the wall means the only way through is upward on the
      // far left. The old greedy chase stepped along whichever axis was further, so it would have
      // moved right into the dead end and stayed there. A shortest path goes up instead.
      BoardImpl board = boardOf(1L, "....H", ".WWWW", "E....");
      Boards.armChasers(board);
      board.moveHero(0, -1); // hero retreats to (0, 3), still on the far side of the wall

      assertEquals(new Posn(1, 0), firstEnemy(board), "The enemy should take the only real route");
    }

    @Test
    @DisplayName("follows its route all the way to the hero")
    void eventuallyCatchesTheHeroByFollowingItsRoute() {
      // The enemy starts on the far side of the wall with a six-step route to the hero. Left to chase
      // an equally mobile hero it should close that gap and catch it, which the greedy chase could not
      // do from here because its first choice led into the dead-end corridor.
      BoardImpl board = boardOf(1L, "....H", ".WWWW", "E....");
      Boards.armChasers(board);

      CollisionResult.Result outcome = CollisionResult.Result.CONTINUE;
      for (int turn = 0; turn < 6 && outcome == CollisionResult.Result.CONTINUE; turn++) {
        outcome = board.moveHero(0, -1).getResults();
      }

      assertEquals(
          CollisionResult.Result.GAME_OVER, outcome, "The enemy never completed its route");
    }

    @Test
    @DisplayName("a walled-off preferred direction falls back rather than standing still")
    void takesAnAlternativeWhenThePreferredDirectionIsBlocked() {
      BoardImpl board = boardOf(1L, "H...", ".W..", ".E..");
      Boards.armChasers(board);
      board.moveHero(0, 1);

      // The wall sits on the enemy's preferred upward step, so it must pick another direction.
      assertNotEquals(new Posn(2, 1), firstEnemy(board));
      assertEquals(PieceType.WALL, board.get(new Posn(1, 1)).getType());
    }
  }

  @Nested
  class Difficulty {

    @Test
    @DisplayName("difficulty applies to the enemies a level spawns, not to ones already placed")
    void changingDifficultyLeavesPlacedEnemiesAlone() {
      // Difficulty is part of a game's setup and fixed for its duration. The toggle is only reachable
      // from the title screen, with no game in progress, so there is nothing to re-arm; and re-arming
      // could only ever swap behaviour, never pace, which produced enemies matching no archetype.
      BoardImpl board = boardOf(1L, "H...", "....", "..E.");
      Enemy placed = (Enemy) board.get(new Posn(2, 2));

      board.setHardMode(true);
      assertTrue(placed.movement() instanceof WanderStrategy, "A placed enemy keeps its behaviour");
      assertEquals(ActionRate.NORMAL, placed.meter().rate());
    }

    @Test
    @DisplayName("hard mode spawns a mix of archetypes rather than three of the same hunter")
    void spawnedEnemiesMatchTheDifficulty() {
      BoardImpl board = new BoardImpl(28, 18, new Random(1L), new BspLevelGenerator());
      board.setHardMode(true);
      board.init(LevelSpec.forLevel(2)); // three enemies, one of each archetype

      Set<Class<?>> hunters = new HashSet<>();
      Set<Class<?>> idlers = new HashSet<>();
      for (int row = 0; row < board.getHeight(); row++) {
        for (int col = 0; col < board.getWidth(); col++) {
          Piece piece = board.get(new Posn(row, col));
          if (piece != null && piece.getType() == PieceType.ENEMY) {
            SightedStrategy sighted = (SightedStrategy) ((Enemy) piece).movement();
            hunters.add(sighted.whenSeen().getClass());
            idlers.add(sighted.whenUnseen().getClass());
          }
        }
      }
      assertEquals(Set.of(ChaseStrategy.class, AmbushStrategy.class), hunters);
      assertEquals(Set.of(WanderStrategy.class, PatrolStrategy.class), idlers);
    }

    @Test
    @DisplayName("easy mode spawns nothing that hunts")
    void easyModeSpawnsOnlyDrifters() {
      BoardImpl board = new BoardImpl(28, 18, new Random(1L), new BspLevelGenerator());
      board.init(LevelSpec.forLevel(4));

      for (int row = 0; row < board.getHeight(); row++) {
        for (int col = 0; col < board.getWidth(); col++) {
          Piece piece = board.get(new Posn(row, col));
          if (piece != null && piece.getType() == PieceType.ENEMY) {
            assertTrue(((Enemy) piece).movement() instanceof WanderStrategy);
          }
        }
      }
    }
  }

  @Nested
  class EasyMode {

    @Test
    @DisplayName("the same seed replays the same enemy wandering")
    void isReproducibleForAGivenSeed() {
      BoardImpl first = boardOf(42L, "H....", ".....", "..E..");
      BoardImpl second = boardOf(42L, "H....", ".....", "..E..");
      for (int move = 0; move < 3; move++) {
        first.moveHero(0, 1);
        second.moveHero(0, 1);
      }

      assertEquals(render(first), render(second));
    }

    @Test
    void differentSeedsDivergeEventually() {
      BoardImpl first = boardOf(1L, "H......", ".......", "...E...");
      BoardImpl second = boardOf(20L, "H......", ".......", "...E...");
      for (int move = 0; move < 4; move++) {
        first.moveHero(0, 1);
        second.moveHero(0, 1);
      }

      assertNotEquals(render(first), render(second));
    }

    @Test
    void movesTheEnemyToAnAdjacentCell() {
      BoardImpl board = boardOf(42L, "H....", ".....", "..E..");
      Posn before = firstEnemy(board);
      board.moveHero(0, 1);
      Posn after = firstEnemy(board);

      int distance = Math.abs(after.row() - before.row()) + Math.abs(after.col() - before.col());
      assertTrue(distance <= 1, "An enemy moved " + distance + " cells in one turn");
    }

    @Test
    @DisplayName("an enemy boxed in on all sides simply stays put")
    void staysPutWhenEveryDirectionIsRefused() {
      BoardImpl board = boardOf(42L, ".W..", "WEW.", ".W.H");
      board.moveHero(0, -1);

      assertEquals(new Posn(1, 1), firstEnemy(board));
    }
  }
}
