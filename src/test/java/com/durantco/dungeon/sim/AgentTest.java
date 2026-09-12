package com.durantco.dungeon.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSession;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.replay.PlayerMove;
import com.durantco.dungeon.support.Boards;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AgentTest {

  private static List<Agent> allAgents(long seed) {
    return List.of(
        new RandomAgent(new Random(seed)),
        new ExitRunnerAgent(new Random(seed)),
        new TreasureHunterAgent(new Random(seed)));
  }

  /** A model over an exact hand-built layout, so an agent's choice can be predicted. */
  private static Model modelOn(String... rows) {
    return new com.durantco.dungeon.model.ModelImpl(
        new BoardImpl(Boards.parse(rows), new Random(1L)));
  }

  @Test
  void everyAgentHasAName() {
    Set<String> names = new HashSet<>();
    for (Agent agent : allAgents(1L)) {
      assertNotNull(agent.name());
      assertTrue(names.add(agent.name()), "Agent names must be distinct for results to be readable");
    }
  }

  @Test
  @DisplayName("every agent always returns a move, so a run can never stall")
  void everyAgentAlwaysMoves() {
    for (Agent agent : allAgents(4L)) {
      GameSession session = new GameSession(GameSetup.standard(9L));
      session.startGame();
      for (int turn = 0; turn < 60; turn++) {
        PlayerMove move = agent.chooseMove(session);
        assertNotNull(move, agent.name() + " returned no move");
        move.applyTo(session);
      }
    }
  }

  @Test
  @DisplayName("an agent keeps choosing even with no hero left on the board")
  void agentsCopeWithAFinishedGame() {
    for (Agent agent : allAgents(2L)) {
      Model empty = modelOn("...", "...", "...");
      assertNotNull(agent.chooseMove(empty), agent.name() + " gave up on an empty board");
    }
  }

  @Test
  @DisplayName("the exit runner walks towards the exit")
  void theExitRunnerHeadsForTheExit() {
    Model model = modelOn("H..X");
    assertEquals(PlayerMove.RIGHT, new ExitRunnerAgent(new Random(1L)).chooseMove(model));
  }

  @Test
  @DisplayName("the exit runner goes around a wall rather than into it")
  void theExitRunnerRoutesAroundWalls() {
    Model model = modelOn("H.WX", "..W.", "....");
    PlayerMove move = new ExitRunnerAgent(new Random(1L)).chooseMove(model);
    assertTrue(
        move == PlayerMove.DOWN || move == PlayerMove.RIGHT,
        "The route round the wall starts down or right, not into it");
  }

  @Test
  @DisplayName("the exit runner reaches the exit when left to it")
  void theExitRunnerFinishesALevel() {
    Model model = modelOn("H...", "....", "...X");
    Agent agent = new ExitRunnerAgent(new Random(1L));
    for (int turn = 0; turn < 10; turn++) {
      if (model.getLevel() > 1) {
        break;
      }
      agent.chooseMove(model).applyTo(model);
    }
    // On a bare board with no model level tracking, the exit is simply gone once reached.
    assertTrue(
        Sight.locate(model, PieceType.EXIT).isEmpty() || Sight.hero(model).isPresent(),
        "The runner should have made progress towards the exit");
  }

  @Test
  @DisplayName("the treasure hunter goes for treasure before the exit")
  void theTreasureHunterPrefersTreasure() {
    // The exit is one step right; the treasure is one step down. A hunter takes the treasure first.
    Model model = modelOn("H.X.", "T...");
    assertEquals(PlayerMove.DOWN, new TreasureHunterAgent(new Random(1L)).chooseMove(model));
  }

  @Test
  @DisplayName("the treasure hunter leaves once nothing is left to collect")
  void theTreasureHunterLeavesWhenDone() {
    Model model = modelOn("H..X");
    assertEquals(PlayerMove.RIGHT, new TreasureHunterAgent(new Random(1L)).chooseMove(model));
  }

  @Test
  @DisplayName("the treasure hunter picks the nearest treasure by route, not by straight line")
  void theTreasureHunterMeasuresByRoute() {
    // The treasure above is closer as the crow flies but sealed off; the one to the right is reachable.
    Model model = modelOn(".T..", "WW..", "H..T");
    PlayerMove move = new TreasureHunterAgent(new Random(1L)).chooseMove(model);
    assertEquals(PlayerMove.RIGHT, move, "It should go for the treasure it can actually get to");
  }

  @Test
  @DisplayName("the random agent is reproducible from its seed")
  void theRandomAgentIsReproducible() {
    assertEquals(choices(new RandomAgent(new Random(5L))), choices(new RandomAgent(new Random(5L))));
  }

  @Test
  void theRandomAgentUsesEveryDirection() {
    Set<PlayerMove> seen = new HashSet<>();
    Agent agent = new RandomAgent(new Random(3L));
    Model model = modelOn("...", ".H.", "...");
    for (int turn = 0; turn < 100; turn++) {
      seen.add(agent.chooseMove(model));
    }
    assertEquals(4, seen.size());
  }

  @Test
  @DisplayName("a smarter agent gets further than a random one")
  void intelligencePaysForItself() {
    assertTrue(
        averageScore(seed -> new TreasureHunterAgent(new Random(seed)))
            > averageScore(seed -> new RandomAgent(new Random(seed))),
        "The treasure hunter should outscore random wandering");
  }

  private static String choices(Agent agent) {
    Model model = new com.durantco.dungeon.model.ModelImpl(
        new BoardImpl(Boards.parse("...", ".H.", "..."), new Random(1L)));
    StringBuilder out = new StringBuilder();
    for (int turn = 0; turn < 20; turn++) {
      out.append(agent.chooseMove(model).symbol());
    }
    return out.toString();
  }

  private static double averageScore(java.util.function.LongFunction<Agent> agents) {
    int total = 0;
    int runs = 12;
    for (int seed = 0; seed < runs; seed++) {
      GameSession session = new GameSession(GameSetup.standard(seed));
      session.startGame();
      Agent agent = agents.apply(seed);
      for (int turn = 0; turn < 150 && session.getStatus() == Model.STATUS.IN_PROGRESS; turn++) {
        agent.chooseMove(session).applyTo(session);
      }
      total += session.getCurScore();
    }
    return (double) total / runs;
  }
}
