package com.durantco.dungeon.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.support.FakeBoard;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The four public move methods now share one private implementation, so the thing worth asserting is
 * that each still asks the board for the direction it names.
 */
class ModelMoveTest {

  @Test
  void upIsOneRowTowardsTheTop() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).moveUp();
    assertEquals(List.of(new FakeBoard.Move(-1, 0)), board.moves());
  }

  @Test
  void downIsOneRowTowardsTheBottom() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).moveDown();
    assertEquals(List.of(new FakeBoard.Move(1, 0)), board.moves());
  }

  @Test
  void leftIsOneColumnTowardsTheStart() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).moveLeft();
    assertEquals(List.of(new FakeBoard.Move(0, -1)), board.moves());
  }

  @Test
  void rightIsOneColumnTowardsTheEnd() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).moveRight();
    assertEquals(List.of(new FakeBoard.Move(0, 1)), board.moves());
  }

  @Test
  @DisplayName("the four directions stay distinct across a sequence of moves")
  void eachDirectionKeepsItsOwnDelta() {
    FakeBoard board = new FakeBoard();
    Model model = new ModelImpl(board);
    model.moveUp();
    model.moveRight();
    model.moveDown();
    model.moveLeft();

    assertEquals(
        List.of(
            new FakeBoard.Move(-1, 0),
            new FakeBoard.Move(0, 1),
            new FakeBoard.Move(1, 0),
            new FakeBoard.Move(0, -1)),
        board.moves());
  }

  @Test
  void oneMoveAsksTheBoardExactlyOnce() {
    FakeBoard board = new FakeBoard();
    new ModelImpl(board).moveUp();
    assertEquals(1, board.moveHeroCalls());
  }
}
