package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.model.Model;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

public class TitleScreenView implements FXComponent {

  private final Controller controller;
  private final Model model;
  private final View view;

  public TitleScreenView(Controller controller, Model model, View view) {
    this.controller = controller;
    this.model = model;
    this.view = view;
  }

  @Override
  public Parent render() {
    Label title = new Label("Dungeon Crawler");
    title.getStyleClass().add("title");

    Label highScore = new Label("High Score: " + model.getHighScore());
    highScore.getStyleClass().add("highScore");

    Label lastScore = new Label("Last Score: " + model.getCurScore());
    lastScore.getStyleClass().add("lastScore");

    HBox scores = new HBox(20, highScore, lastScore);
    scores.setAlignment(Pos.CENTER);

    Button startButton = new Button("Start Game");
    startButton.getStyleClass().add("start-button");
    startButton.setOnAction(e -> controller.startGame());

    Label authorLine = new Label("By: Coleton DuRant");
    authorLine.getStyleClass().add("author-Line");

    String label;
    if (view.isDarkMode()) {
      label = "Light Mode";
    } else {
      label = "Dark Mode";
    }
    ToggleButton toggleButton = new ToggleButton(label);
    toggleButton.setOnAction(e -> view.toggleDarkMode());
    toggleButton.getStyleClass().add("dark-mode");

    String diffLabel;
    if (model.isHardMode()) {
      diffLabel = "Hard Mode";
    } else {
      diffLabel = "Easy Mode";
    }
    ToggleButton hardModeToggle = new ToggleButton(diffLabel);
    hardModeToggle.setOnAction(e -> controller.setHardMode(!model.isHardMode()));
    hardModeToggle.getStyleClass().add("hardMode-button");

    VBox vbox = new VBox(10, title, scores, startButton, hardModeToggle, toggleButton, authorLine);
    vbox.setAlignment(Pos.CENTER);

    StackPane titlePage = new StackPane(vbox);
    titlePage.getStyleClass().add("title-Page");

    return new StackPane(titlePage);
  }
}
