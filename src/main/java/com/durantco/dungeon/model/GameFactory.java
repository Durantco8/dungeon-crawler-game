package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.BoardImpl;
import java.util.Random;

/**
 * Builds a playable game from its description.
 *
 * <p>One place turns a {@link GameSetup} into a model, so the application, a replay, and the simulation
 * harness all assemble a game identically. If they each did their own wiring, a replay could differ
 * from the game it was meant to reproduce in a way no test would catch.
 */
public final class GameFactory {

  private GameFactory() {}

  /**
   * @param setup the game to build
   * @return a model ready for startGame, seeded so the whole game is reproducible
   */
  public static Model create(GameSetup setup) {
    BoardImpl board =
        new BoardImpl(
            setup.width(), setup.height(), new Random(setup.seed()), setup.generator().create());
    board.setDifficulty(setup.difficulty());
    return new ModelImpl(board);
  }
}
