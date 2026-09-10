package com.durantco.dungeon.model.pieces;

public class Treasure extends APiece {

  public Treasure() {
    super("Treasure", "treasure.png");
  }

  public int getValue() {
    return 5;
  }
}
