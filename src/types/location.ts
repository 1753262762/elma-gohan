export interface LocationCoordinates {
  latitude: number
  longitude: number
  accuracy?: number
}

export type LocationErrorCode = 'PERMISSION_DENIED' | 'LOCATION_UNAVAILABLE'

