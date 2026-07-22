<template>
  <basic-container>
    <div class="tag-page">
      <aside class="tag-sidebar">
        <div class="sidebar-head">
          <span>标签类型</span>
          <el-tooltip content="新增标签类型" placement="top">
            <el-button
              v-if="permissionList.typeAddBtn"
              icon="el-icon-plus"
              circle
              size="small"
              @click="openTypeDialog()"
            />
          </el-tooltip>
        </div>
        <button
          type="button"
          class="type-item"
          :class="{ active: !query.tagType }"
          @click="filterByType()"
        >
          <span>全部标签</span>
          <em>{{ totalCount }}</em>
        </button>
        <button
          v-for="item in tagTypes"
          :key="item.typeId"
          type="button"
          class="type-item"
          :class="{ active: String(query.tagType) === String(item.typeId) }"
          @click="filterByType(item.typeId)"
        >
          <span>{{ item.typeName }}</span>
          <em>{{ item.tagCount || 0 }}</em>
          <el-dropdown
            v-if="permissionList.typeEditBtn || permissionList.typeDelBtn"
            trigger="click"
            class="type-more"
            @click.stop
          >
            <el-button icon="el-icon-more" text circle size="small" @click.stop />
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="permissionList.typeEditBtn" @click="openTypeDialog(item)">
                  编辑
                </el-dropdown-item>
                <el-dropdown-item
                  v-if="permissionList.typeDelBtn"
                  divided
                  @click="confirmDeleteType(item)"
                >
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </button>
      </aside>

      <main class="tag-main">
        <section class="tag-search">
          <el-form :inline="true" :model="query">
            <el-form-item label="标签名称">
              <el-input
                v-model="query.tagName"
                clearable
                placeholder="请输入标签名称"
                @keyup.enter="searchChange"
              />
            </el-form-item>
            <el-form-item label="状态">
              <el-select v-model="query.status" clearable placeholder="全部状态" class="status-filter">
                <el-option label="正常" value="0" />
                <el-option label="停用" value="1" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
              <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="tag-toolbar">
          <div class="toolbar-left">
            <el-button
              v-if="permissionList.addBtn"
              type="primary"
              icon="el-icon-plus"
              @click="handleAdd"
            >
              新增标签
            </el-button>
            <el-button
              v-if="permissionList.delBtn"
              type="danger"
              icon="el-icon-delete"
              :disabled="selectionList.length === 0"
              plain
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
          </div>
          <div class="toolbar-right">
            <el-tooltip content="刷新" placement="top">
              <el-button icon="el-icon-refresh" circle @click="onLoad(page)" />
            </el-tooltip>
          </div>
        </section>

        <el-table
          ref="table"
          v-loading="loading"
          :data="data"
          border
          row-key="tagId"
          class="tag-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="44" align="center" />
          <el-table-column type="index" label="#" width="56" align="center" />
          <el-table-column prop="tagName" label="标签名称" min-width="160" align="center">
            <template #default="{ row }">
              <span class="customer-tag-chip">
                <span class="customer-tag-chip__text">{{ row.tagName }}</span>
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="tagType" label="标签类型" width="150" align="center">
            <template #default="{ row }">
              <el-tag effect="plain">{{ tagTypeText[row.tagType] || '未知' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="sortOrder" label="排序" width="90" align="center" />
          <el-table-column prop="status" label="状态" width="110" align="center">
            <template #default="{ row }">
              <el-switch
                :model-value="row.status === '0'"
                :disabled="!permissionList.editBtn"
                @change="checked => handleStatusChange(row, checked)"
              />
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="180" align="center" />
          <el-table-column label="操作" width="96" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                v-if="permissionList.editBtn"
                type="primary"
                text
                icon="el-icon-edit"
                @click="handleEdit(row)"
              >
                编辑
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="tag-pagination">
          <el-pagination
            background
            :current-page="page.currentPage"
            :page-sizes="[10, 20, 30, 40, 50, 100]"
            :page-size="page.pageSize"
            layout="total, sizes, prev, pager, next, jumper"
            :total="page.total"
            @size-change="sizeChange"
            @current-change="currentChange"
          />
        </div>
      </main>
    </div>

    <el-dialog
      :title="tagDialogTitle"
      v-model="tagDialogVisible"
      width="560px"
      append-to-body
      :before-close="closeTagDialog"
    >
      <el-form ref="tagForm" :model="form" :rules="tagRules" label-width="92px">
        <el-form-item label="标签名称" prop="tagName">
          <el-input v-model="form.tagName" maxlength="50" show-word-limit placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签类型" prop="tagType">
          <el-select v-model="form.tagType" placeholder="请选择标签类型" style="width: 100%">
            <el-option
              v-for="item in tagTypes"
              :key="item.typeId"
              :label="item.typeName"
              :value="item.typeId"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="标签颜色" prop="tagColor">
          <div class="color-field">
            <el-color-picker v-model="form.tagColor" />
            <el-input v-model="form.tagColor" placeholder="#1059C6" />
          </div>
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="form.status" active-value="0" inactive-value="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" icon="el-icon-circle-check" @click="handleSubmit">提交</el-button>
          <el-button icon="el-icon-circle-close" @click="closeTagDialog">取消</el-button>
        </span>
      </template>
    </el-dialog>

    <el-dialog
      :title="typeDialogTitle"
      v-model="typeDialogVisible"
      width="460px"
      append-to-body
      :before-close="closeTypeDialog"
    >
      <el-form ref="typeForm" :model="typeForm" :rules="typeRules" label-width="92px">
        <el-form-item label="类型名称" prop="typeName">
          <el-input v-model="typeForm.typeName" maxlength="50" show-word-limit placeholder="请输入类型名称" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="typeForm.sortOrder" :min="1" :precision="0" controls-position="right" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-switch v-model="typeForm.status" active-value="0" inactive-value="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button type="primary" icon="el-icon-circle-check" @click="handleTypeSubmit">提交</el-button>
          <el-button icon="el-icon-circle-close" @click="closeTypeDialog">取消</el-button>
        </span>
      </template>
    </el-dialog>
  </basic-container>
</template>

<script>
import {
  getTagDetail,
  getTagList,
  getTagTypeList,
  removeTag,
  removeTagType,
  submitTag,
  submitTagType,
} from '@/api/business/tag';
import { mapGetters } from 'vuex';

const defaultTagForm = () => ({
  tagId: undefined,
  tagName: '',
  tagType: undefined,
  parkId: 0,
  tagColor: '#1059C6',
  sortOrder: 1,
  status: '0',
});

const defaultTypeForm = () => ({
  typeId: undefined,
  typeName: '',
  sortOrder: 1,
  status: '0',
});

export default {
  name: 'BusinessTag',
  data() {
    return {
      loading: false,
      query: {},
      page: {
        currentPage: 1,
        pageSize: 10,
        total: 0,
      },
      data: [],
      selectionList: [],
      tagTypes: [],
      tagTypeText: {},
      totalCount: 0,
      tagDialogVisible: false,
      tagDialogTitle: '',
      form: defaultTagForm(),
      typeDialogVisible: false,
      typeDialogTitle: '',
      typeForm: defaultTypeForm(),
      tagRules: {
        tagName: [{ required: true, message: '请输入标签名称', trigger: 'blur' }],
        tagType: [{ required: true, message: '请选择标签类型', trigger: 'change' }],
        sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }],
      },
      typeRules: {
        typeName: [{ required: true, message: '请输入类型名称', trigger: 'blur' }],
        sortOrder: [{ required: true, message: '请输入排序', trigger: 'blur' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.customer_tag_add, false),
        editBtn: this.validData(this.permission.customer_tag_edit, false),
        delBtn: this.validData(this.permission.customer_tag_delete, false),
        typeAddBtn: this.validData(this.permission.customer_tag_type_add, false),
        typeEditBtn: this.validData(this.permission.customer_tag_type_edit, false),
        typeDelBtn: this.validData(this.permission.customer_tag_type_delete, false),
      };
    },
    ids() {
      return this.selectionList.map(item => item.tagId).join(',');
    },
  },
  mounted() {
    this.loadTagTypes();
    this.onLoad(this.page);
  },
  methods: {
    loadTagTypes() {
      return getTagTypeList({})
        .then(res => {
          const rows = res.data.data || [];
          this.tagTypes = rows;
          this.totalCount = rows.reduce((sum, item) => sum + (Number(item.tagCount) || 0), 0);
          this.tagTypeText = rows.reduce((map, item) => {
            map[item.typeId] = item.typeName;
            return map;
          }, {});
        })
        .catch(() => {
          this.tagTypes = [];
          this.totalCount = 0;
          this.tagTypeText = {};
        });
    },
    filterByType(typeId) {
      this.query = {
        ...this.query,
        tagType: typeId,
      };
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    searchChange() {
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    handleAdd() {
      if (this.tagTypes.length === 0) {
        this.$message.warning('请先新增标签类型');
        return;
      }
      this.tagDialogTitle = '新增标签';
      this.form = {
        ...defaultTagForm(),
        tagType: this.query.tagType || this.tagTypes[0].typeId,
      };
      this.tagDialogVisible = true;
      this.clearTagValidate();
    },
    handleEdit(row) {
      this.tagDialogTitle = '编辑标签';
      this.openTagDetail(row.tagId);
    },
    openTagDetail(tagId) {
      this.form = defaultTagForm();
      this.tagDialogVisible = true;
      getTagDetail(tagId).then(res => {
        this.form = {
          ...defaultTagForm(),
          ...(res.data.data || {}),
        };
        this.clearTagValidate();
      });
    },
    handleSubmit() {
      this.$refs.tagForm.validate(valid => {
        if (!valid) return;
        submitTag(this.form).then(() => {
          this.$message.success('操作成功!');
          this.closeTagDialog();
          this.loadTagTypes();
          this.onLoad(this.page);
        });
      });
    },
    handleStatusChange(row, checked) {
      submitTag({
        ...row,
        status: checked ? '0' : '1',
      })
        .then(() => {
          this.$message.success('状态已更新');
          this.onLoad(this.page);
          this.loadTagTypes();
        })
        .catch(() => {
          this.onLoad(this.page);
        });
    },
    handleBatchDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.confirmDeleteTag(this.ids, `确定删除选中的 ${this.selectionList.length} 个标签?`);
    },
    confirmDeleteTag(ids, message) {
      this.$confirm(message, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => this.deleteTag(ids, false));
    },
    deleteTag(ids, force) {
      removeTag(ids, force)
        .then(() => {
          this.$message.success('删除成功');
          this.selectionClear();
          this.loadTagTypes();
          this.onLoad(this.page);
        })
        .catch(error => {
          const message =
            error?.response?.data?.msg || error?.response?.data?.message || error?.message || '';
          if (message === '该标签已有客户关联，确定删除该标签？') {
            this.$confirm(message, {
              confirmButtonText: '强制删除',
              cancelButtonText: '取消',
              type: 'warning',
            }).then(() => this.deleteTag(ids, true));
          }
        });
    },
    openTypeDialog(row) {
      this.typeDialogTitle = row ? '编辑标签类型' : '新增标签类型';
      this.typeForm = row
        ? {
            ...defaultTypeForm(),
            ...row,
          }
        : {
            ...defaultTypeForm(),
            sortOrder: this.tagTypes.length + 1,
          };
      this.typeDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.typeForm && this.$refs.typeForm.clearValidate();
      });
    },
    handleTypeSubmit() {
      this.$refs.typeForm.validate(valid => {
        if (!valid) return;
        submitTagType(this.typeForm).then(() => {
          this.$message.success('操作成功!');
          this.closeTypeDialog();
          this.loadTagTypes();
        });
      });
    },
    confirmDeleteType(row) {
      if (Number(row.tagCount) > 0) {
        this.$message.warning('该分类下存在标签，无法删除，请先清空分类内标签');
        return;
      }
      this.$confirm('确定删除该标签类型?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeTagType(row.typeId))
        .then(() => {
          this.$message.success('删除成功');
          if (String(this.query.tagType) === String(row.typeId)) {
            this.query.tagType = undefined;
          }
          this.loadTagTypes();
          this.onLoad(this.page);
        });
    },
    closeTagDialog(done) {
      this.tagDialogVisible = false;
      this.form = defaultTagForm();
      if (typeof done === 'function') done();
    },
    closeTypeDialog(done) {
      this.typeDialogVisible = false;
      this.typeForm = defaultTypeForm();
      if (typeof done === 'function') done();
    },
    clearTagValidate() {
      this.$nextTick(() => {
        this.$refs.tagForm && this.$refs.tagForm.clearValidate();
      });
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    selectionClear() {
      this.selectionList = [];
      this.$refs.table && this.$refs.table.clearSelection();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.onLoad(this.page);
    },
    onLoad(page) {
      this.loading = true;
      getTagList(page.currentPage, page.pageSize, this.query)
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          this.data = result.records || [];
          this.selectionClear();
        })
        .finally(() => {
          this.loading = false;
        });
    },
  },
};
</script>

