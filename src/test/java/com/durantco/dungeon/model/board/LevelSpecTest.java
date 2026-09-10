package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LevelSpecTest {

  @Test
  @DisplayName("cellsRequired counts the hero and the exit alongside the configured pieces")
  void cellsRequiredIncludesTheFixedPieces() {
    assertEquals(2, new LevelSpec(0, 0, 0, 0).cellsRequired());
    assertEquals(6, new LevelSpec(1, 1, 1, 1).cellsRequired());
    assertEquals(12, new LevelSpec(4, 3, 2, 1).cellsRequired());
  }

  @Test
  void enemyCountGrowsWithTheLevel() {
    assertEquals(2, LevelSpec.forLevel(1).enemies());
    assertEquals(3, LevelSpec.forLevel(2).enemies());
    assertEquals(11, LevelSpec.forLevel(10).enemies());
  }

  @Test
  void everythingOtherThanEnemiesIsConstantAcrossLevels() {
    LevelSpec first = LevelSpec.forLevel(1);
    LevelSpec tenth = LevelSpec.forLevel(10);
    assertEquals(first.treasures(), tenth.treasures());
    assertEquals(first.walls(), tenth.walls());
    assertEquals(first.thieves(), tenth.thieves());
  }

  @Test
  void firstLevelNeedsTenCells() {
    assertEquals(10, LevelSpec.forLevel(1).cellsRequired());
  }

  @Test
  void specsWithTheSameCountsAreEqual() {
    assertEquals(new LevelSpec(2, 2, 2, 2), LevelSpec.forLevel(1));
  }

  @Test
  void rejectsNegativeCounts() {
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(-1, 0, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(0, -1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(0, 0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(0, 0, 0, -1));
  }
}
