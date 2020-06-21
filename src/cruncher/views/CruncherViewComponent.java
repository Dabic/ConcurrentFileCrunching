package cruncher.views;

import cruncher.models.CruncherPool;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import observers.IPublisher;
import observers.ISubscriber;
import observers.notifications.CruncherRemovedNotification;

import java.util.ArrayList;
import java.util.List;

public class CruncherViewComponent extends VBox implements IPublisher {
    private String cruncherName;
    private Label cruncherNameLbl;
    private String arity;
    private Label arityLbl;
    private Button removeCruncher;
    private List<ISubscriber> subscribers;
    private Label statusLbl;
    private CruncherLabelsViewComponent cruncherLabelsViewComponent;
    private CruncherPool cruncherPool;
    private Label something;

    public CruncherViewComponent(String cruncherName, String arity, CruncherPool pool) {
        this.cruncherName = cruncherName;
        this.arity = arity;
        this.cruncherPool = pool;
        initComponents();
        addComponents();
        setActions();
        addListeners();
    }

    public void initComponents() {
        cruncherNameLbl = new Label("Name: " + cruncherName);
        arityLbl = new Label("Arity: " + arity);
        removeCruncher = new Button("Remove Cruncher");

        statusLbl = new Label("Crunching: ");
        something = new Label();
        cruncherLabelsViewComponent = new CruncherLabelsViewComponent();
        setPadding(new Insets(3, 3, 3, 3));
        setBorder(new Border(new BorderStroke(Color.LIGHTGREY,
                BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT)));
    }

    public void addComponents() {
        getChildren().add(cruncherNameLbl);
        getChildren().add(arityLbl);
        getChildren().add(removeCruncher);
        getChildren().add(statusLbl);
        getChildren().add(cruncherLabelsViewComponent);
    }

    public void addStatusLabel(String fileName) {
        cruncherLabelsViewComponent.addLabel(fileName);
    }
    public void removeStatusLabel(String fileName) {
        cruncherLabelsViewComponent.removeLabel(fileName);
    }
    public void setActions() {
        removeCruncher.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent actionEvent) {
                notifySubscribers(new CruncherRemovedNotification(cruncherName)); //obavestavam cruncherviewpane
            }
        });
    }

    public void addListeners() {
        cruncherLabelsViewComponent.getChildren().addListener(new ListChangeListener<Node>() {
            @Override
            public void onChanged(Change<? extends Node> change) {
                if (cruncherLabelsViewComponent.getChildren().size() > 0) {
                    removeCruncher.setDisable(true);
                } else {
                    removeCruncher.setDisable(false);
                }
            }
        });
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

    public String getCruncherName() {
        return cruncherName;
    }

    public String getArity() {
        return arity;
    }

    public Label getSomething() {
        return something;
    }
}
