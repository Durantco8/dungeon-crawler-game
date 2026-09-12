package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.Board;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.LevelSpec;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import java.util.ArrayList;
import com.durantco.dungeon.persistence.HighScoreStore;
import com.durantco.dungeon.persistence.InMemoryHighScoreStore;
import java.util.List;
import java.util.Random;

public class ModelImpl implements Model {
  private Board board;
  private int currentScore;
  private final HighScoreStore highScores;
  private int level;
  private Model.STATUS status;
  private List<Observer> observers;
  private boolean hardMode = false;

  /** Creates a model over a fresh board with unseeded randomness, for production use. */
  public ModelImpl(int width, int height) {
    this(new BoardImpl(width, height));
  }

  /**
   * Creates a model over a fresh board with a caller-supplied source of randomness. Seed the
   * randomness to make a whole game reproducible.
   *
   * @param width board width in cells
   * @param height board height in cells
   * @param rng the randomness handed to the board
   */
  public ModelImpl(int width, int height, Random rng) {
    this(new BoardImpl(width, height, rng));
  }

  /**
   * Creates a model over an existing board.
   *
   * @param board the board to drive
   */
  public ModelImpl(Board board) {
    this(board, new InMemoryHighScoreStore());
  }

  /**
   * Creates a model over an existing board, keeping its best score somewhere that outlives the process.
   *
   * @param board the board to drive
   * @param highScores where the best score is read from and recorded to
   */
  public ModelImpl(Board board, HighScoreStore highScores) {
    this.board = board;
    this.currentScore = 0;
    this.highScores = highScores;
    this.level = 0;
    this.status = STATUS.END_GAME;
    this.observers = new ArrayList<>();
  }

  private void notifyObservers() {
    for (Observer o : observers) {
      o.update();
    }
  }

  @Override
  public void setHardMode(boolean hardMode) {
    this.hardMode = hardMode;
    board.setHardMode(hardMode);
    notifyObservers();
  }

  @Override
  public boolean isHardMode() {
    return hardMode;
  }

  @Override
  public int getWidth() {
    return board.getWidth();
  }

  @Override
  public int getHeight() {
    return board.getHeight();
  }

  @Override
  public Piece get(Posn p) {
    return board.get(p);
  }

  @Override
  public int getCurScore() {
    return currentScore;
  }

  @Override
  public int getHighScore() {
    return highScores.highest();
  }

  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public STATUS getStatus() {
    return status;
  }

  @Override
  public void startGame() {
    this.status = STATUS.IN_PROGRESS;
    this.currentScore = 0;
    this.level = 1;
    buildCurrentLevel();
    notifyObservers();
  }

  /**
   * Populates the board for the current level, ending the game if the level's pieces will not fit.
   * Asking the board whether the level fits replaces catching an exception thrown from init.
   */
  private void buildCurrentLevel() {
    LevelSpec spec = LevelSpec.forLevel(level);
    if (!board.canFit(spec)) {
      endGame();
      return;
    }
    board.init(spec);
  }

  @Override
  public void endGame() {
    this.status = STATUS.END_GAME;
    // The store keeps only an improvement, so there is no comparison to duplicate here.
    highScores.record(currentScore);
    notifyObservers();
  }

  @Override
  public void moveUp() {
    move(-1, 0);
  }

  @Override
  public void moveDown() {
    move(1, 0);
  }

  @Override
  public void moveLeft() {
    move(0, -1);
  }

  @Override
  public void moveRight() {
    move(0, 1);
  }

  /**
   * Applies one hero move and resolves whatever it caused: scoring, advancing a level, or ending the
   * game. Observers are notified once, whether or not the move changed anything.
   *
   * @param drow rows to move, positive is downward
   * @param dcol columns to move, positive is rightward
   */
  private void move(int drow, int dcol) {
    CollisionResult result = board.moveHero(drow, dcol);
    currentScore += result.getPoints();
    if (result.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      level++;
      buildCurrentLevel();
    } else if (result.getResults() == CollisionResult.Result.GAME_OVER) {
      endGame();
    }
    notifyObservers();
  }

  @Override
  public void addObserver(Observer o) {
    observers.add(o);
  }
}
