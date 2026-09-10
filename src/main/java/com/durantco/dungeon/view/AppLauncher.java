package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.controller.ControllerImpl;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import com.durantco.dungeon.model.board.BoardImpl;
import com.durantco.dungeon.model.board.BspLevelGenerator;
import java.util.Random;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {

  /**
   * Board size in cells. Large enough for the partitioning generator to produce a recognisable
   * room-and-corridor dungeon; an 8x8 board only has space for one or two rooms.
   */
  private static final int BOARD_WIDTH = 28;

  private static final int BOARD_HEIGHT = 18;

  private static final int WINDOW_WIDTH = 900;
  private static final int WINDOW_HEIGHT = 820;

  @Override
  public void start(Stage stage) {
    stage.setTitle("Coleton's Dungeon Crawler");
    // The composition root: this is where the concrete generator and randomness are chosen.
    Model model =
        new ModelImpl(
            new BoardImpl(BOARD_WIDTH, BOARD_HEIGHT, new Random(), new BspLevelGenerator()));
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
