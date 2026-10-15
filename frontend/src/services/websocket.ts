// services/websocket.ts
import { Client, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// Resolve the STOMP/SockJS endpoint from the environment:
//   1. NEXT_PUBLIC_WS_URL        - explicit full URL (e.g. https://host/ws)
//   2. NEXT_PUBLIC_API_URL + /ws - API base is a bare origin; /ws sits at
//                                   the backend root, not under /api
//   3. http://localhost:8080/ws  - local dev fallback
function resolveWsUrl(): string {
  const explicit = process.env.NEXT_PUBLIC_WS_URL;
  if (explicit) {
    return explicit;
  }
  const apiBase = process.env.NEXT_PUBLIC_API_URL;
  if (apiBase) {
    return `${apiBase.replace(/\/+$/, '')}/ws`;
  }
  return 'http://localhost:8080/ws';
}

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, any> = new Map();

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
        console.log("===> ");
      this.client = new Client({
        webSocketFactory: () => new SockJS(resolveWsUrl()),
        connectHeaders: {
          Authorization: `Bearer ${localStorage.getItem('token')}`,
        },
        onConnect: () => {
          console.log('WebSocket connected');
          resolve();
        },
        onStompError: (frame) => {
          console.error('WebSocket error:', frame);
          reject(frame);
        },
        onDisconnect: () => {
          console.log('WebSocket disconnected');
        },
      });

      this.client.activate();
    });
  }

  subscribe(issueId: number, callback: (message: any) => void): void {
    if (!this.client?.connected) {
      console.warn('WebSocket not connected');
      return;
    }

    const subscription = this.client.subscribe(`/topic/issues/${issueId}`, (message) => {
      console.log("in ws message ==> ", message.body)
      const data = JSON.parse(message.body);
      callback(data);
    });

    this.subscriptions.set(`issue_${issueId}`, subscription);
  }

  unsubscribe(issueId: number): void {
    const subscription = this.subscriptions.get(`issue_${issueId}`);
    if (subscription) {
      subscription.unsubscribe();
      this.subscriptions.delete(`issue_${issueId}`);
    }
  }

  disconnect(): void {
    this.subscriptions.forEach((sub) => sub.unsubscribe());
    this.subscriptions.clear();
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  isConnected(): boolean {
    return this.client?.connected || false;
  }
}

export default new WebSocketService();