package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.Board;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.CollisionResult;
import com.durantco.dungeon.model.pieces.Piece;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class ModelImpl implements Model {
  private Board board;
  private int currentScore;
  private int highScore;
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
    this.board = board;
    this.currentScore = 0;
    this.highScore = 0;
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
    return highScore;
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
    try {
      board.init(level + 1, 2, 2);
    } catch (IllegalArgumentException e) {
      endGame();
    }
    notifyObservers();
  }

  @Override
  public void endGame() {
    this.status = STATUS.END_GAME;
    if (currentScore > highScore) {
      highScore = currentScore;
    }
    notifyObservers();
  }

  @Override
  public void moveUp() {
    CollisionResult result = board.moveHero(-1, 0);
    currentScore += result.getPoints();
    if (result.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      level++;
      try {
        board.init(level + 1, 2, 2);
      } catch (IllegalArgumentException e) {
        endGame();
      }
    } else if (result.getResults() == CollisionResult.Result.GAME_OVER) {
      endGame();
    }
    notifyObservers();
  }

  @Override
  public void moveDown() {
    CollisionResult result = board.moveHero(1, 0);
    currentScore += result.getPoints();
    if (result.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      level++;
      try {
        board.init(level + 1, 2, 2);
      } catch (IllegalArgumentException e) {
        endGame();
      }
    } else if (result.getResults() == CollisionResult.Result.GAME_OVER) {
      endGame();
    }
    notifyObservers();
  }

  @Override
  public void moveLeft() {
    CollisionResult result = board.moveHero(0, -1);
    currentScore += result.getPoints();
    if (result.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      level++;
      try {
        board.init(level + 1, 2, 2);
      } catch (IllegalArgumentException e) {
        endGame();
      }
    } else if (result.getResults() == CollisionResult.Result.GAME_OVER) {
      endGame();
    }
    notifyObservers();
  }

  @Override
  public void moveRight() {
    CollisionResult result = board.moveHero(0, 1);
    currentScore += result.getPoints();
    if (result.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      level++;
      try {
        board.init(level + 1, 2, 2);
      } catch (IllegalArgumentException e) {
        endGame();
      }
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
