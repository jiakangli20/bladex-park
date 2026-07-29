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
      <el-empty v-if="displayPreviewError" :description="displayPreviewError" />
      <div
        v-else-if="previewType === 'docx' && documentBlob"
        class="notice-preview-word"
      >
        <div ref="docxStyle"></div>
        <div ref="docxContainer" class="notice-preview-word-content"></div>
      </div>
      <iframe
        v-else-if="html"
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
        {{ downloadLabel }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script>
import { renderAsync } from 'docx-preview';

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
    downloadLabel: {
      type: String,
      default: '下载Word',
    },
    showPrint: {
      type: Boolean,
      default: false,
    },
    previewType: {
      type: String,
      default: 'html',
    },
    documentBlob: {
      type: Object,
      default: null,
    },
    previewError: {
      type: String,
      default: '',
    },
  },
  emits: ['update:modelValue', 'download', 'print'],
  data() {
    return {
      localPreviewError: '',
      renderVersion: 0,
    };
  },
  computed: {
    visible: {
      get() {
        return this.modelValue;
      },
      set(value) {
        this.$emit('update:modelValue', value);
      },
    },
    displayPreviewError() {
      return this.previewError || this.localPreviewError;
    },
  },
  watch: {
    html: {
      handler() {
        this.scheduleRender();
      },
      immediate: true,
    },
    documentBlob() {
      this.localPreviewError = '';
      this.scheduleRender();
    },
    previewType() {
      this.localPreviewError = '';
      this.scheduleRender();
    },
    visible(value) {
      if (value) {
        this.scheduleRender();
      }
    },
  },
  methods: {
    handleClose() {
      this.$emit('update:modelValue', false);
    },
    scheduleRender() {
      this.$nextTick(() => {
        if (this.previewType === 'docx') {
          this.renderDocx();
          return;
        }
        this.renderFrame();
      });
    },
    async renderDocx() {
      const container = this.$refs.docxContainer;
      if (!container || !this.documentBlob || !this.visible) return;
      const currentVersion = ++this.renderVersion;
      container.innerHTML = '';
      this.localPreviewError = '';
      try {
        await renderAsync(this.documentBlob, container, this.$refs.docxStyle || null, {
          className: 'docx-preview-page',
          inWrapper: true,
          breakPages: true,
          ignoreWidth: false,
          ignoreHeight: false,
          ignoreFonts: false,
          useBase64URL: true,
          renderHeaders: true,
          renderFooters: true,
          renderFootnotes: true,
        });
      } catch (error) {
        if (currentVersion === this.renderVersion) {
          this.localPreviewError = 'Word 内容解析失败，可以下载原文件后查看。';
        }
      }
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

.notice-preview-word {
  box-sizing: border-box;
  width: 100%;
  min-height: 70vh;
  max-height: 70vh;
  padding: 18px;
  overflow: auto;
  background: #eef1f5;
}

.notice-preview-word-content {
  min-width: max-content;
}

:deep(.docx-wrapper) {
  padding: 0;
  background: transparent;
}

:deep(.docx-wrapper > section.docx-preview-page) {
  margin: 0 auto 18px;
  box-shadow: 0 8px 24px rgba(15, 23, 42, 0.12);
}
</style>
