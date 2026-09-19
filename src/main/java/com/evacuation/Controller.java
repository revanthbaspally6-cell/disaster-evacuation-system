package com.evacuation;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

import java.util.*;

public final class Controller {
    private final BorderPane root = new BorderPane();

    private final Graph graph = new Graph();
    private final Pane graphPane = new Pane();

    private final Map<Graph.Node, NodeView> nodeViews = new HashMap<>();
    private final Map<Graph.Edge, EdgeView> edgeViews = new HashMap<>();

    private final ComboBox<Graph.Node> cbSrc = new ComboBox<>();
    private final ComboBox<Graph.Node> cbDst = new ComboBox<>();
    private final TextField tfWeight = new TextField("4");
    private final Button btnAddNode = new Button("Add Node");
    private final Button btnAddEdge = new Button("Add Edge");
    private final Button btnRun = new Button("Run Simulation (Multi-Source Dijkstra)");
    private final Button btnPause = new Button("Pause");
    private final Button btnStep = new Button("Step");
    private final Button btnReset = new Button("Reset Graph");
    private final Button btnSample = new Button("Load Sample Graph");
    private final Button btnCompare = new Button("Compare with Floyd-Warshall");

    private final TextField tfIncrease = new TextField("3");
    private final Button btnIncrease = new Button("Increase Edge Weight");
    private final Button btnToggleEdge = new Button("Disable/Enable Edge");

    private final TextArea outputArea = new TextArea();
    private final ListView<String> pqList = new ListView<>();
    private final Label statusLabel = new Label("Click nodes to toggle Safe Zones (GREEN).");

    private EdgeView selectedEdgeView;
    private NodeView selectedNodeView;

    private Timeline timeline;
    private List<DijkstraMultiSource.Step> steps = List.of();
    private int stepIndex = 0;
    private boolean simulationFinished = false;

    private Map<Graph.Node, Double> lastDist = Map.of();
    private Map<Graph.Node, Graph.Node> lastParent = Map.of();
    private Map<Graph.Node, Graph.Node> lastNearest = Map.of();

    private final Circle evacDot = new Circle(6, Color.web("#7c3aed"));
    private Timeline evacTimeline;

    private int nextNameIndex = 0;

    public Controller() {
        buildUI();
        wireEvents();
        setControlsInitialState();
    }

    public Parent getRoot() {
        return root;
    }

    private void buildUI() {
        graphPane.setStyle("-fx-background-color: linear-gradient(#f8fafc, #eef2ff);");
        graphPane.setPrefSize(800, 700);
        graphPane.getChildren().add(evacDot);
        evacDot.setVisible(false);

        root.setCenter(graphPane);

        VBox left = new VBox(10);
        left.setPadding(new Insets(12));
        left.setPrefWidth(360);
        left.setStyle("-fx-background-color: #0b1220;");

        Label title = new Label("Evacuation Planner");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 18px; -fx-font-weight: 800;");

        Label subtitle = new Label("Multi-Source Dijkstra (Safe Zones expand simultaneously)");
        subtitle.setStyle("-fx-text-fill: #cbd5e1;");

        Separator sep1 = new Separator();

        GridPane edgeForm = new GridPane();
        edgeForm.setHgap(8);
        edgeForm.setVgap(8);

        cbSrc.setPromptText("Source");
        cbDst.setPromptText("Destination");
        cbSrc.setMaxWidth(Double.MAX_VALUE);
        cbDst.setMaxWidth(Double.MAX_VALUE);
        cbSrc.setCellFactory(lv -> new NodeCell());
        cbDst.setCellFactory(lv -> new NodeCell());
        cbSrc.setButtonCell(new NodeCell());
        cbDst.setButtonCell(new NodeCell());

        tfWeight.setPromptText("Weight");

        edgeForm.add(new LabelStyled("Add Node / Edge"), 0, 0, 2, 1);
        edgeForm.add(btnAddNode, 0, 1, 2, 1);
        edgeForm.add(cbSrc, 0, 2);
        edgeForm.add(cbDst, 1, 2);
        edgeForm.add(tfWeight, 0, 3);
        edgeForm.add(btnAddEdge, 1, 3);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.ALWAYS);
        edgeForm.getColumnConstraints().addAll(c1, c2);

