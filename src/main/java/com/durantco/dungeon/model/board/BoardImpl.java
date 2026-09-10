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
  private final Random rng;

  /**
   * Creates an empty board with a caller-supplied source of randomness.
   *
   * @param width board width in cells
   * @param height board height in cells
   * @param rng the randomness used for piece placement and easy-mode enemy movement; seed it to make
   *     a game reproducible
   */
  public BoardImpl(int width, int height, Random rng) {
    this.width = width;
    this.height = height;
    this.board = new Piece[height][width]; // [row] [col]
    this.enemies = new ArrayList<>();
    this.rng = rng;
  }

  /** Creates an empty board with an unseeded source of randomness, for production use. */
  public BoardImpl(int width, int height) {
    this(width, height, new Random());
  }

  /** Creates an unseeded board around an existing grid, for production use. */
  public BoardImpl(Piece[][] board) {
    this(board, new Random());
  }

  /**
   * Creates a board around an existing grid, scanning it for the hero and enemies. Useful for tests
   * that need an exact starting layout rather than a generated one.
   *
   * @param board the grid to adopt; null entries are empty cells
   * @param rng the randomness used for easy-mode enemy movement
   */
  public BoardImpl(Piece[][] board, Random rng) {
    this.width = board[0].length;
    this.height = board.length;
    this.board = board;
    this.enemies = new ArrayList<>();
    this.rng = rng;
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
    int row = rng.nextInt(height);
    int col = rng.nextInt(width);
    while (board[row][col] != null) {
      row = rng.nextInt(height);
      col = rng.nextInt(width);
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
    return board[posn.row()][posn.col()];
  }

  @Override
  public void set(Piece p, Posn newPos) {
    p.setPosn(newPos);
    board[newPos.row()][newPos.col()] = p;
  }

  private boolean inBounds(Posn p) {
    return p.row() >= 0 && p.row() < height && p.col() >= 0 && p.col() < width;
  }

  @Override
  public CollisionResult moveHero(int drow, int dcol) {
    if (hero == null) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    Posn currentHeroP = hero.getPosn();
    Posn target = currentHeroP.offset(drow, dcol);

    // Illegal move conditional checks
    if (!inBounds(target)) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    Piece newPositon = get(target);
    if (newPositon instanceof Wall) {
      return new CollisionResult(0, CollisionResult.Result.CONTINUE);
    }

    CollisionResult heroMoveResult = hero.collide(newPositon);
    board[currentHeroP.row()][currentHeroP.col()] = null; // Clears past hero position
    set(hero, target);

    if (heroMoveResult.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      return heroMoveResult; // Hero found the exit on the board no enemies should be deployed
    }

    /*
    ENEMY MOVEMENT
     */

    int totalPoints = heroMoveResult.getPoints();
    CollisionResult.Result finalResult = heroMoveResult.getResults();

    for (Enemy enemy : enemies) {
      int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};

      if (hardMode) {
        int heroRow = hero.getPosn().row();
        int heroCol = hero.getPosn().col();
        int eRow = enemy.getPosn().row();
        int eCol = enemy.getPosn().col();
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
          int j = rng.nextInt(i + 1);
          int[] temp = directions[i];
          directions[i] = directions[j];
          directions[j] = temp;
        }
      }

      // Try each direction until one works
      for (int[] dir : directions) {
        Posn enemyTarget = enemy.getPosn().offset(dir[0], dir[1]);

        // Illegal move checks
        if (!inBounds(enemyTarget)) {
          continue;
        }
        Piece enemyNewPosition = get(enemyTarget);
        if (enemyNewPosition instanceof Wall
            || enemyNewPosition instanceof Exit
            || enemyNewPosition instanceof Enemy) {
          continue;
        }

        CollisionResult enemyMoveResult = enemy.collide(enemyNewPosition);
        board[enemy.getPosn().row()][enemy.getPosn().col()] = null;
        set(enemy, enemyTarget);

        if (enemyMoveResult.getResults() == CollisionResult.Result.GAME_OVER) {
          finalResult = CollisionResult.Result.GAME_OVER;
        }
        break;
      }
    }
    return new CollisionResult(totalPoints, finalResult);
  }
}
