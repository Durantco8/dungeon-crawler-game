package com.durantco.dungeon.view;

import com.durantco.dungeon.controller.Controller;
import com.durantco.dungeon.model.Model;
import com.durantco.dungeon.model.board.Posn;
import com.durantco.dungeon.model.pieces.Piece;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;

public class GameView implements FXComponent {
  private final Controller controller;
  private final Model model;
  private final View view;

  public GameView(Controller controller, Model model, View view) {
    this.controller = controller;
    this.model = model;
    this.view = view;
  }

  @Override
  public Parent render() {
    GridPane grid = new GridPane();
    for (int row = 0; row < model.getHeight(); row++) {
      for (int col = 0; col < model.getWidth(); col++) {
        Piece piece = model.get(new Posn(row, col));
        Label cell = new Label();
        cell.setMinSize(50, 50);
        cell.getStyleClass().add("cell");
        if (piece != null) {
          String path;
          if (view.isDarkMode()) {
            path = piece.getResourcePath();
          } else {
            path = piece.getName().toLowerCase() + "-light.png";
          }
          ImageView img = new ImageView(new Image(path));
          img.setFitWidth(50);
          img.setFitHeight(50);
          cell.setGraphic(img);
        }
        grid.add(cell, col, row);
      }
    }
    grid.setHgap(0);
    grid.setVgap(0);
    grid.setAlignment(Pos.CENTER);
    Label score = new Label("Score: " + model.getCurScore());
    score.getStyleClass().add("game-score");

    Button up = new Button("Up");
    Button down = new Button("down");
    Button left = new Button("left");
    Button right = new Button("right");
    up.setOnAction(e -> controller.moveUp());
    down.setOnAction(e -> controller.moveDown());
    left.setOnAction(e -> controller.moveLeft());
    right.setOnAction(e -> controller.moveRight());
    up.getStyleClass().add("dpad-button");
    down.getStyleClass().add("dpad-button");
    left.getStyleClass().add("dpad-button");
    right.getStyleClass().add("dpad-button");

    // Bottom HBox for Dpad style
    HBox lowerButtons = new HBox(5, left, down, right);
    lowerButtons.setAlignment(Pos.CENTER);
    VBox dPad = new VBox(5, up, lowerButtons);
    dPad.setAlignment(Pos.CENTER);

    String label;
    if (view.isDarkMode()) {
      label = "Light Mode";
    } else {
      label = "Dark Mode";
    }
    ToggleButton toggleButton = new ToggleButton(label);
    toggleButton.setOnAction(e -> view.toggleDarkMode());
    toggleButton.getStyleClass().add("dark-mode");
    Text thiefMec =
        new Text(
            "Thief! He is looking for your treasure (-5 points). "
                + "However he is also trying not to get killed by the enemy");
    thiefMec.getStyleClass().add("thief-label");

    VBox gameViewPage = new VBox(10, score, grid, dPad, toggleButton, thiefMec);
    gameViewPage.getStyleClass().add("game-Page");
    gameViewPage.setAlignment(Pos.CENTER);

    return gameViewPage;
  }
}
