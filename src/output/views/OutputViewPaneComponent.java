package output.views;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import observers.ISubscriber;
import observers.notifications.CloseApplicationNotification;
import observers.notifications.CruncherStartedForOutputNotification;
import output.models.OutputPool;

public class OutputViewPaneComponent extends HBox implements ISubscriber {
    private OutputListComponent outputListComponent;
    private OutputPlotComponent outputPlotComponent;

    public OutputViewPaneComponent(OutputPool outputPool) {
        initComponents(outputPool);
        addComponents();
    }

    public void initComponents(OutputPool outputPool) {
        outputListComponent = new OutputListComponent(outputPool);
        outputPlotComponent = new OutputPlotComponent();
        outputListComponent.addSubscriber(outputPlotComponent);
        outputPool.addSubscriber(outputPlotComponent);
        outputPool.addSubscriber(outputListComponent);
    }

    public void addComponents() {
        getChildren().add(outputPlotComponent);
        getChildren().add(outputListComponent);
    }

    @Override
    public void update(Object notification) {
        if (notification instanceof CruncherStartedForOutputNotification) {
            outputListComponent.addToList(((CruncherStartedForOutputNotification) notification).getOwner().toString());
        } else if (notification instanceof CloseApplicationNotification) {

        }
    }
}
