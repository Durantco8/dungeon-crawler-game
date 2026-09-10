package com.durantco.dungeon.support;

import com.durantco.dungeon.model.board.AStarPathfinder;
import com.durantco.dungeon.model.board.DungeonLayout;
import com.durantco.dungeon.model.board.MovementContext;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Enemy;
import java.util.Optional;
import java.util.Random;

/**
 * A movement context backed by ASCII art rather than a populated board.
 *
 * <p>Lets a strategy be tested against the exact geometry it is supposed to handle, without spawning a
 * dungeon or manoeuvring a real hero into place. Cells are {@code .} for open and {@code #} for solid.
 */
public final class StubMovementContext implements MovementContext {

  private final DungeonLayout layout;
  private final Posn heroPosition;
  private final Random rng;
  private final AStarPathfinder pathfinder = new AStarPathfinder();

  /**
   * @param heroPosition where the strategy should believe the hero is
   * @param rng the randomness handed to strategies that need it
   * @param rows the geometry, {@code .} open and {@code #} solid
   */
  public StubMovementContext(Posn heroPosition, Random rng, String... rows) {
    boolean[][] walkable = new boolean[rows.length][rows[0].length()];
    for (int row = 0; row < rows.length; row++) {
      for (int col = 0; col < rows[row].length(); col++) {
        walkable[row][col] = rows[row].charAt(col) == '.';
      }
    }
    this.layout = new DungeonLayout(walkable);
    this.heroPosition = heroPosition;
    this.rng = rng;
  }

  @Override
  public Posn heroPosition() {
    return heroPosition;
  }

  @Override
  public boolean canEnter(Enemy enemy, Posn target) {
    return layout.isWalkable(target);
  }

  @Override
  public Optional<Posn> stepTowards(Enemy enemy, Posn goal) {
    return pathfinder.nextStep(enemy.getPosn(), goal, layout::isWalkable);
  }

  @Override
  public Random rng() {
    return rng;
  }
}
