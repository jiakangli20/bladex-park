<template>
  <basic-container>
    <section class="contract-placeholder">
      <h3>合同到期提醒</h3>
    </section>
  </basic-container>
</template>

<script>
export default {
  name: 'ContractExpiring',
};
</script>

<style scoped>
.contract-placeholder {
  min-height: 220px;
  padding: 8px 0;
}

.contract-placeholder h3 {
  margin: 0;
  color: #303133;
  font-size: 18px;
  font-weight: 600;
}
</style>
