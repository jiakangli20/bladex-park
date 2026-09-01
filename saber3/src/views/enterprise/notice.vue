<template>
  <basic-container>
    <div class="notice-page">
      <business-page-intro title="通知公告" subtitle="面向园区企业发布公告、政策、活动与服务通知" />
      <section class="notice-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="公告标题"><el-input v-model="query.title" clearable placeholder="请输入公告标题" /></el-form-item>
          <el-form-item label="公告类型"><el-select v-model="query.category" class="notice-category-select" clearable placeholder="全部类型"><el-option v-for="item in categoryOptions" :key="item.dictKey" :label="item.dictValue" :value="Number(item.dictKey)" /></el-select></el-form-item>
          <el-form-item label="发布日期"><el-date-picker v-model="query.releaseTimeRange" type="daterange" value-format="YYYY-MM-DD HH:mm:ss" range-separator="至" start-placeholder="开始" end-placeholder="结束" /></el-form-item>
          <el-form-item><el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button><el-button icon="el-icon-delete" @click="searchReset">清空</el-button></el-form-item>
        </el-form>
      </section>
      <section class="notice-table-card">
        <div class="notice-toolbar"><div class="toolbar-left"><el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增公告</el-button><el-button v-if="permissionList.delBtn" type="danger" icon="el-icon-delete" plain :disabled="selectionList.length === 0" @click="handleBatchDelete">批量删除</el-button></div><el-tooltip content="刷新"><el-button icon="el-icon-refresh" circle @click="refreshChange" /></el-tooltip></div>
        <el-table v-loading="loading" :data="data" border row-key="id" class="notice-table" @selection-change="selectionChange">
          <el-table-column type="selection" width="44" align="center" />
          <el-table-column prop="title" label="公告标题" min-width="240" align="center" show-overflow-tooltip />
          <el-table-column prop="categoryName" label="公告类型" width="180" align="center"><template #default="{ row }"><el-tag type="primary" effect="plain">{{ row.categoryName || row.category || '-' }}</el-tag></template></el-table-column>
          <el-table-column prop="releaseTime" label="发布日期" width="180" align="center"><template #default="{ row }">{{ dateText(row.releaseTime) }}</template></el-table-column>
          <el-table-column label="首页展示" width="120" align="center"><template #default="{ row }"><el-switch :model-value="Number(row.homeFlag) === 1" :disabled="!permissionList.editBtn" inline-prompt active-text="是" inactive-text="否" @change="toggleHome(row, $event)" /></template></el-table-column>
          <el-table-column label="操作" width="156" fixed="right" align="center"><template #default="{ row }"><div class="table-actions"><el-button v-if="permissionList.viewBtn" type="primary" text @click="openView(row)">查看</el-button><el-button v-if="permissionList.editBtn" type="primary" text @click="openEdit(row)">编辑</el-button></div></template></el-table-column>
        </el-table>
        <div class="notice-pagination"><el-pagination background :current-page="page.currentPage" :page-size="page.pageSize" :page-sizes="page.pageSizes" layout="total, sizes, prev, pager, next, jumper" :total="page.total" @size-change="sizeChange" @current-change="currentChange" /></div>
      </section>
    </div>
    <el-dialog v-model="formVisible" :title="viewMode ? '查看通知公告' : form.id ? '编辑通知公告' : '新增通知公告'" width="950px" class="notice-dialog" append-to-body>
      <el-form :model="form" label-width="90px"><el-form-item label="公告标题"><el-input v-model="form.title" :disabled="viewMode" /></el-form-item><el-row :gutter="18"><el-col :span="12"><el-form-item label="公告类型"><el-select v-model="form.category" :disabled="viewMode" style="width:100%"><el-option v-for="item in categoryOptions" :key="item.dictKey" :label="item.dictValue" :value="Number(item.dictKey)" /></el-select></el-form-item></el-col><el-col :span="12"><el-form-item label="发布日期"><el-date-picker v-model="form.releaseTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" :disabled="viewMode" style="width:100%" /></el-form-item></el-col></el-row><el-form-item label="首页展示"><el-switch v-model="form.homeFlag" :active-value="1" :inactive-value="0" :disabled="viewMode" inline-prompt active-text="是" inactive-text="否" /></el-form-item><el-form-item label="公告内容"><avue-ueditor v-model="form.content" :rows="1.6" action="/blade-resource/oss/endpoint/put-file" :props-http="editorUploadProps" :disabled="viewMode" /></el-form-item></el-form>
      <template #footer><el-button @click="formVisible=false">关闭</el-button><el-button v-if="!viewMode" type="primary" :loading="saving" @click="submitNotice">提交</el-button></template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { getList, update, add, remove, getNotice } from '@/api/desk/notice';
import { getDictionary } from '@/api/system/dict';
import { mapGetters } from 'vuex';

