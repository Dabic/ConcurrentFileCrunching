package fileInput.views;

import cruncher.models.CruncherPool;
import cruncher.models.CruncherWorker;
import fileInput.models.*;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import observers.IPublisher;
import observers.ISubscriber;
import observers.models.DirectoryNotificationModel;
import observers.notifications.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;


public class FileInputViewPaneComponent extends VBox implements IPublisher, ISubscriber {
    private Label paneLbl;
    private ComboBox<String> diskComboBox;
    private Button addFileInputBtn;
    private FileInputViewContainerComponent fileInputViewContainerComponent;
    private List<ISubscriber> subscribers;
    private FileInputPool fileInputPool;
    private LinkedBlockingQueue<FileInputResult> fileInputResultsQueue;
    List<FileInputModel> fileInputModels;
    private String[] disks;
    private CruncherPool cruncherPool;
    private int sleepTime;
    private int diedCount = 0;
    private Semaphore diedCountSemaphore = new Semaphore(1);
    public FileInputViewPaneComponent(int sleepTime, FileInputPool fileInputPool, LinkedBlockingQueue<FileInputResult> fileInputResultsQueue, List<FileInputModel> fileInputModels, String[] disks, CruncherPool cruncherPool) {
        this.cruncherPool = cruncherPool;
        this.fileInputPool = fileInputPool;
        this.fileInputResultsQueue = fileInputResultsQueue;
        this.fileInputModels = fileInputModels;
        this.disks = disks;
        this.sleepTime = sleepTime;
        initComponents();
        addComponents();
        setActions();
        setSubscribersForThisComponent();
        setListenerForComboBox();
    }

