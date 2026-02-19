import speech_recognition as sr

def test_mic_5():
    r = sr.Recognizer()
    # Explicitly use device index 5
    mic = sr.Microphone(device_index=5)
    
    print("Using device index 5 (Logitech StreamCam)...")
    with mic as source:
        print("Adjusting for ambient noise (1s)...")
        r.adjust_for_ambient_noise(source, duration=1)
        print(f"Energy threshold set to: {r.energy_threshold}")
        
        print("Recording for 5 seconds...")
        # record raw audio
        audio = r.listen(source, timeout=5, phrase_time_limit=5)
        print("Recording complete.")
        
    try:
        print("Recognizing via Google...")
        text = r.recognize_google(audio)
        print(f"Transcription: {text}")
    except sr.UnknownValueError:
        print("Google Speech Recognition could not understand audio (Silence/Unintelligible)")
    except sr.RequestError as e:
        print(f"Could not request results from Google Speech Recognition service; {e}")
    except Exception as e:
        print(f"Generic Error: {e}")

if __name__ == "__main__":
    test_mic_5()
