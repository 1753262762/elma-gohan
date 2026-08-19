export const PlatformService = {
  openLocationSettings(): Promise<void> {
    return new Promise((resolve, reject) => {
      uni.openSetting({
        success() {
          resolve()
        },
        fail(error) {
          reject(error)
        },
      })
    })
  },
}

