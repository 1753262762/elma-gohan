<template>
  <view v-if="recommendation" class="result-page">
    <view class="result-nav">
      <button class="back-button" aria-label="返回首页" @click="goHome">←</button>
      <text class="result-brand">ELMA / TODAY</text>
      <view class="nav-pixels" aria-hidden="true" />
    </view>

    <view class="pick-heading">
      <text class="pick-index">TODAY'S PICK · 01</text>
      <text class="pick-kicker">别选了，今天吃这个。</text>
    </view>

    <view class="restaurant-section">
      <text class="category-label">{{ recommendation.restaurant.category.label }}</text>
      <text class="restaurant-name">{{ recommendation.restaurant.name }}</text>
      <text class="restaurant-address">{{ recommendation.restaurant.address }}</text>

      <view class="restaurant-meta">
        <view class="meta-item">
          <text class="meta-value">{{ formatDistance(recommendation.restaurant.distanceMeters) }}</text>
          <text class="meta-label">距离</text>
        </view>
        <view class="meta-rule" />
        <view class="meta-item">
          <text class="meta-value">{{ formatPrice(recommendation.restaurant.averagePrice) }}</text>
          <text class="meta-label">人均</text>
        </view>
        <view class="meta-rule" />
        <view class="meta-item">
          <text class="meta-value">{{ recommendation.restaurant.walkingMinutes }} 分钟</text>
          <text class="meta-label">步行</text>
        </view>
      </view>
    </view>

    <view class="risk-strip">
      <view>
        <text class="risk-caption">RISK CHECK</text>
        <text class="risk-label">{{ riskLabel }}</text>
      </view>
      <view class="risk-signal" aria-hidden="true">
        <view class="risk-dot risk-dot--active" />
        <view class="risk-dot risk-dot--active" />
        <view class="risk-dot" />
        <view class="risk-dot" />
      </view>
    </view>

    <view class="reason-section">
      <view class="reason-heading">
        <text class="reason-title">为什么是它</text>
        <text class="reason-count">{{ String(recommendation.reasons.length).padStart(2, '0') }}</text>
      </view>
      <view v-for="reason in recommendation.reasons" :key="reason" class="reason-row">
        <view class="reason-pixel" aria-hidden="true" />
        <text>{{ reason }}</text>
      </view>
    </view>

    <view class="result-actions">
      <button class="accept-button" @click="showVisualGateMessage">
        <text>就它了</text>
        <text class="accept-arrow">↗</text>
      </button>
      <button
        v-if="recommendation.alternativesRemaining > 0"
        class="reroll-button"
        @click="showVisualGateMessage"
      >
        换一家 · 还可以换 {{ recommendation.alternativesRemaining }} 次
      </button>
    </view>

    <view class="feedback-section">
      <text class="feedback-title">这个答案怎么样？</text>
      <view class="feedback-row">
        <button
          v-for="option in feedbackOptions"
          :key="option.value"
          class="feedback-button"
          :class="{ 'feedback-button--active': feedback === option.value }"
          @click="feedback = option.value"
        >
          <text class="feedback-icon">{{ option.icon }}</text>
          <text>{{ option.label }}</text>
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'

import { visualPreviewRecommendation } from '@/dev/visual-preview'
import type { FeedbackResult, RecommendationResponse, RiskLevel } from '@/types/recommendation'

const riskLabels: Record<RiskLevel, string> = {
  LOW: '低风险',
  MEDIUM_LOW: '中低风险',
  MEDIUM: '中风险',
  HIGH: '高风险',
}

const feedbackOptions: Array<{ icon: string; label: string; value: FeedbackResult }> = [
  { icon: '↑', label: '不错', value: 'LIKE' },
  { icon: '—', label: '一般', value: 'NORMAL' },
  { icon: '↓', label: '踩坑', value: 'DISLIKE' },
]

const recommendation = ref<RecommendationResponse | null>(null)
const feedback = ref<FeedbackResult | null>(null)

const riskLabel = computed(() =>
  recommendation.value ? riskLabels[recommendation.value.risk.riskLevel] : '',
)

onLoad((options) => {
  if (import.meta.env.DEV && options?.preview === '1') {
    recommendation.value = visualPreviewRecommendation
    return
  }

  goHome()
})

function formatDistance(distanceMeters: number) {
  return distanceMeters < 1000 ? `${distanceMeters}m` : `${(distanceMeters / 1000).toFixed(1)}km`
}

function formatPrice(averagePrice: number | null) {
  return averagePrice === null ? '暂无' : `¥${averagePrice}`
}

function goHome() {
  uni.reLaunch({ url: '/pages/home/index' })
}

function showVisualGateMessage() {
  uni.showToast({
    title: '视觉确认后接入完整功能',
    icon: 'none',
  })
}
</script>

<style scoped>
.result-page {
  position: relative;
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  overflow: hidden;
  padding: calc(var(--status-bar-height) + 34rpx) 48rpx 54rpx;
  background: #f8f8fb;
  color: #18203a;
}

.result-page * {
  box-sizing: border-box;
}

.result-nav,
.pick-heading,
.restaurant-meta,
.risk-strip,
.risk-signal,
.reason-heading,
.reason-row,
.accept-button,
.feedback-row,
.feedback-button {
  display: flex;
  align-items: center;
}

