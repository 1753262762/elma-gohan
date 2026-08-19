import { describe, expect, it, vi } from 'vitest'

import { NavigationService, NavigationServiceError } from '@/services/navigation'
import type { RestaurantSummary } from '@/types/recommendation'

const restaurant = {
  name: '老街牛肉粉',
  address: '麓山南路 123 号',
  latitude: 28.2291,
  longitude: 112.9412,
} as RestaurantSummary

describe('navigation service', () => {
  it('opens the server restaurant with GCJ-02 coordinates', async () => {
    const openLocation = vi.fn((options: UniApp.OpenLocationOptions) => {
      options.success?.({ errMsg: 'openLocation:ok' })
    })
    vi.stubGlobal('uni', { openLocation })

    await expect(NavigationService.openRestaurant(restaurant)).resolves.toBeUndefined()
    expect(openLocation).toHaveBeenCalledWith(
      expect.objectContaining({
        latitude: 28.2291,
        longitude: 112.9412,
        name: '老街牛肉粉',
        address: '麓山南路 123 号',
      }),
    )
  })

  it('normalizes map failures', async () => {
    vi.stubGlobal('uni', {
      openLocation: vi.fn((options: UniApp.OpenLocationOptions) => {
        options.fail?.({ errMsg: 'openLocation:fail cancel' })
      }),
    })

    await expect(NavigationService.openRestaurant(restaurant)).rejects.toBeInstanceOf(
      NavigationServiceError,
    )
  })
})

