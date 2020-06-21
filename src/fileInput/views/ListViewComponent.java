package fileInput.views;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.layout.VBox;


public class ListViewComponent extends VBox {
    private Label listViewLabel;
    private ListView<String> listView;

    public ListViewComponent(String listViewName) {
        initComponents(listViewName);
        addComponents();
    }

    public void initComponents(String listViewName) {
        listViewLabel = new Label(listViewName);
        listView = new ListView<String>();
        listView.setPrefHeight(90);
    }

    public void addComponents() {
        getChildren().add(listViewLabel);
        getChildren().add(listView);
    }

    public ListView<String> getListView() {
        return listView;
    }

    public String getSelectedItemFromList() {
        return listView.getSelectionModel().getSelectedItem();
    }
    public void addToListView(String item) {
        listView.getItems().add(item);
    }
    public void deleteFromListView(String item) {
        listView.getItems().remove(item);
    }
}
