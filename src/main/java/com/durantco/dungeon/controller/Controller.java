package com.durantco.dungeon.controller;

public interface Controller {

  void setHardMode(boolean hardMode);

  void moveUp();

  void moveDown();

  void moveLeft();

  void moveRight();

  void startGame();
}
