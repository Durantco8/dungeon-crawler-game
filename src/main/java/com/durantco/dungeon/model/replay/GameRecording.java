package com.durantco.dungeon.model.replay;

import com.durantco.dungeon.model.GameSetup;
import java.util.ArrayList;
import java.util.List;

/**
 * A complete account of a game: how it was set up, and every input since it began.
 *
 * <p>Because the game is reproducible from its seed, this is not a summary of a game but the whole
 * thing. Replaying it reconstructs every dungeon, every enemy's archetype and pace, and every arbitrary
 * choice the board made. That one fact is what makes saving, loading, replaying, and undoing the same
 * problem rather than four.
 *
 * <p>Immutable: adding a move returns a new recording. Undo is then simply an older recording, with no
 * inverse operations to get wrong and no state to restore.
 *
 * @param setup how the game was built
 * @param moves the inputs made since it began, oldest first
 */
public record GameRecording(GameSetup setup, List<PlayerMove> moves) {

  public GameRecording {
    if (setup == null) {
      throw new IllegalArgumentException("A recording needs a setup");
    }
    moves = List.copyOf(moves);
  }

  /**
   * @param setup how the game is built
   * @return a recording of that game before any input
   */
  public static GameRecording startingFrom(GameSetup setup) {
    return new GameRecording(setup, List.of());
  }

  /**
   * @param move the input to append
   * @return a recording with that input on the end
   */
  public GameRecording plus(PlayerMove move) {
    List<PlayerMove> extended = new ArrayList<>(moves);
    extended.add(move);
    return new GameRecording(setup, extended);
  }

  /**
   * @return this recording without its last input, or the same recording if there is none
   */
  public GameRecording withoutLastMove() {
    if (moves.isEmpty()) {
      return this;
    }
    return new GameRecording(setup, moves.subList(0, moves.size() - 1));
  }

  /**
   * @return a recording of the same game before any input
   */
  public GameRecording rewound() {
    return startingFrom(setup);
  }

  /**
   * @param setup the setup to record against
   * @return the same inputs against a different setup
   */
  public GameRecording withSetup(GameSetup setup) {
    return new GameRecording(setup, moves);
  }

  /**
   * @return how many inputs have been recorded
   */
  public int moveCount() {
    return moves.size();
  }

  /**
   * @return true if no input has been recorded yet
   */
  public boolean isAtStart() {
    return moves.isEmpty();
  }
}
