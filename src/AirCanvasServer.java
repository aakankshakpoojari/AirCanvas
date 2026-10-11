import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class AirCanvasServer extends JPanel {
    // Instantiate all your custom data structures from DSA.java
    private CanvasStrokes strokes = new CanvasStrokes();
    private UndoStack undoStack = new UndoStack();
    private RedoStack redoStack = new RedoStack();
    private EventQueue eventQueue = new EventQueue();
    private GestureBuffer gestureBuffer = new GestureBuffer();

    // Variables to track drawing state
    private int strokeCounter = 1;
    private StrokeNode currentStroke = null;
    private String previousGesture = "NONE";
    private int cursorX = 0, cursorY = 0;

    public AirCanvasServer() {
        setPreferredSize(new Dimension(1000, 1000));
        setBackground(Color.WHITE);
    }

    // This method pulls from your custom EventQueue and processes the gesture
    public void processEvent() {
        String eventStr = eventQueue.dequeue();
        if (eventStr == null) return;

        String[] parts = eventStr.split(",");
        if (parts.length < 3) return;

        String rawGesture = parts[0];
        int x = Integer.parseInt(parts[1].trim());
        int y = Integer.parseInt(parts[2].trim());

        cursorX = x;
        cursorY = y;

        // Smooth out the webcam input using your custom circular buffer
        gestureBuffer.addGesture(rawGesture);
        String confirmedGesture = gestureBuffer.getConfirmedGesture();

        // Rule: Any gesture other than DRAW ends the current stroke[cite: 2]
        if (!confirmedGesture.equals("DRAW") && currentStroke != null) {
            undoStack.push(currentStroke);
            redoStack.clear(); // Rule: A new stroke clears the redo stack[cite: 2]
            currentStroke = null;
        }

        // Process the confirmed gesture
        if (confirmedGesture.equals("DRAW")) {
            if (currentStroke == null) {
                // Rule: Each stroke receives an ID in order, such as S1, S2[cite: 2]
                currentStroke = new StrokeNode("S" + strokeCounter++);
                strokes.addStroke(currentStroke);
            }
            currentStroke.points.addPoint(x, y);
        }
        // Rule: UNDO, REDO, CLEAR act only once for each gesture[cite: 2]
        else if (confirmedGesture.equals("UNDO") && !previousGesture.equals("UNDO")) {
            StrokeNode lastStroke = undoStack.pop();
            if (lastStroke != null) {
                strokes.removeStroke(lastStroke);
                redoStack.push(lastStroke);
            }
        }
        else if (confirmedGesture.equals("REDO") && !previousGesture.equals("REDO")) {
            StrokeNode redoStroke = redoStack.pop();
            if (redoStroke != null) {
                strokes.addStroke(redoStroke);
                undoStack.push(redoStroke);
            }
        }
        else if (confirmedGesture.equals("CLEAR") && !previousGesture.equals("CLEAR")) {
            strokes.clearAll();
            undoStack.clear();
            redoStack.clear();
        }

        previousGesture = confirmedGesture;
        repaint(); // Force the screen to redraw with the new data
    }

    // This method traverses your custom linked lists to draw on the screen
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // Draw all strokes in the doubly linked list
        g2d.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2d.setColor(Color.BLUE); // Drawing color

        StrokeNode currStrokeNode = strokes.head;
        while (currStrokeNode != null) {
            PointNode currPoint = currStrokeNode.points.head;
            if (currPoint != null) {
                int prevX = currPoint.x;
                int prevY = currPoint.y;
                currPoint = currPoint.next;
                while (currPoint != null) {
                    g2d.drawLine(prevX, prevY, currPoint.x, currPoint.y);
                    prevX = currPoint.x;
                    prevY = currPoint.y;
                    currPoint = currPoint.next;
                }
            }
            currStrokeNode = currStrokeNode.next;
        }

        // Draw a red dot for the live cursor tracking the index finger
        g2d.setColor(Color.RED);
        g2d.fillOval(cursorX - 5, cursorY - 5, 10, 10);
    }

    public static void main(String[] args) {
        // 1. Setup the GUI Window
        JFrame frame = new JFrame("Air Canvas - Java Server");
        AirCanvasServer canvas = new AirCanvasServer();
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        // 2. Start the Server socket on a background thread so the GUI doesn't freeze
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5000)) {
                System.out.println("Java Server started on localhost port 5000.");
                System.out.println("Waiting for Python client to connect...");

                Socket socket = serverSocket.accept();
                System.out.println("Python client connected successfully!");

                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;

                // Read messages from Python line-by-line[cite: 1]
                while ((line = in.readLine()) != null) {
                    canvas.eventQueue.enqueue(line);
                    // Safely update the GUI with the new event
                    SwingUtilities.invokeLater(() -> canvas.processEvent());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}