package com.durantco.dungeon.model.replay;

import com.durantco.dungeon.model.GameFactory;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.persistence.HighScoreStore;
import com.durantco.dungeon.persistence.InMemoryHighScoreStore;

/**
 * Rebuilds a game from its recording.
 *
 * <p>Deliberately this small. There is no state to restore and nothing to reconcile, because every
 * dungeon, every enemy archetype and pace, and every arbitrary choice the board makes all follow from
 * the seed. Replaying is therefore just playing the same game again with the same inputs, which is why
 * it can be trusted: the replay drives the same model classes along the same path the player did, rather
 * than a parallel implementation that might drift.
 */
public final class GameReplay {

  private GameReplay() {}

  /**
   * Plays a recording back in full.
   *
   * @param recording the game to reconstruct
   * @return a model in the state the recorded game ended in
   */
  public static Model replay(GameRecording recording) {
    return replay(recording, recording.moveCount(), new InMemoryHighScoreStore());
  }

  /**
   * Plays a recording back in full, keeping scores somewhere that outlives the game.
   *
   * @param recording the game to reconstruct
   * @param highScores where the best score is kept
   * @return a model in the state the recorded game ended in
   */
  public static Model replay(GameRecording recording, HighScoreStore highScores) {
    return replay(recording, recording.moveCount(), highScores);
  }

  /**
   * Plays a recording back up to a point, which is how a move is undone.
   *
   * @param recording the game to reconstruct
   * @param moves how many inputs to apply, counted from the start
   * @return a model in the state the game was in after that many inputs
   * @throws IllegalArgumentException if asked for a negative number of moves, or more than were recorded
   */
  public static Model replay(GameRecording recording, int moves) {
    return replay(recording, moves, new InMemoryHighScoreStore());
  }

  /**
   * Plays a recording back up to a point, which is how a move is undone.
   *
   * @param recording the game to reconstruct
   * @param moves how many inputs to apply, counted from the start
   * @param highScores where the best score is kept
   * @return a model in the state the game was in after that many inputs
   * @throws IllegalArgumentException if asked for a negative number of moves, or more than were recorded
   */
  public static Model replay(GameRecording recording, int moves, HighScoreStore highScores) {
    if (moves < 0 || moves > recording.moveCount()) {
      throw new IllegalArgumentException(
          "Cannot replay " + moves + " of " + recording.moveCount() + " recorded moves");
    }

    Model model = GameFactory.create(recording.setup(), highScores);
    model.startGame();
    for (int i = 0; i < moves; i++) {
      recording.moves().get(i).applyTo(model);
    }
    return model;
  }
}
