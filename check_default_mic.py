import pyaudio

p = pyaudio.PyAudio()

try:
    default_device_index = p.get_default_input_device_info()['index']
    default_device_info = p.get_device_info_by_index(default_device_index)
    
    print(f"Default Input Device Index: {default_device_index}")
    print(f"Default Input Device Name: {default_device_info['name']}")
    print("-" * 30)
    print("All Input Devices:")
    for i in range(p.get_device_count()):
        info = p.get_device_info_by_index(i)
        if info['maxInputChannels'] > 0:
             print(f"{i}: {info['name']}")

except Exception as e:
    print(f"Error getting default device: {e}")

p.terminate()
