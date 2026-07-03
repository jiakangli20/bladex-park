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
      <iframe
        v-if="html"
        ref="previewFrame"
        title="通知文件预览"
        class="notice-preview-frame"
      ></iframe>
      <el-empty v-else description="暂无预览内容" />
    </div>
    <template #footer>
      <el-button @click="visible = false">关闭</el-button>
      <el-button
        v-if="showPrint"
        :disabled="!html || loading"
        @click="$emit('print')"
      >
        打印预览
      </el-button>
      <el-button
        type="primary"
        :disabled="!downloadUrl"
        @click="$emit('download')"
      >
        下载Word
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
    showPrint: {
      type: Boolean,
      default: false,
    },
  },
  emits: ['update:modelValue', 'download', 'print'],
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
  watch: {
    html: {
      handler() {
        this.$nextTick(() => this.renderFrame());
      },
      immediate: true,
    },
    visible(value) {
      if (value) {
        this.$nextTick(() => this.renderFrame());
      }
    },
  },
  methods: {
    handleClose() {
      this.$emit('update:modelValue', false);
    },
    renderFrame() {
      const frame = this.$refs.previewFrame;
      if (!frame || !this.html) return;
      const doc = frame.contentDocument || (frame.contentWindow && frame.contentWindow.document);
      if (!doc) return;
      doc.open();
      doc.write(this.html);
      doc.close();
    },
  },
};
</script>

<style lang="scss" scoped>
.notice-preview-body {
  min-height: 320px;
  max-height: 70vh;
  background: #f6f8fb;
  border-radius: 8px;
}

.notice-preview-frame {
  display: block;
  width: 100%;
  min-height: 70vh;
  border: 0;
  background: #fff;
}
</style>
