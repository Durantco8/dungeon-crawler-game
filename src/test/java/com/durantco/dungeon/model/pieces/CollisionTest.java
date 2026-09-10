package com.durantco.dungeon.model.pieces;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.durantco.dungeon.model.board.Posn;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Every ordered pair of mover and destination that the game can produce. The previous instanceof
 * ladders threw IllegalArgumentException for pairs they did not enumerate, so the pairs that used to
 * throw are asserted here as ordinary refusals.
 */
class CollisionTest {

  private static void assertOutcome(
      CollisionResult result, CollisionResult.Result expected, int expectedPoints) {
    assertEquals(expected, result.getResults());
    assertEquals(expectedPoints, result.getPoints());
  }

  @Nested
  class HeroEnters {

    @Test
    void anEmptyCellIsUneventful() {
      assertOutcome(new Hero().collide(null), CollisionResult.Result.CONTINUE, 0);
    }

    @Test
    void treasureScoresItsValue() {
      assertOutcome(new Hero().collide(new Treasure()), CollisionResult.Result.CONTINUE, 5);
    }

    @Test
    void aThiefCostsPoints() {
      assertOutcome(new Hero().collide(new Thief()), CollisionResult.Result.CONTINUE, -5);
    }

    @Test
    void anEnemyEndsTheGame() {
      assertOutcome(new Hero().collide(new Enemy()), CollisionResult.Result.GAME_OVER, 0);
    }

    @Test
    void theExitAdvancesTheLevel() {
      assertOutcome(new Hero().collide(new Exit()), CollisionResult.Result.NEXT_LEVEL, 0);
    }

    @Test
    @DisplayName("a wall refuses the hero instead of throwing")
    void aWallRefusesTheMove() {
      assertOutcome(new Hero().collide(new Wall()), CollisionResult.Result.BLOCKED, 0);
    }

    @Test
    @DisplayName("there is only one hero, so hero into hero is refused rather than fatal")
    void anotherHeroRefusesTheMove() {
      assertOutcome(new Hero().collide(new Hero()), CollisionResult.Result.BLOCKED, 0);
    }
  }

  @Nested
  class EnemyEnters {

    @Test
    void anEmptyCellIsUneventful() {
      assertOutcome(new Enemy().collide(null), CollisionResult.Result.CONTINUE, 0);
    }

    @Test
    @DisplayName("an enemy consumes treasure without scoring")
    void treasureIsConsumedWithoutPoints() {
      assertOutcome(new Enemy().collide(new Treasure()), CollisionResult.Result.CONTINUE, 0);
    }

    @Test
    void aThiefIsRemovedWithoutPoints() {
      assertOutcome(new Enemy().collide(new Thief()), CollisionResult.Result.CONTINUE, 0);
    }

    @Test
    void theHeroEndsTheGame() {
      assertOutcome(new Enemy().collide(new Hero()), CollisionResult.Result.GAME_OVER, 0);
    }

    @Test
    @DisplayName("a wall refuses the enemy instead of throwing")
    void aWallRefusesTheMove() {
      assertOutcome(new Enemy().collide(new Wall()), CollisionResult.Result.BLOCKED, 0);
    }

    @Test
    @DisplayName("the exit refuses the enemy instead of throwing")
    void theExitRefusesTheMove() {
      assertOutcome(new Enemy().collide(new Exit()), CollisionResult.Result.BLOCKED, 0);
    }

    @Test
    void anotherEnemyRefusesTheMove() {
      assertOutcome(new Enemy().collide(new Enemy()), CollisionResult.Result.BLOCKED, 0);
    }
  }

  @Nested
  class NoPairThrows {

    private static final Piece[] DESTINATIONS = {
      null, new Hero(), new Enemy(), new Wall(), new Exit(), new Treasure(), new Thief()
    };

    @Test
    void theHeroCanAttemptToEnterAnything() {
      Hero hero = new Hero();
      for (Piece destination : DESTINATIONS) {
        assertDoesNotThrow(() -> hero.collide(destination));
      }
    }

    @Test
    void anEnemyCanAttemptToEnterAnything() {
      Enemy enemy = new Enemy();
      for (Piece destination : DESTINATIONS) {
        assertDoesNotThrow(() -> enemy.collide(destination));
      }
    }
  }

  @Nested
  class OpenForNewPieceTypes {

    /**
     * A piece type declared entirely outside the production package, to show that adding one needs no
     * edits to any existing piece or to the movers. Its type tag is irrelevant here because nothing
     * renders it.
     */
    private static final class Portcullis implements Piece {
      private Posn position;

      @Override
      public PieceType getType() {
        return PieceType.WALL;
      }

      @Override
      public String getName() {
        return "Portcullis";
      }

      @Override
      public Posn getPosn() {
        return position;
      }

      @Override
      public void setPosn(Posn posn) {
        this.position = posn;
      }

      @Override
      public CollisionResult onHeroEnter(Hero hero) {
        return CollisionResult.blocked();
      }

      @Override
      public CollisionResult onEnemyEnter(Enemy enemy) {
        return CollisionResult.scoring(3);
      }
    }

    @Test
    @DisplayName("a brand new piece type takes part in collisions without changing Hero or Enemy")
    void dispatchesToAPieceTypeTheMoversHaveNeverHeardOf() {
      Portcullis gate = new Portcullis();
      assertOutcome(new Hero().collide(gate), CollisionResult.Result.BLOCKED, 0);
      assertOutcome(new Enemy().collide(gate), CollisionResult.Result.CONTINUE, 3);
    }
  }

  @Nested
  class Outcomes {

    @Test
    void freeIsAnUneventfulContinue() {
      assertOutcome(CollisionResult.free(), CollisionResult.Result.CONTINUE, 0);
    }

    @Test
    void scoringCarriesNegativeValues() {
      assertOutcome(CollisionResult.scoring(-5), CollisionResult.Result.CONTINUE, -5);
    }

    @Test
    void blockedCarriesNoPoints() {
      assertOutcome(CollisionResult.blocked(), CollisionResult.Result.BLOCKED, 0);
    }
  }
}
