import { describe, expect, it, vi } from 'vitest'

import { LocationService, LocationServiceError } from '@/services/location'
import { PlatformService } from '@/services/platform'

describe('location and platform services', () => {
  it('requests foreground GCJ-02 location and returns accuracy', async () => {
    const getLocation = vi.fn((options: UniApp.GetLocationOptions) => {
      options.success?.({
        latitude: 28.2282,
        longitude: 112.9388,
        accuracy: 16,
        altitude: 0,
        verticalAccuracy: 0,
        horizontalAccuracy: 16,
        speed: 0,
      })
    })
    vi.stubGlobal('uni', { getLocation })

    await expect(LocationService.getCurrentLocation()).resolves.toEqual({
      latitude: 28.2282,
      longitude: 112.9388,
      accuracy: 16,
    })
    expect(getLocation).toHaveBeenCalledWith(expect.objectContaining({ type: 'gcj02' }))
  })

  it('classifies permission denial separately', async () => {
    vi.stubGlobal('uni', {
      getLocation: vi.fn((options: UniApp.GetLocationOptions) => {
        options.fail?.({ errMsg: 'getLocation:fail auth deny' })
      }),
    })

    const error = await LocationService.getCurrentLocation().catch((reason: unknown) => reason)

    expect(error).toBeInstanceOf(LocationServiceError)
    expect(error).toMatchObject({ code: 'PERMISSION_DENIED' })
  })

  it('opens the platform authorization settings through the service layer', async () => {
    const openSetting = vi.fn((options: UniApp.OpenSettingOptions) => options.success?.({ authSetting: {} }))
    vi.stubGlobal('uni', { openSetting })

    await expect(PlatformService.openLocationSettings()).resolves.toBeUndefined()
    expect(openSetting).toHaveBeenCalledOnce()
  })
})

