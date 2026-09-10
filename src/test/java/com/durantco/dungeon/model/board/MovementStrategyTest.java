package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.support.StubMovementContext;
import java.util.HashSet;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class MovementStrategyTest {

  private static Enemy enemyAt(Posn where) {
    Enemy enemy = new Enemy();
    enemy.setPosn(where);
    return enemy;
  }

  @Nested
  class Chasing {

    private final MovementStrategy chase = new ChaseStrategy();

    @Test
    void stepsStraightAtTheHeroAcrossOpenGround() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 3), new Random(1L), "....", "....");
      assertEquals(Optional.of(new Posn(0, 1)), chase.chooseStep(enemyAt(new Posn(0, 0)), context));
    }

    @Test
    @DisplayName("goes around a wall rather than into it")
    void routesAroundAWall() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 3), new Random(1L), "....", ".###", "....");
      assertEquals(Optional.of(new Posn(1, 0)), chase.chooseStep(enemyAt(new Posn(2, 0)), context));
    }

    @Test
    void staysPutWhenTheHeroCannotBeReached() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 2), new Random(1L), "..#", "..#", "###");
      assertEquals(Optional.empty(), chase.chooseStep(enemyAt(new Posn(0, 0)), context));
    }

    @Test
    @DisplayName("steps onto the hero when it is next door, which is how a chase ends")
    void stepsOntoAnAdjacentHero() {
      StubMovementContext context = new StubMovementContext(new Posn(0, 1), new Random(1L), "..");
      assertEquals(Optional.of(new Posn(0, 1)), chase.chooseStep(enemyAt(new Posn(0, 0)), context));
    }

    @Test
    void consultsNoRandomness() {
      StubMovementContext first =
          new StubMovementContext(new Posn(2, 2), new Random(1L), "...", "...", "...");
      StubMovementContext second =
          new StubMovementContext(new Posn(2, 2), new Random(999L), "...", "...", "...");
      assertEquals(
          chase.chooseStep(enemyAt(new Posn(0, 0)), first),
          chase.chooseStep(enemyAt(new Posn(0, 0)), second));
    }
  }

  @Nested
  class Wandering {

    private final MovementStrategy wander = new WanderStrategy();

    @Test
    void movesToAnAdjacentCell() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(1L), "...", "...", "...");
      Posn from = new Posn(1, 1);
      Posn to = wander.chooseStep(enemyAt(from), context).orElseThrow();

      int distance = Math.abs(to.row() - from.row()) + Math.abs(to.col() - from.col());
      assertEquals(1, distance);
    }

    @Test
    @DisplayName("finds its one legal move rather than standing still by bad luck")
    void findsTheOnlyOpeningWhenNearlyBoxedIn() {
      // From (0, 0) the only open neighbour is (0, 1): up and left leave the grid, and (1, 0) is
      // solid. Whatever order the shuffle produces, the strategy has to arrive at that one cell.
      StubMovementContext context =
          new StubMovementContext(new Posn(2, 2), new Random(7L), "..#", "#.#", "#..");
      for (int attempt = 0; attempt < 20; attempt++) {
        assertEquals(
            Optional.of(new Posn(0, 1)), wander.chooseStep(enemyAt(new Posn(0, 0)), context));
      }
    }

    @Test
    void staysPutWhenCompletelyBoxedIn() {
      StubMovementContext context =
          new StubMovementContext(new Posn(2, 2), new Random(1L), "#.#", "###", "...");
      assertEquals(Optional.empty(), wander.chooseStep(enemyAt(new Posn(0, 1)), context));
    }

    @Test
    @DisplayName("the same seed wanders the same way")
    void isReproducibleForAGivenSeed() {
      assertEquals(stepsWithSeed(4L), stepsWithSeed(4L));
    }

    @Test
    void differentSeedsWanderDifferently() {
      assertNotEquals(stepsWithSeed(1L), stepsWithSeed(2L));
    }

    @Test
    @DisplayName("given enough turns it will try every direction")
    void isNotBiasedTowardsOneDirection() {
      Set<Posn> seen = new HashSet<>();
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(3L), "...", "...", "...");
      for (int turn = 0; turn < 60; turn++) {
        seen.add(wander.chooseStep(enemyAt(new Posn(1, 1)), context).orElseThrow());
      }
      assertEquals(4, seen.size(), "A shuffled wander should reach all four neighbours");
    }

    private String stepsWithSeed(long seed) {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(seed), "...", "...", "...");
      StringBuilder out = new StringBuilder();
      for (int turn = 0; turn < 10; turn++) {
        out.append(wander.chooseStep(enemyAt(new Posn(1, 1)), context).orElseThrow()).append(';');
      }
      return out.toString();
    }
  }

  @Nested
  class Composition {

    @Test
    void anEnemyDriftsByDefault() {
      assertTrue(new Enemy().movement() instanceof WanderStrategy);
    }

    @Test
    void anEnemyCanBeGivenBehaviourWhenBuilt() {
      assertTrue(new Enemy(new ChaseStrategy()).movement() instanceof ChaseStrategy);
    }

    @Test
    @DisplayName("an enemy can be re-armed, which is how a difficulty change takes effect")
    void anEnemyCanBeReArmed() {
      Enemy enemy = new Enemy(new WanderStrategy());
      enemy.setMovement(new ChaseStrategy());
      assertTrue(enemy.movement() instanceof ChaseStrategy);
    }
  }
}
