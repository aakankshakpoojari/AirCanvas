import cv2
import mediapipe as mp
import socket

# Connect to the Java server running on localhost port 5000[cite: 1]
HOST = '127.0.0.1'
PORT = 5000

try:
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    s.connect((HOST, PORT))
    print("Connected to Java Server successfully!")
except ConnectionRefusedError:
    print("Error: Make sure the Java server is running first!")
    exit()

# Set up MediaPipe for hand tracking[cite: 2]
mp_hands = mp.solutions.hands
hands = mp_hands.Hands(max_num_hands=1, min_detection_confidence=0.7)
mp_draw = mp.solutions.drawing_utils

# Open laptop webcam[cite: 1]
cap = cv2.VideoCapture(0)

def detect_gesture(landmarks):
    """
    Analyzes finger positions to determine the gesture.
    Returns: DRAW, MOVE, UNDO, REDO, CLEAR, or NONE[cite: 1].
    """
    # Tips and PIP joints (lower knuckles) for Index, Middle, Ring, Pinky
    tips = [8, 12, 16, 20]
    pips = [6, 10, 14, 18]

    fingers_up = []
    for tip, pip in zip(tips, pips):
        # If tip is higher than pip (lower y-value), finger is up
        if landmarks.landmark[tip].y < landmarks.landmark[pip].y:
            fingers_up.append(1)
        else:
            fingers_up.append(0)

    # Check thumb (using y-coordinates for a simple "Thumbs Up" check)
    thumb_up = landmarks.landmark[4].y < landmarks.landmark[3].y and landmarks.landmark[4].y < landmarks.landmark[5].y

    # Match the gestures to the rules in the PDF[cite: 1]
    if fingers_up == [1, 0, 0, 0] and not thumb_up:
        return "DRAW"  # Index finger up, others down
    elif fingers_up == [1, 1, 0, 0]:
        return "MOVE"  # Index and middle fingers up
    elif fingers_up == [1, 1, 1, 1] and not thumb_up:
        return "CLEAR" # Open palm with four fingers up
    elif fingers_up == [0, 0, 0, 0]:
        if thumb_up:
            return "REDO"  # Thumbs up, others down
        else:
            return "UNDO"  # Fist (all fingers down)

    return "NONE"
prev_x, prev_y = 0, 0
while cap.isOpened():
    ret, frame = cap.read()
    if not ret: break

    # Mirror the image so it feels like a mirror
    frame = cv2.flip(frame, 1)
    rgb_frame = cv2.cvtColor(frame, cv2.COLOR_BGR2RGB)
    results = hands.process(rgb_frame)

    gesture = "NONE"
    x, y = 0, 0

    if results.multi_hand_landmarks:
            for hand_landmarks in results.multi_hand_landmarks:
                # Draw skeleton on your hand for visual feedback
                mp_draw.draw_landmarks(frame, hand_landmarks, mp_hands.HAND_CONNECTIONS)

                # Get index fingertip position (landmark 8)
                index_tip = hand_landmarks.landmark[8]

                raw_x = int(index_tip.x * 1000)
                raw_y = int(index_tip.y * 1000)

                # --- SMOOTHING ALGORITHM TO FIX JAGGED LINES ---
                if prev_x == 0 and prev_y == 0:
                    x, y = raw_x, raw_y
                else:
                    # 0.6 is the smoothing factor. Lower = smoother but slightly lags behind your finger.
                    x = int(prev_x + 0.6 * (raw_x - prev_x))
                    y = int(prev_y + 0.6 * (raw_y - prev_y))

                prev_x, prev_y = x, y
                # -----------------------------------------------

                gesture = detect_gesture(hand_landmarks)
                break # Only process one hand
    else:
            # If hand leaves the screen, reset the smoother so the line doesn't teleport
            prev_x, prev_y = 0, 0

    # Send text line to Java: GESTURE,x,y followed by newline[cite: 1]
    message = f"{gesture},{x},{y}\n"
    try:
        s.sendall(message.encode('utf-8'))
    except Exception as e:
        print("Connection lost to Java server.")
        break

    # Display the gesture on the Python webcam window
    cv2.putText(frame, f"Gesture: {gesture}", (10, 50), cv2.FONT_HERSHEY_SIMPLEX, 1, (255, 0, 0), 2)
    cv2.imshow("Air Canvas - Webcam", frame)

    # Press 'q' to quit
    if cv2.waitKey(1) & 0xFF == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()
s.close()