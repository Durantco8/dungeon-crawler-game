package com.durantco.dungeon.model.pieces;

import com.durantco.dungeon.model.board.Posn;

/**
 * A single occupant of one board cell.
 *
 * <p>Collision rules live on the piece being entered rather than on the piece doing the entering.
 * {@link MovablePiece#collide} asks the destination what happens, and the destination answers,
 * because the destination is what owns the knowledge: a treasure knows what it is worth, a wall
 * knows it cannot be walked through. A new kind of piece is therefore one new class implementing
 * these two methods, with no edits to any existing piece.
 *
 * <p>The tradeoff is the other axis: a new kind of piece that can *initiate* a collision would add a
 * method here and touch every implementation. That is the intended trade, since the passive piece
 * types are what grow.
 */
public interface Piece {
  /**
   * @return the kind of this piece, for presentation purposes only
   */
  PieceType getType();

  /**
   * @return the human-readable name of this piece
   */
  String getName();

  Posn getPosn();

  void setPosn(Posn posn);

  /**
   * Resolves what happens when the hero moves onto this piece.
   *
   * @param hero the hero attempting to enter this cell
   * @return the outcome, which is {@link CollisionResult.Result#BLOCKED} if the hero cannot enter
   */
  CollisionResult onHeroEnter(Hero hero);

  /**
   * Resolves what happens when an enemy moves onto this piece.
   *
   * @param enemy the enemy attempting to enter this cell
   * @return the outcome, which is {@link CollisionResult.Result#BLOCKED} if the enemy cannot enter
   */
  CollisionResult onEnemyEnter(Enemy enemy);
}
