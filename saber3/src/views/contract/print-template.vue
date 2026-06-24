<template>
  <basic-container>
    <div class="print-template-page">
      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="template-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="模板名称">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="请输入模板名称"
              @keyup.enter="handleSearch"
            />
          </el-form-item>
          <el-form-item label="租金类型">
            <el-select v-model="query.rentType" clearable placeholder="全部类型">
              <el-option label="固定租金" value="fixed" />
              <el-option label="浮动租金" value="floating" />
              <el-option label="自定义" value="custom" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleSearch">搜索</el-button>
            <el-button icon="el-icon-delete" @click="handleReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="template-workspace">
        <aside class="template-list">
          <header class="panel-header">
            <span>合同模板</span>
            <small>当前共 {{ filteredTemplates.length }} 份</small>
          </header>

          <div class="upload-card">
            <el-upload
              drag
              action="#"
              :auto-upload="false"
              :show-file-list="false"
              accept=".doc,.docx,.pdf,.html,.htm,.txt"
              :on-change="handleTemplateUpload"
            >
              <div class="upload-text">
                <strong>上传合同模板</strong>
                <span>支持 doc/docx/pdf/html/txt</span>
              </div>
            </el-upload>
          </div>

          <div class="template-items">
            <button
              v-for="item in filteredTemplates"
              :key="item.key"
              type="button"
              class="template-item"
              :class="{ 'is-active': currentTemplate && currentTemplate.key === item.key }"
              @click="selectTemplate(item)"
            >
              <div class="template-item__main">
                <strong>{{ item.name }}</strong>
                <span>{{ item.description }}</span>
              </div>
              <div class="template-item__meta">
                <el-tag :type="item.rentType === 'fixed' ? 'success' : 'warning'" effect="plain">
                  {{ item.rentTypeName }}
                </el-tag>
                <em>{{ item.version }}</em>
              </div>
            </button>

            <el-empty v-if="filteredTemplates.length === 0" description="暂无模板" />
          </div>
        </aside>

        <section class="template-preview">
          <header class="preview-toolbar">
            <div>
              <span>模板预览</span>
              <small>{{ currentTemplate ? currentTemplate.fileName : '请选择模板' }}</small>
            </div>
            <div v-if="currentTemplate" class="preview-actions">
              <el-button type="primary" @click="downloadTemplate(currentTemplate)">下载</el-button>
            </div>
          </header>

          <div v-if="currentTemplate" class="preview-body">
            <iframe
              v-if="canPreview(currentTemplate)"
              class="document-preview"
              :src="previewUrl(currentTemplate)"
              title="合同模板预览"
            ></iframe>
            <div v-else class="preview-empty">
              <strong>当前文件暂不支持直接预览</strong>
              <span>请点击“下载”查看原始合同模板。</span>
            </div>
          </div>

          <el-empty v-else description="请选择左侧合同模板进行预览" />
        </section>
      </section>
    </div>
  </basic-container>
</template>

<script>
const TEMPLATE_BASE = '/系统所需材料/君联合同';

