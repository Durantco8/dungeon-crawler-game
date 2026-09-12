package com.durantco.dungeon.sim;

import com.durantco.dungeon.model.GameSession;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.LevelSpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Plays games without a screen and reports what happened.
 *
 * <p>Runs go through the same session, model and board a player would drive, so what the numbers describe is
 * the real game. Every run is seeded, so any result in a report can be reproduced exactly by replaying that
 * seed, which is the difference between a statistic and a bug report.
 */
public final class Simulation {

  private Simulation() {}

  /**
   * Plays one game to its end or to a turn limit.
   *
   * @param setup the game to play
   * @param agent who plays it
   * @param maxTurns give up after this many moves, so an agent that cannot die cannot run forever
   * @return what happened
   */
  public static RunResult run(GameSetup setup, Agent agent, int maxTurns) {
    GameSession session = new GameSession(setup);
    session.startGame();

    int turns = 0;
    while (turns < maxTurns && session.getStatus() == Model.STATUS.IN_PROGRESS) {
      agent.chooseMove(session).applyTo(session);
      turns++;
    }
    boolean died = session.getStatus() == Model.STATUS.END_GAME;
    return new RunResult(
        setup.seed(), agent.name(), turns, session.getLevel(), session.getCurScore(), died);
  }

  /**
   * Plays a batch of games on consecutive seeds.
   *
   * @param base the game to play, whose seed is the first of the batch
   * @param agent who plays them
   * @param games how many to play
   * @param maxTurns the per-game turn limit
   * @return what happened in each
   */
  public static List<RunResult> runBatch(GameSetup base, Agent agent, int games, int maxTurns) {
    List<RunResult> results = new ArrayList<>(games);
    for (int i = 0; i < games; i++) {
      results.add(run(base.withSeed(base.seed() + i), agent, maxTurns));
    }
    return results;
  }

  /**
   * Checks that generated levels really are solvable, independently of whether an agent solves them.
   *
   * <p>Separate from playing on purpose. An agent failing a level says something about the agent; a level
   * being unsolvable says something about the generator, and conflating the two would hide the second behind
   * the first.
   *
   * @param base the game whose generator and size are checked
   * @param dungeons how many dungeons to generate
   * @param levelsEach how many levels to build in each
   * @return the tally
   */
  public static SolvabilityTally checkSolvability(GameSetup base, int dungeons, int levelsEach) {
    int generated = 0;
    int solvable = 0;
    int extraAttempts = 0;
    for (int i = 0; i < dungeons; i++) {
      BoardImpl board =
          new BoardImpl(
              base.width(),
              base.height(),
              new Random(base.seed() + i),
              base.generator().create());
      for (int level = 1; level <= levelsEach; level++) {
        board.init(LevelSpec.forLevel(level));
        generated++;
        if (board.isSolvable()) {
          solvable++;
        }
        extraAttempts += board.generationAttempts() - 1;
      }
    }
    return new SolvabilityTally(generated, solvable, extraAttempts);
  }

  /**
   * How many generated levels were solvable, and how much regeneration it took.
   *
   * @param generated how many levels were built
   * @param solvable how many of those could be finished
   * @param regenerations how many layouts were discarded and rebuilt
   */
  public record SolvabilityTally(int generated, int solvable, int regenerations) {

    /**
     * @return the fraction of levels that were solvable, between zero and one
     */
    public double solvableFraction() {
      if (generated == 0) {
        return 1.0;
      }
      return (double) solvable / generated;
    }
  }
}
