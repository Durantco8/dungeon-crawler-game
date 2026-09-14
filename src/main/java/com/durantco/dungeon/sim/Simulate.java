package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import java.util.List;
import java.util.Random;

/**
 * Command line entry point for running games without a screen.
 *
 * <pre>
 *   java -cp target/classes com.durantco.dungeon.sim.Simulate --games 200 --agent treasure-hunter
 * </pre>
 *
 * <p>Separate from the application's main class, and free of JavaFX, so a batch of games can be run anywhere
 * a JVM will start: a terminal, a continuous integration job, or a machine with no display at all.
 */
public final class Simulate {

  private static final int DEFAULT_GAMES = 100;
  private static final int DEFAULT_MAX_TURNS = 500;
  private static final int DEFAULT_DUNGEONS_CHECKED = 50;
  private static final int DEFAULT_LEVELS_CHECKED = 5;

  private Simulate() {}

  /**
   * @param args command line arguments; pass --help to list them
   */
  public static void main(String[] args) {
    if (hasFlag(args, "--help")) {
      System.out.print(usage());
      return;
    }

    long seed = longArg(args, "--seed", 1L);
    int games = intArg(args, "--games", DEFAULT_GAMES);
    int maxTurns = intArg(args, "--max-turns", DEFAULT_MAX_TURNS);
    String agentName = stringArg(args, "--agent", "treasure-hunter");

    GameSetup setup = GameSetup.standard(seed);
    if (hasFlag(args, "--hard")) {
      setup = setup.withDifficulty(Difficulty.HARD);
    }

    Agent agent = agentNamed(agentName, seed);
    List<RunResult> results = Simulation.runBatch(setup, agent, games, maxTurns);
    Simulation.SolvabilityTally tally =
        Simulation.checkSolvability(setup, DEFAULT_DUNGEONS_CHECKED, DEFAULT_LEVELS_CHECKED);

    System.out.print(new SimulationReport(setup, results, tally).render());
  }

  /**
   * @param name the agent asked for
   * @param seed the seed its randomness is drawn from
   * @return that agent
   * @throws IllegalArgumentException if no agent goes by that name
   */
  static Agent agentNamed(String name, long seed) {
    return switch (name) {
      case "random" -> new RandomAgent(new Random(seed));
      case "exit-runner" -> new ExitRunnerAgent(new Random(seed));
      case "treasure-hunter" -> new TreasureHunterAgent(new Random(seed));
      case "survivor" -> new SurvivorAgent(new Random(seed));
      default ->
          throw new IllegalArgumentException(
              "Unknown agent: "
                  + name
                  + ". Try random, exit-runner, treasure-hunter or survivor.");
    };
  }

  /**
   * @return the text printed for --help
   */
  static String usage() {
    return """
        Runs dungeon crawler games with no display and reports what happened.

          --games N       how many games to play (default 100)
          --agent NAME    random, exit-runner, treasure-hunter or survivor
                          (default treasure-hunter)
          --seed N        the first seed; games use consecutive seeds from here (default 1)
          --max-turns N   give up on a game after this many moves (default 500)
          --hard          spawn hunting enemies instead of drifting ones
          --help          print this

        Every game is seeded, so any run in the report can be reproduced by replaying its seed.
        """;
  }

  private static boolean hasFlag(String[] args, String flag) {
    for (String arg : args) {
      if (arg.equals(flag)) {
        return true;
      }
    }
    return false;
  }

  private static String stringArg(String[] args, String name, String fallback) {
    for (int i = 0; i < args.length - 1; i++) {
      if (args[i].equals(name)) {
        return args[i + 1];
      }
    }
    return fallback;
  }

  private static int intArg(String[] args, String name, int fallback) {
    String value = stringArg(args, name, null);
    if (value == null) {
      return fallback;
    }
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " needs a whole number, got: " + value);
    }
  }

  private static long longArg(String[] args, String name, long fallback) {
    String value = stringArg(args, name, null);
    if (value == null) {
      return fallback;
    }
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(name + " needs a whole number, got: " + value);
    }
  }
}
