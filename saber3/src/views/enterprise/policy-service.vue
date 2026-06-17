<template>
  <basic-container>
    <div class="placeholder-page">
      <h2>政策服务</h2>
      <p>该页面入口已接入企业服务菜单，后续可继续迁移政策发布、政策检索和政策申报服务。</p>
    </div>
  </basic-container>
</template>

<script>
export default {
  name: 'EnterprisePolicyService',
};
</script>

<style scoped>
.placeholder-page {
  min-height: 180px;
  padding: 24px;
  border: 1px solid #d9e2ec;
  border-radius: 6px;
  background: #fff;
}

.placeholder-page h2 {
  margin: 0 0 10px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 600;
}

.placeholder-page p {
  margin: 0;
  color: #64748b;
  font-size: 14px;
  line-height: 1.7;
}
</style>
