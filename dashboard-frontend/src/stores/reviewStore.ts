import { defineStore } from 'pinia'
import { ref } from 'vue'
import axios from 'axios'
import type { ReviewRecord, ProgressMessage } from '../types'

export const useReviewStore = defineStore('review', () => {
  const reviews = ref<ReviewRecord[]>([])
  const currentReview = ref<ReviewRecord | null>(null)
  const progressMessages = ref<ProgressMessage[]>([])
  const connected = ref(false)

  async function fetchReviews() {
    const { data } = await axios.get<ReviewRecord[]>('/api/reviews')
    reviews.value = data
  }

  async function fetchReview(id: string) {
    const { data } = await axios.get<ReviewRecord>(`/api/reviews/${id}`)
    currentReview.value = data
  }

  function addProgressMessage(message: ProgressMessage) {
    progressMessages.value.unshift(message)
    if (progressMessages.value.length > 100) {
      progressMessages.value.pop()
    }

    const idx = reviews.value.findIndex(r => r.id === message.reviewId)
    if (idx >= 0) {
      reviews.value[idx].status = message.status
    }

    if (currentReview.value?.id === message.reviewId) {
      currentReview.value.status = message.status
    }
  }

  function setConnected(status: boolean) {
    connected.value = status
  }

  return {
    reviews,
    currentReview,
    progressMessages,
    connected,
    fetchReviews,
    fetchReview,
    addProgressMessage,
    setConnected
  }
})
