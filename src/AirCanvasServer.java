import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class AirCanvasServer extends JPanel {
    private CanvasStrokes strokes = new CanvasStrokes();
    private UndoStack undoStack = new UndoStack();
    private RedoStack redoStack = new RedoStack();
    private EventQueue eventQueue = new EventQueue();
    private GestureBuffer gestureBuffer = new GestureBuffer();

    private int strokeCounter = 1;
    private StrokeNode currentStroke = null;
    private String previousGesture = "NONE";
    private int cursorX = 0, cursorY = 0;

    // NEW: Variable to track current color
    private Color currentColor = Color.BLACK;

    public AirCanvasServer() {
        setPreferredSize(new Dimension(1000, 1000));
        setBackground(Color.WHITE);
    }

    // NEW: Checks if the cursor hovers over the color palette
    private void checkColorSelection(int x, int y) {
        if (y >= 20 && y <= 70) {
            if (x >= 710 && x <= 760) currentColor = Color.BLACK;
            else if (x >= 770 && x <= 820) currentColor = Color.RED;
            else if (x >= 830 && x <= 880) currentColor = Color.GREEN;
            else if (x >= 890 && x <= 940) currentColor = Color.BLUE;
        }
    }

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

        gestureBuffer.addGesture(rawGesture);
        String confirmedGesture = gestureBuffer.getConfirmedGesture();

        // NEW: Check if we are selecting a color
        checkColorSelection(x, y);

        if (!confirmedGesture.equals("DRAW") && currentStroke != null) {
            undoStack.push(currentStroke);
            redoStack.clear();
            currentStroke = null;
        }

        if (confirmedGesture.equals("DRAW")) {
            if (currentStroke == null) {
                // NEW: Pass the current color into the new StrokeNode
                currentStroke = new StrokeNode("S" + strokeCounter++, currentColor);
                strokes.addStroke(currentStroke);
            }
            currentStroke.points.addPoint(x, y);
        }
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
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        // --- NEW: Draw Color Palette (Top Right) ---
        g2d.setColor(Color.BLACK);
        g2d.fillRect(710, 20, 50, 50);
        g2d.setColor(Color.RED);
        g2d.fillRect(770, 20, 50, 50);
        g2d.setColor(Color.GREEN);
        g2d.fillRect(830, 20, 50, 50);
        g2d.setColor(Color.BLUE);
        g2d.fillRect(890, 20, 50, 50);

        // Highlight the currently selected color with a border
        g2d.setColor(Color.GRAY);
        g2d.setStroke(new BasicStroke(3));
        if (currentColor == Color.BLACK) g2d.drawRect(708, 18, 54, 54);
        else if (currentColor == Color.RED) g2d.drawRect(768, 18, 54, 54);
        else if (currentColor == Color.GREEN) g2d.drawRect(828, 18, 54, 54);
        else if (currentColor == Color.BLUE) g2d.drawRect(888, 18, 54, 54);
        // ---------------------------------------------

        // Draw all strokes
        g2d.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        StrokeNode currStrokeNode = strokes.head;
        while (currStrokeNode != null) {

            g2d.setColor(currStrokeNode.color); // NEW: Use the stroke's saved color

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

        // Draw cursor as a dot matching the current color
        g2d.setColor(currentColor);
        g2d.fillOval(cursorX - 5, cursorY - 5, 10, 10);
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Air Canvas - Java Server");
        AirCanvasServer canvas = new AirCanvasServer();
        frame.add(canvas);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);

        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(5000)) {
                System.out.println("Java Server started on localhost port 5000.");
                Socket socket = serverSocket.accept();
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    canvas.eventQueue.enqueue(line);
                    SwingUtilities.invokeLater(() -> canvas.processEvent());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }
}