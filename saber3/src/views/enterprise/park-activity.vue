<template>
  <basic-container>
    <div class="park-activity-page">
      <business-page-intro title="园区活动" subtitle="审核企业活动申请，并管理小程序首页活动发布" />
      <section class="activity-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="活动标题"><el-input v-model="query.title" clearable placeholder="请输入活动标题" /></el-form-item>
          <el-form-item label="所属园区"><el-select v-model="query.parkId" clearable filterable placeholder="全部园区"><el-option v-for="park in parkOptions" :key="park.id" :label="park.name" :value="park.id" /></el-select></el-form-item>
          <el-form-item label="审核状态"><el-select v-model="query.auditStatus" clearable placeholder="全部状态"><el-option v-for="item in auditOptions" :key="item.value" :label="item.label" :value="item.value" /></el-select></el-form-item>
          <el-form-item><el-button type="primary" icon="el-icon-search" @click="search">搜索</el-button><el-button icon="el-icon-delete" @click="reset">清空</el-button></el-form-item>
        </el-form>
      </section>
      <section class="activity-table-card">
        <div class="activity-toolbar"><el-button v-if="permissionList.editBtn" type="primary" icon="el-icon-plus" @click="openAdd">新增活动</el-button><el-tooltip content="刷新" placement="top"><el-button icon="el-icon-refresh" circle @click="load" /></el-tooltip></div>
        <el-table v-loading="loading" :data="data" border row-key="id" class="activity-table">
          <el-table-column prop="title" label="活动标题" min-width="180" align="center" show-overflow-tooltip />
          <el-table-column prop="customerName" label="申请企业" min-width="150" align="center" show-overflow-tooltip><template #default="{ row }">{{ row.customerName || '园区后台' }}</template></el-table-column>
          <el-table-column prop="parkId" label="所属园区" width="130" align="center"><template #default="{ row }">{{ parkName(row.parkId) }}</template></el-table-column>
          <el-table-column prop="coverUrl" label="封面" width="100" align="center"><template #default="{ row }"><el-image v-if="row.coverUrl" :src="row.coverUrl" :preview-src-list="[row.coverUrl]" fit="cover" class="activity-cover" preview-teleported /><span v-else>-</span></template></el-table-column>
          <el-table-column label="活动时间" width="320" align="center"><template #default="{ row }"><span class="single-line-cell">{{ row.startTime }} 至 {{ row.endTime }}</span></template></el-table-column>
          <el-table-column prop="auditStatus" label="审核状态" width="110" align="center"><template #default="{ row }"><el-tag :type="auditType(row.auditStatus)" effect="plain">{{ auditText(row.auditStatus) }}</el-tag></template></el-table-column>
          <el-table-column prop="publishStatus" label="发布状态" width="108" align="center"><template #default="{ row }"><el-tag :type="Number(row.publishStatus) === 1 ? 'success' : 'info'" effect="plain">{{ Number(row.publishStatus) === 1 ? '已发布' : '未发布' }}</el-tag></template></el-table-column>
          <el-table-column label="操作" width="156" fixed="right" align="center"><template #default="{ row }"><div class="table-actions"><el-button v-if="permissionList.auditBtn && row.auditStatus === 'PENDING'" type="warning" text @click="openAudit(row)">审核</el-button><el-button v-else-if="permissionList.editBtn" type="primary" text @click="openEdit(row)">编辑</el-button><el-dropdown v-if="(permissionList.publishBtn && row.auditStatus === 'APPROVED') || permissionList.deleteBtn"><el-button type="primary" text>更多</el-button><template #dropdown><el-dropdown-menu><el-dropdown-item v-if="permissionList.publishBtn && row.auditStatus === 'APPROVED'" @click="togglePublish(row)">{{ Number(row.publishStatus) === 1 ? '下架' : '发布' }}</el-dropdown-item><el-dropdown-item v-if="permissionList.deleteBtn" divided @click="remove(row)">删除</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div></template></el-table-column>
        </el-table>
        <div class="activity-pagination"><el-pagination background :current-page="page.currentPage" :page-size="page.pageSize" :page-sizes="[10, 20, 30, 40, 50, 100]" layout="total, sizes, prev, pager, next, jumper" :total="page.total" @size-change="changeSize" @current-change="changePage" /></div>
      </section>
    </div>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑园区活动' : '新增园区活动'" width="760px" append-to-body>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="104px">
        <el-row :gutter="18"><el-col :span="12"><el-form-item label="活动标题" prop="title"><el-input v-model="form.title" maxlength="200" /></el-form-item></el-col><el-col :span="12"><el-form-item label="所属园区" prop="parkId"><el-select v-model="form.parkId" filterable style="width:100%"><el-option v-for="park in parkOptions" :key="park.id" :label="park.name" :value="park.id" /></el-select></el-form-item></el-col></el-row>
        <el-form-item label="活动封面" prop="coverUrl"><div class="cover-upload"><el-upload action="/api/blade-resource/oss/endpoint/put-file-attach" :headers="uploadHeaders" :show-file-list="false" :on-success="handleCoverSuccess" accept="image/jpeg,image/png,image/webp"><el-image v-if="form.coverUrl" :src="form.coverUrl" fit="cover" class="form-cover" /><el-button v-else icon="el-icon-upload">上传封面</el-button></el-upload><el-input v-model="form.coverUrl" maxlength="500" placeholder="也可以填写图片地址" /></div></el-form-item>
        <el-form-item label="活动简介"><el-input v-model="form.summary" type="textarea" :rows="3" maxlength="1000" show-word-limit /></el-form-item>
        <el-row :gutter="18"><el-col :span="12"><el-form-item label="开始时间" prop="startTime"><el-date-picker v-model="form.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col><el-col :span="12"><el-form-item label="结束时间" prop="endTime"><el-date-picker v-model="form.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col></el-row>
        <el-row :gutter="18"><el-col :span="12"><el-form-item label="活动地点"><el-input v-model="form.address" maxlength="500" /></el-form-item></el-col><el-col :span="12"><el-form-item label="费用说明"><el-input v-model="form.priceText" maxlength="100" /></el-form-item></el-col></el-row>
        <el-row :gutter="18"><el-col :span="12"><el-form-item label="联系人"><el-input v-model="form.contactName" maxlength="30" /></el-form-item></el-col><el-col :span="12"><el-form-item label="联系电话"><el-input v-model="form.contactPhone" maxlength="11" /></el-form-item></el-col></el-row>
        <el-row :gutter="18"><el-col :span="12"><el-form-item label="审核状态"><el-tag :type="auditType(form.auditStatus || 'APPROVED')" effect="plain">{{ auditText(form.auditStatus || 'APPROVED') }}</el-tag></el-form-item></el-col><el-col :span="12"><el-form-item label="排序"><el-input-number v-model="form.sortOrder" :min="0" :precision="0" style="width:100%" /></el-form-item></el-col></el-row>
      </el-form><template #footer><el-button @click="formVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submit">提交</el-button></template>
    </el-dialog>
    <el-dialog v-model="auditVisible" title="审核活动申请" width="520px" append-to-body><el-form label-width="84px"><el-form-item label="活动标题">{{ auditRow.title }}</el-form-item><el-form-item label="审核结果"><el-radio-group v-model="auditForm.status"><el-radio-button label="APPROVED">通过</el-radio-button><el-radio-button label="REJECTED">驳回</el-radio-button></el-radio-group></el-form-item><el-form-item label="审核意见"><el-input v-model="auditForm.opinion" type="textarea" :rows="3" maxlength="500" /></el-form-item></el-form><template #footer><el-button @click="auditVisible=false">取消</el-button><el-button type="primary" :loading="saving" @click="submitAudit">确定</el-button></template></el-dialog>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import { getList as getParkList } from '@/api/park/park';
