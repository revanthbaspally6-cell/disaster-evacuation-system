# Real-Time Disaster Evacuation Planning System with GUI Simulation

Java + JavaFX project demonstrating **Multi-Source Dijkstra** for evacuation planning.

## Requirements
- **JDK 17+**
- **Maven 3.9+**

## Run (recommended)
From the project folder:

```bash
mvn clean javafx:run
```

## Run from IDE (IntelliJ / Eclipse / VS Code)
Add these VM options (JavaFX runtime args):

```text
--add-modules javafx.controls,javafx.graphics
```

If you use Maven, you typically **do not** need to manually download JavaFX SDK.

## Sample usage
- Click **Load Sample Graph**
- Click nodes to toggle **Safe Zones** (nodes turn **GREEN**)
- Click **Run Simulation**
- Use **Pause / Step** to control the animation
- Click a node after completion to show the **final red path** and a small moving dot

