package com.durantco.dungeon.support;

import com.durantco.dungeon.model.Observer;

/** Records how many times it was notified. */
public final class CountingObserver implements Observer {
  private int updates;

  @Override
  public void update() {
    updates++;
  }

  public int updates() {
    return updates;
  }
}
