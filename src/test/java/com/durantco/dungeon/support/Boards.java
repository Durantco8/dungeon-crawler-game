package com.durantco.dungeon.support;

import com.durantco.dungeon.model.board.Board;
import com.durantco.dungeon.model.board.ChaseStrategy;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.model.pieces.Exit;
import com.durantco.dungeon.model.pieces.Hero;
import com.durantco.dungeon.model.pieces.Piece;
import com.durantco.dungeon.model.pieces.PieceType;
import com.durantco.dungeon.model.pieces.Thief;
import com.durantco.dungeon.model.pieces.Treasure;
import com.durantco.dungeon.model.pieces.Wall;

/**
 * Builds exact board layouts from ASCII art, so a test reads as the situation it is describing.
 *
 * <pre>
 *   Piece[][] grid = Boards.parse(
 *       "H..",
 *       ".WE",
 *       "..X");
 * </pre>
 *
 * Legend: {@code H} hero, {@code E} enemy, {@code W} wall, {@code X} exit, {@code T} treasure,
 * {@code F} thief, {@code .} empty.
 */
public final class Boards {

  private Boards() {}

  /**
   * Parses rows of ASCII art into a grid.
   *
   * @param rows one string per board row, one character per cell
   * @return the grid, with null for empty cells
   * @throws IllegalArgumentException if rows are ragged or a character is unknown
   */
  public static Piece[][] parse(String... rows) {
    if (rows.length == 0) {
      throw new IllegalArgumentException("A board needs at least one row");
    }
    int width = rows[0].length();
    Piece[][] grid = new Piece[rows.length][width];
    for (int row = 0; row < rows.length; row++) {
      if (rows[row].length() != width) {
        throw new IllegalArgumentException(
            "Row " + row + " has width " + rows[row].length() + ", expected " + width);
      }
      for (int col = 0; col < width; col++) {
        grid[row][col] = pieceFor(rows[row].charAt(col));
      }
    }
    return grid;
  }

  private static Piece pieceFor(char symbol) {
    switch (symbol) {
      case '.':
        return null;
      case 'H':
        return new Hero();
      case 'E':
        return new Enemy();
      case 'W':
        return new Wall();
      case 'X':
        return new Exit();
      case 'T':
        return new Treasure();
      case 'F':
        return new Thief();
      default:
        throw new IllegalArgumentException("Unknown board symbol: " + symbol);
    }
  }

  /**
   * Arms every enemy on a board with a plain chaser that needs no line of sight.
   *
   * <p>For tests about how an enemy moves rather than about difficulty. Setting a difficulty only
   * affects enemies spawned afterwards, and the hunters it spawns have to see the hero first, so
   * neither is a way to make a hand-built enemy chase on cue.
   *
   * @param board the board whose enemies should chase
   */
  public static void armChasers(Board board) {
    for (int row = 0; row < board.getHeight(); row++) {
      for (int col = 0; col < board.getWidth(); col++) {
        Piece piece = board.get(new Posn(row, col));
        if (piece != null && piece.getType() == PieceType.ENEMY) {
          ((Enemy) piece).setMovement(new ChaseStrategy());
        }
      }
    }
  }
}
