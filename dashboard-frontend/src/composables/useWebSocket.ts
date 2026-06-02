import { ref, onMounted, onUnmounted } from 'vue'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { useReviewStore } from '../stores/reviewStore'
import type { ProgressMessage } from '../types'

export function useWebSocket() {
  const store = useReviewStore()
  const client = ref<Client | null>(null)

  function connect() {
    const stompClient = new Client({
      webSocketFactory: () => new SockJS('/ws'),
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      onConnect: () => {
        store.setConnected(true)

        stompClient.subscribe('/topic/reviews/all', (message) => {
          const progress: ProgressMessage = JSON.parse(message.body)
          store.addProgressMessage(progress)
        })
      },
      onDisconnect: () => {
        store.setConnected(false)
      },
      onStompError: (frame) => {
        console.error('STOMP error:', frame.headers['message'])
        store.setConnected(false)
      }
    })

    stompClient.activate()
    client.value = stompClient
  }

  function disconnect() {
    if (client.value) {
      client.value.deactivate()
      client.value = null
    }
  }

  onMounted(() => connect())
  onUnmounted(() => disconnect())

  return { connect, disconnect }
}
