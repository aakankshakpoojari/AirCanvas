# 🎨 Air Canvas: Touchless Drawing App

A real-time, touchless drawing application that allows users to paint on a digital canvas using hand gestures captured by a webcam. Built as part of a DSA Mini Hackathon, this project implements core Data Structures completely from scratch without using built-in Java collection libraries.

## Features

- **Touchless Drawing:** Use your webcam and index finger to draw on the screen.
- **Custom Color Palette:** Hover over the top-right color boxes to switch between Black, Red, Green, and Blue.
- **Gesture Control:** Undo, redo, move, and clear the canvas using natural hand shapes.
- **Custom Data Structures:** 100% custom-built linked lists, circular arrays, and stacks for memory management.
- **Client-Server Architecture:** Python (Computer Vision) communicates with Java (GUI & Memory) over a local socket connection.

## Data Structures Implemented

To meet the strict requirements of the hackathon, the following structures were built from scratch in `DSA.java`:
1. **Doubly Linked List:** Manages the active strokes on the canvas (`CanvasStrokes`).
2. **Singly Linked List:** Stores the continuous X/Y coordinate points of a single stroke (`PointsList`).
3. **Circular Array (Bounded Stack):** Manages the Undo history, limiting it to the last 3 strokes (`UndoStack`).
4. **Linked Stack:** Manages the Redo history (`RedoStack`).
5. **Circular Array (Queue):** Handles incoming network events smoothly (`EventQueue`).
6. **Circular Buffer:** Stabilizes webcam gesture inputs to prevent jitter (`GestureBuffer`).

## 🖐️ Gesture Guide

| Gesture | Hand Shape | Action |
| :--- | :--- | :--- |
| **DRAW** | ☝️ Index finger up (others down) | Draws a continuous stroke on the canvas. |
| **MOVE** | ✌️ Index & Middle fingers up | Moves the cursor without drawing. |
| **UNDO** | 👎 Thumbs down | Removes the last drawn stroke. |
| **REDO** | 👍 Thumbs up | Restores the most recently undone stroke. |
| **CLEAR**| 🖐️ Open palm (all fingers up) | Wipes the entire canvas clean. |

## Setup & Installation

### Prerequisites
- **Java 17+** (or any modern JDK)
- **Python 3.10 or 3.11**

### Step 1: Start the Java Server
The Java server must be running first to listen for the webcam connection.
1. Open the project in IntelliJ IDEA (or your preferred IDE).
2. Navigate to `src/AirCanvasServer.java`.
3. Compile and run `AirCanvasServer.java`.
4. A white canvas window will open, and the terminal will say: `Waiting for Python client to connect...`

### Step 2: Start the Python Webcam Client
Open a new terminal in the root directory of the project and run the following commands:

**1. Navigate to the Python client folder:**
```bash
cd python_client
```

**2. Create and activate a virtual environment:**

```
# Windows
python -m venv venv
.\venv\Scripts\activate

# Mac/Linux
python3 -m venv venv
source venv/bin/activate
```
**3. Install the required computer vision libraries:**
(Note: Specific versions are used to ensure stable MediaPipe execution)

````
pip install opencv-python mediapipe==0.10.5 protobuf==3.20.3
````
**4. Run the client:**

````
python client.py
````
Your webcam will turn on, and you can start drawing!