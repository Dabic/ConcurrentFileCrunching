package cruncher.views;

import cruncher.models.CruncherPool;
import cruncher.models.CruncherWorker;
import fileInput.models.FileInputResult;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import observers.IPublisher;
import observers.ISubscriber;
import observers.notifications.*;
import output.models.OutputPool;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

public class CruncherViewPaneComponent extends VBox implements IPublisher, ISubscriber {
    private Label cruncherLbl;
    private Button addCruncherBtn;
    private List<ISubscriber> subscribers;
    private List<CruncherViewComponent> crunchers;
    private CruncherPool cruncherPool;
    private OutputPool outputPool;
    private Label something;
    private LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue;
    private int diedCount = 0;
    private int limit;
    private Semaphore diedCountSemaphore = new Semaphore(1);

    public CruncherViewPaneComponent(int limit, CruncherPool cruncherPool, OutputPool outputPool, LinkedBlockingQueue<HashMap<String, HashMap<String, Integer>>> outputQueue) {
        this.cruncherPool = cruncherPool;
        this.outputPool = outputPool;
        this.outputQueue = outputQueue;
        crunchers = new ArrayList<>();
        something = new Label();
        this.limit = limit;
        initComponents();
        addComponents();
        setActions();
    }

    public void initComponents() {
        cruncherLbl = new Label("Crunchers:");
        addCruncherBtn = new Button("Add Cruncher");

        setBorder(new Border(new BorderStroke(Color.GRAY,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
        setPadding(new Insets(10, 10, 10, 10));
        setSpacing(5);
    }

    public void addComponents() {
        getChildren().add(cruncherLbl);
        getChildren().add(addCruncherBtn);
    }

    public void setActions() {
        addCruncherBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                TextInputDialog textInputDialog = new TextInputDialog();
                textInputDialog.setTitle("Add Cruncher Dialog");
                textInputDialog.setHeaderText("Enter cruncher arity");
                Optional<String> result = textInputDialog.showAndWait();
                if (result.isPresent()) {
                    String resultArity = result.get();
                    addCruncher(resultArity);
                }
            }
        });
    }

    public void addCruncher(String arity) {
        CruncherViewComponent cruncher = new CruncherViewComponent("Cruncher " + String.valueOf(crunchers.size()), arity, cruncherPool);
        cruncher.addSubscriber(this);
        crunchers.add(cruncher);
        getChildren().add(cruncher);
        EventHandler<WorkerStateEvent> diedEvent = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                try {
                    diedCountSemaphore.acquire();
                    diedCount++;
                    diedCountSemaphore.release();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
        EventHandler<WorkerStateEvent> otuOfMemoryEvent = new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent workerStateEvent) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Out Of Memory", ButtonType.CLOSE);
                alert.showAndWait();
                System.exit(0);
            }
        };
        CruncherWorker cruncherWorker = new CruncherWorker(
                "Cruncher " + String.valueOf(crunchers.size()-1),
                Integer.parseInt(arity),
                limit,
                outputQueue
        );
        cruncherWorker.setOnCancelled(otuOfMemoryEvent);
        cruncherWorker.setOnSucceeded(diedEvent);
        cruncher.getSomething().textProperty().bind(cruncherWorker.messageProperty());
        cruncher.getSomething().textProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                String message = cruncher.getSomething().getText();
                if (!message.isEmpty()) {
                    String[] actions = message.split(",");
                    String cruncherName = actions[2];
                    if (actions[0].equals("started")) {
                        for (CruncherViewComponent cruncher : crunchers) {
                            if (cruncher.getCruncherName().equals(cruncherName)) {
                                cruncher.addStatusLabel(actions[1]);
                            }
                        }
                    } else if (actions[0].equals("ended")) {
                        for (CruncherViewComponent cruncher : crunchers) {
                            if (cruncher.getCruncherName().equals(cruncherName)) {
                                cruncher.removeStatusLabel(actions[1]);
                            }
                        }
                        notifySubscribers(new CruncherStartedForOutputNotification(actions[1]+"-arity"+arity));
                    }
                }
            }
        });
        cruncherPool.executeCruncher(cruncherWorker);
        cruncherPool.addCruncher(cruncherWorker);
        notifySubscribers(new CruncherAddedNotification(cruncherPool.getCruncherWorkerList())); //obavestavam fileinpuviewpane
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
        if (notification instanceof CruncherRemovedNotification) { //obavestava me cruncherview
            for (CruncherViewComponent cruncher : crunchers) {
                if (cruncher.getCruncherName().equals(((CruncherRemovedNotification) notification).getOwner())) {
                    for (CruncherWorker worker : cruncherPool.getCruncherWorkerList()) {
                        if (worker.getCruncherName().equals(((CruncherRemovedNotification) notification).getOwner())) {
                            worker.getInputQueue().add(new FileInputResult(true));
                            cruncherPool.removeCruncher(worker);
                            break;
                        }
                    }
                    crunchers.remove(cruncher);
                    getChildren().remove(cruncher);
                    notifySubscribers(new CruncherRemovedNotification(cruncherPool.getCruncherWorkerList())); //obavestavam fileinputviewpane
                    break;
                }
            }
        } else if (notification instanceof CrunchingStartedNotification) {
            if (!((CrunchingStartedNotification) notification).getOwner().toString().isEmpty()) {
                String[] actions = ((String)((CrunchingStartedNotification) notification).getOwner()).split(",");
                String cruncherName = actions[3];
                for (CruncherViewComponent cruncher : crunchers) {
                    if (cruncher.getCruncherName().equals(cruncherName)) {
                        cruncher.addStatusLabel(actions[2]);
                        notifySubscribers(new CruncherStartedForOutputNotification("*" + actions[2] + "-arity" + cruncher.getArity()));
                    }
                }
            }
        } else if (notification instanceof CloseApplicationNotification) {
            diedCount = 0;
            for (CruncherWorker cruncherWorker : cruncherPool.getCruncherWorkerList()) {
                cruncherWorker.getInputQueue().add(new FileInputResult(true));
            }
        }
    }

    public boolean areAllCrunchersDead() {
        if (diedCount != cruncherPool.getCruncherWorkerList().size()) {
            return false;
        }
        if (diedCount == cruncherPool.getCruncherWorkerList().size())
            for (CruncherWorker worker : cruncherPool.getCruncherWorkerList()) {
                if (!worker.getInputQueue().isEmpty())
                    return false;
            }
        return true;
    }

    public Semaphore getDiedCountSemaphore() {
        return diedCountSemaphore;
    }

    public CruncherPool getCruncherPool() {
        return cruncherPool;
    }
}
