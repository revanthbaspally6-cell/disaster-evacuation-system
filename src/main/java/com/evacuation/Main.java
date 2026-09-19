package com.evacuation;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) {
        Controller controller = new Controller();
        Scene scene = new Scene(controller.getRoot(), 1200, 760);
        stage.setTitle("Real-Time Disaster Evacuation Planning System (Multi-Source Dijkstra)");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

