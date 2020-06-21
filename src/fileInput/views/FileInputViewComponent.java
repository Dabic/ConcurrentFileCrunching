package fileInput.views;

import cruncher.models.CruncherWorker;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import observers.IPublisher;
import observers.ISubscriber;
import observers.models.CruncherLinkingModel;
import observers.models.DirectoryNotificationModel;
import observers.notifications.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;


public class FileInputViewComponent extends VBox implements IPublisher {
    private String fileInputName;
    private Label fileInputNameLbl;
    private ListViewComponent cruncherListViewComponent;
    private HBox cruncherActionsContainer;
    private ComboBox<String> cruncherComboBox;
    private Button linkCruncherBtn;
    private Button unlinkCruncherBtn;
    private ListViewComponent dirsListViewComponent;
    private VBox mainButtonContainer;
    private HBox dirBtnActionsContainer;
    private Button addDirBtn;
    private Button removeDirBtn;
    private Button startBtn;
    private Button removeFileInputBtn;
    private List<ISubscriber> subscribers;
    private Label statusLbl;
    private Label crunchingMessage;

    public FileInputViewComponent(String fileInputName, List<CruncherWorker> cruncherWorkerList) {
        this.fileInputName = fileInputName;
        initComponents(fileInputName, cruncherWorkerList);
        addComponents();
        setActions();
        setListeners();
    }

    public void initComponents(String fileInputName, List<CruncherWorker> cruncherWorkerList) {
        fileInputNameLbl = new Label(fileInputName);
        crunchingMessage = new Label("");
        cruncherListViewComponent = new ListViewComponent("Crunchers:");
        cruncherActionsContainer = new HBox();
        cruncherActionsContainer.setSpacing(33);
        linkCruncherBtn = new Button("Link cruncher");
        cruncherComboBox = new ComboBox<>();
        for (CruncherWorker cruncherWorker : cruncherWorkerList) {
            cruncherComboBox.getItems().add(cruncherWorker.getCruncherName());
        }
        if (cruncherWorkerList.size() > 0) {
            cruncherComboBox.getSelectionModel().select(0);
            linkCruncherBtn.setDisable(false);
        }
        else
            linkCruncherBtn.setDisable(true);
        unlinkCruncherBtn = new Button("Unlink cruncher");
        unlinkCruncherBtn.setDisable(true);
        dirsListViewComponent = new ListViewComponent("Dirs:");
        mainButtonContainer = new VBox();
        dirBtnActionsContainer = new HBox();
        dirBtnActionsContainer.setSpacing(5);
        addDirBtn = new Button("Add dir");
        removeDirBtn = new Button("Remove dir");
        startBtn = new Button("Start");
        removeFileInputBtn = new Button("Remove disk input");
        statusLbl = new Label("Idle");
    }

