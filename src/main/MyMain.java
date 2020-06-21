package main;

import cruncher.models.CruncherPool;
import cruncher.models.CruncherWorker;
import cruncher.views.CruncherViewPaneComponent;
import fileInput.models.FileInputModel;
import fileInput.models.FileInputPool;
import fileInput.models.FileInputResult;
import fileInput.views.FileInputViewPaneComponent;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import observers.notifications.CloseApplicationNotification;
import output.models.OutputPool;
import output.views.OutputViewPaneComponent;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class MyMain extends Application {

    private Stage primaryStage;
    private HBox rootElement;
    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        HBox root = new HBox();
        rootElement = root;
        primaryStage.setTitle("Word distribution tool");
        primaryStage.setScene(new Scene(root, 300, 275));
        primaryStage.show();
        primaryStage.setMaximized(true);

        int sleepTime = 0;
        int cruncherLimit = 0;
        String[] disks = new String[2];
        try {
            InputStream inputStream = new FileInputStream("src/app.properties");
            Properties appProperties = new Properties();
            appProperties.load(inputStream);
            sleepTime = Integer.parseInt(appProperties.getProperty("file_input_sleep_time"));
            cruncherLimit = Integer.parseInt(appProperties.getProperty("counter_data_limit"));
            disks = appProperties.getProperty("disks").split(";");
        } catch (IOException e) {
            e.printStackTrace();
        }

        LinkedBlockingQueue<FileInputResult> fileInputResultsQueue = new LinkedBlockingQueue<>();
        LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue = new LinkedBlockingQueue<>();
        FileInputPool fileInputPool = new FileInputPool(fileInputResultsQueue, sleepTime, new ArrayList<>());
        CruncherPool cruncherPool = new CruncherPool(outputQueue, cruncherLimit);
        OutputPool outputPool = new OutputPool(outputQueue);
        List<FileInputModel> fileInputModels = new ArrayList<>();
        FileInputViewPaneComponent fileInputComponent = new FileInputViewPaneComponent(sleepTime, fileInputPool, fileInputResultsQueue, fileInputModels, disks, cruncherPool);
        CruncherViewPaneComponent cruncherViewPaneComponent = new CruncherViewPaneComponent(cruncherLimit, cruncherPool, outputPool, outputQueue);
        OutputViewPaneComponent outputViewPaneComponent = new OutputViewPaneComponent(outputPool);
        HBox.setHgrow(outputViewPaneComponent, Priority.ALWAYS);
        cruncherViewPaneComponent.addSubscriber(fileInputComponent);
        fileInputComponent.addSubscriber(cruncherViewPaneComponent);
        cruncherViewPaneComponent.addSubscriber(outputViewPaneComponent);

        root.getChildren().add(fileInputComponent);
        root.getChildren().add(cruncherViewPaneComponent);
        root.getChildren().add(outputViewPaneComponent);
        primaryStage.getScene().getWindow().addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST, this::closeApp);
    }

    private void closeApp(WindowEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setContentText("Finishing all active processes...");
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.initOwner(primaryStage.getScene().getWindow());
        alert.getButtonTypes().remove(ButtonType.OK);
        ((FileInputViewPaneComponent)rootElement.getChildren().get(0)).update(new CloseApplicationNotification(null));
        ((OutputViewPaneComponent)rootElement.getChildren().get(2)).update(new CloseApplicationNotification(null));


        Thread someThread = new Thread(new Task<Boolean>() {
            @Override
            protected Boolean call() throws Exception {
                while (true) {
                    ((FileInputViewPaneComponent)rootElement.getChildren().get(0)).getDiedCountSemaphore().acquire();
                    if (((FileInputViewPaneComponent)rootElement.getChildren().get(0)).areAllFileInputsDead())
                        break;
                    ((FileInputViewPaneComponent)rootElement.getChildren().get(0)).getDiedCountSemaphore().release();
                }
                ((FileInputViewPaneComponent)rootElement.getChildren().get(0)).getDiedCountSemaphore().release();

                List<String> killedCrunchers = new ArrayList<>();
                int killedLen = 0;
                List<CruncherWorker> cruncherWorkers = ((CruncherViewPaneComponent)rootElement.getChildren().get(1)).getCruncherPool().getCruncherWorkerList();
                int killedMax = cruncherWorkers.size();
                while (killedLen != killedMax) {
                    for (CruncherWorker cruncherWorker : cruncherWorkers) {
                        if (notInKilledCrunchers(killedCrunchers, cruncherWorker.getCruncherName()) && cruncherWorker.getInputQueue().isEmpty()) {
                            cruncherWorker.getInputQueue().add(new FileInputResult(true));
                            killedCrunchers.add(cruncherWorker.getCruncherName());
                            killedLen++;
                        }
                    }
                }
                while (true) {
                    ((CruncherViewPaneComponent)rootElement.getChildren().get(1)).getDiedCountSemaphore().acquire();
                    if (((CruncherViewPaneComponent)rootElement.getChildren().get(1)).areAllCrunchersDead())
                        break;
                    ((CruncherViewPaneComponent)rootElement.getChildren().get(1)).getDiedCountSemaphore().release();
                }
                ((CruncherViewPaneComponent)rootElement.getChildren().get(1)).getDiedCountSemaphore().release();

                System.out.println("all died");
                System.exit(0);
                return null;
            }
        });
        someThread.start();

        alert.showAndWait();
    }

    public boolean notInKilledCrunchers(List<String> killed, String name) {
        for (String kill : killed) {
            if (kill.equals(name))
                return false;
        }
        return true;
    }
    public static void main(String[] args) {
        try {
            launch(args);
        } catch (OutOfMemoryError ignore) {

        }
    }
}
