<template>
  <basic-container>
    <div class="customer-page">
      <template v-if="!formVisible">
        <section class="summary-grid">
          <div v-for="item in summaryCards" :key="item.key" class="summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="customer-search">
          <el-form :inline="true" :model="query">
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" clearable placeholder="企业/联系人/电话" @keyup.enter="searchChange" />
            </el-form-item>
            <el-form-item label="所属园区">
              <el-select v-model="query.parkId" clearable placeholder="全部园区">
                <el-option v-for="park in parkList" :key="park.id" :label="park.name" :value="park.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="入驻状态">
              <el-select v-model="query.settlementStatus" clearable placeholder="全部状态">
                <el-option v-for="item in settlementOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="客户状态">
              <el-select v-model="query.status" clearable placeholder="全部状态">
                <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
              <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
            </el-form-item>
          </el-form>
        </section>

        <section class="customer-toolbar">
          <div class="toolbar-left">
            <el-button v-if="permissionList.addBtn" type="primary" icon="el-icon-plus" @click="openAdd">
              新增客户
            </el-button>
            <el-button
              v-if="permissionList.delBtn"
              type="danger"
              plain
              icon="el-icon-delete"
              :disabled="selectionList.length === 0"
              @click="handleBatchDelete"
            >
              批量删除
            </el-button>
            <el-button
              v-if="permissionList.importBtn"
              type="primary"
              plain
              icon="el-icon-upload"
              @click="openImport"
            >
              导入
            </el-button>
            <el-button
              v-if="permissionList.exportBtn"
              type="primary"
              plain
              icon="el-icon-download"
              @click="handleExport"
            >
              导出
            </el-button>
          </div>
          <el-tooltip content="刷新" placement="top">
            <el-button icon="el-icon-refresh" circle @click="reload" />
          </el-tooltip>
        </section>

        <el-table
          v-loading="loading"
          :data="data"
          border
          row-key="customerId"
          class="customer-table"
          @selection-change="selectionChange"
        >
          <el-table-column type="selection" width="48" align="center" />
          <el-table-column prop="enterpriseName" label="企业名称" min-width="220" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <el-button text type="primary" @click="openDetail(row)">{{ row.enterpriseName || '-' }}</el-button>
            </template>
          </el-table-column>
          <el-table-column prop="parkId" label="所属园区" min-width="150" align="center" show-overflow-tooltip>
            <template #default="{ row }">{{ parkName(row.parkId) }}</template>
          </el-table-column>
          <el-table-column prop="contactName" label="联系人" width="120" align="center" />
          <el-table-column prop="contactPhone" label="联系电话" width="140" align="center" />
          <el-table-column prop="settlementStatus" label="入驻状态" width="110" align="center">
            <template #default="{ row }">
              <el-tag :type="settlementType(row.settlementStatus)" effect="plain">
                {{ settlementText(row.settlementStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="客户状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="statusType(row.status)" effect="plain">{{ statusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="riskLevel" label="风险" width="100" align="center">
            <template #default="{ row }">
              <el-tag :type="riskType(row.riskLevel)" effect="plain">{{ riskText(row.riskLevel) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="客户标签" min-width="180" align="center" show-overflow-tooltip>
            <template #default="{ row }">
              <div class="customer-tag-chip-list">
                <span
                  v-for="tag in row.tags || []"
                  :key="tag.tagId"
                  class="customer-tag-chip"
                >
                  <span class="customer-tag-chip__text">{{ tag.tagName }}</span>
                </span>
                <span v-if="!row.tags || row.tags.length === 0" class="muted">未设置</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
          <el-table-column label="操作" width="120" fixed="right" align="center">
            <template #default="{ row }">
              <el-button v-if="permissionList.viewBtn" class="table-action-btn" text type="primary" icon="el-icon-view" @click="openDetail(row)">
                查看
              </el-button>
              <el-button v-if="permissionList.editBtn" class="table-action-btn" text type="primary" icon="el-icon-edit" @click="openEdit(row)">
                编辑
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div class="customer-pagination">
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
      </template>

      <section v-else class="customer-form-page">
        <div class="customer-form-header">
          <div>
            <h3>{{ formTitle }}</h3>
            <span>{{ form.enterpriseName || '客户信息' }}</span>
          </div>
          <div class="customer-form-actions">
            <el-button @click="closeFormPage">返回</el-button>
            <el-button type="primary" :loading="submitLoading" @click="submitForm">保存</el-button>
          </div>
        </div>

        <el-form ref="customerForm" :model="form" :rules="rules" label-width="124px" class="customer-form-card">
          <section class="edit-section">
            <header class="section-head">
              <span>企业基本信息</span>
              <em>用于客户台账、背景调查和行业准入判断</em>
            </header>
            <div class="section-body">
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="企业名称" prop="enterpriseName">
                    <el-input v-model="form.enterpriseName" maxlength="200" placeholder="请输入企业名称">
                      <template #append>
                        <el-button :loading="backgroundLoading" @click.stop="handleBackgroundCheck">
                          背景调查
                        </el-button>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="所属园区" prop="parkId">
                    <el-select v-model="form.parkId" clearable placeholder="请选择所属园区" style="width: 100%">
                      <el-option v-for="park in parkList" :key="park.id" :label="park.name" :value="park.id" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="统一信用代码">
                    <el-input v-model="form.creditCode" maxlength="50" placeholder="请输入统一信用代码" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="成立日期">
                    <el-date-picker
                      v-model="form.establishDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      placeholder="请选择成立日期"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="注册资本(万元)">
                    <el-input-number v-model="form.registeredCapital" :min="0" style="width: 100%" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="企业类型">
                    <el-input v-model="form.enterpriseType" maxlength="100" placeholder="请输入企业类型" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="行业类型">
                    <el-input v-model="form.industry" maxlength="50" placeholder="请输入行业类型">
                      <template #append>
                        <el-button :loading="industryChecking" @click.stop="handleIndustryCheck">
                          检测准入
                        </el-button>
                      </template>
                    </el-input>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-alert
                v-if="industryCheckResult"
                class="industry-check-alert"
                :type="industryCheckAlertType"
                :title="industryCheckMessage"
                :description="industryCheckDescription"
                show-icon
                :closable="false"
              />
              <el-form-item label="经营范围">
                <el-input v-model="form.businessScope" type="textarea" :rows="3" maxlength="2000" />
              </el-form-item>
              <el-form-item label="注册地址">
                <el-input v-model="form.registeredAddress" maxlength="500" />
              </el-form-item>
              <el-form-item label="股权结构">
                <el-input v-model="form.equityStructure" type="textarea" :rows="3" maxlength="1000" />
              </el-form-item>
              <el-form-item label="组织架构">
                <el-input v-model="form.organizationStructure" type="textarea" :rows="3" maxlength="1000" />
              </el-form-item>
              <el-form-item label="客户标签">
                <customer-tag-selector v-model="form.tagIds" />
              </el-form-item>
            </div>
          </section>

          <section class="edit-section">
            <header class="section-head">
              <span>经营状况信息</span>
              <em>用于客户风险持续监控</em>
            </header>
            <div class="section-body">
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="主营业务">
                    <el-input v-model="form.mainBusiness" maxlength="200" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="上年度营收">
                    <el-input-number v-model="form.lastYearRevenue" :min="0" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="主要合作客户">
                <el-input v-model="form.majorClients" type="textarea" :rows="3" maxlength="1000" />
              </el-form-item>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="违法违规">
                    <el-radio-group v-model="form.majorIllegalFlag">
                      <el-radio label="0">否</el-radio>
                      <el-radio label="1">是</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="失信记录">
                    <el-radio-group v-model="form.dishonestFlag">
                      <el-radio label="0">否</el-radio>
                      <el-radio label="1">是</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="行业处罚">
                    <el-radio-group v-model="form.industryPenaltyFlag">
                      <el-radio label="0">否</el-radio>
                      <el-radio label="1">是</el-radio>
                    </el-radio-group>
                  </el-form-item>
                </el-col>
              </el-row>
            </div>
          </section>

          <section class="edit-section">
            <header class="section-head">
              <span>入驻需求信息</span>
              <em>用于后续合同和房源匹配</em>
            </header>
            <div class="section-body">
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="意向载体类型">
                    <el-select v-model="form.carrierTypeArray" multiple placeholder="请选择意向载体类型" style="width: 100%">
                      <el-option v-for="item in carrierTypeOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="意向面积(㎡)">
                    <el-input-number v-model="form.intentArea" :min="0" style="width: 100%" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="使用用途">
                    <el-input v-model="form.usagePurpose" maxlength="200" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="租赁期限(年)">
                    <el-input-number
                      v-model="form.leaseTermYears"
                      :min="0"
                      :precision="2"
                      style="width: 100%"
                    />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="装修要求">
                <el-input v-model="form.decorationRequirement" type="textarea" :rows="3" maxlength="1000" />
              </el-form-item>
              <el-form-item label="配套需求">
                <el-input v-model="form.supportingNeeds" type="textarea" :rows="3" maxlength="1000" />
              </el-form-item>
            </div>
          </section>

          <section class="edit-section">
            <header class="section-head">
              <span>联系人与招商信息</span>
              <em>用于客户日常维护和招商来源追溯</em>
            </header>
            <div class="section-body">
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="联系人" prop="contactName">
                    <el-input v-model="form.contactName" maxlength="100" placeholder="请输入联系人" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="联系电话" prop="contactPhone">
                    <el-input v-model="form.contactPhone" maxlength="20" placeholder="请输入手机号" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="电子邮箱">
                    <el-input v-model="form.contactEmail" maxlength="100" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="职务">
                    <el-input v-model="form.contactPosition" maxlength="100" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="招商渠道">
                    <el-select v-model="form.channel" clearable placeholder="请选择招商渠道" style="width: 100%">
                      <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="第三方渠道">
                    <el-input v-model="form.thirdPartyChannelName" maxlength="100" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="入驻状态">
                    <el-select v-model="form.settlementStatus" placeholder="请选择入驻状态" style="width: 100%">
                      <el-option v-for="item in settlementOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="客户状态">
                    <el-select v-model="form.status" placeholder="请选择客户状态" style="width: 100%">
                      <el-option v-for="item in statusOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="18">
                <el-col :span="12">
                  <el-form-item label="企业规模">
                    <el-input v-model="form.scale" maxlength="20" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="合作等级">
                    <el-select v-model="form.cooperationLevel" placeholder="请选择合作等级" style="width: 100%">
                      <el-option v-for="item in cooperationOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="备注">
                <el-input v-model="form.remark" type="textarea" :rows="3" maxlength="500" />
              </el-form-item>
            </div>
          </section>
        </el-form>
      </section>

      <el-drawer v-model="detailVisible" :title="detailTitle" size="900px" append-to-body>
        <div v-loading="detailLoading" class="customer-detail">
          <div class="detail-header">
            <div class="detail-header-left">
              <h2>{{ current.enterpriseName || '客户详情' }}</h2>
              <span>{{ current.creditCode || '-' }}</span>
            </div>
            <div class="detail-header-right">
              <el-tag :type="statusType(current.status)" effect="light">
                {{ statusText(current.status) }}
              </el-tag>
              <el-tag :type="settlementType(current.settlementStatus)" effect="light">
                {{ settlementText(current.settlementStatus) }}
              </el-tag>
            </div>
          </div>

          <el-divider />

          <el-tabs v-model="activeDetailTab" class="detail-tabs">
            <el-tab-pane label="企业概览" name="overview">
              <el-descriptions title="基本信息" :column="2" border size="small">
                <el-descriptions-item label="企业名称">{{ detailValue(current.enterpriseName) }}</el-descriptions-item>
                <el-descriptions-item label="统一信用代码">{{ detailValue(current.creditCode) }}</el-descriptions-item>
                <el-descriptions-item label="所属行业">{{ detailValue(current.industry) }}</el-descriptions-item>
                <el-descriptions-item label="企业规模">{{ detailValue(current.scale) }}</el-descriptions-item>
                <el-descriptions-item label="联系人">{{ detailValue(current.contactName) }}</el-descriptions-item>
                <el-descriptions-item label="联系电话">{{ detailValue(current.contactPhone) }}</el-descriptions-item>
                <el-descriptions-item label="联系邮箱">{{ detailValue(current.contactEmail) }}</el-descriptions-item>
                <el-descriptions-item label="合作等级">
                  <el-tag v-if="current.cooperationLevel" :type="cooperationType(current.cooperationLevel)" effect="plain">
                    {{ cooperationText(current.cooperationLevel) }}
                  </el-tag>
                  <span v-else>-</span>
                </el-descriptions-item>
                <el-descriptions-item label="企业地址" :span="2">
                  {{ detailValue(current.address || current.registeredAddress) }}
                </el-descriptions-item>
                <el-descriptions-item label="迁入地">{{ detailValue(current.migrationPlace) }}</el-descriptions-item>
                <el-descriptions-item label="迁入原因">{{ detailValue(current.migrationReason) }}</el-descriptions-item>
                <el-descriptions-item label="备注" :span="2">{{ detailValue(current.remark) }}</el-descriptions-item>
              </el-descriptions>

              <el-divider />

              <div class="section-title">客户标签</div>
              <customer-tag-selector
                class="detail-tag-selector"
                :model-value="current.tagIds || []"
                disabled
                empty-text="未设置客户标签"
              />

              <el-divider />

              <div class="section-title">核验与准入状态</div>
              <el-row :gutter="16" class="status-cards">
                <el-col :span="8">
                  <div class="status-card">
                    <div class="status-label">基础信息核验</div>
                    <el-tag :type="verifyType(current.verifyStatus)" effect="light">
                      {{ verifyText(current.verifyStatus) }}
                    </el-tag>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="status-card">
                    <div class="status-label">行业准入</div>
                    <el-tag :type="accessTypeTag(current.industryAccessResult)" effect="light">
                      {{ accessText(current.industryAccessResult) }}
                    </el-tag>
                  </div>
                </el-col>
                <el-col :span="8">
                  <div class="status-card">
                    <div class="status-label">背景调查</div>
                    <el-tag type="primary" effect="light">{{ backgroundTotal }} 条记录</el-tag>
                  </div>
                </el-col>
              </el-row>
            </el-tab-pane>

            <el-tab-pane label="工商信息" name="business">
              <el-alert
                title="工商信息接口预留"
                description="此模块将对接企业工商信息接口，核验企业基本信息的真实性与有效性。接口开通后将自动展示。"
                type="info"
                show-icon
                :closable="false"
                class="detail-alert"
              />
              <el-descriptions title="工商信息（待对接）" :column="2" border size="small">
                <el-descriptions-item label="成立时间">{{ detailValue(current.establishDate) }}</el-descriptions-item>
                <el-descriptions-item label="注册资本">{{ formatMoney(current.registeredCapital) }}</el-descriptions-item>
                <el-descriptions-item label="经营状态">-</el-descriptions-item>
                <el-descriptions-item label="企业类型">{{ detailValue(current.enterpriseType) }}</el-descriptions-item>
                <el-descriptions-item label="法定代表人">-</el-descriptions-item>
                <el-descriptions-item label="注册地址">{{ detailValue(current.registeredAddress || current.address) }}</el-descriptions-item>
                <el-descriptions-item label="经营范围" :span="2">{{ detailValue(current.businessScope) }}</el-descriptions-item>
              </el-descriptions>
            </el-tab-pane>

            <el-tab-pane label="行业准入" name="industry">
              <el-descriptions :column="2" border size="small" class="detail-block">
                <el-descriptions-item label="企业所属行业">{{ detailValue(current.industry) }}</el-descriptions-item>
                <el-descriptions-item label="准入判定结果">
                  <el-tag :type="accessTypeTag(current.industryAccessResult)" effect="plain">
                    {{ accessText(current.industryAccessResult) }}
                  </el-tag>
                </el-descriptions-item>
                <el-descriptions-item label="判定说明" :span="2">
                  {{ detailValue(current.industryAccessReason) }}
                </el-descriptions-item>
              </el-descriptions>

              <div class="section-title">园区行业准入规则</div>
              <el-alert
                title="园区行业准入规则接口预留"
                type="info"
                show-icon
                :closable="false"
                class="detail-alert"
              />
              <el-table :data="industryRuleList" border size="small" empty-text="暂无园区行业准入规则">
                <el-table-column prop="industryKeyword" label="行业关键词" min-width="160" />
                <el-table-column prop="accessType" label="准入类型" width="110" align="center">
                  <template #default="{ row }">
                    <el-tag :type="String(row.accessType) === '3' ? 'danger' : 'warning'" effect="plain">
                      {{ String(row.accessType) === '3' ? '禁入' : '限制' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="reason" label="规则说明" min-width="220" />
                <el-table-column prop="updateTime" label="更新时间" width="170" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="背景调查" name="background">
              <el-alert
                v-if="backgroundNotice"
                :title="backgroundNotice"
                type="info"
                show-icon
                :closable="false"
                class="detail-alert"
              />
              <div v-loading="detailBackgroundLoading">
                <el-tabs v-model="detailBackgroundActiveTab" class="background-tabs">
                  <el-tab-pane :label="`涉诉信息（${backgroundCount('litigationList')}）`" name="litigation">
                    <el-table :data="detailBackgroundData.litigationList" border size="small" empty-text="暂无涉诉记录" class="detail-table">
                      <el-table-column prop="caseNo" label="案号" width="150" />
                      <el-table-column prop="caseReason" label="案由" width="150" />
                      <el-table-column prop="caseType" label="案件类型" width="110" />
                      <el-table-column prop="caseStatus" label="案件状态" width="110" />
                      <el-table-column prop="litigationStatus" label="诉讼地位" width="110" />
                      <el-table-column prop="filingAmount" label="立案标的（元）" width="130" />
                    </el-table>
                  </el-tab-pane>
                  <el-tab-pane :label="`被执行人记录（${backgroundCount('executorList')}）`" name="executor">
                    <el-table :data="detailBackgroundData.executorList" border size="small" empty-text="暂无被执行人记录" class="detail-table">
                      <el-table-column prop="publishDate" label="发布日期" width="120" />
                      <el-table-column prop="court" label="执行法院" width="160" />
                      <el-table-column prop="executionCaseNo" label="执行案号" width="170" />
                      <el-table-column prop="dishonestBehavior" label="失信行为" min-width="220" />
                    </el-table>
                  </el-tab-pane>
                  <el-tab-pane :label="`行政处罚记录（${backgroundCount('penaltyList')}）`" name="penalty">
                    <el-table :data="detailBackgroundData.penaltyList" border size="small" empty-text="暂无行政处罚记录" class="detail-table">
                      <el-table-column prop="penaltyDepartment" label="处罚部门" width="150" />
                      <el-table-column prop="penaltyDecisionNo" label="处罚决定书号" width="170" />
                      <el-table-column prop="penaltyDate" label="处罚日期" width="120" />
                      <el-table-column prop="illegalAct" label="违法行为" width="180" />
                      <el-table-column prop="illegalPeriod" label="违法行为所属期间" width="150" />
                      <el-table-column prop="penaltyContent" label="处罚内容" min-width="220" />
                      <el-table-column prop="updateDate" label="信息更新日期" width="130" />
                    </el-table>
                  </el-tab-pane>
                  <el-tab-pane :label="`股东/高管关联风险记录（${backgroundCount('relatedRiskList')}）`" name="relatedRisk">
                    <el-table :data="detailBackgroundData.relatedRiskList" border size="small" empty-text="暂无股东/高管关联风险记录" class="detail-table">
                      <el-table-column prop="name" label="姓名" width="100" />
                      <el-table-column prop="identity" label="身份" width="120" />
                      <el-table-column prop="riskType" label="关联风险类型" width="150" />
                      <el-table-column prop="riskTime" label="风险发生时间" width="140" />
                      <el-table-column prop="riskSummary" label="风险简要说明" min-width="260" />
                    </el-table>
                  </el-tab-pane>
                </el-tabs>
              </div>
            </el-tab-pane>

            <el-tab-pane label="合同信息" name="contracts">
              <div class="contract-tab-toolbar">
                <el-button
                  v-if="permission.contract_contract_add"
                  type="primary"
                  icon="el-icon-plus"
                  @click="createContract"
                >
                  创建合同
                </el-button>
              </div>
              <el-table :data="contractList" border size="small" empty-text="暂无合同信息" class="detail-table">
                <el-table-column prop="contractNo" label="合同编号" width="150" />
                <el-table-column prop="contractName" label="合同名称" min-width="180" show-overflow-tooltip>
                  <template #default="{ row }">
                    <el-button text type="primary" class="table-link" @click="goContract(row)">
                      {{ row.contractName || row.contractNo || '-' }}
                    </el-button>
                  </template>
                </el-table-column>
                <el-table-column label="合同有效期" width="190">
                  <template #default="{ row }">{{ formatDateRange(row.startDate, row.endDate) }}</template>
                </el-table-column>
                <el-table-column prop="contractStatus" label="合同状态" width="100" align="center">
                  <template #default="{ row }">
                    <el-tag :type="contractStatusType(row.contractStatus)" effect="plain">
                      {{ row.contractStatusName || contractStatusText(row.contractStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column prop="parkName" label="园区名称" width="130" show-overflow-tooltip />
                <el-table-column label="房源信息" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">{{ formatRoomInfo(row) }}</template>
                </el-table-column>
                <el-table-column prop="rentPrice" label="租赁单价" width="100" align="right">
                  <template #default="{ row }">{{ formatMoney(row.rentPrice) }}</template>
                </el-table-column>
                <el-table-column prop="signDate" label="签订日期" width="110" />
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="账单" name="payments">
              <el-table :data="paymentList" border size="small" empty-text="暂无账单" class="detail-table">
                <el-table-column prop="contractNo" label="合同编号" width="150" />
                <el-table-column label="账单编号" width="150">
                  <template #default="{ row }">{{ formatBillNo(row) }}</template>
                </el-table-column>
                <el-table-column prop="feeName" label="费用类型" width="110" />
                <el-table-column label="账期" width="190">
                  <template #default="{ row }">{{ formatDateRange(row.periodStart, row.periodEnd) }}</template>
                </el-table-column>
                <el-table-column prop="amountDue" label="应收（元）" width="110" align="right">
                  <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
                </el-table-column>
                <el-table-column prop="amountPaid" label="实收（元）" width="110" align="right">
                  <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
                </el-table-column>
                <el-table-column prop="payDeadline" label="应缴日期" width="110" />
                <el-table-column prop="payStatus" label="状态" width="90" align="center">
                  <template #default="{ row }">
                    <el-tag :type="payStatusType(row.payStatus)" effect="plain">
                      {{ payStatusText(row.payStatus) }}
                    </el-tag>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>

            <el-tab-pane label="附件资料" name="attachments">
              <div class="attachment-toolbar">
                <el-button type="primary" icon="el-icon-upload" :loading="attachmentUploading" @click="handleAttachmentUpload">
                  上传附件
                </el-button>
                <span class="attachment-tip">支持上传多个附件，不限制数量。附件接口接入后可查看或删除。</span>
              </div>
              <el-table :data="attachmentList" border size="small" empty-text="暂无附件资料" class="detail-table">
                <el-table-column prop="fileName" label="文件名称" min-width="220" show-overflow-tooltip>
                  <template #default="{ row }">
                    <a v-if="row.fileUrl" :href="row.fileUrl" target="_blank">{{ row.fileName || '-' }}</a>
                    <span v-else>{{ row.fileName || '-' }}</span>
                  </template>
                </el-table-column>
                <el-table-column prop="fileSuffix" label="文件类型" width="100" />
                <el-table-column prop="fileSize" label="文件大小" width="110">
                  <template #default="{ row }">{{ formatFileSize(row.fileSize) }}</template>
                </el-table-column>
                <el-table-column prop="createBy" label="上传人" width="100" />
                <el-table-column prop="createTime" label="上传时间" width="160" />
                <el-table-column label="操作" width="120" align="center">
                  <template #default="{ row }">
                    <el-button text type="primary" :disabled="!row.fileUrl" @click="openAttachment(row)">查看</el-button>
                    <el-button text type="danger" @click="handleDeleteAttachment(row)">删除</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-tab-pane>
          </el-tabs>
        </div>
      </el-drawer>

      <el-dialog v-model="tagVisible" title="设置客户标签" width="720px" append-to-body>
        <customer-tag-selector v-model="tagForm.tagIds" />
        <template #footer>
          <el-button @click="tagVisible = false">取消</el-button>
          <el-button type="primary" :loading="tagLoading" @click="submitTags">保存</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="backgroundVisible" title="背景调查" width="520px" append-to-body>
        <el-empty description="未查询到企业数据" />
      </el-dialog>

      <el-dialog
        v-model="importVisible"
        title="客户数据导入"
        width="560px"
        append-to-body
        :before-close="closeImport"
      >
        <div class="import-panel">
          <el-upload
            drag
            action="/blade-park/customer/import"
            name="file"
            accept=".xls,.xlsx"
            :headers="uploadHeaders"
            :limit="1"
            :show-file-list="true"
            :on-success="handleImportSuccess"
            :on-error="handleImportError"
            :before-upload="beforeImportUpload"
          >
            <i class="el-icon-upload"></i>
            <div class="el-upload__text">将 Excel 文件拖到此处，或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">仅支持 .xls / .xlsx 标准模板文件</div>
            </template>
          </el-upload>
          <div class="import-template">
            <span>没有模板？</span>
            <el-button type="primary" plain icon="el-icon-download" @click="handleTemplate">
              下载模板
            </el-button>
          </div>
        </div>
        <template #footer>
          <el-button @click="closeImport">关闭</el-button>
        </template>
      </el-dialog>
    </div>
  </basic-container>
</template>

<script>
import { mapGetters } from 'vuex';
import { ElMessage } from 'element-plus';
import { exportBlob } from '@/api/common';
import {
  addCustomer,
  changeCustomerStatus,
  checkCustomer,
  getCustomerDetail,
  getCustomerList,
  getCustomerStatistics,
  removeCustomer,
  setCustomerTags,
  updateCustomer,
} from '@/api/business/customer';
import { getOpportunityBackgroundByName } from '@/api/business/opportunity';
import { getTagsByCustomer, getTagTypeList } from '@/api/business/tag';
import { getList as getContractList, getPayment as getContractPayment } from '@/api/contract/contract';
import { getList as getParkList } from '@/api/park/park';
import { getToken } from '@/utils/auth';
import { downloadXls } from '@/utils/util';
import CustomerTagSelector from '@/views/business/modules/customer-tag-selector.vue';

const statusOptions = [
  { value: '0', label: '正常', type: 'success' },
  { value: '1', label: '停用', type: 'warning' },
  { value: '2', label: '归档', type: 'info' },
];

const settlementOptions = [
  { value: 0, label: '未入驻', type: 'info' },
  { value: 1, label: '意向', type: 'warning' },
  { value: 2, label: '签约', type: 'primary' },
  { value: 3, label: '入驻', type: 'success' },
];

const cooperationOptions = [
  { value: 1, label: '普通' },
  { value: 2, label: 'VIP' },
  { value: 3, label: '战略' },
];

const createDefaultForm = () => ({
  customerId: null,
  enterpriseName: '',
  creditCode: '',
  parkId: undefined,
  establishDate: '',
  registeredCapital: undefined,
  enterpriseType: '',
  industry: '',
  businessScope: '',
  registeredAddress: '',
  equityStructure: '',
  organizationStructure: '',
  mainBusiness: '',
  lastYearRevenue: undefined,
  majorClients: '',
  majorIllegalFlag: '0',
  dishonestFlag: '0',
  industryPenaltyFlag: '0',
  carrierTypeArray: [],
  carrierTypes: '',
  intentArea: undefined,
  usagePurpose: '',
  leaseTermYears: undefined,
  leaseTermLabel: '',
  decorationRequirement: '',
  supportingNeeds: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  contactPosition: '',
  channel: '',
  thirdPartyChannelName: '',
  address: '',
  settlementStatus: 0,
  status: '0',
  cooperationLevel: 1,
  scale: '',
  remark: '',
  tagIds: [],
});

export default {
  name: 'BusinessCustomer',
  components: {
    CustomerTagSelector,
  },
  data() {
    return {
      loading: false,
      detailLoading: false,
      submitLoading: false,
      tagLoading: false,
      industryChecking: false,
      industryCheckResult: null,
      backgroundLoading: false,
      backgroundVisible: false,
      data: [],
      selectionList: [],
      parkList: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      summaryCards: [
        { key: 'totalCount', label: '客户总数', value: 0 },
        { key: 'settledCount', label: '已入驻', value: 0 },
        { key: 'normalCount', label: '正常客户', value: 0 },
        { key: 'monthNewCount', label: '本月新增', value: 0 },
      ],
      formVisible: false,
      detailVisible: false,
      tagVisible: false,
      importVisible: false,
      activeDetailTab: 'overview',
      current: {},
      customerTags: [],
      tagTypeList: [],
      industryRuleList: [],
      contractList: [],
      paymentList: [],
      attachmentList: [],
      attachmentUploading: false,
      detailBackgroundLoading: false,
      backgroundNotice: '',
      detailBackgroundActiveTab: 'litigation',
      detailBackgroundData: {
        litigationList: [],
        executorList: [],
        penaltyList: [],
        relatedRiskList: [],
      },
      form: createDefaultForm(),
      tagForm: {
        customerId: null,
        tagIds: [],
      },
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        parkId: [{ required: true, message: '请选择所属园区', trigger: 'change' }],
        contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
        contactPhone: [
          { required: true, message: '请输入联系电话', trigger: 'blur' },
          { pattern: /^1[3-9]\d{9}$/, message: '联系电话必须为合法手机号', trigger: 'blur' },
        ],
      },
      statusOptions,
      settlementOptions,
      cooperationOptions,
      carrierTypeOptions: ['写字楼', '厂房', '商铺', '仓库'],
      channelOptions: ['自主拓展', '第三方渠道', '客户主动咨询', '客户小程序申请'],
    };
  },
  computed: {
    ...mapGetters(['permission', 'website']),
    permissionList() {
      return {
        addBtn: this.validData(this.permission.settlement_customer_add, false),
        editBtn: this.validData(this.permission.settlement_customer_edit, false),
        delBtn: this.validData(this.permission.settlement_customer_delete, false),
        viewBtn: this.validData(this.permission.settlement_customer_view, false),
        statusBtn: this.validData(this.permission.settlement_customer_status, false),
        tagBtn: this.validData(this.permission.settlement_customer_tag, false),
        checkBtn: this.validData(this.permission.settlement_customer_check, false),
        importBtn: this.validData(this.permission.settlement_customer_import, false),
        exportBtn: this.validData(this.permission.settlement_customer_export, false),
      };
    },
    uploadHeaders() {
      return {
        [this.website.tokenHeader]: getToken(),
      };
    },
    formTitle() {
      return this.form.customerId ? '编辑客户' : '新增客户';
    },
    detailTitle() {
      return this.current.enterpriseName ? `客户详情 - ${this.current.enterpriseName}` : '客户详情';
    },
    industryCheckAlertType() {
      const type = this.industryCheckResult && this.industryCheckResult.accessType;
      if (String(type) === '1') return 'success';
      if (String(type) === '3') return 'error';
      return 'warning';
    },
    industryCheckMessage() {
      const result = this.industryCheckResult || {};
      return `准入结果：${result.accessText || '未判定'}`;
    },
    industryCheckDescription() {
      const result = this.industryCheckResult || {};
      const parts = [];
      if (result.industryKeyword) {
        parts.push(`命中规则：${result.industryKeyword}`);
      }
      if (result.reason) {
        parts.push(`规则说明：${result.reason}`);
      }
      return parts.join('；') || '请填写行业类型后检测准入结果';
    },
    backgroundTotal() {
      return ['litigationList', 'executorList', 'penaltyList', 'relatedRiskList'].reduce((sum, key) => {
        return sum + this.backgroundCount(key);
      }, 0);
    },
    ids() {
      return this.selectionList.map(item => item.customerId).join(',');
    },
  },
  created() {
    this.loadParkList();
    this.loadStatistics();
    this.onLoad(this.page);
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    defaultForm() {
      return createDefaultForm();
    },
    resetFormState() {
      this.industryCheckResult = null;
      this.backgroundVisible = false;
      this.backgroundLoading = false;
    },
    loadParkList() {
      getParkList(1, 999, {}).then(res => {
        const result = res.data.data || {};
        this.parkList = result.records || [];
      });
    },
    loadStatistics() {
      getCustomerStatistics({ ...this.query }).then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      getCustomerList(page.currentPage, page.pageSize, {
        ...this.query,
        ...params,
      })
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    reload() {
      this.onLoad(this.page);
      this.loadStatistics();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    searchReset() {
      this.query = {};
      this.page.currentPage = 1;
      this.reload();
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.onLoad(this.page);
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.onLoad(this.page);
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    openImport() {
      this.importVisible = true;
    },
    closeImport() {
      this.importVisible = false;
    },
    beforeImportUpload(file) {
      const isExcel = /\.(xls|xlsx)$/i.test(file.name || '');
      if (!isExcel) {
        this.$message.warning('请上传 .xls 或 .xlsx 格式文件');
      }
      return isExcel;
    },
    handleImportSuccess(res) {
      if (res && res.code !== 200) {
        this.$message.error(res.msg || '导入失败');
        return;
      }
      ElMessage.success('导入成功');
      this.closeImport();
      this.reload();
    },
    handleImportError() {
      this.$message.error('导入失败，请检查文件后重试');
    },
    handleExport() {
      exportBlob('/blade-park/customer/export', this.query).then(res => {
        downloadXls(res.data, `客户数据${this.$dayjs().format('YYYY-MM-DD HH:mm:ss')}.xlsx`);
      });
    },
    handleTemplate() {
      exportBlob(`/blade-park/customer/export-template?${this.website.tokenHeader}=${getToken()}`).then(res => {
        downloadXls(res.data, '客户数据模板.xlsx');
      });
    },
    openAdd() {
      this.resetFormState();
      this.form = this.defaultForm();
      this.formVisible = true;
      this.$nextTick(() => {
        if (this.$refs.customerForm) {
          this.$refs.customerForm.clearValidate();
        }
      });
    },
    openEdit(row) {
      this.resetFormState();
      getCustomerDetail(row.customerId).then(res => {
        const detail = res.data.data || {};
        this.form = {
          ...this.defaultForm(),
          ...detail,
          registeredAddress: detail.registeredAddress || detail.address || '',
          majorClients: detail.majorClients || detail.majorCustomers || '',
          majorIllegalFlag: detail.majorIllegalFlag || '0',
          dishonestFlag: detail.dishonestFlag || '0',
          industryPenaltyFlag: detail.industryPenaltyFlag || '0',
          carrierTypeArray: Array.isArray(detail.carrierTypeArray)
            ? detail.carrierTypeArray
            : this.splitCarrierTypes(detail.carrierTypes),
          tagIds: this.resolveTagIds(detail),
        };
        if (this.form.industry) {
          this.handleIndustryCheck(false);
        }
        this.formVisible = true;
        this.$nextTick(() => {
          if (this.$refs.customerForm) {
            this.$refs.customerForm.clearValidate();
          }
        });
      });
    },
    closeFormPage() {
      this.resetFormState();
      this.formVisible = false;
      this.form = this.defaultForm();
    },
    openDetail(row) {
      this.activeDetailTab = 'overview';
      this.current = { ...row };
      this.customerTags = row.tags || [];
      this.tagTypeList = [];
      this.industryRuleList = [];
      this.contractList = [];
      this.paymentList = [];
      this.attachmentList = [];
      this.resetDetailBackground();
      this.detailVisible = true;
      this.detailLoading = true;
      getCustomerDetail(row.customerId)
        .then(res => {
          this.current = res.data.data || {};
          this.customerTags = this.current.tags || this.customerTags;
          this.loadDetailBackground(this.current.enterpriseName);
        })
        .finally(() => {
          this.detailLoading = false;
        });
      this.loadCustomerTags(row.customerId);
      this.loadTagTypes();
      this.loadContracts(row.customerId);
    },
    openTags(row) {
      getCustomerDetail(row.customerId).then(res => {
        const detail = res.data.data || {};
        this.tagForm = {
          customerId: detail.customerId,
          tagIds: this.resolveTagIds(detail),
        };
        this.tagVisible = true;
      });
    },
    submitForm() {
      this.$refs.customerForm.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const action = this.form.customerId ? updateCustomer : addCustomer;
        action(this.normalizePayload(this.form))
          .then(() => {
            this.$message.success('保存成功');
            this.closeFormPage();
            this.reload();
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    submitTags() {
      if (!this.tagForm.customerId) return;
      this.tagLoading = true;
      setCustomerTags(this.tagForm.customerId, this.tagForm.tagIds)
        .then(() => {
          this.$message.success('标签已更新');
          this.tagVisible = false;
          this.reload();
        })
        .finally(() => {
          this.tagLoading = false;
        });
    },
    removeRow(row) {
      this.$confirm(`确定删除客户「${row.enterpriseName}」？`, '提示', { type: 'warning' })
        .then(() => removeCustomer(row.customerId))
        .then(() => {
          this.$message.success('删除成功');
          this.reload();
        });
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的客户?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeCustomer(this.ids))
        .then(() => {
          this.$message.success('删除成功');
          this.selectionList = [];
          this.reload();
        });
    },
    changeStatus(row, status) {
      this.$confirm(`确定将客户状态改为「${this.statusText(status)}」？`, '提示', { type: 'warning' })
        .then(() => changeCustomerStatus(row.customerId, status))
        .then(() => {
          this.$message.success('状态已更新');
          this.reload();
        });
    },
    checkRow(row) {
      checkCustomer(row.customerId).then(() => {
        this.$message.success('核验完成');
        this.reload();
      });
    },
    normalizePayload(row) {
      const carrierTypes = Array.isArray(row.carrierTypeArray)
        ? row.carrierTypeArray.join(',')
        : row.carrierTypes;
      const leaseTermLabel = row.leaseTermYears || row.leaseTermYears === 0
        ? `${row.leaseTermYears}年`
        : row.leaseTermLabel;
      const payload = {
        ...row,
        carrierTypes,
        leaseTermLabel,
        address: row.registeredAddress || row.address,
        creditCode: row.creditCode || null,
        tagIds: Array.isArray(row.tagIds) ? row.tagIds : [],
      };
      delete payload.carrierTypeArray;
      return payload;
    },
    resolveTagIds(detail = {}) {
      if (Array.isArray(detail.tagIds)) {
        return detail.tagIds;
      }
      if (Array.isArray(detail.tags)) {
        return detail.tags.map(tag => tag.tagId || tag.id).filter(Boolean);
      }
      return [];
    },
    splitCarrierTypes(value) {
      return value ? String(value).split(',').map(item => item.trim()).filter(Boolean) : [];
    },
    loadCustomerTags(customerId) {
      if (!customerId) {
        this.customerTags = [];
        return;
      }
      getTagsByCustomer(customerId)
        .then(res => {
          this.customerTags = this.responseList(res);
        })
        .catch(() => {
          this.customerTags = this.current.tags || [];
        });
    },
    loadTagTypes() {
      getTagTypeList({ status: '0' })
        .then(res => {
          this.tagTypeList = this.responseList(res);
        })
        .catch(() => {
          this.tagTypeList = [];
        });
    },
    loadContracts(customerId) {
      if (!customerId) {
        this.contractList = [];
        this.paymentList = [];
        return;
      }
      getContractList(1, 100, { customerId })
        .then(res => {
          this.contractList = this.responsePageRecords(res);
          this.loadPaymentsByContracts(this.contractList);
        })
        .catch(() => {
          this.contractList = [];
          this.paymentList = [];
        });
    },
    loadPaymentsByContracts(contracts) {
      if (!contracts.length) {
        this.paymentList = [];
        return;
      }
      Promise.all(
        contracts.map(contract => {
          return getContractPayment(contract.contractId)
            .then(res => {
              return this.responseList(res).map(item => ({
                ...item,
                contractNo: item.contractNo || contract.contractNo,
                customerName: item.customerName || contract.customerName,
              }));
            })
            .catch(() => []);
        })
      ).then(groups => {
        this.paymentList = groups.reduce((list, group) => list.concat(group), []);
      });
    },
    resetDetailBackground() {
      this.backgroundNotice = '';
      this.detailBackgroundActiveTab = 'litigation';
      this.detailBackgroundData = {
        litigationList: [],
        executorList: [],
        penaltyList: [],
        relatedRiskList: [],
      };
    },
    loadDetailBackground(enterpriseName) {
      const name = (enterpriseName || '').trim();
      if (!name) {
        this.backgroundNotice = '客户缺少企业名称，无法查询背景调查';
        return;
      }
      this.detailBackgroundLoading = true;
      getOpportunityBackgroundByName(name)
        .then(res => {
          const data = res && res.data ? res.data.data || res.data : {};
          if (!data || !data.found) {
            this.resetDetailBackground();
            this.backgroundNotice = '未查询到企业数据';
            return;
          }
          this.backgroundNotice = '';
          this.detailBackgroundData = this.normalizeBackgroundData(data);
        })
        .catch(() => {
          this.resetDetailBackground();
          this.backgroundNotice = '未查询到企业数据';
        })
        .finally(() => {
          this.detailBackgroundLoading = false;
        });
    },
    normalizeBackgroundData(data) {
      return {
        litigationList: Array.isArray(data.litigationList) ? data.litigationList : [],
        executorList: Array.isArray(data.executorList) ? data.executorList : [],
        penaltyList: Array.isArray(data.penaltyList) ? data.penaltyList : [],
        relatedRiskList: Array.isArray(data.relatedRiskList) ? data.relatedRiskList : [],
      };
    },
    backgroundCount(key) {
      const list = this.detailBackgroundData && this.detailBackgroundData[key];
      return Array.isArray(list) ? list.length : 0;
    },
    responseList(res) {
      const data = res && res.data ? res.data.data || res.data : res;
      if (Array.isArray(data)) return data;
      if (data && Array.isArray(data.records)) return data.records;
      if (data && Array.isArray(data.rows)) return data.rows;
      if (res && Array.isArray(res.rows)) return res.rows;
      return [];
    },
    responsePageRecords(res) {
      const data = res && res.data ? res.data.data || res.data : res;
      return (data && data.records) || (data && data.rows) || [];
    },
    getCustomerTagsByType(typeId) {
      return this.customerTags.filter(tag => String(tag.tagType) === String(typeId));
    },
    detailValue(value) {
      return value === null || value === undefined || value === '' ? '-' : value;
    },
    formatMoney(value) {
      if (value === null || value === undefined || value === '') return '-';
      const amount = Number(value);
      return Number.isNaN(amount) ? '-' : amount.toFixed(2);
    },
    formatDateRange(start, end) {
      if (!start && !end) return '-';
      return `${start || '-'} ~ ${end || '-'}`;
    },
    formatRoomInfo(row = {}) {
      return [row.buildingName, row.floorName, row.roomName].filter(Boolean).join(' / ') || '-';
    },
    formatBillNo(row = {}) {
      return row.billNo || row.paymentNo || (row.paymentId ? `ZD${row.paymentId}` : '-');
    },
    formatFileSize(value) {
      const size = Number(value);
      if (!size) return '-';
      if (size < 1024) return `${size} B`;
      if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
      return `${(size / 1024 / 1024).toFixed(1)} MB`;
    },
    handleAttachmentUpload() {
      this.$message.info('附件接口暂未接入');
    },
    openAttachment(row) {
      if (row.fileUrl) {
        window.open(row.fileUrl, '_blank');
      }
    },
    handleDeleteAttachment() {
      this.$message.info('附件接口暂未接入');
    },
    createContract() {
      if (!this.current || !this.current.customerId) {
        this.$message.warning('请先选择客户');
        return;
      }
      this.$router.push({
        path: '/contract/create-template',
        query: {
          mode: 'create',
          customerId: this.current.customerId,
        },
      });
    },
    goContract(row) {
      this.$router.push({
        path: '/contract/contract',
        query: {
          contractId: row.contractId,
          customerId: row.customerId || this.current.customerId,
        },
      });
    },
    handleBackgroundCheck() {
      const enterpriseName = (this.form.enterpriseName || '').trim();
      if (!enterpriseName) {
        this.$message.warning('请先填写企业名称');
        return;
      }
      this.backgroundLoading = true;
      this.backgroundVisible = true;
      this.$nextTick(() => {
        this.backgroundLoading = false;
      });
    },
    handleIndustryCheck(showMessage = true) {
      const shouldMessage = showMessage !== false;
      const industry = (this.form.industry || '').trim();
      if (!industry) {
        this.industryCheckResult = {
          accessType: '0',
          accessText: '未判定',
          reason: '请先填写行业类型',
        };
        if (shouldMessage) {
          this.$message.warning('请先填写行业类型');
        }
        return;
      }
      this.industryChecking = true;
      const blockedKeyword = ['失信', '被执行', '涉诉', '处罚', '禁入'].find(keyword => industry.includes(keyword));
      this.industryCheckResult = blockedKeyword
        ? {
            accessType: '3',
            accessText: '禁入',
            industryKeyword: blockedKeyword,
            reason: '命中本地风险关键词，建议转入人工复核',
          }
        : {
            accessType: '1',
            accessText: '通过',
            industryKeyword: industry,
            reason: '本地准入规则通过，最终结果以客户核验为准',
          };
      this.industryChecking = false;
      if (shouldMessage) {
        this.$message.success('行业准入检测完成');
      }
    },
    parkName(parkId) {
      const park = this.parkList.find(item => String(item.id) === String(parkId));
      return park ? park.name : parkId || '-';
    },
    settlementText(value) {
      const item = this.settlementOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    settlementType(value) {
      const item = this.settlementOptions.find(option => String(option.value) === String(value));
      return item ? item.type : 'info';
    },
    statusText(value) {
      const item = this.statusOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    statusType(value) {
      const item = this.statusOptions.find(option => String(option.value) === String(value));
      return item ? item.type : 'info';
    },
    cooperationText(value) {
      const item = this.cooperationOptions.find(option => String(option.value) === String(value));
      return item ? item.label : '-';
    },
    cooperationType(value) {
      const map = {
        1: 'info',
        2: 'warning',
        3: 'primary',
      };
      return map[String(value)] || 'info';
    },
    verifyText(value) {
      const map = {
        0: '未核验',
        1: '通过',
        2: '不通过',
      };
      return map[String(value)] || '-';
    },
    verifyType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger',
      };
      return map[String(value)] || 'info';
    },
    accessText(value) {
      const map = {
        0: '未判定',
        1: '通过',
        2: '限制',
        3: '禁入',
      };
      return map[String(value)] || '-';
    },
    accessTypeTag(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'danger',
      };
      return map[String(value)] || 'info';
    },
    contractStatusText(value) {
      const map = {
        0: '待签约',
        1: '履约中',
        2: '已到期',
        3: '已续签',
        4: '已终止',
      };
      return map[String(value)] || '-';
    },
    contractStatusType(value) {
      const map = {
        0: 'primary',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
      };
      return map[String(value)] || 'info';
    },
    payStatusText(value) {
      const map = {
        0: '未缴',
        1: '已缴',
        2: '逾期',
        3: '部分缴纳',
      };
      return map[String(value)] || '-';
    },
    payStatusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'danger',
        3: 'warning',
      };
      return map[String(value)] || 'info';
    },
    riskText(value) {
      const map = {
        0: '未排查',
        1: '低',
        2: '中',
        3: '高',
      };
      return map[String(value)] || '-';
    },
    riskType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'danger',
      };
      return map[String(value)] || 'info';
    },
  },
};
</script>

<style lang="scss" scoped>
.customer-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.customer-search {
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.customer-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.customer-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.customer-search :deep(.el-input),
.customer-search :deep(.el-select) {
  width: 168px;
}

.customer-search :deep(.el-input__wrapper),
.customer-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.customer-search :deep(.el-button) {
  height: 36px;
  padding: 0 18px;
}

.customer-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0;
  border: 0;
  background: transparent;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.customer-table {
  width: 100%;
}

.customer-table :deep(.el-table__cell) {
  text-align: center;
}

.customer-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.table-action-btn {
  padding: 0 2px;
}

.table-action-btn + .table-action-btn {
  margin-left: 8px;
}

.customer-pagination {
  display: flex;
  justify-content: flex-end;
}

.customer-form-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.customer-form-header {
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.customer-form-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 18px;
}

.customer-form-header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.customer-form-header span {
  display: block;
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.customer-form-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.customer-form-card {
  padding: 0;
  border: 0;
  background: transparent;
}

.edit-section {
  overflow: hidden;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #fff;
}

.edit-section:last-child {
  margin-bottom: 0;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 15px 20px;
  border-bottom: 1px solid #edf0f5;
}

.section-head span {
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
  line-height: 1.4;
}

.section-head em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
  font-weight: 400;
  line-height: 1.4;
}

.section-body {
  padding: 20px 22px 4px;
}

.industry-check-alert {
  margin-bottom: 18px;
}

.customer-form-card :deep(.el-form-item) {
  margin-bottom: 18px;
}

.customer-form-card :deep(.el-input-group__append) {
  padding: 0;
}

.customer-form-card :deep(.el-input-group__append .el-button) {
  min-width: 92px;
  border: 0;
}

.muted {
  color: #909399;
}

.customer-detail {
  padding: 0 2px 16px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.detail-header-left h2 {
  margin: 0 0 4px;
  color: #1f2937;
  font-size: 20px;
  font-weight: 600;
  line-height: 1.35;
}

.detail-header-left span {
  color: #909399;
  font-size: 13px;
}

.detail-header-right {
  display: flex;
  flex-shrink: 0;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.detail-tabs :deep(.el-tabs__header) {
  margin-bottom: 16px;
}

.section-title {
  margin-bottom: 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.detail-tag-selector {
  padding: 4px 0 0;
}

.empty-inline {
  color: #b8bcc5;
  font-size: 13px;
}

.status-cards {
  margin-bottom: 16px;
}

.status-card {
  display: flex;
  min-height: 86px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.status-label {
  color: #909399;
  font-size: 12px;
}

.detail-alert {
  margin-bottom: 16px;
}

.detail-block {
  margin-bottom: 16px;
}

.background-tabs {
  min-height: 220px;
}

.detail-table {
  width: 100%;
}

.contract-tab-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 12px;
}

.detail-table :deep(.el-table__body-wrapper),
.detail-table :deep(.el-table__header-wrapper) {
  overflow-x: auto;
}

.table-link {
  padding: 0;
  font-weight: 400;
}

.attachment-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

.attachment-tip {
  color: #909399;
  font-size: 13px;
}

.import-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-panel :deep(.el-upload),
.import-panel :deep(.el-upload-dragger) {
  width: 100%;
}

.import-template {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #f8fafc;
  color: #606266;
  font-size: 13px;
}

</style>
