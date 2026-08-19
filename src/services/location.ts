import type { LocationCoordinates, LocationErrorCode } from '@/types/location'

export class LocationServiceError extends Error {
  readonly code: LocationErrorCode

  constructor(code: LocationErrorCode, message: string, options?: { cause?: unknown }) {
    super(message, options)
    this.name = 'LocationServiceError'
    this.code = code
  }
}

function isPermissionDenied(error: UniApp.GeneralCallbackResult): boolean {
  const message = error.errMsg.toLowerCase()
  return (
    message.includes('auth deny') ||
    message.includes('authorize no response') ||
    message.includes('permission denied') ||
    message.includes('system permission denied')
  )
}

export const LocationService = {
  getCurrentLocation(): Promise<LocationCoordinates> {
    return new Promise((resolve, reject) => {
      uni.getLocation({
        type: 'gcj02',
        isHighAccuracy: true,
        success(result) {
          resolve({
            latitude: result.latitude,
            longitude: result.longitude,
            accuracy: result.accuracy,
          })
        },
        fail(error) {
          if (isPermissionDenied(error)) {
            reject(
              new LocationServiceError('PERMISSION_DENIED', '需要定位权限才能获取当前位置', {
                cause: error,
              }),
            )
            return
          }

          reject(
            new LocationServiceError('LOCATION_UNAVAILABLE', '暂时无法获取位置，请稍后重试', {
              cause: error,
            }),
          )
        },
      })
    })
  },
}