export default {
  name: 'ContractPrintTemplate',
  data() {
    return {
      query: {
        keyword: '',
        rentType: '',
      },
      searchedQuery: {
        keyword: '',
        rentType: '',
      },
      currentKey: 'fixed-rent',
      templates: [
        {
          key: 'fixed-rent',
          name: '科技服务中心租赁合同（固定租金）',
          rentType: 'fixed',
          rentTypeName: '固定租金',
          version: '202508版',
          description: '科技服务中心固定租金合同打印模板。',
          fileName: '科技服务中心租赁合同（固定租金）202508版 - 解锁.docx',
          previewFileName: 'preview/fixed-rent.html',
          previewType: 'html',
        },
        {
          key: 'floating-rent',
          name: '科技服务中心租赁合同（浮动租金）',
          rentType: 'floating',
          rentTypeName: '浮动租金',
          version: '202508版',
          description: '科技服务中心浮动租金合同打印模板。',
          fileName: '科技服务中心租赁合同（浮动租金）202508版 - 解锁.docx',
          previewFileName: 'preview/floating-rent.html',
          previewType: 'html',
        },
      ],
    };
  },
  computed: {
    summaryCards() {
      return [
        { key: 'total', label: '模板总数', value: this.templates.length },
        { key: 'fixed', label: '固定租金模板', value: this.templates.filter(item => item.rentType === 'fixed').length },
        { key: 'floating', label: '浮动租金模板', value: this.templates.filter(item => item.rentType === 'floating').length },
        { key: 'version', label: '当前版本', value: '202508' },
      ];
    },
    filteredTemplates() {
      const keyword = (this.searchedQuery.keyword || '').trim();
      const rentType = this.searchedQuery.rentType;
      return this.templates.filter(item => {
        const keywordMatch = !keyword
          || item.name.includes(keyword)
          || item.rentTypeName.includes(keyword)
          || item.description.includes(keyword);
        const rentTypeMatch = !rentType || item.rentType === rentType;
        return keywordMatch && rentTypeMatch;
      });
    },
    currentTemplate() {
      return this.filteredTemplates.find(item => item.key === this.currentKey) || this.filteredTemplates[0] || null;
    },
  },
  methods: {
    handleSearch() {
      this.searchedQuery = { ...this.query };
      if (!this.currentTemplate && this.filteredTemplates.length) {
        this.currentKey = this.filteredTemplates[0].key;
      }
    },
    handleReset() {
      this.query = {
        keyword: '',
        rentType: '',
      };
      this.searchedQuery = { ...this.query };
      this.currentKey = 'fixed-rent';
    },
    selectTemplate(row) {
      this.currentKey = row.key;
    },
    templateUrl(row) {
      if (row.objectUrl) return row.objectUrl;
      return `${TEMPLATE_BASE}/${encodeURIComponent(row.fileName)}`;
    },
    previewUrl(row) {
      if (row.previewUrl) return row.previewUrl;
      return `${TEMPLATE_BASE}/${row.previewFileName}`;
    },
    canPreview(row) {
      return ['html', 'pdf', 'txt'].includes(row.previewType);
    },
    downloadTemplate(row) {
      const link = document.createElement('a');
      link.href = this.templateUrl(row);
      link.download = row.fileName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },
    handleTemplateUpload(file) {
      const rawFile = file.raw;
      if (!rawFile) return;
      const objectUrl = URL.createObjectURL(rawFile);
      const extension = this.fileExtension(rawFile.name);
      const previewTypeMap = {
        html: 'html',
        htm: 'html',
        pdf: 'pdf',
        txt: 'txt',
      };
      const template = {
        key: `upload-${Date.now()}`,
        name: rawFile.name.replace(/\.[^.]+$/, ''),
        rentType: 'custom',
        rentTypeName: '自定义',
        version: '上传模板',
        description: '本地上传的合同模板。',
        fileName: rawFile.name,
        objectUrl,
        previewUrl: previewTypeMap[extension] ? objectUrl : '',
        previewType: previewTypeMap[extension] || extension,
      };
      this.templates.unshift(template);
      this.currentKey = template.key;
      this.searchedQuery = { keyword: '', rentType: '' };
      this.query = { keyword: '', rentType: '' };
    },
    fileExtension(fileName = '') {
      const match = fileName.toLowerCase().match(/\.([^.]+)$/);
      return match ? match[1] : '';
    },
  },
};
</script>

<style scoped>
.print-template-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.template-search {
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.template-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.template-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.template-search :deep(.el-input),
.template-search :deep(.el-select) {
  width: 188px;
}

.template-search :deep(.el-input__wrapper),
.template-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.template-workspace {
  display: grid;
  grid-template-columns: 420px minmax(0, 1fr);
  gap: 14px;
  min-height: 620px;
}

.template-list,
.template-preview {
  min-width: 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.panel-header,
.preview-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 58px;
  padding: 0 18px;
  border-bottom: 1px solid #edf0f5;
}

.panel-header span,
.preview-toolbar span {
  display: block;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.panel-header small,
.preview-toolbar small {
  display: block;
  margin-top: 4px;
  color: #909399;
  font-size: 12px;
}

.template-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px;
}

.upload-card {
  padding: 14px 14px 0;
}

.upload-card :deep(.el-upload) {
  width: 100%;
}

.upload-card :deep(.el-upload-dragger) {
  width: 100%;
  height: 96px;
  padding: 18px;
  border-radius: 6px;
}

.upload-text {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.upload-text strong {
  color: #1f2937;
  font-size: 14px;
}

.upload-text span {
  color: #909399;
  font-size: 12px;
}

.template-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 14px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
  text-align: left;
  cursor: pointer;
}

.template-item.is-active {
  border-color: #1059c6;
  background: #f4f8ff;
}

.template-item__main {
  min-width: 0;
}

.template-item__main strong {
  display: block;
  color: #1f2937;
  font-size: 14px;
  line-height: 1.45;
}

.template-item__main span {
  display: block;
  margin-top: 6px;
  color: #606266;
  font-size: 13px;
  line-height: 1.5;
}

.template-item__meta {
  display: flex;
  flex-shrink: 0;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.template-item__meta em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.preview-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-body {
  height: calc(100% - 58px);
  padding: 18px;
  overflow: auto;
  background: #f7f8fa;
}

.document-preview {
  width: 100%;
  height: 100%;
  min-height: 720px;
  border: 1px solid #dfe4ec;
  border-radius: 6px;
  background: #fff;
}

.preview-empty {
  display: flex;
  min-height: 320px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fff;
  color: #606266;
}

.preview-empty strong {
  color: #303133;
  font-size: 15px;
}

.preview-empty span {
  font-size: 13px;
}

@media (max-width: 1280px) {
  .template-workspace {
    grid-template-columns: 360px minmax(0, 1fr);
  }
}

@media (max-width: 980px) {
  .template-workspace {
    grid-template-columns: 1fr;
  }
}
</style>
