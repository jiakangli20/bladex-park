<template>
  <header class="business-page-intro">
    <div class="business-page-intro__content">
      <strong>{{ title }}</strong>
      <span>{{ subtitle }}</span>
    </div>
    <slot />
  </header>
</template>

<script>
export default {
  name: 'BusinessPageIntro',
  props: {
    title: { type: String, required: true },
    subtitle: { type: String, required: true },
  },
};
</script>

<style scoped>
.business-page-intro {
  min-width: 0;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 2px 0;
}

.business-page-intro__content {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.business-page-intro strong {
  color: #303133;
  font-size: 16px;
  font-weight: 600;
  line-height: 24px;
}

.business-page-intro span {
  margin-top: 3px;
  color: #909399;
  font-size: 12px;
  line-height: 18px;
}
</style>
