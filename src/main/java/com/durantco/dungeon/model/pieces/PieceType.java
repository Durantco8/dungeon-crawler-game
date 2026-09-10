package com.durantco.dungeon.model.pieces;

/**
 * The kind of a piece, as a stable identifier the view can key presentation off.
 *
 * <p>This is a rendering and labelling key only. Game rules must never switch on it: collision
 * behaviour belongs on the piece itself via {@link Piece#onHeroEnter} and {@link
 * Piece#onEnemyEnter}. Dispatching on this enum would reintroduce the instanceof chains it replaced.
 */
public enum PieceType {
  HERO("Hero"),
  ENEMY("Enemy"),
  WALL("Wall"),
  EXIT("Exit"),
  TREASURE("Treasure"),
  THIEF("Thief");

  private final String displayName;

  PieceType(String displayName) {
    this.displayName = displayName;
  }

  /**
   * @return the human-readable name of this piece kind
   */
  public String displayName() {
    return displayName;
  }
}
