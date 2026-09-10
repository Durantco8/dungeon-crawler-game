package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RoomTest {

  @Test
  void centreOfAnOddSizedRoomIsItsMiddleCell() {
    assertEquals(new Posn(3, 6), new Room(2, 5, 3, 3).center());
  }

  @Test
  @DisplayName("an even-sized room biases its centre down and right")
  void centreOfAnEvenSizedRoomIsConsistent() {
    assertEquals(new Posn(2, 3), new Room(0, 0, 6, 4).center());
  }

  @Test
  void theCentreIsInsideTheRoom() {
    Room room = new Room(4, 7, 5, 3);
    assertTrue(room.contains(room.center()));
  }

  @Test
  void containsItsOwnCornersButNotTheCellsBeyond() {
    Room room = new Room(2, 3, 4, 2);
    assertTrue(room.contains(new Posn(2, 3)));
    assertTrue(room.contains(new Posn(3, 6)));
    assertFalse(room.contains(new Posn(1, 3)));
    assertFalse(room.contains(new Posn(2, 2)));
    assertFalse(room.contains(new Posn(4, 3)));
    assertFalse(room.contains(new Posn(2, 7)));
  }

  @Test
  void reportsItsArea() {
    assertEquals(12, new Room(0, 0, 4, 3).area());
  }

  @Test
  void roomsWithTheSameBoundsAreEqual() {
    assertEquals(new Room(1, 2, 3, 4), new Room(1, 2, 3, 4));
  }

  @Test
  void rejectsEmptyOrNegativeRooms() {
    assertThrows(IllegalArgumentException.class, () -> new Room(0, 0, 0, 3));
    assertThrows(IllegalArgumentException.class, () -> new Room(0, 0, 3, 0));
    assertThrows(IllegalArgumentException.class, () -> new Room(0, 0, -1, 3));
  }

  @Test
  void rejectsARoomStartingOutsideTheBoard() {
    assertThrows(IllegalArgumentException.class, () -> new Room(-1, 0, 3, 3));
    assertThrows(IllegalArgumentException.class, () -> new Room(0, -1, 3, 3));
  }
}
