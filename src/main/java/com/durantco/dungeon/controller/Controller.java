package com.durantco.dungeon.controller;

public interface Controller {

  void setHardMode(boolean hardMode);

  void moveUp();

  void moveDown();

  void moveLeft();

  void moveRight();

  void startGame();

  /** Takes back the last move, if there is one. */
  void undo();
}
