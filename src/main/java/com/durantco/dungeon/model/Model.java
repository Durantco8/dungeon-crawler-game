package com.durantco.dungeon.model;

import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;

public interface Model extends Subject {

  void setHardMode(boolean hardMode);

  boolean isHardMode();

  int getWidth();

  int getHeight();

  Piece get(Posn p);

  int getCurScore();

  int getHighScore();

  int getLevel();

  // Change status from IN_PROGRESS and END_GAME
  STATUS getStatus();

  void startGame();

  void endGame();

  void moveUp();

  void moveDown();

  void moveLeft();

  void moveRight();

  enum STATUS {
    END_GAME,
    IN_PROGRESS
  }
}
