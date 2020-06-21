package cruncher.views;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;

public class CruncherLabelsViewComponent extends VBox {
    private List<Label> labels;

    public CruncherLabelsViewComponent() {
        labels = new ArrayList<>();
    }

    public void addLabel(String fileName) {
        Label labelToAdd = new Label(fileName);
        labels.add(labelToAdd);
        getChildren().add(labelToAdd);
    }

    public void removeLabel(String fileName) {
        Label labelToRemove = null;
        for (Label label : labels) {
            if (label.getText().equals(fileName)) {
                labelToRemove = label;
                break;
            }
        }
        getChildren().remove(labelToRemove);
        labels.remove(labelToRemove);
    }

    public List<Label> getLabels() {
        return labels;
    }
}
