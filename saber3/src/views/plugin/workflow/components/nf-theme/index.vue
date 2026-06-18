<template>
  <el-popover popper-class="nf-theme-popover" placement="left" width="auto" trigger="click">
    <el-radio-group v-model="theme">
      <el-row :gutter="15">
        <el-col v-for="(item, index) in themeList" :key="index" :span="8">
          <el-radio :value="item.value">{{ item.label }}</el-radio>
        </el-col>
      </el-row>
    </el-radio-group>
    <template #reference>
      <el-button
        title="主题"
        text
        icon="el-icon-brush"
        style="color: black; font-size: 18px"
      ></el-button
    ></template>
  </el-popover>
</template>

<script>
export default {
  name: 'nf-theme',
  props: {
    modelValue: {
      type: String,
      default: 'default',
    },
    themeList: {
      type: Array,
      default: () => [],
    },
  },
  watch: {
    modelValue: {
      handler(val) {
        this.theme = val;
      },
      immediate: true,
    },
    theme: {
      handler(val) {
        this.$emit('update:modelValue', val);
      },
    },
  },
  data() {
    return {
      theme: '',
    };
  },
};
</script>

<style lang="scss">
.nf-theme-popover {
  .el-radio {
    width: 100%;
    overflow: hidden;
    white-space: nowrap;
    text-overflow: ellipsis;
  }
}
</style>
