package com.durantco.dungeon.model.board;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates rooms and connecting corridors by binary space partitioning.
 *
 * <p>The board starts solid. It is recursively cut in two along whichever axis is longer, until a
 * region is too small to cut or the depth limit is reached. Each resulting leaf has a room carved
 * inside it, with a one-cell margin so neighbouring rooms never merge. As the recursion unwinds, each
 * node joins its two children with an L-shaped corridor.
 *
 * <p>Connecting on the way back up is what makes the dungeon <em>connected by construction</em>: every
 * node links its two subtrees, so by induction the whole floor is one connected region. The
 * reachability check that runs afterwards is therefore an assertion about this property, not a repair
 * step — if it ever fails, this generator has a bug.
 */
public final class BspLevelGenerator implements LevelGenerator {

  private static final int DEFAULT_MIN_ROOM_SIZE = 3;
  private static final int DEFAULT_MAX_DEPTH = 4;

  /** Bias towards splitting the longer axis, as a quarter-steps ratio: 5/4 means 25% longer. */
  private static final int ELONGATION_NUMERATOR = 5;

  private static final int ELONGATION_DENOMINATOR = 4;

  private final int minRoomSize;
  private final int maxDepth;

  /**
   * @param minRoomSize the smallest width or height any room may have
   * @param maxDepth how many times the board may be cut in two
   */
  public BspLevelGenerator(int minRoomSize, int maxDepth) {
    if (minRoomSize < 1) {
      throw new IllegalArgumentException("Rooms must be at least one cell across");
    }
    if (maxDepth < 0) {
      throw new IllegalArgumentException("Split depth cannot be negative");
    }
    this.minRoomSize = minRoomSize;
    this.maxDepth = maxDepth;
  }

  /** A generator with room and depth settings suited to a board of a few hundred cells. */
  public BspLevelGenerator() {
    this(DEFAULT_MIN_ROOM_SIZE, DEFAULT_MAX_DEPTH);
  }

  @Override
  public DungeonLayout generate(int width, int height, Random rng) {
    if (width < minLeafSize() || height < minLeafSize()) {
      throw new IllegalArgumentException(
          "A "
              + width
              + "x"
              + height
              + " board cannot hold a "
              + minRoomSize
              + "-cell room with its margin; at least "
              + minLeafSize()
              + " in each direction is needed");
    }
    boolean[][] walkable = new boolean[height][width]; // everything starts solid
    List<Room> rooms = new ArrayList<>();
    partition(new Rect(0, 0, width, height), 0, walkable, rooms, rng);
    return new DungeonLayout(walkable, rooms);
  }

  @Override
  public int guaranteedWalkableCells(int width, int height) {
    if (width < minLeafSize() || height < minLeafSize()) {
      return 0;
    }
    // Every leaf carves a room of at least minRoomSize squared, and corridors only add to that, so
    // the fewest leaves any partition of this board can produce gives a sound lower bound.
    return minimumLeaves(width, height, 0, new HashMap<>()) * minRoomSize * minRoomSize;
  }

  /** A room needs a one-cell margin on each side, so a leaf is at least this big. */
  private int minLeafSize() {
    return minRoomSize + 2;
  }

  /**
   * Recursively cuts the region in two and carves a room in every leaf.
   *
   * @return a room from this subtree, which the caller joins to its sibling's
   */
  private Room partition(
      Rect area, int depth, boolean[][] walkable, List<Room> rooms, Random rng) {
    boolean canSplitRows = area.height() >= 2 * minLeafSize();
    boolean canSplitCols = area.width() >= 2 * minLeafSize();

    if (depth >= maxDepth || (!canSplitRows && !canSplitCols)) {
      Room room = carveRoom(area, walkable, rng);
      rooms.add(room);
      return room;
    }

    Rect first;
    Rect second;
    if (splitAlongRows(area, canSplitRows, canSplitCols, rng)) {
      int cut = chooseCut(area.height(), rng);
      first = new Rect(area.row(), area.col(), area.width(), cut);
      second = new Rect(area.row() + cut, area.col(), area.width(), area.height() - cut);
    } else {
      int cut = chooseCut(area.width(), rng);
      first = new Rect(area.row(), area.col(), cut, area.height());
      second = new Rect(area.row(), area.col() + cut, area.width() - cut, area.height());
    }

    Room firstRoom = partition(first, depth + 1, walkable, rooms, rng);
    Room secondRoom = partition(second, depth + 1, walkable, rooms, rng);
    carveCorridor(firstRoom.center(), secondRoom.center(), walkable, rng);
    return firstRoom;
  }

