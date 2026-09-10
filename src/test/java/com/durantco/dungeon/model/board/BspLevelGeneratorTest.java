package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BspLevelGeneratorTest {

  private static final int WIDTH = 28;
  private static final int HEIGHT = 18;
  private static final int SEEDS = 500;

  private static DungeonLayout generate(long seed) {
    return new BspLevelGenerator().generate(WIDTH, HEIGHT, new Random(seed));
  }

  /**
   * Flood fills from the first walkable cell. Deliberately written here rather than reusing
   * production code, so this test cannot be satisfied by a bug shared with the thing it checks.
   */
  private static int reachableFrom(DungeonLayout layout, Posn start) {
    Set<Posn> seen = new HashSet<>();
    Deque<Posn> queue = new ArrayDeque<>();
    seen.add(start);
    queue.add(start);
    int[][] steps = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    while (!queue.isEmpty()) {
      Posn at = queue.removeFirst();
      for (int[] step : steps) {
        Posn next = at.offset(step[0], step[1]);
        if (layout.isWalkable(next) && seen.add(next)) {
          queue.add(next);
        }
      }
    }
    return seen.size();
  }

  private static String render(DungeonLayout layout) {
    StringBuilder out = new StringBuilder();
    for (int row = 0; row < layout.height(); row++) {
      for (int col = 0; col < layout.width(); col++) {
        if (layout.isWalkable(new Posn(row, col))) {
          out.append('.');
        } else {
          out.append('#');
        }
      }
    }
    return out.toString();
  }

  @Test
  void producesALayoutOfTheRequestedSize() {
    DungeonLayout layout = generate(1L);
    assertEquals(WIDTH, layout.width());
    assertEquals(HEIGHT, layout.height());
  }

  @Test
  @DisplayName("the floor is one connected region for every seed")
  void everySeedProducesAConnectedFloor() {
    for (int seed = 0; seed < SEEDS; seed++) {
      DungeonLayout layout = generate(seed);
      List<Posn> floor = layout.walkableCells();
      assertFalse(floor.isEmpty(), "Seed " + seed + " produced no floor at all");
      assertEquals(
          layout.walkableCount(),
          reachableFrom(layout, floor.get(0)),
          "Seed " + seed + " produced a floor with an unreachable region");
    }
  }

  @Test
  void carvesMoreThanOneRoom() {
    assertTrue(generate(1L).rooms().size() > 1);
  }

  @Test
  @DisplayName("room count never exceeds the leaves the depth limit allows")
  void respectsTheDepthLimit() {
    int maxLeaves = 1 << 4; // the default depth
    for (int seed = 0; seed < SEEDS; seed++) {
      int rooms = generate(seed).rooms().size();
      assertTrue(rooms >= 1 && rooms <= maxLeaves, "Seed " + seed + " produced " + rooms + " rooms");
    }
  }

  @Test
  void everyRoomMeetsTheMinimumSize() {
    for (int seed = 0; seed < SEEDS; seed++) {
      for (Room room : generate(seed).rooms()) {
        assertTrue(room.width() >= 3, "Seed " + seed + " produced a room " + room.width() + " wide");
        assertTrue(room.height() >= 3, "Seed " + seed + " produced a room " + room.height() + " tall");
      }
    }
  }

  @Test
  @DisplayName("rooms stay inside the board with a margin, so none touches an edge")
  void everyRoomKeepsItsMargin() {
    for (int seed = 0; seed < SEEDS; seed++) {
      for (Room room : generate(seed).rooms()) {
        assertTrue(room.row() >= 1, "Seed " + seed + ": room touches the top edge");
        assertTrue(room.col() >= 1, "Seed " + seed + ": room touches the left edge");
        assertTrue(
            room.row() + room.height() <= HEIGHT - 1, "Seed " + seed + ": room touches the bottom");
        assertTrue(
            room.col() + room.width() <= WIDTH - 1, "Seed " + seed + ": room touches the right");
      }
    }
  }

  @Test
  void roomsNeverOverlap() {
    for (int seed = 0; seed < SEEDS; seed++) {
      Set<Posn> claimed = new HashSet<>();
      for (Room room : generate(seed).rooms()) {
        for (int row = room.row(); row < room.row() + room.height(); row++) {
          for (int col = room.col(); col < room.col() + room.width(); col++) {
            assertTrue(claimed.add(new Posn(row, col)), "Seed " + seed + ": rooms overlap");
          }
        }
      }
    }
  }

  @Test
  void everyRoomCellIsWalkable() {
    for (int seed = 0; seed < 50; seed++) {
      DungeonLayout layout = generate(seed);
      for (Room room : layout.rooms()) {
        for (int row = room.row(); row < room.row() + room.height(); row++) {
          for (int col = room.col(); col < room.col() + room.width(); col++) {
            assertTrue(
                layout.isWalkable(new Posn(row, col)), "Seed " + seed + ": room cell is solid");
          }
        }
      }
    }
  }

  @Test
  @DisplayName("the board is genuinely partitioned, not mostly open floor")
  void leavesSubstantialWallStructure() {
    DungeonLayout layout = generate(1L);
    int cells = WIDTH * HEIGHT;
    assertTrue(
        layout.walkableCount() < cells * 3 / 4,
        "A room-and-corridor dungeon should be substantially solid");
    assertTrue(layout.walkableCount() > cells / 8, "The dungeon should still be mostly playable");
  }

  @Test
  void isReproducibleForAGivenSeed() {
    assertEquals(render(generate(42L)), render(generate(42L)));
  }

  @Test
  void differentSeedsProduceDifferentDungeons() {
    assertNotEquals(render(generate(1L)), render(generate(2L)));
  }

  @Test
  @DisplayName("the guaranteed capacity is never more than a real layout provides")
  void guaranteedCapacityIsSound() {
    BspLevelGenerator generator = new BspLevelGenerator();
    int guaranteed = generator.guaranteedWalkableCells(WIDTH, HEIGHT);
    assertTrue(guaranteed > 0);
    for (int seed = 0; seed < SEEDS; seed++) {
      assertTrue(
          generate(seed).walkableCount() >= guaranteed,
          "Seed " + seed + " produced less floor than the guaranteed lower bound");
    }
  }

  @Test
  void aDepthOfZeroCarvesASingleRoom() {
    DungeonLayout layout = new BspLevelGenerator(3, 0).generate(WIDTH, HEIGHT, new Random(1L));
    assertEquals(1, layout.rooms().size());
  }

  @Test
  void rejectsABoardTooSmallForOneRoom() {
    BspLevelGenerator generator = new BspLevelGenerator(3, 4);
    assertThrows(
        IllegalArgumentException.class, () -> generator.generate(4, 10, new Random(1L)));
    assertThrows(
        IllegalArgumentException.class, () -> generator.generate(10, 4, new Random(1L)));
  }

  @Test
  void reportsNoCapacityForABoardTooSmallForOneRoom() {
    assertEquals(0, new BspLevelGenerator(3, 4).guaranteedWalkableCells(4, 4));
  }

  @Test
  void rejectsNonsenseSettings() {
    assertThrows(IllegalArgumentException.class, () -> new BspLevelGenerator(0, 4));
    assertThrows(IllegalArgumentException.class, () -> new BspLevelGenerator(3, -1));
  }

  @Test
  @DisplayName("a larger minimum room size produces fewer, bigger rooms")
  void minimumRoomSizeIsHonoured() {
    DungeonLayout layout = new BspLevelGenerator(5, 4).generate(WIDTH, HEIGHT, new Random(1L));
    for (Room room : layout.rooms()) {
      assertTrue(room.width() >= 5 && room.height() >= 5, "Room smaller than the minimum: " + room);
    }
  }
}
