import type { RestaurantSummary } from '@/types/recommendation'

export class NavigationServiceError extends Error {
  constructor(message: string, options?: { cause?: unknown }) {
    super(message, options)
    this.name = 'NavigationServiceError'
  }
}

export const NavigationService = {
  openRestaurant(restaurant: RestaurantSummary): Promise<void> {
    return new Promise((resolve, reject) => {
      uni.openLocation({
        latitude: restaurant.latitude,
        longitude: restaurant.longitude,
        name: restaurant.name,
        address: restaurant.address,
        scale: 16,
        success() {
          resolve()
        },
        fail(error) {
          reject(new NavigationServiceError('地图打开失败，请稍后重试', { cause: error }))
        },
      })
    })
  },
}

