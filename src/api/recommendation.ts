import { apiRequest } from '@/api/client'
import type {
  CreateRecommendationRequest,
  FeedbackResponse,
  FeedbackResult,
  RecommendationResponse,
} from '@/types/recommendation'

export function createRecommendation(
  request: CreateRecommendationRequest,
): Promise<RecommendationResponse> {
  return apiRequest<RecommendationResponse>({
    path: '/recommendations',
    method: 'POST',
    data: request,
  })
}

export function rerollRecommendation(recommendationId: string): Promise<RecommendationResponse> {
  return apiRequest<RecommendationResponse>({
    path: `/recommendations/${encodeURIComponent(recommendationId)}/reroll`,
    method: 'POST',
  })
}

export function submitRecommendationFeedback(
  recommendationId: string,
  result: FeedbackResult,
): Promise<FeedbackResponse> {
  return apiRequest<FeedbackResponse>({
    path: `/recommendations/${encodeURIComponent(recommendationId)}/feedback`,
    method: 'POST',
    data: { result },
  })
}
