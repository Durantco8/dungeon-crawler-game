package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.Observer;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class View implements FXComponent, Observer {
  private final Controller controller;
  private final Model model;
  private final Stage stage;
  private boolean darkMode = true;

  public View(Controller controller, Model model, Stage stage) {
    this.controller = controller;
    this.model = model;
    this.stage = stage;
  }

  public Parent render() {
    if (model.getStatus() == Model.STATUS.END_GAME) {
      return new TitleScreenView(controller, model, this).render();
    } else {
      return new GameView(controller, model, this).render();
    }
  }

  @Override
  public void update() {
    Scene scene =
        new Scene(this.render(), stage.getScene().getWidth(), stage.getScene().getHeight());
    String css;
    if (darkMode) {
      css = "dungeon.css";
    } else {
      css = "dungeon-light.css";
    }
    scene.getStylesheets().add(css);
    stage.setScene(scene);
  }

  public boolean isDarkMode() {
    return darkMode;
  }

  public void toggleDarkMode() {
    darkMode = !darkMode;
    update();
  }
}
