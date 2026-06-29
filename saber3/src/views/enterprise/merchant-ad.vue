<template>
  <basic-container>
    <div class="merchant-ad-page">
      <stat-cards :items="summaryCards" />

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="广告标题">
            <el-input v-model="query.adTitle" clearable placeholder="请输入广告标题" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item label="广告位置">
            <el-select v-model="query.adPosition" clearable placeholder="全部位置">
              <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="展示状态">
            <el-select v-model="query.status" clearable placeholder="全部状态">
              <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
            </el-select>
          </el-form-item>
          <el-form-item label="关联商户">
            <el-input v-model="query.merchantName" clearable placeholder="请输入商户名称" @keyup.enter="searchChange" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增广告</el-button>
          <el-button
            v-if="permissionList.delBtn"
            type="danger"
            icon="el-icon-delete"
            plain
            :disabled="selectionList.length === 0"
            @click="handleBatchDelete"
          >
            批量删除
          </el-button>
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="reload" />
        </el-tooltip>
      </section>

      <el-table
        ref="table"
        v-loading="loading"
        :data="data"
        border
        row-key="adId"
        class="contract-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column prop="adTitle" label="广告标题" min-width="170" align="center" show-overflow-tooltip />
        <el-table-column prop="adPosition" label="广告位置" width="130" align="center">
          <template #default="{ row }">{{ positionText(row.adPosition) }}</template>
        </el-table-column>
        <el-table-column prop="coverUrl" label="封面图" width="112" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.coverUrl"
              :src="row.coverUrl"
              :preview-src-list="[row.coverUrl]"
              fit="cover"
              class="ad-cover"
              preview-teleported
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="linkType" label="跳转类型" width="110" align="center">
          <template #default="{ row }">{{ linkTypeText(row.linkType) }}</template>
        </el-table-column>
        <el-table-column label="跳转内容" min-width="180" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ row.merchantName || row.linkUrl || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="76" align="center" />
        <el-table-column prop="startTime" label="展示时间" min-width="210" align="center" show-overflow-tooltip>
          <template #default="{ row }">{{ formatRange(row.startTime, row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="小程序状态" width="116" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210" fixed="right" align="center">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button v-if="permissionList.viewBtn" type="primary" text @click="openView(row)">查看</el-button>
              <el-button v-if="permissionList.editBtn" type="primary" text @click="openEdit(row)">编辑</el-button>
              <el-dropdown v-if="permissionList.statusBtn || permissionList.delBtn" trigger="click">
                <el-button type="primary" text>更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '0'" @click="changeStatus(row, '0')">上架</el-dropdown-item>
                    <el-dropdown-item v-if="permissionList.statusBtn && row.status !== '1'" @click="changeStatus(row, '1')">下架</el-dropdown-item>
                    <el-dropdown-item v-if="permissionList.delBtn" divided @click="removeRow(row)">删除</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
        <el-pagination
          background
          :current-page="page.currentPage"
          :page-sizes="[10, 20, 30, 50]"
          :page-size="page.pageSize"
          layout="total, sizes, prev, pager, next, jumper"
          :total="page.total"
          @size-change="sizeChange"
          @current-change="currentChange"
        />
      </div>
    </div>

    <el-dialog
      v-model="formVisible"
      :title="viewMode ? '查看广告' : form.adId ? '编辑广告' : '新增广告'"
      width="760px"
      append-to-body
      :before-close="closeForm"
    >
      <el-form ref="formRef" :model="form" :rules="rules" :disabled="viewMode" label-width="104px">
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="广告标题" prop="adTitle">
              <el-input v-model="form.adTitle" maxlength="200" show-word-limit placeholder="请输入广告标题" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="广告位置" prop="adPosition">
              <el-select v-model="form.adPosition" placeholder="请选择广告位置" style="width: 100%">
                <el-option v-for="item in positionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="园区ID">
              <el-input-number v-model="form.parkId" :min="1" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序">
              <el-input-number v-model="form.sortOrder" :min="0" :precision="0" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="封面图" prop="coverUrl">
          <div class="cover-upload">
            <el-upload
              action="/api/blade-resource/oss/endpoint/put-file-attach"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleCoverSuccess"
              :disabled="viewMode"
            >
              <el-image v-if="form.coverUrl" :src="form.coverUrl" fit="cover" class="cover-preview" />
              <el-button v-else icon="el-icon-upload">上传封面</el-button>
            </el-upload>
            <el-input v-model="form.coverUrl" maxlength="500" placeholder="也可以直接填写图片地址" />
          </div>
        </el-form-item>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="跳转类型" prop="linkType">
              <el-select v-model="form.linkType" placeholder="请选择跳转类型" style="width: 100%">
                <el-option v-for="item in linkTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="状态" prop="status">
              <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item v-if="form.linkType === 'merchant'" label="关联商户" prop="merchantId">
          <el-select v-model="form.merchantId" filterable clearable placeholder="请选择关联商户" style="width: 100%">
            <el-option v-for="item in merchantOptions" :key="item.merchantId" :label="item.merchantName" :value="item.merchantId" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="form.linkType === 'url'" label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" maxlength="500" placeholder="请输入小程序或 H5 跳转地址" />
        </el-form-item>
        <el-row :gutter="18">
          <el-col :span="12">
            <el-form-item label="开始时间">
              <el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择开始时间" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束时间">
              <el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" placeholder="请选择结束时间" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button icon="el-icon-circle-close" @click="closeForm">取消</el-button>
        <el-button v-if="!viewMode" type="primary" icon="el-icon-circle-check" :loading="submitLoading" @click="submitForm">提交</el-button>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import { getMerchantList } from '@/api/business/merchant';