<style scoped>
.tag-page {
  display: grid;
  min-height: calc(100vh - 150px);
  grid-template-columns: 252px minmax(0, 1fr);
  gap: 12px;
}

.tag-sidebar,
.tag-main {
  min-width: 0;
  background: #fff;
}

.tag-sidebar {
  padding: 14px 8px;
  border: 1px solid #ebeef5;
}

.sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 8px 12px;
  color: #1f2937;
  font-size: 16px;
  font-weight: 600;
}

.type-item {
  position: relative;
  display: flex;
  width: 100%;
  min-height: 40px;
  align-items: center;
  justify-content: space-between;
  border: 0;
  margin-bottom: 4px;
  padding: 0 36px 0 12px;
  background: transparent;
  color: #4b5563;
  cursor: pointer;
  line-height: 1.3;
  text-align: left;
}

.type-item.active {
  background: #ecf5ff;
  color: #2563eb;
}

.type-item span {
  overflow: hidden;
  flex: 1;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.type-item em {
  flex: 0 0 auto;
  color: #64748b;
  font-size: 12px;
  font-style: normal;
}

.type-more {
  position: absolute;
  right: 2px;
  opacity: 0;
}

.type-item:hover .type-more {
  opacity: 1;
}

.tag-main {
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: transparent;
  border: 0;
}

.tag-search {
  padding: 16px 16px 0;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.tag-search .status-filter {
  width: 168px;
}

.tag-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  border: 0;
  background: transparent;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 8px;
}

.tag-table {
  flex: 1;
}

.tag-table :deep(.el-table__cell) {
  text-align: center;
}

.tag-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.tag-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 0;
}

.color-field {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr);
  gap: 8px;
}

@media (max-width: 992px) {
  .tag-page {
    grid-template-columns: 1fr;
  }

  .tag-sidebar {
    min-height: auto;
  }
}
</style>
