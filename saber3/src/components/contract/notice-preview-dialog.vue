<template>
  <el-dialog
    v-model="visible"
    :title="title || '审批表预览'"
    width="960px"
    append-to-body
    class="notice-preview-dialog"
    @close="handleClose"
  >
    <div v-loading="loading" class="notice-preview-body">
      <div v-if="html" class="notice-preview-html" v-html="html"></div>
      <el-empty v-else description="暂无预览内容" />
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        type="primary"
        :disabled="!downloadUrl"
        @click="$emit('download')"
      >
        下载原文件
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
export default {
  name: 'NoticePreviewDialog',
  props: {
    modelValue: {
      type: Boolean,
      default: false,
    },
    title: {
      type: String,
      default: '',
    },
    html: {
      type: String,
      default: '',
    },
    loading: {
      type: Boolean,
      default: false,
    },
    downloadUrl: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue', 'download'],
  computed: {
    visible: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      },
    },
  },
  methods: {
    handleClose() {
      this.$emit('update:modelValue', false);
    },
  },
};
</script>

<style lang="scss" scoped>
.notice-preview-body {
  min-height: 320px;
  max-height: 70vh;
  overflow: auto;
  background: #f6f8fb;
  border-radius: 8px;
}

.notice-preview-html {
  min-height: 320px;
}

.notice-preview-html :deep(body) {
  margin: 0;
}
</style>
