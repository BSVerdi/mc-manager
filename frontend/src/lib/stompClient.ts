import { Client } from "@stomp/stompjs";

type ConnectListener = () => void;
type AuthErrorListener = (message: string) => void;

const connectListeners = new Set<ConnectListener>();
const authErrorListeners = new Set<AuthErrorListener>();

export const stompClient = new Client({
  brokerURL: import.meta.env.VITE_WS_URL,
  reconnectDelay: 5000,

  onConnect: () => {
    stompClient.subscribe("/user/queue/erors", (message) => {
      try {
        const payload = JSON.parse(message.body) as { message: string };

        authErrorListeners.forEach((listener) => listener(payload.message));
      } catch (err) {
        console.error("[stompClient] Erro ao parsear payload de erro: ", err);
      }
    });

    connectListeners.forEach((listener) => listener());
  },

  onStompError(frame) {
    console.error("Erro STOMP: ", frame.body);
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

export function onStompAuthError(listener: AuthErrorListener): () => void {
  authErrorListeners.add(listener);

  return () => authErrorListeners.delete(listener);
}

export const topics = {
  console: (serverId: number | string) => `/topic/${serverId}/console`,
  status: (serverId: number | string) => `/topic/${serverId}/status`,
  metrics: (serverId: number | string) => `/topic/${serverId}/metrics`,
};

export const destinations = {
  command: (serverId: number | string) => `/app/${serverId}/command`,
};

stompClient.activate();
