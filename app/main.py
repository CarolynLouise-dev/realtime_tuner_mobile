from kivy.app import App
from kivy.uix.label import Label
from kivy.clock import Clock
import websocket
import threading
import json

SERVER_WS = "ws://YOUR_SERVER_IP:8000/ws/tuner"

class TunerApp(App):
    def build(self):
        self.label = Label(text="Connecting to tuner...")
        # start websocket thread
        threading.Thread(target=self.ws_thread, daemon=True).start()
        return self.label

    def ws_thread(self):
        def on_message(ws, message):
            data = json.loads(message)
            note = data.get("note")
            status = data.get("status")
            # update UI on main thread
            Clock.schedule_once(lambda dt: self.update_label(note, status))

        ws = websocket.WebSocketApp(SERVER_WS, on_message=on_message)
        ws.run_forever()

    def update_label(self, note, status):
        self.label.text = f"Note: {note}\nStatus: {status}"

if __name__ == "__main__":
    TunerApp().run()