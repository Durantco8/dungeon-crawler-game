package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The whole of hard mode running on a generated dungeon: real walls to see around, real rooms to
 * patrol, three archetypes at three paces, all resolving one turn at a time.
 *
 * <p>The strategies are unit tested against stubbed geometry elsewhere. This is the seam those tests
 * cannot cover: that the board actually supplies what the strategies ask it for.
 */
class HardModeDungeonTest {

  private static final int WIDTH = 28;
  private static final int HEIGHT = 18;

  private static BoardImpl hardBoard(long seed, int level) {
    BoardImpl board = new BoardImpl(WIDTH, HEIGHT, new Random(seed), new BspLevelGenerator());
    board.setHardMode(true);
    board.init(LevelSpec.forLevel(level));
    return board;
  }

  private static List<Enemy> enemiesOn(Board board) {
    List<Enemy> found = new ArrayList<>();
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.ENEMY) {
          found.add((Enemy) piece);
        }
      }
    }
    return found;
  }

  @Test
  @DisplayName("a patroller walks a real dungeon's rooms, not just a stubbed beat")
  void patrollersMoveOnAGeneratedDungeon() {
    // The third archetype patrols when it cannot see the hero, which on a partitioned board is most of
    // the time. Its beat comes from the generator's rooms, so this is what proves the board hands them
    // over.
    boolean somePatrollerMoved = false;
    for (int seed = 0; seed < 30 && !somePatrollerMoved; seed++) {
      BoardImpl board = hardBoard(seed, 3);
      List<Enemy> patrollers = new ArrayList<>();
      for (Enemy enemy : enemiesOn(board)) {
        SightedStrategy sighted = (SightedStrategy) enemy.movement();
        if (sighted.whenUnseen() instanceof PatrolStrategy) {
          patrollers.add(enemy);
        }
      }
      assertFalse(patrollers.isEmpty(), "Level 3 should spawn at least one patrolling guard");

      List<Posn> before = new ArrayList<>();
      for (Enemy patroller : patrollers) {
        before.add(patroller.getPosn());
      }
      for (int turn = 0; turn < 12; turn++) {
        int sideways = 1;
        if (turn % 2 == 1) {
          sideways = -1;
        }
        if (board.moveHero(0, sideways).getResults() == CollisionResult.Result.GAME_OVER) {
          break;
        }
      }
      for (int i = 0; i < patrollers.size(); i++) {
        if (!patrollers.get(i).getPosn().equals(before.get(i))) {
          somePatrollerMoved = true;
        }
      }
    }
    assertTrue(somePatrollerMoved, "No patroller ever left its starting cell");
  }

  @Test
  @DisplayName("hard mode plays without a piece ever being lost or duplicated")
  void thePieceCountHoldsThroughoutAHardGame() {
    for (int seed = 0; seed < 25; seed++) {
      BoardImpl board = hardBoard(seed, 4);
      int enemies = enemiesOn(board).size();
      assertEquals(5, enemies, "Level 4 spawns five enemies");

      for (int turn = 0; turn < 40; turn++) {
        int sideways = 1;
        if (turn % 2 == 1) {
          sideways = -1;
        }
        CollisionResult result = board.moveHero(0, sideways);
        if (result.getResults() != CollisionResult.Result.CONTINUE) {
          break;
        }
        assertEquals(
            enemies, enemiesOn(board).size(), "Seed " + seed + " lost an enemy on turn " + turn);
      }
    }
  }

  @Test
  @DisplayName("every piece still agrees with its cell after a long hard game")
  void positionsStayConsistentThroughAHardGame() {
    BoardImpl board = hardBoard(11L, 5);
    for (int turn = 0; turn < 40; turn++) {
      int sideways = 1;
      if (turn % 2 == 1) {
        sideways = -1;
      }
      if (board.moveHero(0, sideways).getResults() != CollisionResult.Result.CONTINUE) {
        break;
      }
    }
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
  @DisplayName("a hard game replays identically from the same seed, archetypes and paces included")
  void aHardGameIsReproducible() {
    assertEquals(transcript(2024L), transcript(2024L));
  }

  /** Plays a fixed sequence of moves and records everything that happened, as a comparable string. */
  private static String transcript(long seed) {
    Model model = new ModelImpl(new BoardImpl(WIDTH, HEIGHT, new Random(seed), new BspLevelGenerator()));
    model.setHardMode(true);
    model.startGame();

    StringBuilder log = new StringBuilder();
    for (int turn = 0; turn < 60; turn++) {
      switch (turn % 4) {
        case 0 -> model.moveRight();
        case 1 -> model.moveDown();
        case 2 -> model.moveLeft();
        default -> model.moveUp();
      }
      log.append(model.getCurScore()).append(':').append(model.getLevel()).append(':');
      for (int row = 0; row < HEIGHT; row++) {
        for (int col = 0; col < WIDTH; col++) {
          Piece piece = model.get(new Posn(row, col));
          if (piece == null) {
            log.append('.');
          } else {
            log.append(piece.getType().name().charAt(0));
          }
        }
      }
      log.append('\n');
    }
    return log.toString();
  }
}
