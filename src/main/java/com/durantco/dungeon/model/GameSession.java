package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.Difficulty;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.replay.GameRecording;
import com.durantco.dungeon.model.replay.GameReplay;
import com.durantco.dungeon.model.replay.PlayerMove;
import com.durantco.dungeon.persistence.HighScoreStore;
import com.durantco.dungeon.persistence.InMemoryHighScoreStore;
import java.util.ArrayList;
import java.util.List;

/**
 * A game being played, together with the record of how it got there.
 *
 * <p>Presents itself as a {@link Model} and forwards to whichever model is current, so the view observes
 * the session and never notices that undo replaces the model underneath it. That indirection is what undo
 * needs: rebuilding a game produces a new model, and a view holding the old one directly would be left
 * watching a game nobody is playing.
 *
 * <p>Undo works by dropping the last input and replaying what remains. There are no inverse operations and
 * nothing is snapshotted, which matters because enemies carry state: where they last saw the hero, how far
 * along a patrol they are, how much energy they have banked. A snapshot would have to capture all of it,
 * and every new archetype would be one more thing to remember to save. Replaying reconstructs it because
 * it reconstructs everything.
 *
 * <p>The cost is that undo is proportional to the length of the game rather than constant. For a game of a
 * few hundred moves that is a few milliseconds, which is a good trade for not being able to get it wrong.
 */
public final class GameSession implements Model {

  private final HighScoreStore highScores;
  private final List<Observer> observers = new ArrayList<>();

  private GameRecording recording;
  private Model current;

  /**
   * @param setup the game to play
   * @param highScores where the best score is kept
   */
  public GameSession(GameSetup setup, HighScoreStore highScores) {
    this.highScores = highScores;
    this.recording = GameRecording.startingFrom(setup);
    this.current = watch(GameFactory.create(setup, highScores));
  }

  /**
   * @param setup the game to play, keeping scores only for this process
   */
  public GameSession(GameSetup setup) {
    this(setup, new InMemoryHighScoreStore());
  }

  /**
   * @return the game as played so far, which can be saved and replayed
   */
  public GameRecording recording() {
    return recording;
  }

  /**
   * Continues a previously recorded game, as happens when a save is loaded.
   *
   * @param recording the game to take up
   */
  public void resume(GameRecording recording) {
    this.recording = recording;
    this.current = watch(GameReplay.replay(recording, highScores));
    notifyObservers();
  }

  @Override
  public void undo() {
    if (recording.isAtStart()) {
      return; // nothing to take back
    }
    this.recording = recording.withoutLastMove();
    this.current = watch(GameReplay.replay(recording, highScores));
    notifyObservers();
  }

  @Override
  public boolean canUndo() {
    return !recording.isAtStart();
  }

  @Override
  public void startGame() {
    // Rebuild rather than re-initialise, so a second game on this session is the same game a fresh
    // session with this seed would have played. Re-initialising would carry the first game's spent
    // randomness into the second.
    this.recording = recording.rewound();
    this.current = watch(GameFactory.create(recording.setup(), highScores));
    current.startGame();
  }

  @Override
  public void endGame() {
    current.endGame();
  }

  @Override
  public void setHardMode(boolean hardMode) {
    Difficulty difficulty = Difficulty.EASY;
    if (hardMode) {
      difficulty = Difficulty.HARD;
    }
    // Difficulty describes what the levels contain, so it belongs to the setup rather than the inputs.
    this.recording = recording.withSetup(recording.setup().withDifficulty(difficulty));
    current.setHardMode(hardMode);
  }

  @Override
  public void moveUp() {
    apply(PlayerMove.UP);
  }

  @Override
  public void moveDown() {
    apply(PlayerMove.DOWN);
  }

  @Override
  public void moveLeft() {
    apply(PlayerMove.LEFT);
  }

  @Override
  public void moveRight() {
    apply(PlayerMove.RIGHT);
  }

  @Override
  public boolean isHardMode() {
    return current.isHardMode();
  }

  @Override
  public int getWidth() {
    return current.getWidth();
  }

  @Override
  public int getHeight() {
    return current.getHeight();
  }

  @Override
  public Piece get(Posn p) {
    return current.get(p);
  }

  @Override
  public boolean heroCanEnter(Posn p) {
    return current.heroCanEnter(p);
  }

  @Override
  public int getCurScore() {
    return current.getCurScore();
  }

  @Override
  public int getHighScore() {
    return current.getHighScore();
  }

  @Override
  public int getLevel() {
    return current.getLevel();
  }

  @Override
  public STATUS getStatus() {
    return current.getStatus();
  }

  @Override
  public void addObserver(Observer o) {
    observers.add(o);
  }

  /**
   * Records an input and carries it out. A refused move is recorded like any other: whether it is refused
   * is the board's answer, and a replay has to reproduce the game move for move regardless.
   */
  private void apply(PlayerMove move) {
    this.recording = recording.plus(move);
    move.applyTo(current);
  }

  /**
   * Subscribes to a model's updates so they reach this session's own observers.
   *
   * <p>Subscribed after any replay has finished, so reconstructing a game does not fire one notification
   * per replayed move at a view that only cares about the result.
   */
  private Model watch(Model model) {
    model.addObserver(this::notifyObservers);
    return model;
  }

  private void notifyObservers() {
    for (Observer o : observers) {
      o.update();
    }
  }
}
