package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.controller.ControllerImpl;
import com.durantco.dungeon.model.GameFactory;
import com.durantco.dungeon.model.GameSetup;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.persistence.FileHighScoreStore;
import java.util.Random;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {

  private static final int WINDOW_WIDTH = 900;
  private static final int WINDOW_HEIGHT = 820;

  @Override
  public void start(Stage stage) {
    stage.setTitle("Coleton's Dungeon Crawler");
    // The composition root. A fresh seed each launch makes every session a different dungeon, and
    // describing the game as a setup rather than as scattered literals is what lets it be recorded.
    Model model =
        GameFactory.create(
            GameSetup.standard(new Random().nextLong()), FileHighScoreStore.inUserHome());
    Controller controller = new ControllerImpl(model);
    View view = new View(controller, model, stage);
    model.addObserver(view);

    String css;
    if (view.isDarkMode()) {
      css = "dungeon.css";
    } else {
      css = "dungeon-light.css";
    }
    Scene scene = new Scene(view.render(), WINDOW_WIDTH, WINDOW_HEIGHT);
    scene.getStylesheets().add(css);
    stage.setScene(scene);
    stage.show();
  }
}
