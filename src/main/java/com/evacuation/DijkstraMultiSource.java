package com.evacuation;

import java.util.*;

public final class DijkstraMultiSource {
    public enum StepType {
        INIT_SOURCE,
        POP_NODE,
        RELAX_EDGE_TRY,
        RELAX_EDGE_SUCCESS,
        DONE
    }

    public static final class Result {
        public final Map<Graph.Node, Double> dist;
        public final Map<Graph.Node, Graph.Node> parent;
        public final Map<Graph.Node, Graph.Node> nearestSafeZone;
        public final List<Step> steps;

        public Result(Map<Graph.Node, Double> dist,
                      Map<Graph.Node, Graph.Node> parent,
                      Map<Graph.Node, Graph.Node> nearestSafeZone,
                      List<Step> steps) {
            this.dist = dist;
            this.parent = parent;
            this.nearestSafeZone = nearestSafeZone;
            this.steps = steps;
        }
    }

    public static final class Step {
        public final StepType type;
        public final Graph.Node node;
        public final Graph.Edge edge;
        public final Graph.Node from;
        public final Graph.Node to;
        public final double oldDist;
        public final double newDist;
        public final Map<String, Double> pqSnapshot; // nodeName -> key

        private Step(StepType type,
                     Graph.Node node,
                     Graph.Edge edge,
                     Graph.Node from,
                     Graph.Node to,
                     double oldDist,
                     double newDist,
                     Map<String, Double> pqSnapshot) {
            this.type = type;
            this.node = node;
            this.edge = edge;
            this.from = from;
            this.to = to;
            this.oldDist = oldDist;
            this.newDist = newDist;
            this.pqSnapshot = pqSnapshot;
        }

        public static Step initSource(Graph.Node s, Map<String, Double> pqSnapshot) {
            return new Step(StepType.INIT_SOURCE, s, null, null, null, Double.POSITIVE_INFINITY, 0.0, pqSnapshot);
        }

        public static Step pop(Graph.Node u, double distU, Map<String, Double> pqSnapshot) {
            return new Step(StepType.POP_NODE, u, null, null, null, distU, distU, pqSnapshot);
        }

        public static Step relaxTry(Graph.Edge e, Graph.Node from, Graph.Node to, double oldDist, double cand, Map<String, Double> pqSnapshot) {
            return new Step(StepType.RELAX_EDGE_TRY, null, e, from, to, oldDist, cand, pqSnapshot);
        }

        public static Step relaxSuccess(Graph.Edge e, Graph.Node from, Graph.Node to, double oldDist, double newDist, Map<String, Double> pqSnapshot) {
            return new Step(StepType.RELAX_EDGE_SUCCESS, null, e, from, to, oldDist, newDist, pqSnapshot);
        }

        public static Step done(Map<String, Double> pqSnapshot) {
            return new Step(StepType.DONE, null, null, null, null, 0, 0, pqSnapshot);
        }
    }

    private static final class PQItem {
        final Graph.Node node;
        final double key;
        final Graph.Node source;

        PQItem(Graph.Node node, double key, Graph.Node source) {
            this.node = node;
            this.key = key;
            this.source = source;
        }
    }

    public Result computeWithSteps(Graph g) {
        Map<Graph.Node, Double> dist = new HashMap<>();
        Map<Graph.Node, Graph.Node> parent = new HashMap<>();
        Map<Graph.Node, Graph.Node> nearest = new HashMap<>();
        List<Step> steps = new ArrayList<>();

        for (Graph.Node n : g.nodes()) {
            dist.put(n, Double.POSITIVE_INFINITY);
            parent.put(n, null);
            nearest.put(n, null);
        }

        PriorityQueue<PQItem> pq = new PriorityQueue<>(Comparator.comparingDouble(it -> it.key));

        List<Graph.Node> sources = g.safeZones();
        for (Graph.Node s : sources) {
            dist.put(s, 0.0);
            nearest.put(s, s);
            pq.add(new PQItem(s, 0.0, s));
            steps.add(Step.initSource(s, snapshotPQ(pq)));
        }

        Set<Graph.Node> settled = new HashSet<>();

        while (!pq.isEmpty()) {
            PQItem cur = pq.poll();
            Graph.Node u = cur.node;
            if (cur.key != dist.get(u)) continue; // stale
            if (settled.contains(u)) continue;
            settled.add(u);

            steps.add(Step.pop(u, dist.get(u), snapshotPQ(pq)));

            for (Graph.Edge e : g.edgesOf(u)) {
                if (!e.enabled()) continue;
                Graph.Node v = e.other(u);
                if (settled.contains(v)) continue;

                double du = dist.get(u);
                double cand = du + e.weight();
                double old = dist.get(v);
                steps.add(Step.relaxTry(e, u, v, old, cand, snapshotPQ(pq)));

                if (cand < old) {
                    dist.put(v, cand);
                    parent.put(v, u);
                    Graph.Node src = nearest.get(u);
                    nearest.put(v, src);
                    pq.add(new PQItem(v, cand, src));
                    steps.add(Step.relaxSuccess(e, u, v, old, cand, snapshotPQ(pq)));
                }
            }
        }

        steps.add(Step.done(snapshotPQ(pq)));
        return new Result(dist, parent, nearest, steps);
    }

    private static Map<String, Double> snapshotPQ(PriorityQueue<PQItem> pq) {
        Map<String, Double> snap = new LinkedHashMap<>();
        List<PQItem> items = new ArrayList<>(pq);
        items.sort(Comparator.comparingDouble(i -> i.key));
        for (PQItem it : items) {
            snap.put(it.node.name(), it.key);
        }
        return snap;
    }

    public static List<Graph.Node> reconstructPath(Graph.Node start, Map<Graph.Node, Graph.Node> parent) {
        List<Graph.Node> path = new ArrayList<>();
        Graph.Node cur = start;
        while (cur != null) {
            path.add(cur);
            cur = parent.get(cur);
        }
        return path; // start -> ... -> safe zone (reverse direction if needed by caller)
    }
}

