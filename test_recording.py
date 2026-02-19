import speech_recognition as sr
import time

def test_mic():
    r = sr.Recognizer()
    mic = sr.Microphone()
    
    print("Listing microphones:")
    for i, name in enumerate(sr.Microphone.list_microphone_names()):
        print(f"{i}: {name}")
        
    print("\nAttempting to record from default microphone...")
    with mic as source:
        print("Adjusting for ambient noise (please wait)...")
        r.adjust_for_ambient_noise(source, duration=1)
        print("Recording for 3 seconds...")
        audio = r.listen(source, timeout=3, phrase_time_limit=3)
        print("Recording complete.")
        
    try:
        print("Recognizing...")
        text = r.recognize_google(audio)
        print(f"Transcription: {text}")
    except Exception as e:
        print(f"Error recognizing: {e}")

if __name__ == "__main__":
    test_mic()
