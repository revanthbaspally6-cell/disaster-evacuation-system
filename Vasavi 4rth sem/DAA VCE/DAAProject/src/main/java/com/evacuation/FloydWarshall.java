package com.evacuation;

import java.util.*;

public final class FloydWarshall {
    public static final class Result {
        public final double[][] dist;
        public final int[][] next;
        public final List<Graph.Node> nodes;

        public Result(double[][] dist, int[][] next, List<Graph.Node> nodes) {
            this.dist = dist;
            this.next = next;
            this.nodes = nodes;
        }
    }

    public Result compute(Graph g) {
        List<Graph.Node> nodes = new ArrayList<>(g.nodes());
        int n = nodes.size();
        Map<Graph.Node, Integer> idx = new HashMap<>();
        for (int i = 0; i < n; i++) idx.put(nodes.get(i), i);

        double[][] dist = new double[n][n];
        int[][] next = new int[n][n];

        for (int i = 0; i < n; i++) {
            Arrays.fill(dist[i], Double.POSITIVE_INFINITY);
            Arrays.fill(next[i], -1);
            dist[i][i] = 0;
            next[i][i] = i;
        }

        for (Graph.Edge e : g.edges()) {
            if (!e.enabled()) continue;
            int a = idx.get(e.a());
            int b = idx.get(e.b());
            double w = e.weight();
            if (w < dist[a][b]) {
                dist[a][b] = w;
                dist[b][a] = w;
                next[a][b] = b;
                next[b][a] = a;
            }
        }

        for (int k = 0; k < n; k++) {
            for (int i = 0; i < n; i++) {
                if (dist[i][k] == Double.POSITIVE_INFINITY) continue;
                for (int j = 0; j < n; j++) {
                    double cand = dist[i][k] + dist[k][j];
                    if (cand < dist[i][j]) {
                        dist[i][j] = cand;
                        next[i][j] = next[i][k];
                    }
                }
            }
        }

        return new Result(dist, next, nodes);
    }

    public static List<Integer> reconstructPath(int u, int v, int[][] next) {
        if (next[u][v] == -1) return List.of();
        List<Integer> path = new ArrayList<>();
        int cur = u;
        path.add(cur);
        while (cur != v) {
            cur = next[cur][v];
            if (cur == -1) return List.of();
            path.add(cur);
        }
        return path;
    }
}

