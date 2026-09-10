package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.Posn;

/** Shared naming and position state for pieces. */
public abstract class APiece implements Piece {
  private final String name;
  private final String resourcePath;
  private Posn position;

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

  /*
   * onHeroEnter and onEnemyEnter are deliberately left abstract rather than given blocking
   * defaults. A new piece type must state both outcomes explicitly, so forgetting one is a
   * compile error instead of a silently impassable cell.
   */
}
