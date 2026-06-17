<template>
  <finance-payment scope="overdue" />
</template>

<script>
import FinancePayment from './payment.vue';

export default {
  name: 'FinanceBillsOverdue',
  components: {
    FinancePayment,
  },
};
</script>