    public void initComponents() {
        paneLbl = new Label("File inputs:");
        diskComboBox = new ComboBox<>(FXCollections.observableArrayList(Arrays.asList(disks)));
        diskComboBox.getSelectionModel().select(0);
        addFileInputBtn = new Button("Add File Input");
        fileInputViewContainerComponent = new FileInputViewContainerComponent(cruncherPool, fileInputModels, fileInputPool);

        setPadding(new Insets(10, 10, 10, 10));
        setBorder(new Border(new BorderStroke(Color.GRAY,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));

    }

    public void addComponents() {
        getChildren().add(paneLbl);
        getChildren().add(diskComboBox);
        getChildren().add(addFileInputBtn);
        getChildren().add(fileInputViewContainerComponent);
    }

    public void setActions() {
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
        addFileInputBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                FileInputModel fileInputModel = new FileInputModel(
                        diskComboBox.getSelectionModel().getSelectedItem(),
                        new FileInputWorker(
                                fileInputPool,
                                diskComboBox.getSelectionModel().getSelectedItem(),
                                new LinkedBlockingQueue<>(),
                                new ArrayList<>(),
                                new LinkedBlockingQueue<>(),
                                fileInputPool.getExecutedFiles(),
                                fileInputPool.getExecutedFilesSemaphore(),
                                sleepTime
                        )
                );
                fileInputModel.getFileInputWorker().setOnSucceeded(diedEvent);
                addFileInputBtn.setDisable(true);
                fileInputModels.add(fileInputModel);
                notifySubscribers(new FileInputAddedNotification(fileInputModel)); //obavestavam fileinputviewcontainera
            }
        });
    }

    public void setListenerForComboBox() {
        diskComboBox.valueProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                for (FileInputModel fileInputModel : fileInputModels) {
                    if (fileInputModel.getId().equals(diskComboBox.getSelectionModel().getSelectedItem())) {
                        addFileInputBtn.setDisable(true);
                        return;
                    }
                }
                addFileInputBtn.setDisable(false);
            }
        });
    }

    public void setSubscribersForThisComponent() {
        addSubscriber(fileInputViewContainerComponent);
        fileInputViewContainerComponent.addSubscriber(this);
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
        if (notification instanceof DirectoryAddedNotification) { //cuo od fileinputviewcontainera
            DirectoryNotificationModel notificationModel = (DirectoryNotificationModel)((DirectoryAddedNotification) notification).getOwner();
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(notificationModel.getFileInputName())) {
                    fileInputModel.addDirectory(notificationModel.getDirectoryPath());
                    List<File> newFiles = new ArrayList<>();
                    for (String file : fileInputModel.getDirectories()) {
                        newFiles.add(new File(file));
                    }
                    fileInputModel.getFileInputWorker().setFiles(newFiles);
                }
            }
        } else if (notification instanceof DirectoryRemovedNotification) { //cuo od fileinputviewcontainera
            DirectoryNotificationModel notificationModel = (DirectoryNotificationModel)((DirectoryRemovedNotification) notification).getOwner();
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(notificationModel.getFileInputName())) {
                    fileInputModel.removeDirectory(notificationModel.getDirectoryPath());
                    List<File> newFiles = new ArrayList<>();
                    for (String file : fileInputModel.getDirectories()) {
                        newFiles.add(new File(file));
                    }
                    fileInputModel.getFileInputWorker().setFiles(newFiles);
                }
                fileInputPool.getExecutedFiles().removeIf(fileModel -> fileModel.getFilePath().startsWith(notificationModel.getDirectoryPath()));
            }
        } else if (notification instanceof FileInputRemovedNotification) { //cuo od fileinputviewcontainera

            for (FileInputModel fileInputModel : fileInputModels) {
                System.out.println(fileInputModel.getId() + " " + ((FileInputRemovedNotification) notification).getOwner());
                if (fileInputModel.getId().equals(((FileInputRemovedNotification) notification).getOwner())) {
                    for (String dir : fileInputModel.getDirectories()) {
                        fileInputPool.getExecutedFiles().removeIf(fileModel -> fileModel.getFilePath().startsWith(dir));
                    }
                    fileInputModels.remove(fileInputModel);

                    break;
                }
            }
            if (diskComboBox.getSelectionModel().getSelectedItem().equals(((FileInputRemovedNotification) notification).getOwner())) {
                addFileInputBtn.setDisable(false);
            }
        } else if (notification instanceof CruncherAddedNotification) { //cuo od cruncherviewpane
            fileInputViewContainerComponent.setComboBoxItems((List<CruncherWorker>)((CruncherAddedNotification) notification).getOwner());
        } else if (notification instanceof CruncherRemovedNotification) { //cuo od cruncherviewpane
            fileInputViewContainerComponent.setComboBoxItems((List<CruncherWorker>)((CruncherRemovedNotification) notification).getOwner());
        } else if (notification instanceof FileInputStartedNotification) {
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(((FileInputStartedNotification) notification).getOwner())) {
                    if (!fileInputModel.isExecuted()) {
                        fileInputPool.executeWorker(fileInputModel);
                        Thread newThread = new Thread(fileInputModel.getFileInputWorker());
                        newThread.start();
                        fileInputModel.setExecuted(true);
                    } else {
                        fileInputModel.getFileInputWorker().started();
                    }
                }
            }
        } else if (notification instanceof CrunchingStartedNotification) {
            notifySubscribers(notification); //obavestavam cruncherviewpane
        } else if (notification instanceof FileInputPausedNotification) {
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(((FileInputPausedNotification) notification).getOwner())) {
                    fileInputModel.getFileInputWorker().paused();
                }
            }
        } else if (notification instanceof CloseApplicationNotification) {
            diedCount = 0;
            for (FileInputModel fileInputModel : fileInputModels) {
                if (!fileInputModel.isExecuted()) {
                    Thread newThread = new Thread(fileInputModel.getFileInputWorker());
                    newThread.start();
                }
                fileInputModel.getFileInputWorker().getInputQueue().add(new File("poison_file.txt"));
            }
        }
    }

    public Semaphore getDiedCountSemaphore() {
        return diedCountSemaphore;
    }

    public boolean areAllFileInputsDead() {
        if (diedCount == fileInputModels.size())
            return true;
        return false;
    }

}
