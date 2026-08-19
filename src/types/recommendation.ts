export type Radius = 500 | 1000 | 2000 | 3000
export type FeedbackResult = 'LIKE' | 'NORMAL' | 'DISLIKE'
export type RiskLevel = 'LOW' | 'MEDIUM_LOW' | 'MEDIUM' | 'HIGH'
export type BusinessStatus = 'OPEN' | 'CLOSED' | 'UNKNOWN'

export interface CreateRecommendationRequest {
  latitude: number
  longitude: number
  radius: Radius
  maxBudget: number | null
  category: string
  dislikes: string[]
}

export interface SubmitFeedbackRequest {
  result: FeedbackResult
}

export interface FeedbackResponse {
  feedbackId: string
  recommendationId: string
  restaurantId: string
  result: FeedbackResult
  recordedAt: string
}

export interface RecommendationResponse {
  recommendationId: string
  restaurant: RestaurantSummary
  risk: RiskAssessment
  reasons: string[]
  alternativesRemaining: number
}

export interface RestaurantSummary {
  id: string
  name: string
  latitude: number
  longitude: number
  address: string
  category: RestaurantCategory
  distanceMeters: number
  walkingMinutes: number
  averagePrice: number | null
  rating: number | null
  businessStatus: BusinessStatus
}

export interface RestaurantCategory {
  code: string
  label: string
}

export interface RiskAssessment {
  riskScore: number
  riskLevel: RiskLevel
  reasons: string[]
  algorithmVersion: string
}