    public void addComponents() {
        getChildren().add(fileInputNameLbl);
        getChildren().add(cruncherListViewComponent);

        cruncherActionsContainer.getChildren().add(cruncherComboBox);
        cruncherActionsContainer.getChildren().add(linkCruncherBtn);
        cruncherActionsContainer.getChildren().add(unlinkCruncherBtn);

        getChildren().add(cruncherActionsContainer);
        getChildren().add(dirsListViewComponent);

        dirBtnActionsContainer.getChildren().add(addDirBtn);
        dirBtnActionsContainer.getChildren().add(removeDirBtn);
        removeDirBtn.setDisable(true);

        dirBtnActionsContainer.getChildren().add(startBtn);
        dirBtnActionsContainer.getChildren().add(removeFileInputBtn);

        mainButtonContainer.getChildren().add(dirBtnActionsContainer);

        getChildren().add(mainButtonContainer);

        getChildren().add(statusLbl);

        setSpacing(10);
        setPadding(new Insets(3, 3, 3, 3));
        setMargin(this, new Insets(10, 0, 0, 0));
        setBorder(new Border(new BorderStroke(Color.LIGHTGREY,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    public void setActions() {
        addDirBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                DirectoryChooser directoryChooser = new DirectoryChooser();
                directoryChooser.setInitialDirectory(new File("C:\\Users\\vlada\\Desktop\\" + fileInputName));
                File selectedDir = directoryChooser.showDialog(getScene().getWindow());
                if (selectedDir != null)
                    addDirectory(selectedDir.getAbsolutePath());
            }
        });

        removeDirBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                removeDirectory();
                if (dirsListViewComponent.getSelectedItemFromList() == null || dirsListViewComponent.getSelectedItemFromList().equals("")) {
                    removeDirBtn.setDisable(true);
                }
            }
        });

        removeFileInputBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                removeFileInput();
            }
        });

        linkCruncherBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String selectedCruncher = cruncherComboBox.getSelectionModel().getSelectedItem();
                cruncherListViewComponent.addToListView(selectedCruncher);
                linkCruncherBtn.setDisable(true);
                //obavestavam fileinputviewcontainer
                notifySubscribers(new CruncherLinkedNotification(new CruncherLinkingModel(fileInputName, selectedCruncher)));
            }
        });
        unlinkCruncherBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                String selectedCruncher = cruncherListViewComponent.getSelectedItemFromList();
                cruncherListViewComponent.deleteFromListView(selectedCruncher);
                if (cruncherListViewComponent.getSelectedItemFromList() == null)
                    unlinkCruncherBtn.setDisable(true);
                //obavestavam fileinputviewcontainer
                notifySubscribers(new CruncherUnlinkedNotification(new CruncherLinkingModel(fileInputName, selectedCruncher)));
                boolean disable = false;
                for (String cruncherInList : cruncherListViewComponent.getListView().getItems()) {
                    if (cruncherInList.equals(cruncherComboBox.getSelectionModel().getSelectedItem())) {
                        disable = true;
                        break;
                    }
                }
                linkCruncherBtn.setDisable(disable);
            }
        });

        startBtn.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                if (startBtn.getText().equals("Start")) {
                    startBtn.setText("Pause");
                    notifySubscribers(new FileInputStartedNotification(fileInputName)); //obavestavam fileinputviewContainera
                } else {
                    startBtn.setText("Start");
                    notifySubscribers(new FileInputPausedNotification(fileInputName)); //obavestavam fileinputviewContainera
                }

            }
        });
    }

    public void setListeners() {
        statusLbl.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observableValue, String s, String t1) {
                if (statusLbl.getText().equals("Idle") || statusLbl.getText().isEmpty()) {
                    removeFileInputBtn.setDisable(false);
                } else {
                    removeFileInputBtn.setDisable(true);
                }
            }
        });
        dirsListViewComponent.getListView().getSelectionModel().getSelectedItems().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (dirsListViewComponent.getSelectedItemFromList() == null || dirsListViewComponent.getSelectedItemFromList().equals("")) {
                    removeDirBtn.setDisable(true);
                } else {
                    removeDirBtn.setDisable(false);
                }
            }
        });
        cruncherComboBox.getSelectionModel().selectedItemProperty().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (cruncherComboBox.getSelectionModel().getSelectedItem() == null || cruncherComboBox.getSelectionModel().getSelectedItem().equals("")) {
                    linkCruncherBtn.setDisable(true);
                } else {
                    String selectedCruncher = cruncherComboBox.getSelectionModel().getSelectedItem();
                    boolean added = false;
                    for (String listCruncher : cruncherListViewComponent.getListView().getItems()) {
                        if (listCruncher.equals(selectedCruncher))
                            added = true;
                    }
                    linkCruncherBtn.setDisable(added);

                }
            }
        });
        cruncherListViewComponent.getListView().getSelectionModel().getSelectedItems().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                if (cruncherListViewComponent.getSelectedItemFromList() == null || cruncherListViewComponent.getSelectedItemFromList().equals("")) {
                    unlinkCruncherBtn.setDisable(true);
                } else {
                    unlinkCruncherBtn.setDisable(false);
                }
            }
        });
    }

    public void addDirectory(String dirPath) {
        dirsListViewComponent.addToListView(dirPath);
        notifySubscribers(new DirectoryAddedNotification(new DirectoryNotificationModel(fileInputName, dirPath)));
    }

    public void removeDirectory() {
        String dirPath = dirsListViewComponent.getSelectedItemFromList();
        dirsListViewComponent.deleteFromListView(dirPath);
        notifySubscribers(new DirectoryRemovedNotification(new DirectoryNotificationModel(fileInputName, dirPath)));
    }

    public void removeFileInput() {
        notifySubscribers(new FileInputRemovedNotification(fileInputName)); //obavestavam fileinputviewcontainer
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

    public String getFileInputName() {
        return fileInputName;
    }

    public ComboBox<String> getCruncherComboBox() {
        return cruncherComboBox;
    }

    public Label getStatusLbl() {
        return statusLbl;
    }

    public Label getCrunchingMessage() {
        return crunchingMessage;
    }
}
