package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** The original scatter layout, kept as a named strategy and as the baseline for BSP. */
class RandomScatterGeneratorTest {

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
  void leavesEverythingWalkableWhenAskedForNoWalls() {
    DungeonLayout layout = new RandomScatterGenerator(0).generate(5, 4, new Random(1L));
    assertEquals(20, layout.walkableCount());
  }

  @Test
  void blocksExactlyTheRequestedNumberOfCells() {
    DungeonLayout layout = new RandomScatterGenerator(6).generate(5, 4, new Random(1L));
    assertEquals(14, layout.walkableCount());
  }

  @Test
  void producesALayoutOfTheRequestedSize() {
    DungeonLayout layout = new RandomScatterGenerator(2).generate(7, 3, new Random(1L));
    assertEquals(7, layout.width());
    assertEquals(3, layout.height());
  }

  @Test
  void isReproducibleForAGivenSeed() {
    DungeonLayout first = new RandomScatterGenerator(5).generate(6, 6, new Random(99L));
    DungeonLayout second = new RandomScatterGenerator(5).generate(6, 6, new Random(99L));
    assertEquals(render(first), render(second));
  }

  @Test
  void differentSeedsScatterDifferently() {
    DungeonLayout first = new RandomScatterGenerator(5).generate(6, 6, new Random(1L));
    DungeonLayout second = new RandomScatterGenerator(5).generate(6, 6, new Random(2L));
    assertNotEquals(render(first), render(second));
  }

  @Test
  void canBlockEveryCell() {
    DungeonLayout layout = new RandomScatterGenerator(9).generate(3, 3, new Random(1L));
    assertEquals(0, layout.walkableCount());
  }

  @Test
  void reportsItsGuaranteedWalkableSpace() {
    assertEquals(64, new RandomScatterGenerator(0).guaranteedWalkableCells(8, 8));
    assertEquals(62, new RandomScatterGenerator(2).guaranteedWalkableCells(8, 8));
    assertEquals(0, new RandomScatterGenerator(100).guaranteedWalkableCells(8, 8));
  }

  @Test
  void rejectsANegativeWallCount() {
    assertThrows(IllegalArgumentException.class, () -> new RandomScatterGenerator(-1));
  }

  @Test
  @DisplayName("asking for more walls than there are cells fails rather than looping forever")
  void rejectsMoreWallsThanCells() {
    RandomScatterGenerator generator = new RandomScatterGenerator(10);
    assertThrows(IllegalArgumentException.class, () -> generator.generate(3, 3, new Random(1L)));
  }
}
