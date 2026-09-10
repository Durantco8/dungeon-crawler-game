package com.durantco.dungeon.model.board;

public class Posn {
  private final int row;
  private final int col;

  public Posn(int row, int col) {
    this.row = row;
    this.col = col;
  }

  public int getRow() {
    return row;
  }

  public int getCol() {
    return col;
  }

  @Override
  public String toString() {
    return row + "," + col;
  }
}