import {
  addAd,
  changeAdStatus,
  getAdDetail,
  getAdPage,
  getAdStatistics,
  removeAd,
  updateAd,
} from '@/api/business/merchant-ad';

const positionOptions = [
  { value: 'miniapp_home', label: '小程序首页' },
  { value: 'value_service', label: '增值服务' },
  { value: 'merchant_list', label: '商户列表' },
];

const linkTypeOptions = [
  { value: 'none', label: '不跳转' },
  { value: 'merchant', label: '关联商户' },
  { value: 'url', label: '外部链接' },
];

const statusOptions = [
  { value: '0', label: '已上架', type: 'success' },
  { value: '1', label: '已下架', type: 'info' },
];

const defaultForm = () => ({
  adId: null,
  parkId: null,
  adTitle: '',
  adPosition: 'miniapp_home',
  coverUrl: '',
  linkType: 'none',
  linkUrl: '',
  merchantId: null,
  sortOrder: 0,
  startTime: '',
  endTime: '',
  status: '1',
  remark: '',
});

export default {
  name: 'EnterpriseMerchantAd',
  data() {
    const validateMerchant = (rule, value, callback) => {
      if (this.form.linkType === 'merchant' && !value) {
        callback(new Error('请选择关联商户'));
        return;
      }
      callback();
    };
    const validateLinkUrl = (rule, value, callback) => {
      if (this.form.linkType === 'url' && !value) {
        callback(new Error('请输入跳转链接'));
        return;
      }
      callback();
    };
    return {
      query: {},
      loading: false,
      submitLoading: false,
      data: [],
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      summaryCards: [
        { key: 'totalCount', label: '广告总数', value: 0 },
        { key: 'onlineCount', label: '已上架', value: 0 },
        { key: 'offlineCount', label: '已下架', value: 0 },
        { key: 'waitingCount', label: '待展示', value: 0 },
      ],
      positionOptions,
      linkTypeOptions,
      statusOptions,
      merchantOptions: [],
      formVisible: false,
      viewMode: false,
      form: defaultForm(),
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      rules: {
        adTitle: [{ required: true, message: '请输入广告标题', trigger: 'blur' }],
        adPosition: [{ required: true, message: '请选择广告位置', trigger: 'change' }],
        coverUrl: [{ required: true, message: '请上传或填写封面图', trigger: 'blur' }],
        linkType: [{ required: true, message: '请选择跳转类型', trigger: 'change' }],
        merchantId: [{ validator: validateMerchant, trigger: 'change' }],
        linkUrl: [{ validator: validateLinkUrl, trigger: 'blur' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.merchant_service_ad_add, false),
        editBtn: this.validData(this.permission.merchant_service_ad_edit, false),
        delBtn: this.validData(this.permission.merchant_service_ad_delete, false),
        viewBtn: this.validData(this.permission.merchant_service_ad_view, false),
        statusBtn: this.validData(this.permission.merchant_service_ad_status, false),
      };
    },
    ids() {
      return this.selectionList.map(item => item.adId).join(',');
    },
  },
  created() {
    this.loadMerchantOptions();
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    positionText(value) {
      const item = this.positionOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    linkTypeText(value) {
      const item = this.linkTypeOptions.find(option => option.value === value);
      return item ? item.label : value || '-';
    },
    statusText(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.label : '-';
    },
    statusTagType(value) {
      const item = this.statusOptions.find(option => option.value === value);
      return item ? item.type : '';
    },
    formatRange(start, end) {
      if (!start && !end) return '长期展示';
      if (start && end) return `${start} 至 ${end}`;
      return start ? `${start} 起` : `至 ${end}`;
    },
    loadMerchantOptions() {
      getMerchantList({ status: '0' }).then(res => {
        this.merchantOptions = res.data.data || [];
      });
    },
    loadStatistics() {
      getAdStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getAdPage(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const payload = res.data.data || {};
          this.data = payload.records || [];
          this.page.total = Number(payload.total) || 0;
          this.selectionClear();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    searchChange() {
      this.page.currentPage = 1;
      this.loadStatistics();
      this.onLoad(this.page);
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.loadStatistics();
      this.onLoad(this.page);
    },
    reload() {
      this.loadStatistics();
      this.onLoad(this.page);
    },
    sizeChange(size) {
      this.page.pageSize = size;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    currentChange(current) {
      this.page.currentPage = current;
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      if (this.$refs.table) {
        this.$refs.table.clearSelection();
      }
    },
    openAdd() {
      this.viewMode = false;
      this.form = defaultForm();
      this.formVisible = true;
    },
    openEdit(row) {
      this.viewMode = false;
      this.loadDetail(row.adId);
    },
    openView(row) {
      this.viewMode = true;
      this.loadDetail(row.adId);
    },
    loadDetail(adId) {
      getAdDetail(adId).then(res => {
        this.form = {
          ...defaultForm(),
          ...(res.data.data || {}),
        };
        this.formVisible = true;
      });
    },
    closeForm() {
      this.formVisible = false;
      this.viewMode = false;
      this.submitLoading = false;
      this.form = defaultForm();
    },
    submitForm() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const payload = {
          ...this.form,
          merchantId: this.form.linkType === 'merchant' ? this.form.merchantId : null,
          linkUrl: this.form.linkType === 'url' ? this.form.linkUrl : '',
        };
        const request = payload.adId ? updateAd(payload) : addAd(payload);
        request
          .then(() => {
            this.$message.success('操作成功');
            this.closeForm();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    removeRow(row) {
      this.$confirm('确定删除该广告?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removeAd(String(row.adId)).then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
      });
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的广告?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        removeAd(this.ids).then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
      });
    },
    changeStatus(row, status) {
      changeAdStatus(row.adId, status).then(() => {
        this.$message.success(status === '0' ? '广告已上架' : '广告已下架');
        this.loadStatistics();
        this.onLoad(this.page);
      });
    },
    handleCoverSuccess(response) {
      if (!response || !response.success) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      this.form.coverUrl = data.link || data.url || '';
      this.$message.success('上传成功');
    },
  },
};
</script>

<style scoped>
.merchant-ad-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.contract-search,
.contract-toolbar {
  border-radius: 10px;
}

.contract-search {
  padding: 16px 18px 4px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.contract-search :deep(.el-form-item) {
  margin-right: 20px;
  margin-bottom: 12px;
}

.contract-search :deep(.el-input),
.contract-search :deep(.el-select) {
  width: 190px;
}

.contract-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.toolbar-left {
  display: flex;
  gap: 10px;
}

.contract-table {
  width: 100%;
  border-radius: 0;
}

.ad-cover {
  width: 72px;
  height: 44px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
  white-space: nowrap;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.cover-upload {
  display: grid;
  grid-template-columns: 118px 1fr;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.cover-preview {
  width: 108px;
  height: 68px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
}

.merchant-ad-page :deep(.el-button),
.merchant-ad-page :deep(.el-input__wrapper),
.merchant-ad-page :deep(.el-select__wrapper),
.merchant-ad-page :deep(.el-input-number .el-input__wrapper) {
  border-radius: 6px;
}

@media (max-width: 960px) {
  .toolbar-left {
    flex-wrap: wrap;
  }

  .cover-upload {
    grid-template-columns: 1fr;
  }
}
</style>