        VBox simBox = new VBox(8);
        simBox.getChildren().addAll(
                new LabelStyled("Simulation Controls"),
                btnRun,
                new HBox(8, btnPause, btnStep),
                btnReset,
                btnSample,
                btnCompare
        );

        VBox disasterBox = new VBox(8);
        HBox incRow = new HBox(8, tfIncrease, btnIncrease);
        tfIncrease.setPrefWidth(60);
        disasterBox.getChildren().addAll(
                new LabelStyled("Disaster Controls (select an edge)"),
                incRow,
                btnToggleEdge
        );

        VBox pqBox = new VBox(8);
        LabelStyled pqTitle = new LabelStyled("Priority Queue (live)");
        pqList.setPrefHeight(160);
        pqBox.getChildren().addAll(pqTitle, pqList);

        VBox outBox = new VBox(8);
        LabelStyled outTitle = new LabelStyled("Output");
        outputArea.setEditable(false);
        outputArea.setWrapText(false);
        outputArea.setPrefHeight(220);
        outBox.getChildren().addAll(outTitle, outputArea);

        statusLabel.setStyle("-fx-text-fill: #93c5fd;");

        left.getChildren().addAll(title, subtitle, statusLabel, sep1, edgeForm, simBox, disasterBox, pqBox, outBox);
        VBox.setVgrow(outBox, Priority.ALWAYS);
        root.setLeft(left);
    }

    private void wireEvents() {
        btnAddNode.setOnAction(e -> addNodeAuto());

        btnAddEdge.setOnAction(e -> {
            Graph.Node a = cbSrc.getValue();
            Graph.Node b = cbDst.getValue();
            Double w = parseDouble(tfWeight.getText());
            if (a == null || b == null || w == null || w <= 0) {
                showAlert("Invalid Edge", "Select source, destination and a positive weight.");
                return;
            }
            if (edgeAlreadyExists(a, b)) {
                showAlert("Edge Exists", "An edge already exists between those nodes.");
                return;
            }
            Graph.Edge edge = graph.addUndirectedEdge(a, b, w);
            addEdgeView(edge);
            refreshComboData();
        });

        btnReset.setOnAction(e -> resetGraph());
        btnSample.setOnAction(e -> loadSampleGraph());

        btnRun.setOnAction(e -> runSimulation());
        btnPause.setOnAction(e -> pauseOrResume());
        btnStep.setOnAction(e -> stepOnce());

        btnIncrease.setOnAction(e -> {
            if (selectedEdgeView == null) {
                showAlert("No Edge Selected", "Click an edge line to select it first.");
                return;
            }
            Double delta = parseDouble(tfIncrease.getText());
            if (delta == null || delta <= 0) {
                showAlert("Invalid Increase", "Enter a positive number.");
                return;
            }
            Graph.Edge edge = selectedEdgeView.model();
            edge.setWeight(edge.weight() + delta);
            selectedEdgeView.refreshWeight();
            output("Edge " + edge.a().name() + "-" + edge.b().name() + " weight increased to " + edge.weight());
        });

        btnToggleEdge.setOnAction(e -> {
            if (selectedEdgeView == null) {
                showAlert("No Edge Selected", "Click an edge line to select it first.");
                return;
            }
            Graph.Edge edge = selectedEdgeView.model();
            edge.setEnabled(!edge.enabled());
            selectedEdgeView.setState(edge.enabled() ? EdgeView.State.SELECTED : EdgeView.State.DISABLED);
            output("Edge " + edge.a().name() + "-" + edge.b().name() + " is now " + (edge.enabled() ? "ENABLED" : "DISABLED"));
        });

        btnCompare.setOnAction(e -> compareFloydWarshall());

        graphPane.setOnMouseClicked(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                clearSelections();
            }
        });
    }

    private void setControlsInitialState() {
        btnPause.setDisable(true);
        btnStep.setDisable(true);
        btnIncrease.setDisable(false);
        btnToggleEdge.setDisable(false);
        pqList.getItems().clear();
        outputArea.clear();
    }

    private void addNodeAuto() {
        String name = nextNodeName();
        double x = 120 + (Math.random() * 520);
        double y = 120 + (Math.random() * 460);
        Graph.Node n = graph.addNode(name, x, y);
        addNodeView(n);
        refreshComboData();
    }

    private void addNodeView(Graph.Node n) {
        NodeView nv = new NodeView(n);
        nodeViews.put(n, nv);

        final double[] dragDelta = new double[2];
        nv.setOnMousePressed(e -> {
            if (timelineRunning()) return;
            dragDelta[0] = e.getSceneX() - nv.getLayoutX();
            dragDelta[1] = e.getSceneY() - nv.getLayoutY();
        });
        nv.setOnMouseDragged(e -> {
            if (timelineRunning()) return;
            nv.relocate(e.getSceneX() - dragDelta[0], e.getSceneY() - dragDelta[1]);
            nv.syncPositionFromView();
            updateConnectedEdges(n);
        });
        nv.setOnMouseClicked(e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            if (timelineRunning()) return;

            // toggle safe zone
            n.setSafeZone(!n.isSafeZone());
            nv.refreshFill();
            output(n.name() + " safe zone: " + (n.isSafeZone() ? "YES" : "NO"));

            // select node (for showing path after run)
            if (selectedNodeView != null) selectedNodeView.setSelected(false);
            selectedNodeView = nv;
            selectedNodeView.setSelected(true);

            if (simulationFinished) {
                showFinalPathFrom(n);
            }
        });

        graphPane.getChildren().add(nv);
    }

    private void addEdgeView(Graph.Edge e) {
        EdgeView ev = new EdgeView(e);
        edgeViews.put(e, ev);

        NodeView a = nodeViews.get(e.a());
        NodeView b = nodeViews.get(e.b());
        ev.updateEndpoints(a, b);

        ev.setOnMouseClicked(me -> {
            if (me.getButton() != MouseButton.PRIMARY) return;
            if (timelineRunning()) return;
            selectEdge(ev);
            me.consume();
        });

        // draw edges behind nodes
        graphPane.getChildren().add(0, ev);
    }

    private void selectEdge(EdgeView ev) {
        if (selectedEdgeView != null) {
            selectedEdgeView.setState(selectedEdgeView.model().enabled() ? EdgeView.State.NORMAL : EdgeView.State.DISABLED);
        }
        selectedEdgeView = ev;
        selectedEdgeView.setState(selectedEdgeView.model().enabled() ? EdgeView.State.SELECTED : EdgeView.State.DISABLED);
        output("Selected edge: " + ev.model().a().name() + " - " + ev.model().b().name());
    }

    private void clearSelections() {
        if (selectedEdgeView != null) {
            selectedEdgeView.setState(selectedEdgeView.model().enabled() ? EdgeView.State.NORMAL : EdgeView.State.DISABLED);
            selectedEdgeView = null;
        }
        if (selectedNodeView != null) {
            selectedNodeView.setSelected(false);
            selectedNodeView = null;
        }
    }

    private void updateConnectedEdges(Graph.Node n) {
        for (Graph.Edge e : graph.edgesOf(n)) {
            EdgeView ev = edgeViews.get(e);
            if (ev == null) continue;
            ev.updateEndpoints(nodeViews.get(e.a()), nodeViews.get(e.b()));
        }
    }

    private void refreshComboData() {
        List<Graph.Node> list = new ArrayList<>(graph.nodes());
        cbSrc.getItems().setAll(list);
        cbDst.getItems().setAll(list);
    }

    private boolean edgeAlreadyExists(Graph.Node a, Graph.Node b) {
        for (Graph.Edge e : graph.edges()) {
            if ((e.a() == a && e.b() == b) || (e.a() == b && e.b() == a)) return true;
        }
        return false;
    }

    private void resetGraph() {
        stopTimelines();
        simulationFinished = false;
        stepIndex = 0;
        steps = List.of();
        lastDist = Map.of();
        lastParent = Map.of();
        lastNearest = Map.of();

        graph.clear();
        nodeViews.clear();
        edgeViews.clear();
        graphPane.getChildren().clear();
        graphPane.getChildren().add(evacDot);
        evacDot.setVisible(false);

        nextNameIndex = 0;
        refreshComboData();
        pqList.getItems().clear();
        outputArea.clear();
        statusLabel.setText("Click nodes to toggle Safe Zones (GREEN).");
    }

    private void loadSampleGraph() {
        resetGraph();

        // 8 nodes: A..H
        Graph.Node A = graph.addNode("A", 170, 140);
        Graph.Node B = graph.addNode("B", 360, 120);
        Graph.Node C = graph.addNode("C", 560, 170);
        Graph.Node D = graph.addNode("D", 250, 310);
        Graph.Node E = graph.addNode("E", 470, 320);
        Graph.Node F = graph.addNode("F", 670, 330);
        Graph.Node G = graph.addNode("G", 340, 520);
        Graph.Node H = graph.addNode("H", 610, 520);

        for (Graph.Node n : graph.nodes()) addNodeView(n);

        // edges (some high weights)
        addEdgeView(graph.addUndirectedEdge(A, B, 4));
        addEdgeView(graph.addUndirectedEdge(B, C, 6));
        addEdgeView(graph.addUndirectedEdge(A, D, 7));
        addEdgeView(graph.addUndirectedEdge(B, D, 3));
        addEdgeView(graph.addUndirectedEdge(B, E, 8));
        addEdgeView(graph.addUndirectedEdge(C, E, 2));
        addEdgeView(graph.addUndirectedEdge(E, F, 4));
        addEdgeView(graph.addUndirectedEdge(D, G, 5));
        addEdgeView(graph.addUndirectedEdge(E, G, 12)); // disaster-ish high weight
        addEdgeView(graph.addUndirectedEdge(F, H, 3));
        addEdgeView(graph.addUndirectedEdge(G, H, 6));

        // safe zones: C and G
        C.setSafeZone(true);
        G.setSafeZone(true);
        nodeViews.get(C).refreshFill();
        nodeViews.get(G).refreshFill();

        refreshComboData();
        statusLabel.setText("Sample loaded. Safe Zones: C, G (GREEN). Click nodes to toggle.");
        output("Loaded sample graph with 8 nodes and 2 safe zones.");
    }

    private void runSimulation() {
        stopTimelines();
        simulationFinished = false;
        evacDot.setVisible(false);

        if (graph.nodes().isEmpty()) {
            showAlert("No Graph", "Add nodes/edges or load a sample graph first.");
            return;
        }
        if (graph.safeZones().isEmpty()) {
            showAlert("No Safe Zones", "Click at least one node to mark it as a Safe Zone (GREEN).");
            return;
        }

        clearPathHighlight();
        resetNodeStatesForRun();
        pqList.getItems().clear();
        outputArea.clear();
        statusLabel.setText("Running multi-source Dijkstra... (orange = relax, yellow = processing)");

        btnRun.setDisable(true);
        btnPause.setDisable(true);
        btnStep.setDisable(true);

        Task<DijkstraMultiSource.Result> task = new Task<>() {
            @Override
            protected DijkstraMultiSource.Result call() {
                return new DijkstraMultiSource().computeWithSteps(graph);
            }
        };

        task.setOnSucceeded(e -> {
            DijkstraMultiSource.Result res = task.getValue();
            lastDist = res.dist;
            lastParent = res.parent;
            lastNearest = res.nearestSafeZone;
            steps = res.steps;
            stepIndex = 0;

            prepareTimeline();
            btnRun.setDisable(false);
            btnPause.setDisable(false);
            btnStep.setDisable(false);
            playTimeline();
        });

        task.setOnFailed(e -> {
            btnRun.setDisable(false);
            btnPause.setDisable(true);
            btnStep.setDisable(true);
            showAlert("Error", "Failed to run algorithm: " + (task.getException() == null ? "" : task.getException().getMessage()));
        });

        Thread t = new Thread(task, "dijkstra-worker");
        t.setDaemon(true);
        t.start();
    }

    private void prepareTimeline() {
        timeline = new Timeline(new KeyFrame(Duration.seconds(0.7), e -> applyNextStep()));
        timeline.setCycleCount(Timeline.INDEFINITE);
    }

    private void playTimeline() {
        if (timeline == null) return;
        timeline.play();
        btnPause.setText("Pause");
    }

    private void pauseOrResume() {
        if (timeline == null) return;
        if (timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.pause();
            btnPause.setText("Resume");
        } else {
            timeline.play();
            btnPause.setText("Pause");
        }
    }

    private void stepOnce() {
        if (timeline != null && timeline.getStatus() == Timeline.Status.RUNNING) {
            timeline.pause();
            btnPause.setText("Resume");
        }
        applyNextStep();
    }

    private void applyNextStep() {
        if (steps == null || stepIndex >= steps.size()) {
            finishSimulation();
            return;
        }

        DijkstraMultiSource.Step s = steps.get(stepIndex++);
        switch (s.type) {
            case INIT_SOURCE -> {
                NodeView nv = nodeViews.get(s.node);
                nv.setDistance(0.0);
                nv.setNearestSafeZone(s.node.name());
                nv.refreshFill();
                writePQ(s.pqSnapshot);
            }
            case POP_NODE -> {
                clearEdgeRelaxHighlight();
                resetNonSafeNodeStatesExceptPath();
                NodeView nv = nodeViews.get(s.node);
                nv.setState(NodeView.State.PROCESSING);
                nv.setDistance(lastDist.get(s.node));
                Graph.Node sz = lastNearest.get(s.node);
                nv.setNearestSafeZone(sz == null ? null : sz.name());
                writePQ(s.pqSnapshot);
            }
            case RELAX_EDGE_TRY -> {
                EdgeView ev = edgeViews.get(s.edge);
                if (ev != null) ev.setState(EdgeView.State.RELAXING);
                NodeView from = nodeViews.get(s.from);
                NodeView to = nodeViews.get(s.to);
                if (from != null) from.setState(NodeView.State.PROCESSING);
                if (to != null && !to.model().isSafeZone()) to.setState(NodeView.State.UNVISITED);
                writePQ(s.pqSnapshot);
            }
            case RELAX_EDGE_SUCCESS -> {
                NodeView to = nodeViews.get(s.to);
                if (to != null) {
                    to.setDistance(s.newDist);
                    Graph.Node sz = lastNearest.get(s.to);
                    to.setNearestSafeZone(sz == null ? null : sz.name());
                }
                writePQ(s.pqSnapshot);
            }
            case DONE -> {
                finishSimulation();
            }
        }
    }

    private void finishSimulation() {
        stopTimelineOnly();
        simulationFinished = true;
        clearEdgeRelaxHighlight();
        for (Graph.Node n : graph.nodes()) {
            NodeView nv = nodeViews.get(n);
            if (nv == null) continue;
            if (!n.isSafeZone()) nv.setState(NodeView.State.SETTLED);
            nv.setDistance(lastDist.getOrDefault(n, Double.POSITIVE_INFINITY));
            Graph.Node sz = lastNearest.get(n);
            nv.setNearestSafeZone(sz == null ? null : sz.name());
            nv.refreshFill();
        }
        statusLabel.setText("Done. Click any node to highlight its shortest path to the nearest Safe Zone.");
        outputFinalTable();
    }

    private void outputFinalTable() {
        StringBuilder sb = new StringBuilder();
        sb.append("FINAL RESULT (Nearest Safe Zone for each node)\n");
        sb.append("------------------------------------------------\n");
        for (Graph.Node n : graph.nodes()) {
            double d = lastDist.getOrDefault(n, Double.POSITIVE_INFINITY);
            Graph.Node sz = lastNearest.get(n);
            sb.append(n.name())
                    .append(" | dist=")
                    .append(d == Double.POSITIVE_INFINITY ? "∞" : (Math.round(d * 10.0) / 10.0))
                    .append(" | safeZone=")
                    .append(sz == null ? "-" : sz.name())
                    .append(" | path=");
            sb.append(pathString(n));
            sb.append("\n");
        }
        outputArea.setText(sb.toString());
    }

    private String pathString(Graph.Node start) {
        if (lastParent == null || lastParent.isEmpty()) return "-";
        List<Graph.Node> path = DijkstraMultiSource.reconstructPath(start, lastParent);
        StringJoiner sj = new StringJoiner(" -> ");
        for (Graph.Node n : path) sj.add(n.name());
        return sj.toString();
    }

    private void showFinalPathFrom(Graph.Node start) {
        clearPathHighlight();
        if (lastParent == null || lastParent.isEmpty()) return;
        if (lastDist.getOrDefault(start, Double.POSITIVE_INFINITY) == Double.POSITIVE_INFINITY) return;

        List<Graph.Node> path = DijkstraMultiSource.reconstructPath(start, lastParent);
        if (path.size() <= 1) return;

        // highlight nodes
        for (Graph.Node n : path) {
            NodeView nv = nodeViews.get(n);
            if (nv != null && !n.isSafeZone()) nv.setState(NodeView.State.PATH);
            if (nv != null) nv.refreshFill();
        }

        // highlight edges along path
        for (int i = 0; i < path.size() - 1; i++) {
            Graph.Node u = path.get(i);
            Graph.Node v = path.get(i + 1);
            Graph.Edge e = findEdge(u, v);
            if (e != null) {
                EdgeView ev = edgeViews.get(e);
                if (ev != null) ev.setState(EdgeView.State.PATH);
            }
        }

        animateEvacDot(path);
    }

    private void animateEvacDot(List<Graph.Node> path) {
        if (evacTimeline != null) evacTimeline.stop();
        evacDot.setVisible(true);
        evacDot.toFront();

        List<NodeView> pv = new ArrayList<>();
        for (Graph.Node n : path) pv.add(nodeViews.get(n));
        if (pv.contains(null)) return;

        evacDot.setCenterX(pv.get(0).getLayoutX() + 22);
        evacDot.setCenterY(pv.get(0).getLayoutY() + 22);

        evacTimeline = new Timeline();
        double t = 0;
        for (int i = 0; i < pv.size() - 1; i++) {
            NodeView a = pv.get(i);
            NodeView b = pv.get(i + 1);
            double ax = a.getLayoutX() + 22;
            double ay = a.getLayoutY() + 22;
            double bx = b.getLayoutX() + 22;
            double by = b.getLayoutY() + 22;

            // simple linear motion by setting center on keyframes
            evacTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> {
                evacDot.setCenterX(ax);
                evacDot.setCenterY(ay);
            }));
            t += 0.35;
            evacTimeline.getKeyFrames().add(new KeyFrame(Duration.seconds(t), e -> {
                evacDot.setCenterX(bx);
                evacDot.setCenterY(by);
            }));
        }
        evacTimeline.play();
    }

    private Graph.Edge findEdge(Graph.Node a, Graph.Node b) {
        for (Graph.Edge e : graph.edgesOf(a)) {
            if ((e.a() == a && e.b() == b) || (e.a() == b && e.b() == a)) return e;
        }
        return null;
    }

    private void resetNodeStatesForRun() {
        for (Graph.Node n : graph.nodes()) {
            NodeView nv = nodeViews.get(n);
            if (nv == null) continue;
            nv.setState(NodeView.State.UNVISITED);
            nv.setDistance(Double.POSITIVE_INFINITY);
            nv.setNearestSafeZone(null);
            nv.refreshFill();
        }
        for (Graph.Edge e : graph.edges()) {
            EdgeView ev = edgeViews.get(e);
            if (ev == null) continue;
            ev.setState(e.enabled() ? EdgeView.State.NORMAL : EdgeView.State.DISABLED);
        }
    }

    private void resetNonSafeNodeStatesExceptPath() {
        for (Graph.Node n : graph.nodes()) {
            NodeView nv = nodeViews.get(n);
            if (nv == null) continue;
            if (nv.getState() == NodeView.State.PATH) continue;
            if (!n.isSafeZone()) nv.setState(NodeView.State.UNVISITED);
            nv.refreshFill();
        }
    }

    private void clearEdgeRelaxHighlight() {
        for (Graph.Edge e : graph.edges()) {
            EdgeView ev = edgeViews.get(e);
            if (ev == null) continue;
            if (!e.enabled()) {
                ev.setState(EdgeView.State.DISABLED);
            } else if (ev.getState() == EdgeView.State.RELAXING) {
                ev.setState(EdgeView.State.NORMAL);
            }
        }
    }

    private void clearPathHighlight() {
        if (evacTimeline != null) evacTimeline.stop();
        evacDot.setVisible(false);

        for (Graph.Node n : graph.nodes()) {
            NodeView nv = nodeViews.get(n);
            if (nv != null && nv.getState() == NodeView.State.PATH) {
                nv.setState(NodeView.State.SETTLED);
                nv.refreshFill();
            }
        }
        for (Graph.Edge e : graph.edges()) {
            EdgeView ev = edgeViews.get(e);
            if (ev != null && ev.getState() == EdgeView.State.PATH) {
                ev.setState(e.enabled() ? EdgeView.State.NORMAL : EdgeView.State.DISABLED);
            }
        }
    }

    private void stopTimelineOnly() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        btnPause.setText("Pause");
    }

    private void stopTimelines() {
        stopTimelineOnly();
        if (evacTimeline != null) {
            evacTimeline.stop();
            evacTimeline = null;
        }
        evacDot.setVisible(false);
    }

    private boolean timelineRunning() {
        return timeline != null && timeline.getStatus() == Timeline.Status.RUNNING;
    }

    private void writePQ(Map<String, Double> pqSnapshot) {
        List<String> items = new ArrayList<>();
        for (Map.Entry<String, Double> e : pqSnapshot.entrySet()) {
            items.add(e.getKey() + " : " + (Math.round(e.getValue() * 10.0) / 10.0));
        }
        pqList.getItems().setAll(items);
    }

    private void compareFloydWarshall() {
        if (graph.nodes().isEmpty()) {
            showAlert("No Graph", "Add nodes/edges or load a sample graph first.");
            return;
        }
        long t0 = System.nanoTime();
        FloydWarshall.Result fw = new FloydWarshall().compute(graph);
        long t1 = System.nanoTime();
        double ms = (t1 - t0) / 1_000_000.0;

        // For comparison, pick nearest safe zone distance for each node using FW.
        Map<Graph.Node, Double> fwNearest = new LinkedHashMap<>();
        Map<Graph.Node, Graph.Node> fwSz = new LinkedHashMap<>();
        List<Graph.Node> safe = graph.safeZones();
        Map<Graph.Node, Integer> idx = new HashMap<>();
        for (int i = 0; i < fw.nodes.size(); i++) idx.put(fw.nodes.get(i), i);

        for (Graph.Node n : fw.nodes) {
            double best = Double.POSITIVE_INFINITY;
            Graph.Node bestSz = null;
            for (Graph.Node s : safe) {
                double d = fw.dist[idx.get(n)][idx.get(s)];
                if (d < best) {
                    best = d;
                    bestSz = s;
                }
            }
            fwNearest.put(n, best);
            fwSz.put(n, bestSz);
        }

        StringBuilder sb = new StringBuilder();
        sb.append(outputArea.getText()).append("\n\n");
        sb.append("FLOYD-WARSHALL (comparison only)\n");
        sb.append("--------------------------------\n");
        sb.append("Time: ").append(Math.round(ms * 100.0) / 100.0).append(" ms\n");
        for (Map.Entry<Graph.Node, Double> en : fwNearest.entrySet()) {
            Graph.Node n = en.getKey();
            double d = en.getValue();
            Graph.Node sz = fwSz.get(n);
            sb.append(n.name())
                    .append(" | dist=")
                    .append(d == Double.POSITIVE_INFINITY ? "∞" : (Math.round(d * 10.0) / 10.0))
                    .append(" | safeZone=")
                    .append(sz == null ? "-" : sz.name())
                    .append("\n");
        }
        outputArea.setText(sb.toString());
    }

    private void output(String line) {
        Platform.runLater(() -> {
            if (!outputArea.getText().isEmpty() && !outputArea.getText().endsWith("\n")) {
                outputArea.appendText("\n");
            }
            outputArea.appendText(line + "\n");
        });
    }

    private String nextNodeName() {
        int i = nextNameIndex++;
        if (i < 26) return String.valueOf((char) ('A' + i));
        return "N" + i;
    }

    private static Double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (Exception ex) {
            return null;
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(title);
        a.setContentText(msg);
        a.showAndWait();
    }

    private static final class LabelStyled extends Label {
        LabelStyled(String text) {
            super(text);
            setStyle("-fx-text-fill: #e5e7eb; -fx-font-weight: 800;");
        }
    }

    private static final class NodeCell extends ListCell<Graph.Node> {
        @Override
        protected void updateItem(Graph.Node item, boolean empty) {
            super.updateItem(item, empty);
            if (empty || item == null) setText(null);
            else setText(item.name());
        }
    }
}

