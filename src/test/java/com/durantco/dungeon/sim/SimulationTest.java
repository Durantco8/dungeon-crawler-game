package com.durantco.dungeon.sim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SimulationTest {

  private static final GameSetup SETUP = GameSetup.standard(1L);

  @Nested
  class Running {

    @Test
    void aRunReportsWhatHappened() {
      RunResult result = Simulation.run(SETUP, new ExitRunnerAgent(new Random(1L)), 200);

      assertEquals(1L, result.seed());
      assertEquals("exit-runner", result.agentName());
      assertTrue(result.turns() > 0 && result.turns() <= 200);
      assertTrue(result.levelReached() >= 1);
      assertTrue(result.levelsCleared() == result.levelReached() - 1);
    }

    @Test
    @DisplayName("a run stops at the turn limit rather than going on forever")
    void respectsTheTurnLimit() {
      RunResult result = Simulation.run(SETUP, new RandomAgent(new Random(1L)), 12);
      assertTrue(result.turns() <= 12, "Ran for " + result.turns() + " turns");
    }

    @Test
    @DisplayName("the same seed and agent produce the same run, so a report can be reproduced")
    void isReproducible() {
      RunResult first = Simulation.run(SETUP, new RandomAgent(new Random(5L)), 150);
      RunResult second = Simulation.run(SETUP, new RandomAgent(new Random(5L)), 150);
      assertEquals(first, second);
    }

    @Test
    void differentSeedsProduceDifferentRuns() {
      List<RunResult> results =
          Simulation.runBatch(SETUP, new ExitRunnerAgent(new Random(1L)), 20, 200);
      long distinct = results.stream().map(RunResult::turns).distinct().count();
      assertTrue(distinct > 1, "Twenty games should not all last exactly as long");
    }

    @Test
    @DisplayName("a batch walks consecutive seeds so a report covers a known range")
    void aBatchUsesConsecutiveSeeds() {
      List<RunResult> results =
          Simulation.runBatch(GameSetup.standard(100L), new RandomAgent(new Random(1L)), 5, 50);

      assertEquals(5, results.size());
      for (int i = 0; i < 5; i++) {
        assertEquals(100L + i, results.get(i).seed());
      }
    }
  }

  @Nested
  class GeneratorChecks {

    @Test
    @DisplayName("every generated level is solvable, checked apart from whether an agent solves it")
    void everyLevelIsSolvable() {
      Simulation.SolvabilityTally tally = Simulation.checkSolvability(SETUP, 30, 4);

      assertEquals(120, tally.generated());
      assertEquals(120, tally.solvable());
      assertEquals(1.0, tally.solvableFraction());
    }

    @Test
    @DisplayName("the partitioning generator never has to discard a layout")
    void noLayoutIsEverRegenerated() {
      assertEquals(0, Simulation.checkSolvability(SETUP, 30, 4).regenerations());
    }

    @Test
    void anEmptyTallyCountsAsEntirelySolvable() {
      assertEquals(1.0, new Simulation.SolvabilityTally(0, 0, 0).solvableFraction());
    }
  }

  @Nested
  class Reporting {

    private static SimulationReport reportOf(List<RunResult> results) {
      return new SimulationReport(SETUP, results, new Simulation.SolvabilityTally(10, 10, 0));
    }

    private static RunResult run(long seed, int turns, int level, int score, boolean died) {
      return new RunResult(seed, "test", turns, level, score, died);
    }

    @Test
    @DisplayName("the clear rate for a level counts only the runs that got there")
    void clearRateIsRelativeToRunsThatReachedTheLevel() {
      // Four runs reach level 1; two get past it. Of those two, one gets past level 2.
      SimulationReport report =
          reportOf(
              List.of(
                  run(1, 10, 1, 0, true),
                  run(2, 20, 1, 0, true),
                  run(3, 30, 2, 5, true),
                  run(4, 40, 3, 10, true)));

      assertEquals(0.5, report.clearRateForLevel(1));
      assertEquals(0.5, report.clearRateForLevel(2));
      assertEquals(0.0, report.clearRateForLevel(3));
    }

    @Test
    void anUnreachedLevelHasNoClearRate() {
      assertEquals(0.0, reportOf(List.of(run(1, 5, 1, 0, true))).clearRateForLevel(9));
    }

    @Test
    void reportsTheDeepestLevelAnyoneReached() {
      assertEquals(
          4, reportOf(List.of(run(1, 5, 2, 0, true), run(2, 5, 4, 0, true))).deepestLevel());
    }

    @Test
    @DisplayName("survival is reported as percentiles, since an average hides the spread")
    void reportsSurvivalPercentiles() {
      SimulationReport report =
          reportOf(
              List.of(
                  run(1, 1, 1, 0, true),
                  run(2, 10, 1, 0, true),
                  run(3, 100, 1, 0, true),
                  run(4, 1000, 1, 0, false)));

      assertEquals(1, report.survivalTurnsAt(0));
      assertEquals(1000, report.survivalTurnsAt(100));
      assertTrue(report.survivalTurnsAt(50) >= 1 && report.survivalTurnsAt(50) <= 1000);
    }

    @Test
    void reportsHowOftenTheHeroIsCaught() {
      assertEquals(
          0.5,
          reportOf(List.of(run(1, 5, 1, 0, true), run(2, 5, 1, 0, false))).deathRate());
    }

    @Test
    void reportsTheAverageScore() {
      assertEquals(
          15.0, reportOf(List.of(run(1, 5, 1, 10, true), run(2, 5, 1, 20, true))).averageScore());
    }

    @Test
    @DisplayName("the shortest run is named, because it is the one worth replaying first")
    void namesTheShortestRun() {
      RunResult shortest =
          reportOf(List.of(run(1, 50, 1, 0, true), run(77, 2, 1, 0, true))).shortestRun();
      assertEquals(77L, shortest.seed());
      assertEquals(2, shortest.turns());
    }

    @Test
    void anEmptyBatchReportsZeroesRatherThanFailing() {
      SimulationReport empty = reportOf(List.of());
      assertEquals(0.0, empty.deathRate());
      assertEquals(0.0, empty.averageScore());
      assertEquals(0, empty.survivalTurnsAt(50));
      assertEquals(1, empty.deepestLevel());
    }

    @Test
    void theRenderedReportCoversEverySection() {
      String text =
          reportOf(List.of(run(1, 10, 2, 5, true), run(2, 40, 1, 0, false))).render();

      assertTrue(text.contains("Clear rate by level"));
      assertTrue(text.contains("Survival turns"));
      assertTrue(text.contains("Outcomes"));
      assertTrue(text.contains("Generator"));
      assertTrue(text.contains("28x18"));
      assertTrue(text.contains("solvable"));
    }

    @Test
    @DisplayName("the report names the difficulty it was run at")
    void theRenderedReportNamesTheDifficulty() {
      SimulationReport hard =
          new SimulationReport(
              SETUP.withDifficulty(Difficulty.HARD),
              List.of(run(1, 10, 1, 0, true)),
              new Simulation.SolvabilityTally(1, 1, 0));
      assertTrue(hard.render().contains("HARD"));
    }
  }

  @Nested
  class CommandLine {

    @Test
    void resolvesEveryAgentByName() {
      assertEquals("random", Simulate.agentNamed("random", 1L).name());
      assertEquals("exit-runner", Simulate.agentNamed("exit-runner", 1L).name());
      assertEquals("treasure-hunter", Simulate.agentNamed("treasure-hunter", 1L).name());
    }

    @Test
    @DisplayName("an unknown agent says what the options are")
    void rejectsAnUnknownAgent() {
      IllegalArgumentException failure =
          assertThrows(IllegalArgumentException.class, () -> Simulate.agentNamed("genius", 1L));
      assertTrue(failure.getMessage().contains("treasure-hunter"));
    }

    @Test
    void theUsageTextExplainsEveryOption() {
      String usage = Simulate.usage();
      for (String option :
          List.of("--games", "--agent", "--seed", "--max-turns", "--hard", "--help")) {
        assertTrue(usage.contains(option), "Usage does not mention " + option);
      }
    }
  }
}
