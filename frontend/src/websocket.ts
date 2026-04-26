import { Client } from '@stomp/stompjs';
import { useBankStore } from './store';

export const connectWebSocket = () => {
  const client = new Client({
    brokerURL: 'ws://localhost:8080/ws', // Backend WebSocket
    connectHeaders: {},
    debug: function (str) {
      // console.log(str);
    },
    reconnectDelay: 5000,
    heartbeatIncoming: 4000,
    heartbeatOutgoing: 4000,
  });

  client.onConnect = function (frame) {
    console.log('Connected: ' + frame);
    client.subscribe('/topic/os-events', (message) => {
      if (message.body) {
        useBankStore.getState().addLog(message.body);
      }
    });
  };

  client.onStompError = function (frame) {
    console.log('Broker reported error: ' + frame.headers['message']);
    console.log('Additional details: ' + frame.body);
  };

  client.activate();
  return client;
};