import { auditActivity, getActivityPage, publishActivity, removeActivity, submitActivity } from '@/api/business/park-activity';

const defaultForm = () => ({ id: null, parkId: null, title: '', coverUrl: '', summary: '', startTime: '', endTime: '', address: '', priceText: '免费', contactName: '', contactPhone: '', publishStatus: 0, sortOrder: 0 });
const auditOptions = [{ value: 'DRAFT', label: '草稿', type: 'info' }, { value: 'PENDING', label: '待审核', type: 'primary' }, { value: 'APPROVED', label: '已通过', type: 'success' }, { value: 'REJECTED', label: '已驳回', type: 'danger' }];
export default {
  name: 'EnterpriseParkActivity',
  data() { return { query: {}, data: [], loading: false, saving: false, page: { currentPage: 1, pageSize: 10, total: 0 }, parkOptions: [], auditOptions, formVisible: false, form: defaultForm(), auditVisible: false, auditRow: {}, auditForm: { status: 'APPROVED', opinion: '' }, rules: { title: [{ required: true, message: '请输入活动标题', trigger: 'blur' }], parkId: [{ required: true, message: '请选择园区', trigger: 'change' }], coverUrl: [{ required: true, message: '请上传活动封面', trigger: 'blur' }], startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }], endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }] } }; },
  computed: { ...mapGetters(['permission', 'userInfo']), uploadHeaders() { return { 'Blade-Auth': `bearer ${getToken()}`, 'Blade-Requested-With': 'BladeHttpRequest', 'Tenant-Id': this.userInfo?.tenantId || this.userInfo?.tenant_id || '000000' }; }, permissionList() { const p = this.permission; return { editBtn: Boolean(p.park_activity_edit), auditBtn: Boolean(p.park_activity_audit), publishBtn: Boolean(p.park_activity_publish), deleteBtn: Boolean(p.park_activity_delete) }; } },
  created() { getParkList(1, 999, { status: '0' }).then(res => { this.parkOptions = res.data.data?.records || []; }); this.load(); },
  methods: {
    parkName(id) { return this.parkOptions.find(item => String(item.id) === String(id))?.name || id || '-'; },
    auditText(value) { return this.auditOptions.find(item => item.value === value)?.label || value || '已通过'; },
    auditType(value) { return this.auditOptions.find(item => item.value === value)?.type || 'success'; },
    handleCoverSuccess(response) { if (!response?.success) { this.$message.error(response?.msg || '上传失败'); return; } this.form.coverUrl = response.data?.link || response.data?.url || ''; if (!this.form.coverUrl) this.$message.error('OSS 未返回图片地址'); },
    load() { this.loading = true; getActivityPage(this.page.currentPage, this.page.pageSize, this.query).then(res => { const payload = res.data.data || {}; this.data = payload.records || []; this.page.total = Number(payload.total) || 0; }).finally(() => { this.loading = false; }); },
    search() { this.page.currentPage = 1; this.load(); }, reset() { this.query = {}; this.search(); },
    changeSize(size) { this.page.pageSize = size; this.page.currentPage = 1; this.load(); }, changePage(current) { this.page.currentPage = current; this.load(); },
    openAdd() { this.form = defaultForm(); this.formVisible = true; }, openEdit(row) { this.form = { ...defaultForm(), ...row }; this.formVisible = true; },
    submit() { this.$refs.formRef.validate(valid => { if (!valid) return; if (this.form.endTime <= this.form.startTime) { this.$message.warning('结束时间必须晚于开始时间'); return; } this.saving = true; const enterpriseApplication = Boolean(this.form.id && this.form.customerId); submitActivity(this.form).then(() => { this.$message.success(enterpriseApplication ? '内容已更新，活动已下架并重新进入待审核' : '保存成功'); this.formVisible = false; this.load(); }).finally(() => { this.saving = false; }); }); },
    openAudit(row) { this.auditRow = row; this.auditForm = { status: 'APPROVED', opinion: '' }; this.auditVisible = true; },
    submitAudit() { if (this.auditForm.status === 'REJECTED' && !this.auditForm.opinion.trim()) { this.$message.warning('驳回时请填写审核意见'); return; } this.saving = true; auditActivity(this.auditRow.id, this.auditForm.status, this.auditForm.opinion).then(() => { this.$message.success('审核完成'); this.auditVisible = false; this.load(); }).finally(() => { this.saving = false; }); },
    togglePublish(row) { const status = Number(row.publishStatus) === 1 ? 0 : 1; publishActivity(row.id, status).then(() => { this.$message.success(status ? '已发布' : '已下架'); this.load(); }); },
    remove(row) { this.$confirm('确定删除该活动?').then(() => removeActivity(row.id).then(() => { this.$message.success('删除成功'); this.load(); })); },
  },
};
</script>

<style scoped>
.park-activity-page { display:flex; flex-direction:column; gap:16px; }
.activity-search { padding:16px 18px 4px; border:1px solid #e5e7eb; border-radius:10px; background:#fff; }
.activity-search :deep(.el-form-item) { margin-bottom:12px; }
.activity-table-card { overflow:hidden; border:1px solid #e5e7eb; border-radius:10px; background:#fff; }
.activity-toolbar { min-height:58px; display:flex; align-items:center; justify-content:space-between; padding:12px 16px; }
.activity-cover { width:64px; height:42px; border-radius:6px; }
.cover-upload { width:100%; display:flex; align-items:center; gap:12px; }
.cover-upload :deep(.el-input) { flex:1; }
.form-cover { width:120px; height:72px; border-radius:6px; }
.activity-pagination { display:flex; justify-content:flex-end; padding:16px; }
.table-actions { display:flex; align-items:center; justify-content:center; gap:10px; white-space:nowrap; }
</style>
