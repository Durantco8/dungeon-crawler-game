package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.controller.ControllerImpl;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.ModelImpl;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class AppLauncher extends Application {
  @Override
  public void start(Stage stage) {
    stage.setTitle("Coleton's Dungeon Crawler");
    Model model = new ModelImpl(8, 8);
    Controller controller = new ControllerImpl(model);
    View view = new View(controller, model, stage);
    model.addObserver(view);

    String css;
    if (view.isDarkMode()) {
      css = "dungeon.css";
    } else {
      css = "dungeon-light.css";
    }
    Scene scene = new Scene(view.render(), 900, 700);
    scene.getStylesheets().add(css);
    stage.setScene(scene);
    stage.show();
  }
}
