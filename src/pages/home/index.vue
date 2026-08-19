<template>
  <view class="home-page">
    <view class="pixel-spark pixel-spark--one" aria-hidden="true" />
    <view class="pixel-spark pixel-spark--two" aria-hidden="true" />

    <view class="brand-row">
      <text class="brand">ELMA</text>
      <text class="edition">MEAL DECISION / 01</text>
    </view>

    <view class="hero">
      <text class="product-name">家今天的饭</text>
      <text class="hero-copy">别选了，\n今天吃这个。</text>
      <text class="hero-note">只给你一个认真筛过的答案。</text>
    </view>

    <view class="location-row">
      <view class="location-copy">
        <view class="location-mark" aria-hidden="true" />
        <view>
          <text class="location-label">当前定位</text>
          <text class="location-value">已获取当前位置</text>
        </view>
      </view>
      <button class="text-action" @click="showVisualGateMessage">重新定位</button>
    </view>

    <view class="choice-section">
      <view class="section-heading">
        <text class="section-index">01</text>
        <text class="section-title">走多远</text>
      </view>
      <view class="choice-row">
        <button
          v-for="option in radiusOptions"
          :key="option.value"
          class="choice-button"
          :class="{ 'choice-button--active': radius === option.value }"
          @click="radius = option.value"
        >
          {{ option.label }}
        </button>
      </view>
    </view>

    <view class="choice-section">
      <view class="section-heading">
        <text class="section-index">02</text>
        <text class="section-title">花多少</text>
      </view>
      <view class="choice-row">
        <button
          v-for="option in budgetOptions"
          :key="String(option.value)"
          class="choice-button"
          :class="{ 'choice-button--active': budget === option.value }"
          @click="budget = option.value"
        >
          {{ option.label }}
        </button>
      </view>
    </view>

    <view class="preference-grid">
      <view class="preference-field preference-field--category">
        <text class="field-label">想吃什么</text>
        <view class="category-value">
          <text>随便</text>
          <text class="category-code">ANY</text>
        </view>
      </view>
      <label class="preference-field preference-field--input">
        <text class="field-label">不想吃</text>
        <input
          v-model="dislikesInput"
          class="dislikes-input"
          maxlength="309"
          placeholder="香菜、内脏……"
          placeholder-class="dislikes-placeholder"
        />
      </label>
    </view>

    <view class="decision-area">
      <button class="decision-button" @click="showVisualGateMessage">
        <text>帮我选</text>
        <text class="decision-arrow">→</text>
      </button>
      <text class="decision-caption">ONE GOOD CHOICE, NOT A LIST.</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'

import type { Radius } from '@/types/recommendation'

const radiusOptions: Array<{ label: string; value: Radius }> = [
  { label: '500m', value: 500 },
  { label: '1km', value: 1000 },
  { label: '2km', value: 2000 },
  { label: '3km', value: 3000 },
]

const budgetOptions: Array<{ label: string; value: number | null }> = [
  { label: '¥20', value: 20 },
  { label: '¥40', value: 40 },
  { label: '¥70', value: 70 },
  { label: '不限', value: null },
]

const radius = ref<Radius>(1000)
const budget = ref<number | null>(null)
const dislikesInput = ref('')

function showVisualGateMessage() {
  uni.showToast({
    title: '视觉确认后接入完整功能',
    icon: 'none',
  })
}
</script>

<style scoped>
.home-page {
  position: relative;
  display: flex;
  box-sizing: border-box;
  width: 100%;
  min-height: 100vh;
  flex-direction: column;
  overflow: hidden;
  padding: calc(var(--status-bar-height) + 40rpx) 48rpx 48rpx;
  background: #f8f8fb;
  color: #18203a;
}

.home-page * {
  box-sizing: border-box;
}

.pixel-spark {
  position: absolute;
  width: 10rpx;
  height: 10rpx;
  background: #5b61d6;
  box-shadow: 16rpx 0 0 #d9d2f6, 0 16rpx 0 #bfe8db;
}

.pixel-spark--one {
  top: 166rpx;
  right: 68rpx;
}

.pixel-spark--two {
  top: 470rpx;
  right: 102rpx;
  transform: rotate(45deg) scale(0.75);
}

