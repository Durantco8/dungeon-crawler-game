package com.durantco.dungeon.support;

import com.durantco.dungeon.model.board.AStarPathfinder;
import com.durantco.dungeon.model.board.DungeonLayout;
import com.durantco.dungeon.model.board.LineOfSight;
import com.durantco.dungeon.model.board.MovementContext;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.board.Room;
import com.durantco.dungeon.model.pieces.Enemy;
import java.util.List;
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
  private Posn heroHeading = new Posn(0, 0);
  private List<Room> rooms = List.of();
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

  /**
   * Sets the direction the hero should appear to be travelling.
   *
   * @param heading a one-cell offset
   * @return this context, for chaining
   */
  public StubMovementContext heading(Posn heading) {
    this.heroHeading = heading;
    return this;
  }

  /**
   * Sets the rooms a patrolling strategy should see.
   *
   * @param rooms the rooms to report
   * @return this context, for chaining
   */
  public StubMovementContext withRooms(Room... rooms) {
    this.rooms = List.of(rooms);
    return this;
  }

  @Override
  public boolean hasLineOfSight(Posn from, Posn to) {
    return LineOfSight.isClear(from, to, layout::isWalkable);
  }

  @Override
  public Posn heroHeading() {
    return heroHeading;
  }

  @Override
  public List<Room> rooms() {
    return rooms;
  }

  @Override
  public Random rng() {
    return rng;
  }
}
