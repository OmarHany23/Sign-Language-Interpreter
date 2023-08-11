from keras.models import load_model
import mediapipe as mp
import cv2
import numpy as np

holistic = mp.solutions.holistic
hands = mp.solutions.hands
holis = holistic.Holistic()
drawing = mp.solutions.drawing_utils



def frames_extraction(video_path):
    model=load_model("./video_words/words3.h5")
    SEQUENCE_LENGTH = 20
    X = []
    CLASSES_LIST=['book', 'fine', 'walk']
    # Read the Video File using the VideoCapture object.
    video_reader = cv2.VideoCapture(video_path)
    
    # Get the total number of frames in the video.
    video_frames_count = int(video_reader.get(cv2.CAP_PROP_FRAME_COUNT))

    # Calculate the the interval after which frames will be added to the list.
    skip_frames_window = max(int(video_frames_count/SEQUENCE_LENGTH), 1)
    # Iterate through the Video Frames.
    for frame_counter in range(SEQUENCE_LENGTH):

        # Set the current frame position of the video.
        video_reader.set(cv2.CAP_PROP_POS_FRAMES, frame_counter * skip_frames_window)

        # Reading the frame from the video. 
        success, frame = video_reader.read() 

        # Check if Video frame is not successfully read then break the loop
        if not success:
            break
        lst = []

        res = holis.process(cv2.cvtColor(frame, cv2.COLOR_BGR2RGB))

        if res.left_hand_landmarks:
            for i in res.left_hand_landmarks.landmark:
                lst.append(i.x - res.left_hand_landmarks.landmark[8].x)
                lst.append(i.y - res.left_hand_landmarks.landmark[8].y)
        else:
            for i in range(42):
                lst.append(0.0)

        if res.right_hand_landmarks:
            for i in res.right_hand_landmarks.landmark:
                lst.append(i.x - res.right_hand_landmarks.landmark[8].x)
                lst.append(i.y - res.right_hand_landmarks.landmark[8].y)
        else:
            for i in range(42):
                lst.append(0.0)


        X.append(lst)

    #np.save(f"{name}.npy", np.array(X))
    #print(np.array(X).shape)
    # Release the VideoCapture object. 
    video_reader.release()
    # Passing the  pre-processed frames to the model and get the predicted probabilities.
    predicted_labels_probabilities =model.predict(np.expand_dims(X, axis = 0))[0]

    # Get the index of class with highest probability.
    predicted_label = np.argmax(predicted_labels_probabilities)

    # Get the class name using the retrieved index.
    predicted_class_name = CLASSES_LIST[predicted_label]

    # Return the frames list.
    return predicted_class_name
