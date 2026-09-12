package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.GameSetup;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Turns a batch of runs into something readable.
 *
 * <p>Reports a distribution rather than an average for survival, because the average of a run that dies on
 * turn three and one that survives four hundred describes neither. Percentiles say what actually tends to
 * happen.
 */
public final class SimulationReport {

  private final GameSetup setup;
  private final List<RunResult> results;
  private final Simulation.SolvabilityTally solvability;

  /**
   * @param setup the game the batch was played on
   * @param results what happened in each run
   * @param solvability the generator's own tally, checked independently of play
   */
  public SimulationReport(
      GameSetup setup, List<RunResult> results, Simulation.SolvabilityTally solvability) {
    this.setup = setup;
    this.results = List.copyOf(results);
    this.solvability = solvability;
  }

  /**
   * The share of runs reaching a level that went on to clear it.
   *
   * @param level the level in question
   * @return a fraction between zero and one, or zero if no run got that far
   */
  public double clearRateForLevel(int level) {
    int reached = 0;
    int cleared = 0;
    for (RunResult result : results) {
      if (result.levelReached() >= level) {
        reached++;
      }
      if (result.levelReached() > level) {
        cleared++;
      }
    }
    if (reached == 0) {
      return 0.0;
    }
    return (double) cleared / reached;
  }

  /**
   * @return the highest level any run reached
   */
  public int deepestLevel() {
    int deepest = 1;
    for (RunResult result : results) {
      deepest = Math.max(deepest, result.levelReached());
    }
    return deepest;
  }

  /**
   * @param percentile which percentile, from 0 to 100
   * @return the number of turns survived at that percentile
   */
  public int survivalTurnsAt(int percentile) {
    if (results.isEmpty()) {
      return 0;
    }
    List<Integer> turns = new ArrayList<>();
    for (RunResult result : results) {
      turns.add(result.turns());
    }
    turns.sort(Integer::compareTo);
    int index = percentile * (turns.size() - 1) / 100;
    return turns.get(index);
  }

  /**
   * @return the share of runs in which the hero was caught
   */
  public double deathRate() {
    if (results.isEmpty()) {
      return 0.0;
    }
    int died = 0;
    for (RunResult result : results) {
      if (result.died()) {
        died++;
      }
    }
    return (double) died / results.size();
  }

  /**
   * @return the mean score across runs
   */
  public double averageScore() {
    if (results.isEmpty()) {
      return 0.0;
    }
    int total = 0;
    for (RunResult result : results) {
      total += result.score();
    }
    return (double) total / results.size();
  }

  /**
   * @return the seed and outcome of the shortest run, which is the one worth replaying first
   */
  public RunResult shortestRun() {
    RunResult shortest = results.get(0);
    for (RunResult result : results) {
      if (result.turns() < shortest.turns()) {
        shortest = result;
      }
    }
    return shortest;
  }

  /**
   * @return the whole report as text
   */
  public String render() {
    StringBuilder out = new StringBuilder();
    String agent = "none";
    if (!results.isEmpty()) {
      agent = results.get(0).agentName();
    }
    out.append("Dungeon Crawler simulation\n");
    out.append("==========================\n");
    out.append(line("agent", agent));
    out.append(line("games", Integer.toString(results.size())));
    out.append(line("board", setup.width() + "x" + setup.height()));
    out.append(line("difficulty", setup.difficulty().name()));
    out.append(line("first seed", Long.toString(setup.seed())));
    out.append('\n');

    out.append("Clear rate by level\n");
    out.append("-------------------\n");
    for (int level = 1; level <= deepestLevel(); level++) {
      int reached = 0;
      for (RunResult result : results) {
        if (result.levelReached() >= level) {
          reached++;
        }
      }
      out.append(
          String.format(
              Locale.ROOT,
              "  level %-3d reached by %-5d cleared %5.1f%%%n",
              level,
              reached,
              clearRateForLevel(level) * 100));
    }
    out.append('\n');

    out.append("Survival turns\n");
    out.append("--------------\n");
    for (int percentile : new int[] {10, 25, 50, 75, 90, 100}) {
      out.append(
          String.format(
              Locale.ROOT, "  p%-4d %6d turns%n", percentile, survivalTurnsAt(percentile)));
    }
    out.append('\n');

    out.append("Outcomes\n");
    out.append("--------\n");
    out.append(String.format(Locale.ROOT, "  hero caught   %5.1f%%%n", deathRate() * 100));
    out.append(String.format(Locale.ROOT, "  average score %8.1f%n", averageScore()));
    if (!results.isEmpty()) {
      RunResult shortest = shortestRun();
      out.append(
          String.format(
              Locale.ROOT,
              "  shortest run  %d turns on seed %d, replay that seed to see it%n",
              shortest.turns(),
              shortest.seed()));
    }
    out.append('\n');

    out.append("Generator\n");
    out.append("---------\n");
    out.append(
        String.format(
            Locale.ROOT,
            "  levels built  %d%n  solvable      %.1f%%%n  regenerations %d%n",
            solvability.generated(),
            solvability.solvableFraction() * 100,
            solvability.regenerations()));
    return out.toString();
  }

  private static String line(String label, String value) {
    return String.format(Locale.ROOT, "  %-12s %s%n", label, value);
  }
}
