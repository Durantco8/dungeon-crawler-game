package com.durantco.dungeon.view;

import com.durantco.dungeon.model.pieces.PieceType;
import java.util.EnumMap;
import java.util.Map;

/**
 * Maps a piece kind to its sprite resource for the current theme.
 *
 * <p>This is the only place in the application that knows sprite filenames. The model deals in
 * {@link PieceType} and has no knowledge of image assets.
 */
public final class PieceSprites {

  private static final Map<PieceType, String> BASE_NAMES = new EnumMap<>(PieceType.class);

  static {
    BASE_NAMES.put(PieceType.HERO, "hero");
    BASE_NAMES.put(PieceType.ENEMY, "enemy");
    BASE_NAMES.put(PieceType.WALL, "wall");
    BASE_NAMES.put(PieceType.EXIT, "exit");
    BASE_NAMES.put(PieceType.TREASURE, "treasure");
    BASE_NAMES.put(PieceType.THIEF, "thief");
  }

  private PieceSprites() {}

  /**
   * Resolves the sprite resource for a piece kind.
   *
   * @param type the kind of piece to draw
   * @param darkMode whether the dark theme is active
   * @return the classpath-relative resource name of the sprite
   * @throws IllegalStateException if a piece kind has no registered sprite
   */
  public static String pathFor(PieceType type, boolean darkMode) {
    String base = BASE_NAMES.get(type);
    if (base == null) {
      throw new IllegalStateException("No sprite registered for piece type " + type);
    }
    if (darkMode) {
      return base + ".png";
    }
    return base + "-light.png";
  }
}
