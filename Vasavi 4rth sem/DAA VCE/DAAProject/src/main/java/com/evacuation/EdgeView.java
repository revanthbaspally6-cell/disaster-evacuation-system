package com.evacuation;

import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;

public final class EdgeView extends Group {
    public enum State { NORMAL, RELAXING, PATH, DISABLED, SELECTED }

    private final Graph.Edge edge;
    private final Line line;
    private final Label weightLabel;
    private State state = State.NORMAL;

    public EdgeView(Graph.Edge edge) {
        this.edge = edge;
        this.line = new Line();
        this.line.setStrokeWidth(3);
        this.line.setStrokeLineCap(javafx.scene.shape.StrokeLineCap.ROUND);

        this.weightLabel = new Label(formatWeight(edge.weight()));
        this.weightLabel.setStyle("-fx-background-color: rgba(255,255,255,0.85); -fx-padding: 2 6 2 6; -fx-background-radius: 10;");
        this.weightLabel.setTextFill(Color.web("#111827"));

        setCursor(Cursor.HAND);
        getChildren().addAll(line, weightLabel);
        refreshStyle();
    }

    public Graph.Edge model() { return edge; }

    public void updateEndpoints(NodeView a, NodeView b) {
        double ax = a.getLayoutX() + 22;
        double ay = a.getLayoutY() + 22;
        double bx = b.getLayoutX() + 22;
        double by = b.getLayoutY() + 22;

        line.setStartX(ax);
        line.setStartY(ay);
        line.setEndX(bx);
        line.setEndY(by);

        double mx = (ax + bx) / 2.0;
        double my = (ay + by) / 2.0;
        weightLabel.relocate(mx - 16, my - 10);
    }

    public void setState(State s) {
        this.state = s;
        refreshStyle();
    }

    public State getState() { return state; }

    public void refreshWeight() {
        weightLabel.setText(formatWeight(edge.weight()));
    }

    public void refreshStyle() {
        if (!edge.enabled()) {
            line.setStroke(Color.web("#9ca3af")); // gray
            line.getStrokeDashArray().setAll(10.0, 8.0);
            return;
        }
        line.getStrokeDashArray().clear();

        switch (state) {
            case NORMAL -> line.setStroke(Color.web("#111827")); // near-black
            case RELAXING -> line.setStroke(Color.web("#fb923c")); // orange
            case PATH -> line.setStroke(Color.web("#ef4444")); // red
            case DISABLED -> line.setStroke(Color.web("#9ca3af"));
            case SELECTED -> line.setStroke(Color.web("#2563eb")); // blue
        }
    }

    private static String formatWeight(double w) {
        if (Math.abs(w - Math.rint(w)) < 1e-9) return "w=" + (int) w;
        return "w=" + (Math.round(w * 10.0) / 10.0);
    }
}