export default {
  data() {
    return {
      form: { homeFlag: 0 },
      query: {},
      loading: true,
      page: {
        pageSize: 10,
        pageSizes: [10, 20, 30, 40, 50, 100],
        currentPage: 1,
        total: 0,
      },
      formVisible: false,
      viewMode: false,
      saving: false,
      categoryOptions: [],
      editorUploadProps: { res: 'data', url: 'link' },
      data: [],
      selectionList: [],
    };
  },
  created() { getDictionary({ code: 'notice' }).then(res => { this.categoryOptions = res.data.data || []; }); this.onLoad(this.page); },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.notice_add, false),
        viewBtn: this.validData(this.permission.notice_view, false),
        delBtn: this.validData(this.permission.notice_delete, false),
        editBtn: this.validData(this.permission.notice_edit, false),
      };
    },
  },
  methods: {
    dateText(value) { return value ? String(value).slice(0, 10) : '-'; },
    openAdd() { this.form = { homeFlag: 0 }; this.viewMode = false; this.formVisible = true; },
    openEdit(row) { this.viewMode = false; getNotice(row.id).then(res => { this.form = { homeFlag: 0, ...(res.data.data || row) }; this.formVisible = true; }); },
    openView(row) { this.viewMode = true; getNotice(row.id).then(res => { this.form = { homeFlag: 0, ...(res.data.data || row) }; this.formVisible = true; }); },
    submitNotice() { if (!this.form.title || !this.form.releaseTime) { this.$message.warning('请填写公告标题和发布日期'); return; } this.saving = true; const action = this.form.id ? update : add; action({ ...this.form, homeFlag: Number(this.form.homeFlag) === 1 ? 1 : 0 }).then(() => { this.$message.success('操作成功'); this.formVisible = false; this.onLoad(this.page); }).finally(() => { this.saving = false; }); },
    selectionChange(selection) { this.selectionList = selection; },
    toggleHome(row, value) { const homeFlag = value ? 1 : 0; update({ ...row, homeFlag }).then(() => { row.homeFlag = homeFlag; this.$message.success(homeFlag ? '已设置为首页公告' : '已取消首页展示'); this.onLoad(this.page, this.query); }); },
    handleBatchDelete() { if (!this.selectionList.length) { this.$message.warning('请选择要删除的公告'); return; } this.$confirm(`确定删除选中的 ${this.selectionList.length} 条公告吗？`, '提示', { type: 'warning' }).then(() => remove(this.selectionList.map(item => item.id).join(','))).then(() => { this.selectionList = []; this.$message.success('删除成功'); this.onLoad(this.page, this.query); }); },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    searchChange(params = this.query, done) {
      this.query = params || {};
      this.page.currentPage = 1;
      this.onLoad(this.page, this.query);
      if (done) done();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page, this.query);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page, this.query);
    },
    refreshChange() {
      this.onLoad(this.page, this.query);
    },
    onLoad(page, params = {}) {
      const { releaseTimeRange } = this.query;
      let values = {
        ...params,
        ...this.query,
      };
      if (releaseTimeRange) {
        values = {
          ...values,
          releaseTime_datege: releaseTimeRange[0],
          releaseTime_datelt: releaseTimeRange[1],
        };
        values.releaseTimeRange = null;
      }
      this.loading = true;
      getList(page.currentPage, page.pageSize, values)
        .then(res => {
          const data = res.data.data || {};
          this.page.total = Number(data.total) || 0;
          this.data = data.records || [];
        })
        .finally(() => { this.loading = false; });
    },
  },
};
</script>

<style scoped>
.notice-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.notice-search { padding: 16px 18px 4px; border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; }
.notice-search :deep(.el-form-item) { margin-bottom: 12px; }
.notice-search :deep(.notice-category-select) { width: 220px; }
.notice-table-card { overflow: hidden; border: 1px solid #e5e7eb; border-radius: 10px; background: #fff; }
.notice-toolbar { min-height: 58px; display: flex; align-items: center; justify-content: space-between; padding: 12px 16px; }
.toolbar-left { display: flex; align-items: center; gap: 10px; }
.notice-pagination { display: flex; justify-content: flex-end; padding: 16px; }
.table-actions { display: flex; align-items: center; justify-content: center; gap: 10px; white-space: nowrap; }
:deep(.notice-dialog) { width: min(950px, calc(100vw - 40px)); margin-top: 8vh; }
:deep(.notice-dialog .el-dialog__body) { max-height: 68vh; overflow-y: auto; padding-bottom: 8px; }
:deep(.notice-dialog .el-form-item) { margin-bottom: 16px; }

@media (max-width: 768px) {
  .notice-search :deep(.el-form-item) { display: flex; margin-right: 0; }
  .notice-search :deep(.el-form-item__content) { min-width: 0; flex: 1; }
  .notice-search :deep(.notice-category-select) { width: 100%; }
}
</style>
