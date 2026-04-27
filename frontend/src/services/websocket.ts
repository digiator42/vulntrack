// services/websocket.ts
import { Client, Stomp } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, any> = new Map();

  connect(token: string): Promise<void> {
    return new Promise((resolve, reject) => {
        console.log("===> ");
      this.client = new Client({
        webSocketFactory: () => new SockJS(`http://localhost:8080/ws`),
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