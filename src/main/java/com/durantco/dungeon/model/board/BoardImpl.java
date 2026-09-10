package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class BoardImpl implements Board {
  private Piece[][] board;
  private Hero hero;
  private List<Enemy> enemies;
  private int width;
  private int height;
  private boolean hardMode = false;

  public BoardImpl(int width, int height) {
    this.width = width;
    this.height = height;
    this.board = new Piece[height][width]; // [row] [col]
    this.enemies = new ArrayList<>();
  }

  public BoardImpl(Piece[][] board) {
    this.width = board[0].length;
    this.height = board.length;
    this.board = board;
    this.enemies = new ArrayList<>();
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        if (board[i][j] instanceof Hero) {
          this.hero = (Hero) board[i][j];
        } else if (board[i][j] instanceof Enemy) {
          this.enemies.add((Enemy) board[i][j]);
        }
      }
    }
  }

  @Override
  public void setHardMode(boolean hardMode) {
    this.hardMode = hardMode;
  }

  // Random width and height helper method to find empty spaces
  private Posn randomSpace() {
    Random random = new Random();
    int row = random.nextInt(height);
    int col = random.nextInt(width);
    while (board[row][col] != null) {
      row = random.nextInt(height);
      col = random.nextInt(width);
    }
    return new Posn(row, col); // Returns once a empty position on the board is found
  }

  @Override
  public void init(int enemies, int treasures, int walls) {
    int piecesCount = enemies + treasures + walls + 2 + 2;
    int openSpaces = width * height;

    // Clear the board --> visit every index and set to null
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        board[i][j] = null;
      }
    }
    // Condition check to see if there are enough spots for pieces
    if (piecesCount > openSpaces) {
      throw new IllegalArgumentException();
    }
    // set a position for the hero
    this.hero = new Hero();
    set(hero, randomSpace());
    // set a position for the exit
    Exit exit = new Exit();
    set(exit, randomSpace());
    // set position for each enemy
    this.enemies = new ArrayList<>(); // clear the list
    for (int i = 0; i < enemies; i++) {
      Enemy enemy = new Enemy();
      set(enemy, randomSpace());
      this.enemies.add(enemy);
    }
    // set a position for each treasure
    for (int i = 0; i < treasures; i++) {
      Treasure treasure = new Treasure();
      set(treasure, randomSpace());
    }
    // set a position for each wall
    for (int i = 0; i < walls; i++) {
      Wall w = new Wall();
      set(w, randomSpace());
    }
    for (int i = 0; i < 2; i++) {
      Thief thief = new Thief();
      set(thief, randomSpace());
    }
  }

  @Override
  public int getWidth() {
    return width;
  }

  @Override
  public int getHeight() {
    return height;
  }

  @Override
  public Piece get(Posn posn) {
    return board[posn.getRow()][posn.getCol()];
  }

  @Override
  public void set(Piece p, Posn newPos) {
    p.setPosn(newPos);
    board[newPos.getRow()][newPos.getCol()] = p;
  }

  @Override
  public CollisionResult moveHero(int drow, int dcol) {
    if (hero == null) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    Posn currentHeroP = hero.getPosn();
    int newRow = currentHeroP.getRow() + drow;
    int newCol = currentHeroP.getCol() + dcol;

    // Illegal move conditional checks
    if (newRow < 0 || newRow >= height || newCol < 0 || newCol >= width) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    Piece newPositon = board[newRow][newCol];
    if (newPositon instanceof Wall) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    CollisionResult heroMoveResult = hero.collide(newPositon);
    board[currentHeroP.getRow()][currentHeroP.getCol()] = null; // Clears past hero position
    set(hero, new Posn(newRow, newCol));

    if (heroMoveResult.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      return heroMoveResult; // Hero found the exit on the board no enemies should be deployed
    }

    /*
    ENEMY MOVEMENT
     */

    int totalPoints = heroMoveResult.getPoints();
    CollisionResult.Result finalResult = heroMoveResult.getResults();

    Random random = new Random();
    for (Enemy enemy : enemies) {
      int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

      if (hardMode) {
        int heroRow = hero.getPosn().getRow();
        int heroCol = hero.getPosn().getCol();
        int eRow = enemy.getPosn().getRow();
        int eCol = enemy.getPosn().getCol();
        int dRow = heroRow - eRow;
        int dCol = heroCol - eCol;

        int colDir;
        if (dCol > 0) {
          colDir = 1;
        } else {
          colDir = -1;
        }

        int rowDir;
        if (dRow > 0) {
          rowDir = 1;
        } else {
          rowDir = -1;
        }

        if (Math.abs(dRow) >= Math.abs(dCol)) {
          directions = new int[][] {{rowDir, 0}, {0, colDir}, {-rowDir, 0}, {0, -colDir}};
        } else {
          directions = new int[][] {{0, colDir}, {rowDir, 0}, {0, -colDir}, {-rowDir, 0}};
        }
      } else {
        for (int i = directions.length - 1; i > 0; i--) {
          int j = random.nextInt(i + 1);
          int[] temp = directions[i];
          directions[i] = directions[j];
          directions[j] = temp;
        }
      }

      // Try each direction until one works
      for (int[] dir : directions) {
        int eRow = enemy.getPosn().getRow() + dir[0];
        int eCol = enemy.getPosn().getCol() + dir[1];

        // Illegal move checks
        if (eRow < 0 || eRow >= height || eCol < 0 || eCol >= width) {
          continue;
        }
        Piece enemyNewPosition = board[eRow][eCol];
        if (enemyNewPosition instanceof Wall
            || enemyNewPosition instanceof Exit
            || enemyNewPosition instanceof Enemy) {
          continue;
        }

        CollisionResult enemyMoveResult = enemy.collide(enemyNewPosition);
        board[enemy.getPosn().getRow()][enemy.getPosn().getCol()] = null;
        set(enemy, new Posn(eRow, eCol));

        if (enemyMoveResult.getResults() == CollisionResult.Result.GAME_OVER) {
          finalResult = CollisionResult.Result.GAME_OVER;
        }
        break;
      }
    }
    return new CollisionResult(totalPoints, finalResult);
  }
}
