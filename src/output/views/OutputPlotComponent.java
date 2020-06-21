package output.views;

import javafx.geometry.Rectangle2D;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Screen;
import observers.ISubscriber;
import observers.notifications.OutputSingleResultNotification;
import observers.notifications.OutputSumResultsNotification;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;

public class OutputPlotComponent extends HBox implements ISubscriber {
    private LineChart<Number, Number> scatterChart;
    private NumberAxis xAxis = new NumberAxis(0, 100, 5);
    private NumberAxis yAxis = new NumberAxis(0, 10, 1);

    public OutputPlotComponent() {
        scatterChart = new LineChart<>(xAxis, yAxis);
        getChildren().add(scatterChart);
        Rectangle2D primaryScreenBounds = Screen.getPrimary().getVisualBounds();
        HBox.setHgrow(this, Priority.ALWAYS);
        HBox.setHgrow(scatterChart, Priority.ALWAYS);
        scatterChart.setMinHeight(primaryScreenBounds.getHeight()-50);
    }


    @Override
    public void update(Object notification) {
        if (notification instanceof OutputSingleResultNotification) {
            scatterChart.getData().clear();
            XYChart.Series series1 = new XYChart.Series();
            int i = 0;
            int max = 0;

            for (Map.Entry<String, Integer> entry : ((SortedSet<Map.Entry<String, Integer>>)((OutputSingleResultNotification) notification).getOwner())) {
                if (max < entry.getValue())
                    max = entry.getValue();
                series1.getData().add(new XYChart.Data(i, entry.getValue()));
                i++;
                if (i == 100) {
                    yAxis.setUpperBound(max);
                    yAxis.setTickUnit(max / 20);
                    scatterChart.getData().add(series1);
                    break;
                }
            }
        }
    }
}
