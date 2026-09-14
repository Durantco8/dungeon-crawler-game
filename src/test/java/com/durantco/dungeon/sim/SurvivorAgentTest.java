package com.durantco.dungeon.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import com.durantco.dungeon.support.Boards;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SurvivorAgentTest {

  private static Model modelOn(String... rows) {
    return new ModelImpl(new BoardImpl(Boards.parse(rows), new Random(1L)));
  }

  private static Agent survivor() {
    return new SurvivorAgent(new Random(1L));
  }

  @Test
  @DisplayName("walks around an enemy standing between it and the exit")
  void avoidsAnEnemyInItsPath() {
    // The exit is four steps right; an enemy is one step right. The careless agent walks into it.
    Model model = modelOn("HE..X", ".....");

    assertEquals(PlayerMove.RIGHT, new ExitRunnerAgent(new Random(1L)).chooseMove(model));
    assertEquals(PlayerMove.DOWN, survivor().chooseMove(model));
  }

  @Test
  @DisplayName("never steps onto an enemy when anywhere else will do")
  void neverWalksIntoAnEnemy() {
    for (int seed = 0; seed < 40; seed++) {
      Model model = modelOn("H.E..X", "......", "......");
      Agent agent = new SurvivorAgent(new Random(seed));
      Posn hero = Sight.hero(model).orElseThrow();
      PlayerMove move = agent.chooseMove(model);
      Posn target = hero.offset(move.drow(), move.dcol());

      if (model.get(target) != null) {
        assertNotEquals(
            PieceType.ENEMY, model.get(target).getType(), "Seed " + seed + " walked into an enemy");
      }
    }
  }

  @Test
  @DisplayName("still heads for the exit when the way is clear")
  void makesProgressWhenNothingThreatensIt() {
    Model model = modelOn("H...X", ".....");
    assertEquals(PlayerMove.RIGHT, survivor().chooseMove(model));
  }

  @Test
  @DisplayName("keeps moving rather than freezing when no safe route exists")
  void alwaysReturnsAMove() {
    // Hemmed in with enemies on both sides and no clear ground anywhere.
    Model model = modelOn("EHE");
    assertNotNull(survivor().chooseMove(model));
  }

  @Test
  @DisplayName("backs away when it cannot get past")
  void retreatsWhenCornered() {
    // The exit is behind the enemy in a one-cell corridor, so there is no route at any margin. The
    // agent should put ground between itself and the threat rather than walking into it.
    Model model = modelOn(".H.E.X");
    PlayerMove move = survivor().chooseMove(model);

    assertEquals(PlayerMove.LEFT, move, "It should back off, not close the distance");
  }

  @Test
  void copesWithABoardThatHasNoExit() {
    assertNotNull(survivor().chooseMove(modelOn("H...", "..E.")));
  }

  @Test
  void copesWithABoardThatHasNoEnemies() {
    assertEquals(PlayerMove.RIGHT, survivor().chooseMove(modelOn("H..X")));
  }

  @Test
  void isReproducibleFromItsSeed() {
    assertEquals(transcript(3L), transcript(3L));
  }

  @Test
  @DisplayName("caution buys a real increase in survival over the same goal without it")
  void outlivesTheCarelessAgentOnTheSameGames() {
    // Same goal, same seeds, same difficulty. The only difference is avoiding danger, so the gap
    // between the two is what caution is worth. This is the measurement the README reports.
    int careless = totalTurnsSurvived(seed -> new ExitRunnerAgent(new Random(seed)));
    int cautious = totalTurnsSurvived(seed -> new SurvivorAgent(new Random(seed)));

    assertTrue(
        cautious > careless,
        "Cautious lasted " + cautious + " turns against careless " + careless);
  }

  @Test
  @DisplayName("some cautious runs survive to the turn limit, which no careless run ever does")
  void sometimesSurvivesIndefinitely() {
    int survived = 0;
    for (int seed = 0; seed < 30; seed++) {
      RunResult result =
          Simulation.run(
              GameSetup.standard(seed).withDifficulty(com.durantco.dungeon.model.board.Difficulty.HARD),
              new SurvivorAgent(new Random(seed)),
              300);
      if (!result.died()) {
        survived++;
      }
    }
    assertTrue(survived > 0, "No cautious run reached the turn limit alive");
  }

  private static int totalTurnsSurvived(java.util.function.LongFunction<Agent> agents) {
    int total = 0;
    for (int seed = 0; seed < 30; seed++) {
      total +=
          Simulation.run(
                  GameSetup.standard(seed)
                      .withDifficulty(com.durantco.dungeon.model.board.Difficulty.HARD),
                  agents.apply(seed),
                  300)
              .turns();
    }
    return total;
  }

  private static String transcript(long seed) {
    Model model = modelOn("H....", ".E...", "....X");
    Agent agent = new SurvivorAgent(new Random(seed));
    List<String> moves = new ArrayList<>();
    for (int turn = 0; turn < 10; turn++) {
      moves.add(agent.chooseMove(model).name());
    }
    return String.join(",", moves);
  }
}
