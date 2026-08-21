import { flushPromises, mount } from '@vue/test-utils'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

import * as recommendationApi from '@/api/recommendation'
import ResultPage from '@/pages/result/index.vue'
import { NavigationService } from '@/services/navigation'
import { recommendationStore } from '@/stores/recommendation'
import type { CreateRecommendationRequest, RecommendationResponse } from '@/types/recommendation'

vi.mock('@dcloudio/uni-app', () => ({
  onLoad: (callback: () => void) => callback(),
}))

const request: CreateRecommendationRequest = {
  latitude: 28.2282,
  longitude: 112.9388,
  radius: 1000,
  maxBudget: null,
  category: 'ANY',
  dislikes: [],
}

function recommendation(overrides: Partial<RecommendationResponse> = {}): RecommendationResponse {
  return {
    recommendationId: '3c34f61d-cb41-4850-aafe-1505f3312f06',
    restaurant: {
      id: 'restaurant-a',
      name: '老街牛肉粉',
      latitude: 28.2291,
      longitude: 112.9412,
      address: '麓山南路 123 号',
      category: { code: 'NOODLES', label: '粉面' },
      distanceMeters: 1620,
      walkingMinutes: 18,
      averagePrice: null,
      rating: null,
      businessStatus: 'OPEN',
    },
    risk: {
      riskScore: 48,
      riskLevel: 'MEDIUM_LOW',
      confidence: 0.4,
      reasons: ['信息有限'],
      algorithmVersion: 'risk-v0.3',
    },
    reasons: ['距离可接受'],
    alternativesRemaining: 0,
    ...overrides,
  }
}

describe('result page acceptance states', () => {
  beforeEach(() => {
    vi.stubGlobal('uni', {
      reLaunch: vi.fn(),
      showToast: vi.fn(),
    })
  })

  afterEach(() => {
    recommendationStore.clear()
    vi.restoreAllMocks()
    vi.unstubAllGlobals()
  })

  it('returns home when opened without recommendation state', () => {
    const wrapper = mount(ResultPage)

    expect(wrapper.html()).toBe('<!--v-if-->')
    expect(uni.reLaunch).toHaveBeenCalledWith({ url: '/pages/home/index' })
  })

  it('renders nullable fields, risk label, reasons, and exhausted alternatives', () => {
    recommendationStore.setCurrent(recommendation(), request)
    const wrapper = mount(ResultPage)

    expect(wrapper.text()).toContain('老街牛肉粉')
    expect(wrapper.text()).toContain('1.6km')
    expect(wrapper.text()).toContain('暂无')
    expect(wrapper.text()).toContain('中低风险')
    expect(wrapper.text()).toContain('距离可接受')
    expect(wrapper.find('.reroll-button').exists()).toBe(false)
    expect(wrapper.text()).toContain('备选已用完')
  })

  it('shows reroll loading and replaces the view only with the server response', async () => {
    recommendationStore.setCurrent(recommendation({ alternativesRemaining: 5 }), request)
    let finishReroll!: (value: RecommendationResponse) => void
    const rerollSpy = vi
      .spyOn(recommendationApi, 'rerollRecommendation')
      .mockReturnValue(new Promise((resolve) => (finishReroll = resolve)))
    const wrapper = mount(ResultPage)

    await wrapper.find('.reroll-button').trigger('click')
    expect(wrapper.find('.reroll-button').text()).toContain('正在换一家')
    expect(wrapper.find('.accept-button').attributes('disabled')).toBeDefined()

    finishReroll(
      recommendation({
        restaurant: { ...recommendation().restaurant, id: 'restaurant-b', name: '南门盖饭' },
        alternativesRemaining: 0,
      }),
    )
    await flushPromises()

    expect(rerollSpy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('南门盖饭')
    expect(wrapper.find('.reroll-button').exists()).toBe(false)
  })

  it('records feedback once and keeps all feedback buttons disabled', async () => {
    recommendationStore.setCurrent(recommendation(), request)
    const feedbackSpy = vi.spyOn(recommendationApi, 'submitRecommendationFeedback').mockResolvedValue({
      feedbackId: 'feedback-id',
      recommendationId: recommendation().recommendationId,
      restaurantId: 'restaurant-a',
      result: 'LIKE',
      recordedAt: '2026-08-19T09:30:00+08:00',
    })
    const wrapper = mount(ResultPage)

    await wrapper.findAll('.feedback-button')[0].trigger('click')
    await flushPromises()

    expect(feedbackSpy).toHaveBeenCalledOnce()
    expect(wrapper.text()).toContain('这家反馈已记录')
    expect(wrapper.findAll('.feedback-button').every((button) => button.attributes('disabled') !== undefined)).toBe(true)
  })

  it('shows navigation errors and restores the action after loading', async () => {
    recommendationStore.setCurrent(recommendation(), request)
    vi.spyOn(NavigationService, 'openRestaurant').mockRejectedValue(new Error('map failed'))
    const wrapper = mount(ResultPage)

    await wrapper.find('.accept-button').trigger('click')
    await flushPromises()

    expect(wrapper.find('.operation-error').text()).toBe('请求失败，请稍后再试')
    expect(wrapper.find('.accept-button').attributes('disabled')).toBeUndefined()
    expect(wrapper.find('.accept-button').text()).toContain('就它了')
  })
})
