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

class ArchetypeTest {

  private static Enemy enemyAt(Posn where) {
    Enemy enemy = new Enemy();
    enemy.setPosn(where);
    return enemy;
  }

  @Nested
  class Ambushing {

    private final MovementStrategy ambush = new AmbushStrategy();

    @Test
    @DisplayName("aims ahead of the hero, not at it")
    void headsForWhereTheHeroIsGoing() {
      // The hero is at (1, 4) running left, so three cells ahead of it is (1, 1). The enemy sits
      // between the two: chasing sends it right, towards the hero, while ambushing sends it left, to
      // the spot the hero is heading for.
      StubMovementContext context =
          new StubMovementContext(new Posn(1, 4), new Random(1L), ".....", ".....")
              .heading(new Posn(0, -1));
      Enemy enemy = enemyAt(new Posn(1, 2));

      assertEquals(Optional.of(new Posn(1, 1)), ambush.chooseStep(enemy, context));
      assertEquals(
          Optional.of(new Posn(1, 3)),
          new ChaseStrategy().chooseStep(enemy, context),
          "A chaser would go the other way, towards the hero itself");
    }

    @Test
    @DisplayName("chases directly before the hero has moved at all")
    void fallsBackToChasingWithoutAHeading() {
      StubMovementContext noHeading =
          new StubMovementContext(new Posn(0, 0), new Random(1L), "...", "...", "...");
      assertEquals(
          new ChaseStrategy().chooseStep(enemyAt(new Posn(2, 2)), noHeading),
          ambush.chooseStep(enemyAt(new Posn(2, 2)), noHeading));
    }

    @Test
    @DisplayName("shortens its lead when the far ambush point is unreachable")
    void aimsCloserWhenItCannotGetFarAhead() {
      // The hero runs right towards a wall, so two and three cells ahead are off the board or solid.
      // The ambusher must settle for intercepting one cell ahead rather than giving up.
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 1), new Random(1L), "...#", "....")
              .heading(new Posn(0, 1));
      Enemy enemy = enemyAt(new Posn(1, 0));

