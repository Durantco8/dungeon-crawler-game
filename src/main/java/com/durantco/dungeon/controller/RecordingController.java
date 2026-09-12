package com.durantco.dungeon.controller;

import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.PlayerMove;

/**
 * Keeps a recording of the game as it is played, by wrapping the real controller.
 *
 * <p>A decorator rather than a change to the model, so nothing below this point knows or cares that it is
 * being recorded. That matters for replay: the model a replay drives has to behave identically to the one
 * the player drove, which is easiest to guarantee when it is the same class doing the same work, reached
 * the same way.
 */
public final class RecordingController implements Controller {

  private final Controller delegate;
  private GameRecording recording;

  /**
   * @param delegate the controller doing the real work
   * @param setup the game being played, which the recording is built against
   */
  public RecordingController(Controller delegate, GameSetup setup) {
    this.delegate = delegate;
    this.recording = GameRecording.startingFrom(setup);
  }

  /**
   * @return the game as recorded so far
   */
  public GameRecording recording() {
    return recording;
  }

  /**
   * Replaces the recording, as happens when a game is loaded or a move is undone.
   *
   * @param recording the recording this controller should continue
   */
  public void setRecording(GameRecording recording) {
    this.recording = recording;
  }

  @Override
  public void startGame() {
    // A recording covers one game, so starting another discards the previous inputs while keeping the
    // setup they were played against.
    this.recording = recording.rewound();
    delegate.startGame();
  }

  @Override
  public void setHardMode(boolean hardMode) {
    Difficulty difficulty = Difficulty.EASY;
    if (hardMode) {
      difficulty = Difficulty.HARD;
    }
    // Difficulty belongs to the setup rather than the input list, because it decides what the levels
    // contain rather than what the player did in them.
    this.recording = recording.withSetup(recording.setup().withDifficulty(difficulty));
    delegate.setHardMode(hardMode);
  }

  @Override
  public void moveUp() {
    this.recording = recording.plus(PlayerMove.UP);
    delegate.moveUp();
  }

  @Override
  public void moveDown() {
    this.recording = recording.plus(PlayerMove.DOWN);
    delegate.moveDown();
  }

  @Override
  public void moveLeft() {
    this.recording = recording.plus(PlayerMove.LEFT);
    delegate.moveLeft();
  }

  @Override
  public void moveRight() {
    this.recording = recording.plus(PlayerMove.RIGHT);
    delegate.moveRight();
  }
}
