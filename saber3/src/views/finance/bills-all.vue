<template>
  <finance-payment scope="all" />
</template>

<script>
import FinancePayment from './payment.vue';

export default {
  name: 'FinanceBillsAll',
  components: {
    FinancePayment,
  },
};
</script>
