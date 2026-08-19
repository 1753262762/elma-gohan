import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import {
  createRecommendation,
  rerollRecommendation,
  submitRecommendationFeedback,
} from '@/api/recommendation'
import { NavigationService } from '@/services/navigation'
import { recommendationStore } from '@/stores/recommendation'
import type { CreateRecommendationRequest, RecommendationResponse } from '@/types/recommendation'

const USER_ID = 'f57b640c-e4b2-4702-aa2f-2a9dacaf6910'
const RECOMMENDATION_ID = '3c34f61d-cb41-4850-aafe-1505f3312f06'

function recommendation(restaurantId: string, name: string, alternativesRemaining: number) {
  return {
    recommendationId: RECOMMENDATION_ID,
    restaurant: {
      id: restaurantId,
      name,
      latitude: 28.2291,
      longitude: 112.9412,
      address: '麓山南路 123 号',
      category: { code: 'NOODLES', label: '粉面' },
      distanceMeters: 620,
      walkingMinutes: 8,
      averagePrice: 26,
      rating: 4.5,
      businessStatus: 'OPEN',
    },
    risk: {
      riskScore: 18,
      riskLevel: 'LOW',
      reasons: ['评分稳定'],
      algorithmVersion: 'risk-v0.1',
    },
    reasons: ['距离近'],
    alternativesRemaining,
  } satisfies RecommendationResponse
}

describe('V0.1 business flow', () => {
  const initial = recommendation('restaurant-a', '老街牛肉粉', 2)
  const rerolled = recommendation('restaurant-b', '南门盖饭', 1)

  beforeEach(() => {
    vi.stubEnv('VITE_API_BASE_URL', 'http://localhost:8080/api/v1')
    vi.stubGlobal('uni', {
      getStorageSync: vi.fn(() => USER_ID),
      setStorageSync: vi.fn(),
      request: vi.fn((options: UniApp.RequestOptions) => {
        if (options.url.endsWith('/recommendations')) {
          options.success?.({ data: initial, statusCode: 201, header: {}, cookies: [] })
          return
        }
        if (options.url.endsWith('/reroll')) {
          options.success?.({ data: rerolled, statusCode: 200, header: {}, cookies: [] })
          return
        }
        options.success?.({
          data: {
            feedbackId: 'feedback-id',
            recommendationId: RECOMMENDATION_ID,
            restaurantId: 'restaurant-b',
            result: 'LIKE',
            recordedAt: '2026-08-19T09:30:00+08:00',
          },
          statusCode: 201,
          header: {},
          cookies: [],
        })
      }),
      openLocation: vi.fn((options: UniApp.OpenLocationOptions) => {
        options.success?.({ errMsg: 'openLocation:ok' })
      }),
    })
  })

  afterEach(() => recommendationStore.clear())

  it('walks through create, reroll, navigation, and feedback once', async () => {
    const request: CreateRecommendationRequest = {
      latitude: 28.2282,
      longitude: 112.9388,
      radius: 1000,
      maxBudget: 40,
      category: 'MEAL',
      dislikes: ['香菜'],
    }

    recommendationStore.setCurrent(await createRecommendation(request), request)
    recommendationStore.replaceCurrent(await rerollRecommendation(RECOMMENDATION_ID))
    await NavigationService.openRestaurant(recommendationStore.state.current!.restaurant)
    recommendationStore.recordFeedback(
      await submitRecommendationFeedback(RECOMMENDATION_ID, 'LIKE'),
    )

    expect(recommendationStore.state.current?.restaurant.name).toBe('南门盖饭')
    expect(recommendationStore.state.current?.alternativesRemaining).toBe(1)
    expect(recommendationStore.getCurrentFeedback()).toBe('LIKE')
    expect(uni.request).toHaveBeenCalledTimes(3)
    expect(uni.openLocation).toHaveBeenCalledOnce()
  })
})

