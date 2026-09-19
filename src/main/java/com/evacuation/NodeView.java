package com.evacuation;

import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public final class NodeView extends StackPane {
    public enum State { UNVISITED, PROCESSING, SETTLED, PATH }

    private final Graph.Node node;
    private final Circle circle;
    private final Label nameLabel;
    private final Label infoLabel;

    private State state = State.UNVISITED;
    private boolean selected = false;

    public NodeView(Graph.Node node) {
        this.node = node;

        circle = new Circle(22);
        circle.setStroke(Color.web("#1f2937"));
        circle.setStrokeWidth(2);

        nameLabel = new Label(node.name());
        nameLabel.setStyle("-fx-font-weight: 700; -fx-text-fill: #111827;");

        infoLabel = new Label("Dist: ∞\nSZ: -");
        infoLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #111827;");
        infoLabel.setAlignment(Pos.CENTER);

        setAlignment(Pos.CENTER);
        setPickOnBounds(false);
        setCursor(Cursor.HAND);
        getChildren().addAll(circle, nameLabel);

        // show info below node (simple)
        infoLabel.setTranslateY(34);
        getChildren().add(infoLabel);

        relocate(node.x() - 22, node.y() - 22);
        refreshFill();
    }

    public Graph.Node model() { return node; }

    public void setState(State s) {
        this.state = s;
        refreshFill();
    }

    public State getState() { return state; }

    public void setSelected(boolean selected) {
        this.selected = selected;
        circle.setStroke(selected ? Color.web("#2563eb") : Color.web("#1f2937"));
        circle.setStrokeWidth(selected ? 3 : 2);
    }

    public void setDistance(Double d) {
        String distText = (d == null || d.isInfinite()) ? "∞" : (Math.round(d * 10.0) / 10.0) + "";
        infoLabel.setText(infoLabel.getText().replaceAll("Dist:.*", "Dist: " + distText));
    }

    public void setNearestSafeZone(String name) {
        String sz = (name == null) ? "-" : name;
        String[] lines = infoLabel.getText().split("\n");
        if (lines.length == 2) {
            infoLabel.setText(lines[0] + "\nSZ: " + sz);
        } else {
            infoLabel.setText("Dist: ∞\nSZ: " + sz);
        }
    }

    public void syncPositionFromView() {
        node.setX(getLayoutX() + 22);
        node.setY(getLayoutY() + 22);
    }

    public void syncPositionToView() {
        relocate(node.x() - 22, node.y() - 22);
    }

    public void refreshFill() {
        if (node.isSafeZone()) {
            circle.setFill(Color.web("#22c55e")); // green
            return;
        }
        switch (state) {
            case UNVISITED -> circle.setFill(Color.web("#60a5fa")); // blue
            case PROCESSING -> circle.setFill(Color.web("#facc15")); // yellow
            case SETTLED -> circle.setFill(Color.web("#a7f3d0")); // light greenish
            case PATH -> circle.setFill(Color.web("#ef4444")); // red
        }
    }
}

