# 🚨 Disaster Evacuation System — Dynamic Graph Routing & Visualizer

A Java & JavaFX-based emergency evacuation routing and simulation platform. The system models urban transit networks as dynamic weighted graphs, simulating hazard occurrences (fires, structural blockages, floodings) and computing optimal escape paths using **Multi-Source Dijkstra** and **Floyd-Warshall** algorithms.

---

## 🚀 Key Capabilities & Algorithms

- **🗺️ Interactive JavaFX Network Graph**: Visual representation of buildings, road segments, intersection nodes, and designated safety exits.
- **⚡ Multi-Source Dijkstra Algorithm**: Simultaneously computes the shortest, safest route from any occupant location to the nearest available safe shelter.
- **🔄 Floyd-Warshall All-Pairs Shortest Path**: Pre-computes global matrix distances for real-time rerouting when multiple corridors are blocked.
- **🔥 Dynamic Hazard & Obstacle Injection**: Allows users to dynamically disable road links or increase hazard penalties during live simulation.
- **📊 Interactive Node & Edge State Management**: Real-time visual feedback with color-coded safety corridors, hazard zones, and calculated escape vectors.

---

## 🏗️ Architecture & Core Components

```
src/main/java/com/evacuation/
├── Main.java                 # JavaFX Application Entry Point
├── Controller.java           # Simulation Controller & UI Event Handler
├── Graph.java                # Weighted Graph Data Structure (Adjacency List)
├── DijkstraMultiSource.java  # Multi-Source Shortest Path Algorithm
├── FloydWarshall.java        # All-Pairs Shortest Path Matrix Computation
├── NodeView.java             # UI Component for Graph Vertices & Exit Zones
└── EdgeView.java             # UI Component for Dynamic Roadways & Hazard States
```

---

## 🛠️ Tech Stack & Prerequisites

- **Language**: Java 17+
- **GUI Framework**: JavaFX
- **Build System**: Apache Maven
- **Core Principles**: Graph Theory, Greedy Routing, Dynamic Programming

---

## 📦 Build & Run Instructions

### 1. Clone the repository
```bash
git clone https://github.com/revanthbaspally6-cell/disaster-evacuation-system.git
cd disaster-evacuation-system
```

### 2. Build & Execute with Maven
```bash
# Build the project
mvn clean compile

# Run the JavaFX Application
mvn javafx:run
```

---

## 👨‍💻 Author
- **Revanth Baspally** — [GitHub Profile](https://github.com/revanthbaspally6-cell)
