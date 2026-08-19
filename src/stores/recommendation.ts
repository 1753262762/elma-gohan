import { reactive, readonly } from 'vue'

import type {
  CreateRecommendationRequest,
  FeedbackResponse,
  FeedbackResult,
  RecommendationResponse,
} from '@/types/recommendation'

interface RecommendationState {
  current: RecommendationResponse | null
  lastRequest: CreateRecommendationRequest | null
  feedbackByRestaurantId: Record<string, FeedbackResponse>
}

const state = reactive<RecommendationState>({
  current: null,
  lastRequest: null,
  feedbackByRestaurantId: {},
})

export const recommendationStore = {
  state: readonly(state),

  setCurrent(response: RecommendationResponse, request: CreateRecommendationRequest) {
    state.current = response
    state.lastRequest = request
    state.feedbackByRestaurantId = {}
  },

  replaceCurrent(response: RecommendationResponse) {
    state.current = response
  },

  getCurrentFeedback(): FeedbackResult | null {
    const restaurantId = state.current?.restaurant.id
    return restaurantId ? state.feedbackByRestaurantId[restaurantId]?.result ?? null : null
  },

  recordFeedback(response: FeedbackResponse) {
    state.feedbackByRestaurantId[response.restaurantId] = response
  },

  clear() {
    state.current = null
    state.lastRequest = null
    state.feedbackByRestaurantId = {}
  },
}
