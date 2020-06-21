package output.views;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import observers.IPublisher;
import observers.ISubscriber;
import observers.notifications.OutputSumResultsNotification;
import output.models.OutputPool;
import output.models.OutputSingleResultWorker;
import output.models.OutputSumResultsWorker;

import java.util.*;

public class OutputListComponent extends VBox implements IPublisher, ISubscriber {

    private ListView<String> listView;
    private Button singleResultBtn;
    private Button sumResultsBtn;
    private ProgressBar progressBar;
    private List<ISubscriber> subscribers;

    public OutputListComponent(OutputPool outputPool) {
        initComponents();
        addComponents();
        setActions(outputPool);
        addListeners();
        progressBar.progressProperty().set(101.0);
    }

    public void initComponents() {
        listView = new ListView<>();
        listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        singleResultBtn = new Button("Single Result");
        singleResultBtn.setDisable(true);
        sumResultsBtn = new Button("Sum Results");
        sumResultsBtn.setDisable(true);
        progressBar = new ProgressBar();

    }

    public void addComponents() {
        getChildren().add(listView);
        getChildren().add(singleResultBtn);
        getChildren().add(sumResultsBtn);
        getChildren().add(progressBar);
    }

    public void setActions(OutputPool outputPool) {
        singleResultBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (listView.getSelectionModel().getSelectedItem().startsWith("*")) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setContentText("Requested result is not ready yet.");
                    alert.showAndWait();
                    return;
                }
                Task<SortedSet<Map.Entry<String, Integer>>> task = new OutputSingleResultWorker();
                progressBar.progressProperty().bind(task.progressProperty());
                EventHandler<WorkerStateEvent> jobDone = new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        progressBar.setVisible(false);
                    }
                };
                outputPool.getSingleResult(task, listView.getSelectionModel().getSelectedItem());
                task.setOnSucceeded(jobDone);
            }
        });

        sumResultsBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                List<String> selected = new ArrayList<>(listView.getSelectionModel().getSelectedItems());
                Task<HashMap<String, Integer>> task = new OutputSumResultsWorker();
                progressBar.progressProperty().bind(task.progressProperty());
                EventHandler<WorkerStateEvent> jobDone = new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent workerStateEvent) {
                        progressBar.setVisible(false);
                    }
                };
                task.setOnSucceeded(jobDone);
                TextInputDialog dialog = new TextInputDialog();
                dialog.setContentText("Please enter sum name:");
                Optional<String> res = dialog.showAndWait();
                System.out.println(selected);
                if (res.isPresent())
                    outputPool.getResultsSum(res.get(), task, selected);
            }
        });
    }

    public void addListeners() {
        listView.getSelectionModel().getSelectedItems().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (listView.getSelectionModel().getSelectedItems().isEmpty()) {
                    singleResultBtn.setDisable(true);
                    sumResultsBtn.setDisable(true);
                } else if (listView.getSelectionModel().getSelectedItems().size() > 1){
                    singleResultBtn.setDisable(true);
                    sumResultsBtn.setDisable(false);
                } else {
                    singleResultBtn.setDisable(false);
                    sumResultsBtn.setDisable(true);
                }
            }
        });

        progressBar.progressProperty().addListener(new ChangeListener<Number>() {
            @Override
            public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
                if (t1.equals(101.0) || t1.equals(100.0)) {
                    progressBar.setVisible(false);
                } else {
                    progressBar.setVisible(true);
                }
            }
        });
    }
    public void addToList(String name) {
        if (name.charAt(0) != '*') {
            List<String> newValues = new ArrayList<>();
            for (String items : listView.getItems()) {
                if (items.substring(1, items.length()).equals(name)) {
                    newValues.add(items.replace("*", ""));
                } else {
                    newValues.add(items);
                }
            }
            listView.getItems().clear();
            listView.getItems().addAll(newValues);
        } else {
            for (String item : listView.getItems()) {
                if (item.equals(name) || name.substring(1, name.length()).equals(item))
                    return;
            }
            listView.getItems().add(name);
        }
    }

    @Override
    public void addSubscriber(ISubscriber sub) {
        if(sub == null)
            return;
        if(this.subscribers ==null)
            this.subscribers = new ArrayList<>();
        if(this.subscribers.contains(sub))
            return;
        this.subscribers.add(sub);
    }

    @Override
    public void removeSubscriber(ISubscriber sub) {
        if(sub == null || this.subscribers == null || !this.subscribers.contains(sub))
            return;
        this.subscribers.remove(sub);
    }

    @Override
    public void notifySubscribers(Object notification) {
        if(notification == null || this.subscribers == null || this.subscribers.isEmpty())
            return;

        for(ISubscriber listener : subscribers){
            listener.update(notification);
        }
    }

    @Override
    public void update(Object notification) {
        if (notification instanceof OutputSumResultsNotification) {
            String name = String.valueOf(((OutputSumResultsNotification) notification).getOwner());
            listView.getItems().add(name);
        }
    }
}
