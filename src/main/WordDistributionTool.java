package main;

import fileInput.models.FileInputPool;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class WordDistributionTool extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        HBox root = new HBox();
        primaryStage.setTitle("Word distribution tool");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
    }
}
