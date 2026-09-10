package com.durantco.dungeon.model.board;

import com.durantco.dungeon.model.pieces.Enemy;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Walks a fixed beat between the centres of the level's rooms, ignoring the hero entirely.
 *
 * <p>A patroller is a hazard to route around rather than something hunting you, which is what makes it
 * worth having alongside the chaser: it makes parts of the dungeon dangerous rather than making the
 * hero the only thing that matters.
 *
 * <p>Each patroller starts at a different point on the beat so several do not walk in step. Boards
 * built from a fixed layout have no rooms, so there is nothing to patrol and the enemy drifts instead.
 */
public final class PatrolStrategy implements MovementStrategy {

  private final MovementStrategy fallback = new WanderStrategy();

  private List<Posn> beat;
  private int waypoint;

  @Override
  public Optional<Posn> chooseStep(Enemy enemy, MovementContext context) {
    if (beat == null) {
      beat = plotBeat(context);
      if (!beat.isEmpty()) {
        waypoint = context.rng().nextInt(beat.size());
      }
    }
    if (beat.isEmpty()) {
      return fallback.chooseStep(enemy, context);
    }

    if (enemy.getPosn().equals(beat.get(waypoint))) {
      waypoint = (waypoint + 1) % beat.size();
    }

    // Try each waypoint once, so an unreachable stretch of the beat is skipped rather than looped on.
    for (int tried = 0; tried < beat.size(); tried++) {
      Optional<Posn> step = context.stepTowards(enemy, beat.get(waypoint));
      if (step.isPresent()) {
        return step;
      }
      waypoint = (waypoint + 1) % beat.size();
    }
    return fallback.chooseStep(enemy, context);
  }

  private static List<Posn> plotBeat(MovementContext context) {
    List<Posn> stops = new ArrayList<>();
    for (Room room : context.rooms()) {
      stops.add(room.center());
    }
    return List.copyOf(stops);
  }
}
