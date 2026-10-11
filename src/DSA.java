// DSA.java

// Represents a single X,Y coordinate on the screen
class PointNode {
    int x, y;
    PointNode next;

    public PointNode(int x, int y) {
        this.x = x;
        this.y = y;
        this.next = null;
    }
}

// 4. Singly linked list with a tail pointer
class PointsList {
    PointNode head, tail;

    public void addPoint(int x, int y) {
        PointNode newNode = new PointNode(x, y);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
    }
}

// Represents a continuous line drawn on the screen
class StrokeNode {
    String strokeId;
    PointsList points;
    StrokeNode prev, next;

    public StrokeNode(String strokeId) {
        this.strokeId = strokeId;
        this.points = new PointsList();
        this.prev = null;
        this.next = null;
    }
}

// 3. Doubly linked list
class CanvasStrokes {
    StrokeNode head, tail;

    public void addStroke(StrokeNode newStroke) {
        if (head == null) {
            head = newStroke;
            tail = newStroke;
        } else {
            tail.next = newStroke;
            newStroke.prev = tail;
            tail = newStroke;
        }
    }

    public void removeStroke(StrokeNode stroke) {
        if (stroke == null) return;

        if (stroke.prev != null) stroke.prev.next = stroke.next;
        else head = stroke.next;

        if (stroke.next != null) stroke.next.prev = stroke.prev;
        else tail = stroke.prev;
    }

    public void clearAll() {
        head = null;
        tail = null;
    }
}

// 5. Undo history: Bounded stack implemented with a circular array (remembers latest 3)[cite: 1, 2]
class UndoStack {
    StrokeNode[] stack = new StrokeNode[3];
    int top = -1;
    int count = 0;

    public void push(StrokeNode stroke) {
        top = (top + 1) % 3; // Circular wrap around
        stack[top] = stroke;
        if (count < 3) count++;
    }

    public StrokeNode pop() {
        if (count == 0) return null;
        StrokeNode stroke = stack[top];
        top = (top - 1 + 3) % 3; // Wrap around backwards
        count--;
        return stroke;
    }

    // 7. Clear operation[cite: 1]
    public void clear() {
        stack = new StrokeNode[3];
        top = -1;
        count = 0;
    }
}

// Node for the Redo Stack
class RedoNode {
    StrokeNode stroke;
    RedoNode next;
    public RedoNode(StrokeNode stroke) { this.stroke = stroke; }
}

// 6. Redo history: Stack implemented with linked nodes[cite: 1]
class RedoStack {
    RedoNode top;

    public void push(StrokeNode stroke) {
        RedoNode newNode = new RedoNode(stroke);
        newNode.next = top;
        top = newNode;
    }

    public StrokeNode pop() {
        if (top == null) return null;
        StrokeNode stroke = top.stroke;
        top = top.next;
        return stroke;
    }

    // 7. Clear operation[cite: 1]
    public void clear() {
        top = null;
    }
}

// 2. Event handling: Queue implemented with a circular array[cite: 1]
class EventQueue {
    String[] queue = new String[100];
    int front = 0, rear = 0, size = 0;

    public void enqueue(String event) {
        if (size == queue.length) return; // Queue full
        queue[rear] = event;
        rear = (rear + 1) % queue.length;
        size++;
    }

    public String dequeue() {
        if (size == 0) return null;
        String event = queue[front];
        front = (front + 1) % queue.length;
        size--;
        return event;
    }
}

// 1. Gesture confirmation: Array used as a circular buffer[cite: 1]
// This smooths out the webcam input so it doesn't flicker between gestures
class GestureBuffer {
    String[] buffer = new String[5];
    int index = 0;

    public void addGesture(String gesture) {
        buffer[index] = gesture;
        index = (index + 1) % buffer.length;
    }

    public String getConfirmedGesture() {
        int maxCount = 0;
        String confirmed = "NONE";
        // Find most frequent gesture in our buffer
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] == null) continue;
            int count = 0;
            for (int j = 0; j < buffer.length; j++) {
                if (buffer[i].equals(buffer[j])) count++;
            }
            if (count > maxCount) {
                maxCount = count;
                confirmed = buffer[i];
            }
        }
        return confirmed;
    }
}