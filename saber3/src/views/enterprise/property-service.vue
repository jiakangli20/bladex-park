<template>
  <basic-container>
    <div class="entry-page">
      <div class="entry-head">
        <h2>物业服务</h2>
        <p>统一维护物业服务配置，并集中处理企业服务工单。</p>
      </div>
      <div class="entry-grid">
        <button type="button" class="entry-card" @click="go('/enterprise/property-service-config')">
          <span class="entry-icon">配</span>
          <strong>物业服务配置</strong>
          <em>维护服务目录、服务类型、收费标准与启停状态</em>
        </button>
        <button type="button" class="entry-card" @click="go('/enterprise/property-workorder')">
          <span class="entry-icon">单</span>
          <strong>工单处理</strong>
          <em>录入工单、跟进处置、评价与关闭服务工单</em>
        </button>
      </div>
    </div>
  </basic-container>
</template>

<script>
export default {
  name: 'EnterprisePropertyService',
  methods: {
    go(path) {
      this.$router.push({ path });
    },
  },
};
</script>

<style scoped>
.entry-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.entry-head h2 {
  margin: 0 0 6px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.entry-head p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
}

.entry-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 14px;
}

.entry-card {
  min-height: 120px;
  padding: 18px;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: #fff;
  color: #1f2937;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;
}

.entry-card:hover {
  border-color: #2563eb;
  box-shadow: 0 8px 20px rgba(37, 99, 235, 0.08);
}

.entry-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  margin-bottom: 14px;
  border-radius: 6px;
  background: #e8f1ff;
  color: #1d4ed8;
  font-weight: 600;
}

.entry-card strong {
  display: block;
  margin-bottom: 8px;
  font-size: 16px;
}

.entry-card em {
  display: block;
  color: #64748b;
  font-size: 13px;
  font-style: normal;
  line-height: 1.6;
}
</style>
