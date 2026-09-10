package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.Posn;

public abstract class APiece implements Piece {
  private final String name;
  private final String resourcePath;
  private Posn position;

  public APiece() {
    this.name = null;
    this.resourcePath = null;
  }

  public APiece(String name, String resourcePath) {
    this.name = name;
    this.resourcePath = resourcePath;
  }

  @Override
  public String getResourcePath() {
    return resourcePath;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public Posn getPosn() {
    return position;
  }

  @Override
  public void setPosn(Posn posn) {
    this.position = posn;
  }
}
