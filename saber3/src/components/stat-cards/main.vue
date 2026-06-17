<template>
  <section class="stat-cards" :class="{ 'stat-cards--compact': compact }">
    <div v-for="item in items" :key="item.key || item.label" class="stat-cards__item">
      <span class="stat-cards__label">{{ item.label }}</span>
      <strong class="stat-cards__value">{{ item.value }}</strong>
      <em v-if="item.desc" class="stat-cards__desc">{{ item.desc }}</em>
    </div>
  </section>
</template>

<script>
export default {
  name: 'StatCards',
  props: {
    items: {
      type: Array,
      default: () => [],
    },
    compact: {
      type: Boolean,
      default: false,
    },
  },
};
</script>

<style scoped>
.stat-cards {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 12px;
}

.stat-cards__item {
  min-height: 86px;
  padding: 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.stat-cards__label {
  display: block;
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.stat-cards__value {
  display: block;
  margin-top: 8px;
  color: #303133;
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
}

.stat-cards__desc {
  display: block;
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  font-style: normal;
  line-height: 1.4;
}

.stat-cards--compact {
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin-bottom: 12px;
}

.stat-cards--compact .stat-cards__item {
  min-height: 72px;
  padding: 14px 16px;
  border-radius: 6px;
}

.stat-cards--compact .stat-cards__label {
  color: #909399;
  font-size: 12px;
}

.stat-cards--compact .stat-cards__value {
  margin-top: 6px;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

@media (max-width: 1200px) {
  .stat-cards--compact {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .stat-cards--compact {
    grid-template-columns: 1fr;
  }
}
</style>
