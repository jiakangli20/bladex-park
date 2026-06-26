<template>
  <nf-form-start ref="form-start" :props="props" />
</template>

<script>
import NfFormStart from '../../../components/nf-form-start/index.vue';
import { Base64 } from 'js-base64';

export default {
  components: {
    NfFormStart,
  },
  watch: {
    '$route.params.params': {
      handler(val) {
        if (val) {
          const param = JSON.parse(Base64.decode(decodeURIComponent(val)));
          const { processId, processDefKey, params } = param;
          this.props = {
            processDefId: processId,
            processDefKey,
            params,
          };
        }
      },
      immediate: true,
    },
  },
  mounted() {
    this.$refs['form-start'].show();
  },
  data() {
    return {
      props: {},
    };
  },
};
</script>

<style lang="scss" scoped></style>