  private boolean splitAlongRows(Rect area, boolean canSplitRows, boolean canSplitCols, Random rng) {
    if (!canSplitCols) {
      return true;
    }
    if (!canSplitRows) {
      return false;
    }
    if (area.height() * ELONGATION_DENOMINATOR > area.width() * ELONGATION_NUMERATOR) {
      return true; // clearly taller than wide
    }
    if (area.width() * ELONGATION_DENOMINATOR > area.height() * ELONGATION_NUMERATOR) {
      return false; // clearly wider than tall
    }
    return rng.nextBoolean();
  }

  /** Picks a cut leaving both sides big enough to hold a room. */
  private int chooseCut(int length, Random rng) {
    int lowest = minLeafSize();
    int highest = length - minLeafSize();
    return lowest + rng.nextInt(highest - lowest + 1);
  }

  private Room carveRoom(Rect area, boolean[][] walkable, Random rng) {
    int widestRoom = area.width() - 2;
    int tallestRoom = area.height() - 2;
    int roomWidth = minRoomSize + rng.nextInt(widestRoom - minRoomSize + 1);
    int roomHeight = minRoomSize + rng.nextInt(tallestRoom - minRoomSize + 1);
    int col = area.col() + 1 + rng.nextInt(area.width() - roomWidth - 1);
    int row = area.row() + 1 + rng.nextInt(area.height() - roomHeight - 1);

    Room room = new Room(row, col, roomWidth, roomHeight);
    for (int r = row; r < row + roomHeight; r++) {
      for (int c = col; c < col + roomWidth; c++) {
        walkable[r][c] = true;
      }
    }
    return room;
  }

  /** Joins two cells with an L-shaped corridor, turning either at the start or at the end. */
  private void carveCorridor(Posn from, Posn to, boolean[][] walkable, Random rng) {
    if (rng.nextBoolean()) {
      carveRow(from.row(), from.col(), to.col(), walkable);
      carveColumn(to.col(), from.row(), to.row(), walkable);
    } else {
      carveColumn(from.col(), from.row(), to.row(), walkable);
      carveRow(to.row(), from.col(), to.col(), walkable);
    }
  }

  private void carveRow(int row, int fromCol, int toCol, boolean[][] walkable) {
    for (int col = Math.min(fromCol, toCol); col <= Math.max(fromCol, toCol); col++) {
      walkable[row][col] = true;
    }
  }

  private void carveColumn(int col, int fromRow, int toRow, boolean[][] walkable) {
    for (int row = Math.min(fromRow, toRow); row <= Math.max(fromRow, toRow); row++) {
      walkable[row][col] = true;
    }
  }

  /**
   * The fewest leaves any partition of this region can yield, minimised over every axis and cut the
   * generator could choose. Memoised because the same region size recurs across many cuts.
   */
  private int minimumLeaves(int width, int height, int depth, Map<Long, Integer> memo) {
    boolean canSplitRows = height >= 2 * minLeafSize();
    boolean canSplitCols = width >= 2 * minLeafSize();
    if (depth >= maxDepth || (!canSplitRows && !canSplitCols)) {
      return 1;
    }
    long key = (((long) depth * 4096 + width) * 4096) + height;
    Integer cached = memo.get(key);
    if (cached != null) {
      return cached;
    }

    int fewest = Integer.MAX_VALUE;
    if (canSplitRows) {
      for (int cut = minLeafSize(); cut <= height - minLeafSize(); cut++) {
        int total =
            minimumLeaves(width, cut, depth + 1, memo)
                + minimumLeaves(width, height - cut, depth + 1, memo);
        fewest = Math.min(fewest, total);
      }
    }
    if (canSplitCols) {
      for (int cut = minLeafSize(); cut <= width - minLeafSize(); cut++) {
        int total =
            minimumLeaves(cut, height, depth + 1, memo)
                + minimumLeaves(width - cut, height, depth + 1, memo);
        fewest = Math.min(fewest, total);
      }
    }

    memo.put(key, fewest);
    return fewest;
  }

  /** A region of the board being partitioned, addressed by its top-left cell. */
  private record Rect(int row, int col, int width, int height) {}
}