.brand-row,
.section-heading,
.location-row,
.location-copy,
.choice-row,
.category-value,
.decision-button {
  display: flex;
  align-items: center;
}

.brand-row {
  justify-content: space-between;
}

.brand {
  font-size: 28rpx;
  font-weight: 700;
  letter-spacing: 6rpx;
}

.edition {
  color: #747d97;
  font-size: 18rpx;
  letter-spacing: 2rpx;
}

.hero {
  display: flex;
  flex-direction: column;
  margin-top: 54rpx;
}

.product-name {
  color: #5b61d6;
  font-size: 24rpx;
  font-weight: 600;
  letter-spacing: 3rpx;
}

.hero-copy {
  margin-top: 16rpx;
  font-size: 64rpx;
  font-weight: 700;
  letter-spacing: -2rpx;
  line-height: 1.15;
  white-space: pre-line;
}

.hero-note {
  margin-top: 20rpx;
  color: #66708a;
  font-size: 24rpx;
  letter-spacing: 1rpx;
}

.location-row {
  justify-content: space-between;
  margin-top: 52rpx;
  padding: 24rpx 0;
  border-top: 2rpx solid #dde1ec;
  border-bottom: 2rpx solid #dde1ec;
}

.location-copy {
  gap: 20rpx;
}

.location-mark {
  width: 24rpx;
  height: 24rpx;
  border: 6rpx solid #5b61d6;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
}

.location-label,
.location-value {
  display: block;
}

.location-label {
  color: #747d97;
  font-size: 20rpx;
}

.location-value {
  margin-top: 4rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.text-action {
  margin: 0;
  padding: 8rpx 0 8rpx 20rpx;
  background: transparent;
  color: #5b61d6;
  font-size: 22rpx;
  line-height: 1;
}

.choice-section {
  margin-top: 34rpx;
}

.section-heading {
  gap: 14rpx;
}

.section-index {
  color: #5b61d6;
  font-size: 18rpx;
  letter-spacing: 1rpx;
}

.section-title,
.field-label {
  font-size: 24rpx;
  font-weight: 600;
}

.choice-row {
  gap: 14rpx;
  margin-top: 18rpx;
}

.choice-button {
  width: 0;
  min-width: 0;
  flex: 1 1 0;
  margin: 0;
  padding: 19rpx 6rpx;
  border: 2rpx solid #dde1ec;
  border-radius: 12rpx;
  background: transparent;
  color: #49526c;
  font-size: 24rpx;
  line-height: 1;
}

.choice-button--active {
  border-color: #5b61d6;
  background: #e8e9ff;
  color: #343a9f;
  font-weight: 600;
}

.preference-grid {
  display: grid;
  grid-template-columns: 0.82fr 1.18fr;
  gap: 16rpx;
  margin-top: 36rpx;
}

.preference-field {
  min-width: 0;
  padding: 22rpx 24rpx;
  border-radius: 12rpx;
  background: #eef0f8;
}

.field-label {
  display: block;
  color: #66708a;
  font-size: 20rpx;
}

.category-value {
  justify-content: space-between;
  margin-top: 14rpx;
  font-size: 26rpx;
  font-weight: 600;
}

.category-code {
  color: #5b61d6;
  font-size: 17rpx;
  letter-spacing: 1rpx;
}

.dislikes-input {
  width: 100%;
  height: 42rpx;
  margin-top: 7rpx;
  color: #18203a;
  font-size: 25rpx;
}

.dislikes-placeholder {
  color: #9ca3b7;
}

.decision-area {
  margin-top: auto;
  padding-top: 42rpx;
}

.decision-button {
  width: 100%;
  justify-content: space-between;
  margin: 0;
  padding: 28rpx 34rpx;
  border-radius: 12rpx;
  background: #5b61d6;
  color: #ffffff;
  font-size: 30rpx;
  font-weight: 600;
  line-height: 1;
  text-align: left;
}

.decision-arrow {
  font-size: 40rpx;
  font-weight: 400;
}

.decision-caption {
  display: block;
  margin-top: 16rpx;
  color: #8a92a8;
  font-size: 17rpx;
  letter-spacing: 2rpx;
  text-align: center;
}
</style>
