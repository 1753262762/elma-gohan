import { beforeEach, describe, expect, it, vi } from 'vitest'

import { apiRequest } from '@/api/client'
import {
  createRecommendation,
  rerollRecommendation,
  submitRecommendationFeedback,
} from '@/api/recommendation'
import type {
  CreateRecommendationRequest,
  FeedbackResponse,
  RecommendationResponse,
} from '@/types/recommendation'

vi.mock('@/api/client', () => ({ apiRequest: vi.fn() }))

const request: CreateRecommendationRequest = {
  latitude: 28.2282,
  longitude: 112.9388,
  radius: 1000,
  maxBudget: 40,
  category: 'ANY',
  dislikes: ['香菜'],
}

describe('recommendation API', () => {
  beforeEach(() => vi.mocked(apiRequest).mockReset())

  it('posts the contract request to /recommendations', async () => {
    const response = { recommendationId: 'recommendation-id' } as RecommendationResponse
    vi.mocked(apiRequest).mockResolvedValue(response)

    await expect(createRecommendation(request)).resolves.toBe(response)
    expect(apiRequest).toHaveBeenCalledWith({
      path: '/recommendations',
      method: 'POST',
      data: request,
    })
  })

  it('rerolls only through the recommendation session endpoint', async () => {
    const response = { recommendationId: 'recommendation-id' } as RecommendationResponse
    vi.mocked(apiRequest).mockResolvedValue(response)

    await expect(rerollRecommendation('session/id')).resolves.toBe(response)
    expect(apiRequest).toHaveBeenCalledWith({
      path: '/recommendations/session%2Fid/reroll',
      method: 'POST',
    })
  })

  it('submits only the selected feedback result', async () => {
    const response = { feedbackId: 'feedback-id' } as FeedbackResponse
    vi.mocked(apiRequest).mockResolvedValue(response)

    await expect(submitRecommendationFeedback('session-id', 'DISLIKE')).resolves.toBe(response)
    expect(apiRequest).toHaveBeenCalledWith({
      path: '/recommendations/session-id/feedback',
      method: 'POST',
      data: { result: 'DISLIKE' },
    })
  })
})
