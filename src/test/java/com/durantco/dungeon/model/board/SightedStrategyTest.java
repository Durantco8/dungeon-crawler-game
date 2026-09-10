package com.durantco.dungeon.model.board;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.durantco.dungeon.model.pieces.Enemy;
import com.durantco.dungeon.support.StubMovementContext;
import java.util.Optional;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SightedStrategyTest {

  /** A stand-in behaviour that reports whether it was the one consulted. */
  private static final class Marker implements MovementStrategy {
    private final Posn answer;
    private int calls;

    Marker(Posn answer) {
      this.answer = answer;
    }

    @Override
    public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
      calls++;
      return Optional.of(answer);
    }
  }

  private static Enemy enemyAt(Posn where) {
    Enemy enemy = new Enemy();
    enemy.setPosn(where);
    return enemy;
  }

  @Test
  @DisplayName("hunts while the hero is in plain view")
  void usesTheHuntingBehaviourWhenTheHeroIsVisible() {
    Marker hunt = new Marker(new Posn(9, 9));
    Marker idle = new Marker(new Posn(1, 1));
    StubMovementContext context =
        new StubMovementContext(new Posn(0, 3), new Random(1L), "....", "....");

    Optional<Posn> step =
        new SightedStrategy(hunt, idle).chooseStep(enemyAt(new Posn(0, 0)), context);

    assertEquals(Optional.of(new Posn(9, 9)), step);
    assertEquals(1, hunt.calls);
    assertEquals(0, idle.calls);
  }

  @Test
  @DisplayName("does not hunt a hero it has never seen")
  void usesTheIdleBehaviourWhenSightIsBlocked() {
    Marker hunt = new Marker(new Posn(9, 9));
    Marker idle = new Marker(new Posn(1, 1));
    // A solid row between the two, so nothing has ever been in view.
    StubMovementContext context =
        new StubMovementContext(new Posn(0, 0), new Random(1L), "....", "####", "....");

    Optional<Posn> step =
        new SightedStrategy(hunt, idle).chooseStep(enemyAt(new Posn(2, 0)), context);

    assertEquals(Optional.of(new Posn(1, 1)), step);
    assertEquals(0, hunt.calls);
    assertEquals(1, idle.calls);
  }

  @Test
  void doesNotHuntAHeroBeyondItsSightRange() {
    Marker hunt = new Marker(new Posn(9, 9));
    Marker idle = new Marker(new Posn(1, 1));
    StubMovementContext context =
        new StubMovementContext(new Posn(0, 6), new Random(1L), ".......");

    new SightedStrategy(hunt, idle, 3).chooseStep(enemyAt(new Posn(0, 0)), context);

    assertEquals(0, hunt.calls, "Six cells away is outside a range of three");
    assertEquals(1, idle.calls);
  }

  @Test
  @DisplayName("a hero exactly at the edge of vision is still visible")
  void seesToTheEdgeOfItsRange() {
    Marker hunt = new Marker(new Posn(9, 9));
    StubMovementContext context =
        new StubMovementContext(new Posn(0, 3), new Random(1L), "....");

    new SightedStrategy(hunt, new Marker(new Posn(1, 1)), 3)
        .chooseStep(enemyAt(new Posn(0, 0)), context);

    assertEquals(1, hunt.calls);
  }

  @Test
  @DisplayName("heads for where it last saw the hero once the trail goes cold")
  void followsItsMemoryAfterLosingSight() {
    SightedStrategy sighted = new SightedStrategy(new ChaseStrategy(), new Marker(new Posn(1, 1)));
    Enemy enemy = enemyAt(new Posn(0, 0));

    // First it sees the hero at the far end of an open corridor.
    StubMovementContext inView =
        new StubMovementContext(new Posn(0, 4), new Random(1L), ".....", ".....");
    sighted.chooseStep(enemy, inView);
    assertTrue(sighted.isTrackingHero());

    // Then the hero slips behind a wall. It is no longer visible, so the enemy has only its memory of
    // the sighting at (0, 4) to go on, and should set off along row zero towards it.
    StubMovementContext hidden =
        new StubMovementContext(new Posn(2, 0), new Random(1L), ".....", "#####", ".....");
    Optional<Posn> step = sighted.chooseStep(enemy, hidden);

    assertEquals(Optional.of(new Posn(0, 1)), step, "It should walk towards its last sighting");
    assertTrue(sighted.isTrackingHero());
  }

  @Test
  @DisplayName("gives up once it reaches the last sighting and finds nothing")
  void forgetsTheHeroOnArriving() {
    Marker idle = new Marker(new Posn(7, 7));
    SightedStrategy sighted = new SightedStrategy(new ChaseStrategy(), idle);
    Enemy enemy = enemyAt(new Posn(0, 0));

    StubMovementContext inView =
        new StubMovementContext(new Posn(0, 2), new Random(1L), "...", "...");
    sighted.chooseStep(enemy, inView);
    assertTrue(sighted.isTrackingHero());

    // The enemy arrives at the remembered cell while the hero is nowhere in view.
    enemy.setPosn(new Posn(0, 2));
    StubMovementContext hidden =
        new StubMovementContext(new Posn(2, 0), new Random(1L), "...", "###", "...");
    Optional<Posn> step = sighted.chooseStep(enemy, hidden);

    assertEquals(Optional.of(new Posn(7, 7)), step);
    assertFalse(sighted.isTrackingHero(), "Standing on the last sighting should end the search");
    assertEquals(1, idle.calls);
  }

  @Test
  void startsOutTrackingNothing() {
    assertFalse(new SightedStrategy(new ChaseStrategy(), new WanderStrategy()).isTrackingHero());
  }

  @Test
  void remembersWhatItWraps() {
    ChaseStrategy hunt = new ChaseStrategy();
    PatrolStrategy idle = new PatrolStrategy();
    SightedStrategy sighted = new SightedStrategy(hunt, idle, 5);

    assertEquals(hunt, sighted.whenSeen());
    assertEquals(idle, sighted.whenUnseen());
    assertEquals(5, sighted.sightRange());
  }

  @Test
  void rejectsANegativeSightRange() {
    assertThrows(
        IllegalArgumentException.class,
        () -> new SightedStrategy(new ChaseStrategy(), new WanderStrategy(), -1));
  }
}
