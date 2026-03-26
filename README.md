# Guitar Tuner Android

A real-time guitar tuning application using the **Yin algorithm** for pitch detection.

## Core Features
* **Pitch Detection:** Uses `Yin` algorithm to extract frequency ($Hz$) from raw audio buffers.
* **Precision Tuning:** Calculates deviation in **Cents** using:
    $$Cents = 1200 \cdot \log_2\left(\frac{f_{detected}}{f_{target}}\right)$$
* **Visual Feedback:**
    * **Needle Gauge:** Smoothly animated pointer showing cents deviation.
    * **Bubble Indicator:** Displays the absolute cent error for micro-tuning.
    * **Status Colors:** Green (In tune), Red (Sharp), Yellow (Flat).
* **Signal Processing:** * **RMS Filtering:** Ignores background noise below a specific amplitude threshold.
    * **Low-pass Smoothing:** Prevents needle jitter by averaging cent values over time.

## Technical Details
* **Sampling Rate:** 44100 Hz.
* **Update Rate:** ~30 FPS (to balance UI smoothness and CPU usage).
* **Frequency Range:** Optimized for guitar ($40Hz$ to $1000Hz$).

## Setup
1.  Grant **Record Audio** permission.
2.  Select a target string (E2, A2, D3, G3, B3, E4).
3.  Pluck the string and adjust the tuning peg until the indicator turns green.

## Project Structure
* `MainActivity.java`: UI logic and threading.
* `audio/AudioRecorder.java`: Manages the microphone input stream.
* `core/Yin.java`: Pitch detection implementation.
* `core/TunerEngine.java`: Math utility for frequency and cent conversion.
* `core/NoteMapper.java`: Maps $Hz$ to musical note names.
