package com.durantco.dungeon.model.board;

/**
 * A recorded description of which level generator a game uses, and how it is configured.
 *
 * <p>A replay has to rebuild the exact dungeon the original game played, so the generator cannot be
 * chosen at the composition root and forgotten: it has to travel with the game as data.
 *
 * <p>Sealed on purpose. Generators are few and slow to change, and exhaustiveness is a feature here
 * rather than a cost: adding one produces a compile error at every point that has to persist or rebuild
 * a spec, which is exactly where silent omissions would otherwise break replay. That is the opposite
 * trade-off from collision dispatch, where piece types grow often and adding one must not force edits
 * elsewhere.
 */
public sealed interface GeneratorSpec {

  /**
   * @return a generator configured as this spec describes
   */
  LevelGenerator create();

  /**
   * Rooms and corridors by binary space partitioning.
   *
   * @param minRoomSize the smallest width or height any room may have
   * @param maxSplitDepth how many times the board may be cut in two
   */
  record Bsp(int minRoomSize, int maxSplitDepth) implements GeneratorSpec {
    @Override
    public LevelGenerator create() {
      return new BspLevelGenerator(minRoomSize, maxSplitDepth);
    }
  }

  /**
   * Walls scattered at random, as the game originally did.
   *
   * @param wallCount how many walls to scatter
   */
  record Scatter(int wallCount) implements GeneratorSpec {
    @Override
    public LevelGenerator create() {
      return new RandomScatterGenerator(wallCount);
    }
  }
}
