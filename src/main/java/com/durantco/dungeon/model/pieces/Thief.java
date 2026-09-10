package com.durantco.dungeon.model.pieces;

public class Thief extends APiece {
  public Thief() {
    super("Thief", "thief.png");
  }

  public int getValue() {
    return -5;
  }
}
