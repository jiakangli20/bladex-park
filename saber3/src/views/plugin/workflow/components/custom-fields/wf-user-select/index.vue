<template>
  <div>
    <el-input
      v-model="name"
      :size="size"
      suffix-icon="el-icon-user"
      :placeholder="placeholder || '人员选择'"
      readonly
      :disabled="disabled"
      @click="handleSelect"
    ></el-input>
    <!-- 人员选择弹窗 -->
    <nf-user-select
      ref="user-select"
      :check-type="checkType"
      :default-checked="modelValue"
      @onConfirm="handleUserSelectConfirm"
    ></nf-user-select>
  </div>
</template>
<script>
import { getUser } from '../../../api/process/user';

import NfUserSelect from '../../nf-user-select/index.vue';

export default {
  name: 'user-select',
  components: { NfUserSelect },
  emits: ['update:modelValue'],
  props: {
    modelValue: [String, Number],
    checkType: {
      // radio单选 checkbox多选
      type: String,
      default: () => {
        return 'radio';
      },
    },
    size: {
      type: String,
      default: () => {
        return 'default';
      },
    },
    readonly: {
      type: Boolean,
      default: false,
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    placeholder: String,
    userUrl: {
      type: String,
      default: () => {
        return '/blade-system/search/user';
      },
    },
    change: Function,
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val) {
          const name = [];
          const checks = (val + '').split(',');
          const asyncList = [];
          checks.forEach(c => {
            asyncList.push(getUser(c));
          });
          Promise.all(asyncList).then(res => {
            res.forEach(r => {
              const data = r.data.data;
              if (data) name.push(data.realName);
            });
            this.name = name.join(',');
          });
        } else this.name = '';
      },
      immediate: true,
    },
  },
  data() {
    return {
      name: '',
    };
  },
  methods: {
    handleSelect() {
      if (this.readonly || this.disabled) return;
      else this.$refs['user-select'].visible = true;
    },
    handleUserSelectConfirm(id) {
      this.$emit('update:modelValue', id);
      if (this.change && typeof this.change == 'function') this.change({ value: id });
    },
  },
};
</script>
<style lang="scss"></style>
