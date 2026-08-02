<template>
  <nf-form-detail ref="form-detail" :props="props" />
</template>

<script>
import NfFormDetail from '../../../components/nf-form-detail/index.vue';
import { Base64 } from 'js-base64';

export default {
  components: {
    NfFormDetail,
  },
  watch: {
    '$route.params.params': {
      handler(val) {
        if (val) {
          const param = JSON.parse(Base64.decode(decodeURIComponent(val)));
          const { taskId, processInsId } = param;
          this.props = {
            taskId,
            processInsId,
          };
        }
      },
      immediate: true,
    },
  },
  mounted() {
    this.$refs['form-detail'].show();
  },
  data() {
    return {
      props: {},
    };
  },
  methods: {},
};
</script>

<style lang="scss" scoped></style>
