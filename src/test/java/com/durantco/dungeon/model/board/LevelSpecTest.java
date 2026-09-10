package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LevelSpecTest {

  @Test
  @DisplayName("cellsRequired counts the hero and the exit alongside the configured pieces")
  void cellsRequiredIncludesTheFixedPieces() {
    assertEquals(2, new LevelSpec(0, 0, 0).cellsRequired());
    assertEquals(5, new LevelSpec(1, 1, 1).cellsRequired());
    assertEquals(10, new LevelSpec(4, 3, 1).cellsRequired());
  }

  @Test
  @DisplayName("walls are the generator's business, so they are not part of the requirement")
  void cellsRequiredCountsOnlyPlacedPieces() {
    // A level asks for actors; where the walls go is decided by the LevelGenerator.
    assertEquals(8, LevelSpec.forLevel(1).cellsRequired());
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
    assertEquals(first.thieves(), tenth.thieves());
  }

  @Test
  void specsWithTheSameCountsAreEqual() {
    assertEquals(new LevelSpec(2, 2, 2), LevelSpec.forLevel(1));
  }

  @Test
  void rejectsNegativeCounts() {
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(-1, 0, 0));
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(0, -1, 0));
    assertThrows(IllegalArgumentException.class, () -> new LevelSpec(0, 0, -1));
  }
}
