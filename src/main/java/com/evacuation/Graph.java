package com.evacuation;

import java.util.*;

public class Graph {
    public static final class Node {
        private final int id;
        private String name;
        private double x;
        private double y;
        private boolean safeZone;

        public Node(int id, String name, double x, double y) {
            this.id = id;
            this.name = name;
            this.x = x;
            this.y = y;
        }

        public int id() { return id; }
        public String name() { return name; }
        public void setName(String name) { this.name = name; }

        public double x() { return x; }
        public double y() { return y; }
        public void setX(double x) { this.x = x; }
        public void setY(double y) { this.y = y; }

        public boolean isSafeZone() { return safeZone; }
        public void setSafeZone(boolean safeZone) { this.safeZone = safeZone; }
    }

    public static final class Edge {
        private final int id;
        private final Node a;
        private final Node b;
        private double weight;
        private boolean enabled = true;

        public Edge(int id, Node a, Node b, double weight) {
            this.id = id;
            this.a = a;
            this.b = b;
            this.weight = weight;
        }

        public int id() { return id; }
        public Node a() { return a; }
        public Node b() { return b; }
        public double weight() { return weight; }
        public void setWeight(double weight) { this.weight = weight; }
        public boolean enabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        public Node other(Node n) {
            if (n == a) return b;
            if (n == b) return a;
            throw new IllegalArgumentException("Node not part of this edge");
        }
    }

    private int nextNodeId = 0;
    private int nextEdgeId = 0;
    private final List<Node> nodes = new ArrayList<>();
    private final List<Edge> edges = new ArrayList<>();
    private final Map<Node, List<Edge>> adj = new HashMap<>();

    public List<Node> nodes() { return Collections.unmodifiableList(nodes); }
    public List<Edge> edges() { return Collections.unmodifiableList(edges); }
    public List<Edge> edgesOf(Node n) { return adj.getOrDefault(n, List.of()); }

    public Node addNode(String name, double x, double y) {
        Node n = new Node(nextNodeId++, name, x, y);
        nodes.add(n);
        adj.put(n, new ArrayList<>());
        return n;
    }

    public Edge addUndirectedEdge(Node a, Node b, double weight) {
        if (a == null || b == null) throw new IllegalArgumentException("Null endpoint");
        if (a == b) throw new IllegalArgumentException("Self edge not allowed");
        Edge e = new Edge(nextEdgeId++, a, b, weight);
        edges.add(e);
        adj.get(a).add(e);
        adj.get(b).add(e);
        return e;
    }

    public void clear() {
        nodes.clear();
        edges.clear();
        adj.clear();
        nextNodeId = 0;
        nextEdgeId = 0;
    }

    public List<Node> safeZones() {
        List<Node> sz = new ArrayList<>();
        for (Node n : nodes) if (n.isSafeZone()) sz.add(n);
        return sz;
    }
}

