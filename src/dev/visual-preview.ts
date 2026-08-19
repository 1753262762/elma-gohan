import type { RecommendationResponse } from '@/types/recommendation'

/** Visual-gate fixture. Remove after the approved UI is wired to the real API. */
export const visualPreviewRecommendation: RecommendationResponse = {
  recommendationId: '3c34f61d-cb41-4850-aafe-1505f3312f06',
  restaurant: {
    id: '8322a6eb-186b-4be7-b4e5-980c9ef93042',
    name: '老街牛肉粉',
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
    reasons: ['评分稳定', '店铺信息完整', '价格位于正常范围'],
    algorithmVersion: 'risk-v0.1',
  },
  reasons: ['离你很近，走过去刚刚好', '价格在预算内，不用临时纠结', '基础信息完整，今天可以放心一点'],
  alternativesRemaining: 2,
}