.result-nav {
  justify-content: space-between;
}

.back-button {
  width: 56rpx;
  height: 56rpx;
  margin: 0;
  padding: 0;
  border: 2rpx solid #dde1ec;
  border-radius: 12rpx;
  background: transparent;
  color: #18203a;
  font-size: 28rpx;
  line-height: 52rpx;
}

.result-brand {
  font-size: 19rpx;
  font-weight: 600;
  letter-spacing: 3rpx;
}

.nav-pixels {
  width: 10rpx;
  height: 10rpx;
  margin-right: 16rpx;
  background: #5b61d6;
  box-shadow: 14rpx 0 0 #d9d2f6, 0 14rpx 0 #bfe8db;
}

.pick-heading {
  justify-content: space-between;
  margin-top: 54rpx;
}

.pick-index {
  color: #5b61d6;
  font-size: 18rpx;
  letter-spacing: 2rpx;
}

.pick-kicker {
  color: #747d97;
  font-size: 20rpx;
}

.restaurant-section {
  margin-top: 42rpx;
}

.category-label {
  display: inline-block;
  padding: 8rpx 15rpx;
  border-radius: 8rpx;
  background: #e8e9ff;
  color: #343a9f;
  font-size: 20rpx;
  font-weight: 600;
}

.restaurant-name,
.restaurant-address {
  display: block;
}

.restaurant-name {
  margin-top: 18rpx;
  font-size: 62rpx;
  font-weight: 700;
  letter-spacing: -2rpx;
  line-height: 1.16;
}

.restaurant-address {
  margin-top: 14rpx;
  color: #66708a;
  font-size: 23rpx;
}

.restaurant-meta {
  margin-top: 38rpx;
  padding: 27rpx 0;
  border-top: 2rpx solid #dde1ec;
  border-bottom: 2rpx solid #dde1ec;
}

.meta-item {
  min-width: 0;
  flex: 1 1 0;
}

.meta-value,
.meta-label {
  display: block;
  text-align: center;
}

.meta-value {
  font-size: 28rpx;
  font-weight: 700;
}

.meta-label {
  margin-top: 6rpx;
  color: #747d97;
  font-size: 18rpx;
}

.meta-rule {
  width: 2rpx;
  height: 42rpx;
  background: #dde1ec;
}

.risk-strip {
  justify-content: space-between;
  margin-top: 30rpx;
  padding: 24rpx 26rpx;
  border-radius: 12rpx;
  background: #dff2ec;
}

.risk-caption,
.risk-label {
  display: block;
}

.risk-caption {
  color: #477568;
  font-size: 16rpx;
  letter-spacing: 2rpx;
}

.risk-label {
  margin-top: 4rpx;
  font-size: 27rpx;
  font-weight: 700;
}

.risk-signal {
  gap: 8rpx;
}

.risk-dot {
  width: 12rpx;
  height: 12rpx;
  background: #b7cec7;
}

.risk-dot--active {
  background: #2f7d6a;
}

.reason-section {
  margin-top: 42rpx;
}

.reason-heading {
  justify-content: space-between;
  padding-bottom: 16rpx;
}

.reason-title {
  font-size: 25rpx;
  font-weight: 700;
}

.reason-count {
  color: #5b61d6;
  font-size: 18rpx;
  letter-spacing: 2rpx;
}

.reason-row {
  gap: 20rpx;
  padding: 17rpx 0;
  border-top: 2rpx solid #eaecf3;
  color: #3f4862;
  font-size: 23rpx;
}

.reason-pixel {
  width: 10rpx;
  height: 10rpx;
  flex: 0 0 auto;
  background: #5b61d6;
  box-shadow: 6rpx 6rpx 0 #d9d2f6;
}

.result-actions {
  margin-top: 38rpx;
}

.accept-button {
  width: 100%;
  justify-content: space-between;
  margin: 0;
  padding: 27rpx 34rpx;
  border-radius: 12rpx;
  background: #5b61d6;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 1;
  text-align: left;
}

.accept-arrow {
  font-size: 36rpx;
  font-weight: 400;
}

.reroll-button {
  width: 100%;
  margin: 16rpx 0 0;
  padding: 23rpx 24rpx;
  border: 2rpx solid #cfd3e1;
  border-radius: 12rpx;
  background: transparent;
  color: #3f4862;
  font-size: 23rpx;
  line-height: 1;
}

.feedback-section {
  margin-top: 38rpx;
}

.feedback-title {
  display: block;
  color: #747d97;
  font-size: 20rpx;
  text-align: center;
}

.feedback-row {
  gap: 12rpx;
  margin-top: 16rpx;
}

.feedback-button {
  flex: 1;
  justify-content: center;
  gap: 8rpx;
  margin: 0;
  padding: 17rpx 10rpx;
  border-radius: 10rpx;
  background: #eef0f8;
  color: #59627c;
  font-size: 21rpx;
  line-height: 1;
}

.feedback-button--active {
  background: #e8e9ff;
  color: #343a9f;
  font-weight: 600;
}

.feedback-icon {
  color: #5b61d6;
  font-size: 24rpx;
}
</style>
