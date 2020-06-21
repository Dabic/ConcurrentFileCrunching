package fileInput.views;

import cruncher.models.CruncherPool;
import cruncher.models.CruncherWorker;
import fileInput.models.FileInputModel;
import fileInput.models.FileInputPool;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import observers.IPublisher;
import observers.ISubscriber;
import observers.models.CruncherLinkingModel;
import observers.models.DirectoryNotificationModel;
import observers.notifications.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FileInputViewContainerComponent extends VBox implements ISubscriber, IPublisher {

    private List<FileInputModel> fileInputModels;
    private List<ISubscriber> subscribers;
    private List<FileInputViewComponent> fileInputs;
    private CruncherPool cruncherPool;
    private FileInputPool fileInputPool;
    private Label crunchingMessage;
    public FileInputViewContainerComponent(CruncherPool cruncherPool, List<FileInputModel> fileInputModels, FileInputPool fileInputPool) {
        this.fileInputModels = fileInputModels;
        fileInputs = new ArrayList<>();
        this.cruncherPool = cruncherPool;
        this.fileInputPool = fileInputPool;
        this.crunchingMessage = new Label();
    }

    public void addFileInput(FileInputModel fileInputModel) {
        FileInputViewComponent fileInputViewComponent = new FileInputViewComponent(fileInputModel.getId(), cruncherPool.getCruncherWorkerList());
        fileInputViewComponent.addSubscriber(this);
        fileInputViewComponent.getStatusLbl().textProperty().bind(fileInputModel.getFileInputWorker().messageProperty());
        fileInputViewComponent.getStatusLbl().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (fileInputViewComponent.getStatusLbl().getText().equals("outOfMemory")) {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Out Of Memory", ButtonType.CLOSE);
                    alert.showAndWait();
                    System.exit(0);
                } else if (fileInputViewComponent.getStatusLbl().getText().isEmpty()) {
                    fileInputViewComponent.getStatusLbl().setText("Idle");
                }
            }
        });
        fileInputViewComponent.getCrunchingMessage().textProperty().bind(fileInputModel.getFileInputWorker().titleProperty());
        fileInputViewComponent.getCrunchingMessage().textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                String message = fileInputViewComponent.getCrunchingMessage().getText();
                if (!message.isEmpty()) {
                    if (message.split(",")[0].equals(fileInputModel.getId()))
                        notifySubscribers(new CrunchingStartedNotification(message)); //obavestavam fileinputviewpane
                }
            }
        });
        fileInputs.add(fileInputViewComponent);
        getChildren().add(fileInputViewComponent);
    }

    public void removeFileInput(String fileInputName) {
        for (FileInputViewComponent fileInputViewComponent : fileInputs) {
            if (fileInputViewComponent.getFileInputName().equals(fileInputName)) {
                getChildren().remove(fileInputViewComponent);
                fileInputs.remove(fileInputViewComponent);
                break;
            }
        }
        for (FileInputModel fileInputModel : fileInputModels) {
            if (fileInputModel.getId().equals(fileInputName)) {
                fileInputModel.getFileInputWorker().getInputQueue().add(new File("poison_file.txt"));
                fileInputModels.remove(fileInputModel);
                break;
            }
        }
    }

    public void setComboBoxItems(List<CruncherWorker> workers) {
        for (FileInputViewComponent fileInputViewComponent : fileInputs) {
            fileInputViewComponent.getCruncherComboBox().getItems().clear();
            for (CruncherWorker worker : workers) {
                fileInputViewComponent.getCruncherComboBox().getItems().add(worker.getCruncherName());
                if (fileInputViewComponent.getCruncherComboBox().getItems().size() > 0) {
                    fileInputViewComponent.getCruncherComboBox().getSelectionModel().select(0);

                }
            }
        }
    }
    @Override
    public void update(Object notification) {
        if (notification instanceof FileInputAddedNotification) { //cuo do fileinputviewpane
            FileInputModel fileInputModel = (FileInputModel)((FileInputAddedNotification) notification).getOwner();
            addFileInput(fileInputModel);
        } else if (notification instanceof DirectoryAddedNotification) { //cuo od fileinputview
            DirectoryNotificationModel notificationModel = (DirectoryNotificationModel)((DirectoryAddedNotification) notification).getOwner();
            notifySubscribers(notification); //obavestavam fileinputviewpane
        } else if (notification instanceof DirectoryRemovedNotification) { //cuo od fileinputview
            DirectoryNotificationModel notificationModel = (DirectoryNotificationModel)((DirectoryRemovedNotification) notification).getOwner();
            notifySubscribers(notification); //obavestavam fileinputviewpane
        } else if (notification instanceof FileInputRemovedNotification) { //cuo od fileinputview
            removeFileInput((String)((FileInputRemovedNotification) notification).getOwner());
            notifySubscribers(notification); //obavestavam fileinputviewpane
        } else if (notification instanceof CruncherLinkedNotification) { //cuo od fileinputview
            CruncherLinkingModel cruncherLinkingModel = (CruncherLinkingModel)((CruncherLinkedNotification) notification).getOwner();
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(cruncherLinkingModel.getFileInputModelName())) {
                    CruncherWorker worker = null;
                    for (CruncherWorker cruncherWorker : cruncherPool.getCruncherWorkerList()) {
                        if (cruncherWorker.getCruncherName().equals(cruncherLinkingModel.getCruncherName())) {
                            worker = cruncherWorker;
                            break;
                        }
                    }
                    if (worker != null)
                        fileInputModel.getCruncherWorkers().add(worker);
                }
            }
        } else if (notification instanceof CruncherUnlinkedNotification) { //cuo od fileinputview
            CruncherLinkingModel cruncherLinkingModel = (CruncherLinkingModel)((CruncherUnlinkedNotification) notification).getOwner();
            for (FileInputModel fileInputModel : fileInputModels) {
                if (fileInputModel.getId().equals(cruncherLinkingModel.getFileInputModelName())) {
                    for (CruncherWorker cruncherWorker : fileInputModel.getCruncherWorkers()) {
                        if (cruncherWorker.getCruncherName().equals(cruncherLinkingModel.getCruncherName())) {
                            fileInputModel.getCruncherWorkers().remove(cruncherWorker);
                            break;
                        }
                    }
                    break;
                }
            }
        } else if (notification instanceof FileInputStartedNotification) { //obavestavam fileinputviewpane
            notifySubscribers(notification);
        } else if (notification instanceof FileInputPausedNotification) { //obavestavam fileinputviewpane
            notifySubscribers(notification);
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
}
