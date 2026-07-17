import { Client } from "@stomp/stompjs";

type ConnectListener = () => void;
const connectListeners = new Set<ConnectListener>();

export const stompClient = new Client({
  brokerURL: import.meta.env.VITE_WS_URL,
  reconnectDelay: 5000,

  onConnect: () => {
    connectListeners.forEach((listener) => listener());
  },

  onStompError(frame) {
    console.error("Erro STOMP: ", frame);
  },

  onWebSocketError(error) {
    console.error("Erro WebSocket: ", error);
  },
});

export function onStompConnect(listener: ConnectListener): () => void {
  connectListeners.add(listener);

  if (stompClient.connected) listener();

  return () => connectListeners.delete(listener);
}

stompClient.activate();