      Posn oneAhead = new Posn(0, 2);
      assertEquals(
          context.stepTowards(enemy, oneAhead),
          ambush.chooseStep(enemy, context),
          "It should be routing towards the nearest reachable interception point");
    }

    @Test
    void holdsPositionWhenNeitherTheAmbushNorTheHeroCanBeReached() {
      // The hero's row is sealed off from the enemy's. No interception point is reachable, and neither
      // is the hero, so the fallback finds nothing either and the enemy stays put rather than
      // thrashing.
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(1L), ".#.", "###", "...")
              .heading(new Posn(0, 1));
      assertEquals(Optional.empty(), ambush.chooseStep(enemyAt(new Posn(2, 0)), context));
    }

    @Test
    void isDeterministic() {
      StubMovementContext first =
          new StubMovementContext(new Posn(1, 4), new Random(1L), ".....", ".....")
              .heading(new Posn(0, -1));
      StubMovementContext second =
          new StubMovementContext(new Posn(1, 4), new Random(77L), ".....", ".....")
              .heading(new Posn(0, -1));
      assertEquals(
          ambush.chooseStep(enemyAt(new Posn(1, 2)), first),
          ambush.chooseStep(enemyAt(new Posn(1, 2)), second));
    }
  }

  @Nested
  class Patrolling {

    @Test
    @DisplayName("walks towards a room centre regardless of where the hero is")
    void headsForItsBeatNotTheHero() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(0L), "....", "....", "....")
              .withRooms(new Room(2, 3, 1, 1));

      // The hero is at the top left; the only stop on the beat is bottom right, so it walks away.
      Optional<Posn> step = new PatrolStrategy().chooseStep(enemyAt(new Posn(1, 1)), context);
      assertTrue(step.isPresent());
      assertNotEquals(new Posn(0, 1), step.orElseThrow(), "A patroller ignores the hero");
    }

    @Test
    @DisplayName("moves on to the next stop once it reaches one")
    void advancesAlongItsBeat() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(0L), ".....", ".....", ".....")
              .withRooms(new Room(0, 0, 1, 1), new Room(0, 4, 1, 1));
      PatrolStrategy patrol = new PatrolStrategy();
      Enemy enemy = enemyAt(new Posn(0, 0));

      // Standing on one stop, it must set off for another rather than staying.
      Optional<Posn> step = patrol.chooseStep(enemy, context);
      assertEquals(Optional.of(new Posn(0, 1)), step);
    }

    @Test
    @DisplayName("walks a repeating circuit rather than stopping at the end")
    void loopsItsBeat() {
      StubMovementContext context =
          new StubMovementContext(new Posn(4, 4), new Random(0L), "...", "...", "...")
              .withRooms(new Room(0, 0, 1, 1), new Room(2, 2, 1, 1));
      PatrolStrategy patrol = new PatrolStrategy();
      Enemy enemy = enemyAt(new Posn(0, 0));

      Set<Posn> visited = new HashSet<>();
      for (int turn = 0; turn < 24; turn++) {
        Posn next = patrol.chooseStep(enemy, context).orElseThrow();
        enemy.setPosn(next);
        visited.add(next);
      }
      assertTrue(visited.contains(new Posn(0, 0)), "The circuit should return to its first stop");
      assertTrue(visited.contains(new Posn(2, 2)), "The circuit should reach its second stop");
    }

    @Test
    @DisplayName("drifts when there are no rooms to patrol")
    void fallsBackToWanderingWithoutABeat() {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(5L), "...", "...", "...");
      Optional<Posn> step = new PatrolStrategy().chooseStep(enemyAt(new Posn(1, 1)), context);

      assertTrue(step.isPresent(), "With no beat it should still move");
      Posn to = step.orElseThrow();
      assertEquals(1, Math.abs(to.row() - 1) + Math.abs(to.col() - 1));
    }

    @Test
    void isReproducibleForAGivenSeed() {
      assertEquals(circuitWithSeed(3L), circuitWithSeed(3L));
    }

    private String circuitWithSeed(long seed) {
      StubMovementContext context =
          new StubMovementContext(new Posn(0, 0), new Random(seed), "....", "....", "....")
              .withRooms(new Room(0, 0, 1, 1), new Room(2, 3, 1, 1));
      PatrolStrategy patrol = new PatrolStrategy();
      Enemy enemy = enemyAt(new Posn(1, 1));
      StringBuilder route = new StringBuilder();
      for (int turn = 0; turn < 12; turn++) {
        Posn next = patrol.chooseStep(enemy, context).orElseThrow();
        enemy.setPosn(next);
        route.append(next).append(';');
      }
      return route.toString();
    }
  }

  @Nested
  class DifficultyMix {

    @Test
    void easyModeOnlyDrifts() {
      for (int index = 0; index < 9; index++) {
        assertTrue(Difficulty.EASY.strategyFor(index, new Random(1L)) instanceof WanderStrategy);
      }
    }

    @Test
    @DisplayName("hard mode cycles through the three hunting archetypes")
    void hardModeMixesArchetypes() {
      assertTrue(Difficulty.HARD.strategyFor(0, new Random(1L)) instanceof ChaseStrategy);
      assertTrue(Difficulty.HARD.strategyFor(1, new Random(1L)) instanceof AmbushStrategy);
      assertTrue(Difficulty.HARD.strategyFor(2, new Random(1L)) instanceof PatrolStrategy);
      assertTrue(Difficulty.HARD.strategyFor(3, new Random(1L)) instanceof ChaseStrategy);
    }

    @Test
    @DisplayName("the mix is fixed by spawn order, so a seed always produces the same level")
    void theMixDoesNotDependOnRandomness() {
      for (int index = 0; index < 9; index++) {
        assertEquals(
            Difficulty.HARD.strategyFor(index, new Random(1L)).getClass(),
            Difficulty.HARD.strategyFor(index, new Random(9876L)).getClass());
      }
    }
  }
}
