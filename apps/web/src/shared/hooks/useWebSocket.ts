import { useEffect, useRef, useCallback } from 'react';
import { getAccessToken } from '../api/client';

type MessageHandler = (data: any) => void;

/**
 * WebSocket hook: 连接 sync-service，支持自动重连和心跳。
 */
export function useSyncWebSocket(onMessage: MessageHandler) {
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<number>(0);
  const pingTimer = useRef<number>(0);
  const onMessageRef = useRef<MessageHandler>(onMessage);
  onMessageRef.current = onMessage;

  const connect = useCallback(() => {
    const token = getAccessToken();
    if (!token) return;

    // WebSocket 连接到 sync-service（通过 Vite proxy 或直接连接）
    const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const host = window.location.hostname;
    const wsUrl = `${protocol}//${host}:8083/ws?token=${token}`;

    try {
      const ws = new WebSocket(wsUrl);
      wsRef.current = ws;

      ws.onopen = () => {
        console.log('[SyncWS] connected');
        // 心跳：每30秒发一次 ping
        pingTimer.current = window.setInterval(() => {
          if (ws.readyState === WebSocket.OPEN) {
            ws.send('ping');
          }
        }, 30000);
      };

      ws.onmessage = (event) => {
        if (event.data === 'pong') return;
        try {
          const data = JSON.parse(event.data);
          onMessageRef.current(data);
        } catch {
          // 非 JSON 消息忽略
        }
      };

      ws.onclose = (event) => {
        console.log('[SyncWS] disconnected:', event.code);
        clearInterval(pingTimer.current);
        wsRef.current = null;
        // 5秒后重连
        reconnectTimer.current = window.setTimeout(connect, 5000);
      };

      ws.onerror = () => {
        // onclose 会接着触发，由 onclose 处理重连
      };
    } catch {
      // 连接失败，5秒后重试
      reconnectTimer.current = window.setTimeout(connect, 5000);
    }
  }, []);

  useEffect(() => {
    connect();
    return () => {
      clearTimeout(reconnectTimer.current);
      clearInterval(pingTimer.current);
      wsRef.current?.close();
    };
  }, [connect]);

  return wsRef;
}
