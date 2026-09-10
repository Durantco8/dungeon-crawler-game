package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class BoardImpl implements Board {
  private Piece[][] board;
  private Hero hero;
  private List<Enemy> enemies;
  private int width;
  private int height;
  private boolean hardMode = false;
  private final Random rng;
  private final LevelGenerator generator;
  private final AStarPathfinder pathfinder = new AStarPathfinder();

  /** The wall count the game shipped with, used when no generator is supplied. */
  private static final int DEFAULT_SCATTERED_WALLS = 2;

  /**
   * How many layouts init will try before giving up. The partitioning generator is connected by
   * construction so it should never need a second attempt; the budget exists for generators that make
   * no such guarantee, such as the original random scatter.
   */
  private static final int MAX_GENERATION_ATTEMPTS = 10;

  private int generationAttempts;

  /**
   * Creates an empty board with a caller-supplied source of randomness and level generator.
   *
   * @param width board width in cells
   * @param height board height in cells
   * @param rng the randomness used for piece placement and easy-mode enemy movement; seed it to make
   *     a game reproducible
   * @param generator the strategy that lays out each level's walls and floor
   */
  public BoardImpl(int width, int height, Random rng, LevelGenerator generator) {
    this.width = width;
    this.height = height;
    this.board = new Piece[height][width]; // [row] [col]
    this.enemies = new ArrayList<>();
    this.rng = rng;
    this.generator = generator;
  }

  /** Creates an empty board that scatters walls at random, as the game originally did. */
  public BoardImpl(int width, int height, Random rng) {
    this(width, height, rng, new RandomScatterGenerator(DEFAULT_SCATTERED_WALLS));
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
    this.generator = new RandomScatterGenerator(DEFAULT_SCATTERED_WALLS);
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        Piece piece = board[i][j];
        if (piece == null) {
          continue;
        }
        // Adopted pieces do not know where they are yet; the grid is the source of truth.
        piece.setPosn(new Posn(i, j));
        if (piece instanceof Hero) {
          this.hero = (Hero) piece;
        } else if (piece instanceof Enemy) {
          this.enemies.add((Enemy) piece);
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
  public boolean canFit(LevelSpec spec) {
    return spec.cellsRequired() <= generator.guaranteedWalkableCells(width, height);
  }

  @Override
  public void init(LevelSpec spec) {
    if (!canFit(spec)) {
      throw new IllegalArgumentException(
          "Level needs "
              + spec.cellsRequired()
              + " walkable cells but this generator guarantees only "
              + generator.guaranteedWalkableCells(width, height));
    }

    // Generate, populate, verify, and try again if the level turned out unsolvable. For the
    // partitioning generator the verification is an assertion that always holds; for a generator with
    // no connectivity guarantee it is what stops an unwinnable level reaching the player.
    for (int attempt = 1; attempt <= MAX_GENERATION_ATTEMPTS; attempt++) {
      DungeonLayout layout = generator.generate(width, height, rng);
      if (layout.width() != width || layout.height() != height) {
        throw new IllegalStateException("Generator returned a layout of the wrong size");
      }
      if (layout.walkableCount() < spec.cellsRequired()) {
        continue; // not enough floor this time
      }
      populate(layout, spec);
      if (isSolvable()) {
        this.generationAttempts = attempt;
        return;
      }
    }
    throw new IllegalStateException(
        "No solvable level found in " + MAX_GENERATION_ATTEMPTS + " attempts");
  }

  /** Clears the board, walls off the solid cells, and scatters the level's pieces over the floor. */
  private void populate(DungeonLayout layout, LevelSpec spec) {
    // Clear the board --> visit every index and set to null
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        board[i][j] = null;
      }
    }

    // Solid cells become walls, so the rest of placement only has to avoid occupied cells.
    for (int i = 0; i < height; i++) {
      for (int j = 0; j < width; j++) {
        Posn at = new Posn(i, j);
        if (!layout.isWalkable(at)) {
          set(new Wall(), at);
        }
      }
    }

    // set a position for the hero
    this.hero = new Hero();
    set(hero, randomSpace());
    // set a position for the exit
    set(new Exit(), randomSpace());
    // set position for each enemy
    this.enemies = new ArrayList<>(); // clear the list
    for (int i = 0; i < spec.enemies(); i++) {
      Enemy enemy = new Enemy();
      set(enemy, randomSpace());
      this.enemies.add(enemy);
    }
    // set a position for each treasure
    for (int i = 0; i < spec.treasures(); i++) {
      set(new Treasure(), randomSpace());
    }
    // set a position for each thief
    for (int i = 0; i < spec.thieves(); i++) {
      set(new Thief(), randomSpace());
    }
  }

  /**
   * Whether the hero can actually finish this level: reach the exit, and collect every treasure.
   *
   * <p>Passability is asked of the pieces themselves rather than tested with instanceof. A cell blocks
   * the hero exactly when entering it would be refused, which is the same rule movement uses, so the
   * two can never disagree.
   *
   * @return true if the exit and every treasure lie in the hero's reachable region
   */
  public boolean isSolvable() {
    if (hero == null) {
      return false;
    }
    Set<Posn> reachable = Reachability.floodFrom(hero.getPosn(), this::heroCanEnter);
    for (int row = 0; row < height; row++) {
      for (int col = 0; col < width; col++) {
        Posn at = new Posn(row, col);
        Piece piece = board[row][col];
        if (piece == null) {
          continue;
        }
        boolean mustBeReachable =
            piece.getType() == PieceType.EXIT || piece.getType() == PieceType.TREASURE;
        if (mustBeReachable && !reachable.contains(at)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean heroCanEnter(Posn p) {
    if (!inBounds(p)) {
      return false;
    }
    Piece occupant = board[p.row()][p.col()];
    if (occupant == null) {
      return true;
    }
    return occupant.onHeroEnter(hero).getResults() != CollisionResult.Result.BLOCKED;
  }

  /**
   * How many layouts the most recent init had to try. One means the first layout was accepted.
   *
   * @return the attempt count of the last successful init, or zero if init has not run
   */
  public int generationAttempts() {
    return generationAttempts;
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

  /**
   * Where one enemy should step this turn.
   *
   * <p>In hard mode the enemy follows a shortest path to the hero, so it rounds corners and walks
   * around walls instead of pressing against them. In easy mode it wanders, trying the four directions
   * in a shuffled order.
   *
   * @return the cell to step into, or empty if the enemy has nowhere to go
   */
  private Optional<Posn> chooseEnemyStep(Enemy enemy) {
    if (hardMode) {
      return pathfinder.nextStep(
          enemy.getPosn(), hero.getPosn(), target -> enemyCanEnter(enemy, target));
    }
    return wanderStep(enemy);
  }

  /** Picks the first of the four directions, shuffled, that this enemy is allowed to enter. */
  private Optional<Posn> wanderStep(Enemy enemy) {
    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
    for (int i = directions.length - 1; i > 0; i--) {
      int j = rng.nextInt(i + 1);
      int[] swap = directions[i];
      directions[i] = directions[j];
      directions[j] = swap;
    }
    for (int[] direction : directions) {
      Posn target = enemy.getPosn().offset(direction[0], direction[1]);
      if (enemyCanEnter(enemy, target)) {
        return Optional.of(target);
      }
    }
    return Optional.empty();
  }

  /**
   * Whether an enemy may enter a cell. Asked of the occupant rather than tested with instanceof, so
   * pathfinding and movement can never disagree. The hero's cell counts as enterable: reaching it is
   * the point, and it ends the game rather than being refused.
   */
  private boolean enemyCanEnter(Enemy enemy, Posn p) {
    if (!inBounds(p)) {
      return false;
    }
    Piece occupant = board[p.row()][p.col()];
    if (occupant == null) {
      return true;
    }
    return occupant.onEnemyEnter(enemy).getResults() != CollisionResult.Result.BLOCKED;
  }

  private boolean inBounds(Posn p) {
    return p.row() >= 0 && p.row() < height && p.col() >= 0 && p.col() < width;
  }

  @Override
  public CollisionResult moveHero(int drow, int dcol) {
    if (hero == null) {
      return CollisionResult.free();
    }

    Posn currentHeroP = hero.getPosn();
    Posn target = currentHeroP.offset(drow, dcol);

    // Illegal move conditional checks
    if (!inBounds(target)) {
      return CollisionResult.free();
    }

    CollisionResult heroMoveResult = hero.collide(get(target));
    if (heroMoveResult.getResults() == CollisionResult.Result.BLOCKED) {
      // The destination refused the hero. The turn is spent without the enemies acting, which is
      // the behaviour walls have always had.
      return CollisionResult.free();
    }

    if (heroMoveResult.getResults() == CollisionResult.Result.GAME_OVER) {
      // The hero has walked into an enemy. Stop resolving the turn instead of completing the move:
      // writing the hero into the enemy's cell would erase the enemy from the grid, leaving a board
      // that no longer describes a real game state.
      return heroMoveResult;
    }

    board[currentHeroP.row()][currentHeroP.col()] = null; // Clears past hero position
    set(hero, target);

    if (heroMoveResult.getResults() == CollisionResult.Result.NEXT_LEVEL) {
      return heroMoveResult; // Hero found the exit on the board no enemies should be deployed
    }

    /*
    ENEMY MOVEMENT
     */

    // Past this point the hero's move was uneventful: the refused, fatal and level-completing
    // outcomes have all returned already. Only the hero scores, so the turn's points are fixed here.
    int totalPoints = heroMoveResult.getPoints();

    for (Enemy enemy : enemies) {
      Optional<Posn> step = chooseEnemyStep(enemy);
      if (step.isEmpty()) {
        continue; // nowhere to go this turn
      }

      Posn enemyTarget = step.get();
      CollisionResult enemyMoveResult = enemy.collide(get(enemyTarget));
      if (enemyMoveResult.getResults() == CollisionResult.Result.BLOCKED) {
        continue; // the step chooser only offers cells this enemy may enter, so this is belt and braces
      }

      if (enemyMoveResult.getResults() == CollisionResult.Result.GAME_OVER) {
        // This enemy has reached the hero. End the turn here rather than stepping onto the hero's
        // cell and erasing it, and give no remaining enemy a turn.
        return new CollisionResult(totalPoints, CollisionResult.Result.GAME_OVER);
      }

      board[enemy.getPosn().row()][enemy.getPosn().col()] = null;
      set(enemy, enemyTarget);
    }
    return CollisionResult.scoring(totalPoints);
  }
}
