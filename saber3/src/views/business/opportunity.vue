<template>
  <basic-container>
    <div class="opportunity-page">
      <template v-if="!followPageVisible">
        <section class="summary-grid">
          <div v-for="item in summaryCards" :key="item.key" class="summary-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
          </div>
        </section>

        <section class="opportunity-search">
          <el-form :inline="true" :model="query">
          <el-form-item label="关键词">
            <el-input
              v-model="query.keyword"
              clearable
              placeholder="企业名/联系人"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="联系方式">
            <el-input
              v-model="query.contactKeyword"
              clearable
              placeholder="联系人/电话"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="query.opportunityStatus" clearable placeholder="全部状态">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="招商渠道">
            <el-select v-model="query.channel" clearable placeholder="全部渠道">
              <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item label="租期">
            <el-select v-model="query.leaseTermLabel" clearable placeholder="全部租期">
              <el-option v-for="item in leaseTermOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">搜索</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">清空</el-button>
          </el-form-item>
          </el-form>
        </section>

        <section class="opportunity-toolbar">
        <div class="toolbar-left">
          <el-button
            v-if="permissionList.addBtn"
            type="primary"
            icon="el-icon-plus"
            @click="handleAdd"
          >
            新增商机
          </el-button>
          <el-button
            type="primary"
            plain
            icon="el-icon-setting"
            @click="openIndustryRuleDialog"
          >
            行业准入规则
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
        </div>
        <el-tooltip content="刷新" placement="top">
          <el-button icon="el-icon-refresh" circle @click="onLoad(page)" />
        </el-tooltip>
        </section>

        <el-table
        v-loading="loading"
        :data="data"
        border
        row-key="opportunityId"
        class="opportunity-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="48" align="center" />
        <el-table-column
          prop="opportunityNo"
          label="商机编号"
          width="200"
          align="center"
          class-name="opportunity-no-column"
          show-overflow-tooltip
        />
        <el-table-column
          prop="enterpriseName"
          label="企业名称"
          min-width="230"
          align="center"
          class-name="enterprise-name-column"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-button
              v-if="permissionList.viewBtn"
              class="enterprise-link"
              text
              type="primary"
              @click="handleView(row)"
            >
              {{ row.enterpriseName || '-' }}
            </el-button>
            <span v-else>{{ row.enterpriseName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="背景调查" width="110" align="center">
          <template #default="{ row }">
            <el-button class="table-link" text type="primary" @click="handleBackgroundPlaceholder(row)">
              查看
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="opportunityStatus" label="商机状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTypeMap[row.opportunityStatus] || ''" effect="plain">
              {{ statusTextMap[row.opportunityStatus] || row.opportunityStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="客户标签" min-width="160" align="center">
          <template #default="{ row }">
            <div v-if="formatTags(row).length" class="customer-tag-chip-list">
              <span
                v-for="tag in formatTags(row)"
                :key="tag.tagId || tag.tagName"
                class="customer-tag-chip"
              >
                <span class="customer-tag-chip__text">{{ tag.tagName }}</span>
              </span>
            </div>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="contactName" label="联系人姓名" width="120" align="center" />
        <el-table-column prop="contactPhone" label="联系电话" width="135" align="center" />
        <el-table-column prop="channel" label="招商渠道" width="115" align="center" />
        <el-table-column prop="carrierTypes" label="意向载体类型" width="140" align="center">
          <template #default="{ row }">{{ formatCarrierTypes(row.carrierTypes) }}</template>
        </el-table-column>
        <el-table-column prop="intentArea" label="意向租赁面积(㎡)" width="150" align="center" />
        <el-table-column prop="leaseTermLabel" label="意向租赁期限" width="130" align="center" />
        <el-table-column prop="followUser" label="跟进人" width="110" align="center" />
        <el-table-column prop="createTime" label="创建时间" width="170" align="center" />
        <el-table-column prop="lastFollowTime" label="跟进时间" width="170" align="center" />
        <el-table-column label="操作" width="96" fixed="right" align="center">
          <template #default="{ row }">
            <el-button
              v-if="permissionList.followBtn"
              text
              type="primary"
              icon="el-icon-chat-line-square"
              @click="openFollow(row)"
            >
              跟进
            </el-button>
          </template>
        </el-table-column>
        </el-table>

        <div class="opportunity-pagination">
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

      <section v-else class="follow-page">
        <div class="follow-page__header">
          <div>
            <h3>商机跟进</h3>
            <span>{{ form.enterpriseName || '-' }}</span>
          </div>
          <div class="follow-page__actions">
            <el-button @click="closeFollowPage">返回</el-button>
            <el-button type="primary" :loading="followLoading" @click="handleSaveFollowPage">
              保存跟进
            </el-button>
          </div>
        </div>

        <el-form
          ref="followPageFormRef"
          :model="form"
          :rules="rules"
          label-width="118px"
          class="opportunity-form follow-page__form"
        >
          <section class="form-section">
            <header>跟进信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="跟进人" prop="followUserId">
                  <el-select
                    v-model="form.followUserId"
                    filterable
                    clearable
                    placeholder="请选择用户账号"
                    style="width: 100%"
                    @change="handleFollowUserChange"
                  >
                    <el-option
                      v-for="user in userOptions"
                      :key="userId(user)"
                      :label="userLabel(user)"
                      :value="userId(user)"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商机状态" prop="opportunityStatus">
                  <el-select v-model="form.opportunityStatus" style="width: 100%">
                    <el-option
                      v-for="item in statusOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="16">
                <el-form-item label="跟进内容">
                  <el-input
                    v-model="followForm.followContent"
                    type="textarea"
                    :rows="4"
                    placeholder="请输入本次跟进内容"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item v-if="form.opportunityStatus !== 'DEAL'" label="下次跟进">
                  <el-date-picker
                    v-model="followForm.nextFollowTime"
                    type="datetime"
                    value-format="YYYY-MM-DD HH:mm:ss"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section follow-record-section">
            <header>跟进记录</header>
            <el-timeline v-if="followList.length" class="follow-record-timeline">
              <el-timeline-item
                v-for="item in followList"
                :key="item.followId || item.createTime"
                hide-timestamp
              >
                <div class="timeline-title follow-record-meta">
                  {{ item.followUser || '-' }} ·
                  {{ statusTextMap[item.opportunityStatus] || item.opportunityStatus || '-' }}
                  <span v-if="item.nextFollowTime"> · 下次跟进：{{ item.nextFollowTime }}</span>
                  <span v-else-if="item.followTime"> · 跟进时间：{{ item.followTime }}</span>
                </div>
                <div class="timeline-text">{{ item.followContent || '-' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无跟进记录" />
          </section>

          <section class="form-section">
            <header>企业基本信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业名称" prop="enterpriseName">
                  <el-input v-model="form.enterpriseName" placeholder="请输入企业名称">
                    <template #append>
                      <el-button :loading="backgroundLoading" @click.stop="handleBackgroundCheck">
                        背景调查
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="统一信用代码" prop="creditCode">
                  <el-input v-model="form.creditCode" placeholder="请输入统一信用代码" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="成立日期">
                  <el-date-picker
                    v-model="form.establishDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="注册资本">
                  <el-input-number v-model="form.registeredCapital" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业类型">
                  <el-input v-model="form.enterpriseType" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="行业类型">
                  <el-input v-model="form.industryType" placeholder="请输入行业类型">
                    <template #append>
                      <div class="industry-input-actions">
                        <el-button :loading="industryChecking" @click.stop="handleIndustryCheck">
                          行业检测
                        </el-button>
                        <el-button
                          v-if="permissionList.industryRuleBtn"
                          @click.stop="openIndustryRuleDialog"
                        >
                          规则配置
                        </el-button>
                      </div>
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
              <el-input v-model="form.businessScope" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="注册地址">
              <el-input v-model="form.registeredAddress" />
            </el-form-item>
            <el-form-item label="客户标签">
              <customer-tag-selector v-model="form.tagIds" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>经营状况信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="主营业务">
                  <el-input v-model="form.mainBusiness" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上年度营收">
                  <el-input-number v-model="form.lastYearRevenue" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="主要合作客户">
              <el-input v-model="form.majorClients" type="textarea" :rows="2" />
            </el-form-item>
            <el-row :gutter="18">
              <el-col :span="8">
                <el-form-item label="违法违规">
                  <el-radio-group v-model="form.majorIllegalFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="失信记录">
                  <el-radio-group v-model="form.dishonestFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="行业处罚">
                  <el-radio-group v-model="form.industryPenaltyFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section">
            <header>入驻需求信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="意向载体" prop="carrierTypeArray">
                  <el-select v-model="form.carrierTypeArray" multiple style="width: 100%">
                    <el-option v-for="item in carrierTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="意向面积" prop="intentArea">
                  <el-input-number v-model="form.intentArea" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="使用用途" prop="usagePurpose">
                  <el-input v-model="form.usagePurpose" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租赁期限" prop="leaseTermYears">
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
              <el-input v-model="form.decorationRequirement" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="配套需求">
              <el-input v-model="form.supportingNeeds" type="textarea" :rows="2" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>联系人与招商信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="负责人姓名" prop="contactName">
                  <el-input v-model="form.contactName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input v-model="form.contactPhone" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.contactEmail" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职务">
                  <el-input v-model="form.contactPosition" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="联系地址">
              <el-input v-model="form.contactAddress" maxlength="500" placeholder="请输入联系地址" />
            </el-form-item>
            <el-form-item label="上传身份证">
              <el-upload
                action="/api/blade-resource/oss/endpoint/put-file-attach"
                :headers="uploadHeaders"
                :file-list="idCardFileList"
                :on-success="handleIdCardUploadSuccess"
                :on-remove="handleIdCardUploadRemove"
                multiple
              >
                <el-button icon="el-icon-upload">上传身份证</el-button>
              </el-upload>
            </el-form-item>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="招商渠道" prop="channel">
                  <el-select v-model="form.channel" style="width: 100%">
                    <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第三方渠道">
                  <el-input v-model="form.thirdPartyChannelName" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </section>
        </el-form>
      </section>

      <el-drawer
        v-model="formDrawerVisible"
        :title="formTitle"
        size="76%"
        append-to-body
        :before-close="closeFormDrawer"
      >
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          :disabled="view"
          label-width="118px"
          class="opportunity-form"
        >
          <section class="form-section">
            <header>跟进信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="跟进人" prop="followUserId">
                  <el-select
                    v-model="form.followUserId"
                    filterable
                    clearable
                    placeholder="请选择用户账号"
                    style="width: 100%"
                    @change="handleFollowUserChange"
                  >
                    <el-option
                      v-for="user in userOptions"
                      :key="userId(user)"
                      :label="userLabel(user)"
                      :value="userId(user)"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="商机状态" prop="opportunityStatus">
                  <el-select v-model="form.opportunityStatus" style="width: 100%">
                    <el-option
                      v-for="item in statusOptions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section">
            <header>企业基本信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业名称" prop="enterpriseName">
                  <el-input v-model="form.enterpriseName" placeholder="请输入企业名称">
                    <template #append>
                      <el-button :loading="backgroundLoading" @click.stop="handleBackgroundCheck">
                        背景调查
                      </el-button>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="统一信用代码" prop="creditCode">
                  <el-input v-model="form.creditCode" placeholder="请输入统一信用代码" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="成立日期">
                  <el-date-picker
                    v-model="form.establishDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    style="width: 100%"
                  />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="注册资本">
                  <el-input-number v-model="form.registeredCapital" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="企业类型">
                  <el-input v-model="form.enterpriseType" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="行业类型">
                  <el-input v-model="form.industryType" placeholder="请输入行业类型">
                    <template #append>
                      <div class="industry-input-actions">
                        <el-button :loading="industryChecking" @click.stop="handleIndustryCheck">
                          行业检测
                        </el-button>
                        <el-button
                          v-if="permissionList.industryRuleBtn && !view"
                          @click.stop="openIndustryRuleDialog"
                        >
                          规则配置
                        </el-button>
                      </div>
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
              <el-input v-model="form.businessScope" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="注册地址">
              <el-input v-model="form.registeredAddress" />
            </el-form-item>
            <el-form-item label="客户标签">
              <customer-tag-selector v-model="form.tagIds" :disabled="view" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>经营状况信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="主营业务">
                  <el-input v-model="form.mainBusiness" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="上年度营收">
                  <el-input-number v-model="form.lastYearRevenue" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="主要合作客户">
              <el-input v-model="form.majorClients" type="textarea" :rows="2" />
            </el-form-item>
            <el-row :gutter="18">
              <el-col :span="8">
                <el-form-item label="违法违规">
                  <el-radio-group v-model="form.majorIllegalFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="失信记录">
                  <el-radio-group v-model="form.dishonestFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="行业处罚">
                  <el-radio-group v-model="form.industryPenaltyFlag">
                    <el-radio label="0">否</el-radio>
                    <el-radio label="1">是</el-radio>
                  </el-radio-group>
                </el-form-item>
              </el-col>
            </el-row>
          </section>

          <section class="form-section">
            <header>入驻需求信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="意向载体" prop="carrierTypeArray">
                  <el-select v-model="form.carrierTypeArray" multiple style="width: 100%">
                    <el-option v-for="item in carrierTypeOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="意向面积" prop="intentArea">
                  <el-input-number v-model="form.intentArea" :min="0" style="width: 100%" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="使用用途" prop="usagePurpose">
                  <el-input v-model="form.usagePurpose" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="租赁期限" prop="leaseTermYears">
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
              <el-input v-model="form.decorationRequirement" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="配套需求">
              <el-input v-model="form.supportingNeeds" type="textarea" :rows="2" />
            </el-form-item>
          </section>

          <section class="form-section">
            <header>联系人与招商信息</header>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="负责人姓名" prop="contactName">
                  <el-input v-model="form.contactName" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="联系电话" prop="contactPhone">
                  <el-input v-model="form.contactPhone" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.contactEmail" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="职务">
                  <el-input v-model="form.contactPosition" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="联系地址">
              <el-input v-model="form.contactAddress" maxlength="500" placeholder="请输入联系地址" />
            </el-form-item>
            <el-form-item label="上传身份证">
              <el-upload
                action="/api/blade-resource/oss/endpoint/put-file-attach"
                :headers="uploadHeaders"
                :file-list="idCardFileList"
                :on-success="handleIdCardUploadSuccess"
                :on-remove="handleIdCardUploadRemove"
                :before-remove="beforeIdCardRemove"
                :disabled="view"
                multiple
              >
                <el-button v-if="!view" icon="el-icon-upload">上传身份证</el-button>
              </el-upload>
            </el-form-item>
            <el-row :gutter="18">
              <el-col :span="12">
                <el-form-item label="招商渠道" prop="channel">
                  <el-select v-model="form.channel" style="width: 100%">
                    <el-option v-for="item in channelOptions" :key="item" :label="item" :value="item" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="第三方渠道">
                  <el-input v-model="form.thirdPartyChannelName" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="3" />
            </el-form-item>
          </section>

          <section v-if="view" class="form-section">
            <header>跟进记录</header>
            <el-timeline v-if="followList.length">
              <el-timeline-item
                v-for="item in followList"
                :key="item.followId || item.createTime"
                :timestamp="item.followTime"
              >
                <div class="timeline-title">
                  {{ item.followUser || '-' }} ·
                  {{ statusTextMap[item.opportunityStatus] || item.opportunityStatus || '-' }}
                </div>
                <div class="timeline-text">{{ item.followContent || '-' }}</div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-else description="暂无跟进记录" />
          </section>

          <section v-if="view" class="form-section">
            <header>附件</header>
            <el-upload
              v-if="permissionList.fileUploadBtn"
              :show-file-list="false"
              :http-request="handleUploadFile"
            >
              <el-button icon="el-icon-upload" type="primary" plain>上传附件</el-button>
            </el-upload>
            <div v-if="fileList.length" class="file-list">
              <a v-for="item in fileList" :key="item.fileId" :href="item.fileUrl" target="_blank">
                {{ item.fileName || item.fileUrl }}
              </a>
            </div>
            <el-empty v-else description="暂无附件" />
          </section>
        </el-form>

        <template #footer>
          <div class="drawer-footer">
            <el-button @click="closeFormDrawer">关闭</el-button>
            <el-button v-if="!view" type="primary" :loading="submitLoading" @click="handleSubmit">
              保存
            </el-button>
          </div>
        </template>
      </el-drawer>

      <el-dialog v-model="backgroundVisible" title="背景调查" width="520px" append-to-body>
        <el-empty description="未查询到企业数据" />
      </el-dialog>

      <industry-rule-dialog ref="industryRuleDialog" @ok="handleIndustryRulesChanged" />
    </div>
  </basic-container>
</template>

<script>
import {
  addOpportunity,
  addOpportunityFollow,
  getOpportunityDetail,
  getOpportunityFileList,
  getOpportunityList,
  getOpportunityStatistics,
  removeOpportunity,
  updateOpportunity,
  uploadOpportunityFile,
} from '@/api/business/opportunity';
import { checkIndustryRule } from '@/api/business/customer';
import { getList as getUserList } from '@/api/system/user';
import IndustryRuleDialog from './modules/industry-rule-dialog.vue';
import CustomerTagSelector from './modules/customer-tag-selector.vue';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';

const createDefaultForm = () => ({
  opportunityStatus: 'INITIAL',
  followUserId: '',
  followUser: '',
  contactEmail: '',
  contactAddress: '',
  idCardFiles: '',
  majorIllegalFlag: '0',
  dishonestFlag: '0',
  industryPenaltyFlag: '0',
  carrierTypeArray: [],
  tagIds: [],
});

export default {
  name: 'BusinessOpportunity',
  components: { CustomerTagSelector, IndustryRuleDialog },
  data() {
    return {
      loading: false,
      submitLoading: false,
      followLoading: false,
      backgroundLoading: false,
      industryChecking: false,
      data: [],
      selectionList: [],
      query: {},
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      summaryCards: [
        { key: 'totalCount', label: '商机总数', value: 0 },
        { key: 'followCount', label: '跟进中', value: 0 },
        { key: 'dealCount', label: '已成交', value: 0 },
        { key: 'lostCount', label: '已流失', value: 0 },
      ],
      formDrawerVisible: false,
      formMode: 'add',
      form: createDefaultForm(),
      followList: [],
      fileList: [],
      idCardFileList: [],
      userOptions: [],
      currentRecord: null,
      followPageVisible: false,
      followForm: {
        opportunityStatus: 'INITIAL',
        followContent: '',
        nextFollowTime: '',
      },
      backgroundVisible: false,
      backgroundData: {},
      industryCheckResult: null,
      statusOptions: [
        { value: 'LEAD', label: '潜在线索' },
        { value: 'INITIAL', label: '初步沟通' },
        { value: 'DEEP', label: '深入洽谈' },
        { value: 'DEAL', label: '成交' },
        { value: 'LOST', label: '流失' },
      ],
      statusTextMap: {
        DRAFT: '初步沟通',
        AUDIT: '初步沟通',
        LEAD: '潜在线索',
        INITIAL: '初步沟通',
        DEEP: '深入洽谈',
        DEAL: '成交',
        LOST: '流失',
      },
      statusTypeMap: {
        LEAD: '',
        INITIAL: 'info',
        DEEP: 'warning',
        DEAL: 'success',
        LOST: 'danger',
      },
      channelOptions: ['自主招商', '中介推荐', '政府推荐', '第三方渠道', '线上咨询', '其他'],
      carrierTypeOptions: ['办公', '研发', '生产', '仓储', '商业', '配套'],
      leaseTermOptions: ['1年以内', '1-3年', '3-5年', '5年以上'],
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      rules: {
        enterpriseName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
        creditCode: [{ required: true, message: '请输入统一信用代码', trigger: 'blur' }],
        followUserId: [{ required: true, message: '请选择跟进人', trigger: 'change' }],
        contactName: [{ required: true, message: '请输入负责人姓名', trigger: 'blur' }],
        contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
        channel: [{ required: true, message: '请选择招商渠道', trigger: 'change' }],
        opportunityStatus: [{ required: true, message: '请选择商机状态', trigger: 'change' }],
      },
    };
  },
  computed: {
    ...mapGetters(['permission']),
    view() {
      return this.formMode === 'view';
    },
    formTitle() {
      const titleMap = {
        add: '新增商机',
        edit: '编辑商机',
        view: '商机详情',
      };
      return titleMap[this.formMode] || '商机';
    },
    permissionList() {
      return {
        addBtn: this.validData(this.permission.business_opportunity_add, false),
        editBtn: this.validData(this.permission.business_opportunity_edit, false),
        delBtn: this.validData(this.permission.business_opportunity_delete, false),
        viewBtn: this.validData(this.permission.business_opportunity_view, false),
        followBtn: this.validData(this.permission.business_opportunity_follow, false),
        fileUploadBtn: this.validData(this.permission.business_opportunity_file_upload, false),
        industryRuleBtn: this.validData(this.permission.business_opportunity_industry_rule, false),
      };
    },
    ids() {
      return this.selectionList.map(item => item.opportunityId).join(',');
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
  },
  watch: {
    '$route.query': {
      handler() {
        this.syncFollowRoute();
      },
    },
  },
  created() {
    this.loadUserOptions();
    this.loadStatistics();
    this.onLoad(this.page);
    this.syncFollowRoute();
  },
  methods: {
    validData(value, defaultValue) {
      return value === undefined || value === null ? defaultValue : value;
    },
    responseData(res) {
      return res && res.data ? (res.data.data || res.data) : {};
    },
    loadUserOptions() {
      getUserList(1, 500, { status: 1 }).then(res => {
        const data = this.responseData(res) || {};
        this.userOptions = Array.isArray(data.records) ? data.records : [];
      }).catch(() => {
        this.userOptions = [];
      });
    },
    userLabel(user = {}) {
      const name = user.realName || user.name || user.nickName || user.account || user.userName || user.id;
      const account = user.account || user.userName;
      return account && name && account !== name ? `${name}（${account}）` : String(name || '-');
    },
    userId(user = {}) {
      return String(user.id || user.userId || '');
    },
    userDisplayName(user = {}) {
      return user.realName || user.name || user.nickName || user.account || user.userName || '';
    },
    handleFollowUserChange(userId) {
      const user = this.userOptions.find(item => this.userId(item) === String(userId));
      this.form.followUser = user ? this.userDisplayName(user) : '';
      if (!userId) {
        this.form.followUserId = '';
      }
    },
    parseFileList(value) {
      if (!value) return [];
      try {
        const list = typeof value === 'string' ? JSON.parse(value) : value;
        return (Array.isArray(list) ? list : []).map((item, index) => {
          const url = typeof item === 'string' ? item : item.url || item.link || '';
          return {
            uid: `${Date.now()}-${index}`,
            name: typeof item === 'string' ? item.split('/').pop() : item.name || item.originalName || `附件${index + 1}`,
            url,
            status: 'success',
          };
        });
      } catch (error) {
        return [];
      }
    },
    syncIdCardFiles() {
      this.form.idCardFiles = JSON.stringify(
        this.idCardFileList
          .map(file => ({
            name: file.name,
            url: file.url,
          }))
          .filter(file => file.url)
      );
    },
    handleIdCardUploadSuccess(response, file) {
      if (!response || !response.success) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      this.idCardFileList.push({
        uid: file.uid,
        name: data.originalName || data.name || file.name,
        url: data.link || data.url || '',
        status: 'success',
      });
      this.syncIdCardFiles();
      this.$message.success('上传成功');
    },
    handleIdCardUploadRemove(file) {
      this.idCardFileList = this.idCardFileList.filter(item => item.uid !== file.uid);
      this.syncIdCardFiles();
    },
    beforeIdCardRemove() {
      return !this.view;
    },
    buildSubmitPayload() {
      this.syncIdCardFiles();
      return {
        ...this.form,
        followUserId: this.form.followUserId || null,
      };
    },
    loadStatistics() {
      getOpportunityStatistics().then(res => {
        const stats = res.data.data || {};
        this.summaryCards = this.summaryCards.map(item => ({
          ...item,
          value: Number(stats[item.key]) || 0,
        }));
      });
    },
    onLoad(page, params = {}) {
      this.loading = true;
      const query = {
        ...this.query,
        ...params,
      };
      getOpportunityList(page.currentPage, page.pageSize, query)
        .then(res => {
          const result = res.data.data || {};
          this.data = result.records || [];
          this.page.total = Number(result.total) || 0;
        })
        .finally(() => {
          this.loading = false;
        });
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
    handleAdd() {
      this.formMode = 'add';
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.idCardFileList = [];
      this.industryCheckResult = null;
      this.formDrawerVisible = true;
    },
    handleEdit(row) {
      this.openForm('edit', row);
    },
    handleView(row) {
      this.openForm('view', row);
    },
    openForm(mode, row) {
      this.formMode = mode;
      this.formDrawerVisible = true;
      this.industryCheckResult = null;
      getOpportunityDetail(row.opportunityId).then(res => {
        const detail = res.data.data || {};
        this.form = {
          ...createDefaultForm(),
          ...detail,
          followUserId: detail.followUserId ? String(detail.followUserId) : '',
          carrierTypeArray: detail.carrierTypeArray || this.splitCarrierTypes(detail.carrierTypes),
          tagIds: detail.tagIds || (detail.tags || []).map(tag => tag.tagId),
        };
        this.idCardFileList = this.parseFileList(this.form.idCardFiles);
        this.followList = detail.followList || [];
        this.fileList = detail.fileList || [];
      });
    },
    closeFormDrawer() {
      this.formDrawerVisible = false;
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.idCardFileList = [];
      this.industryCheckResult = null;
      this.$nextTick(() => {
        this.$refs.formRef && this.$refs.formRef.clearValidate();
      });
    },
    handleSubmit() {
      this.$refs.formRef.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        const payload = this.buildSubmitPayload();
        const action = payload.opportunityId ? updateOpportunity : addOpportunity;
        action(payload)
          .then(() => {
            this.$message.success('保存成功');
            this.closeFormDrawer();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.submitLoading = false;
          });
      });
    },
    rowDel(row) {
      this.$confirm('确定删除该商机?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeOpportunity(row.opportunityId))
        .then(() => {
          this.$message.success('删除成功');
          this.loadStatistics();
          this.onLoad(this.page);
        });
    },
    handleBatchDelete() {
      if (!this.ids) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定删除选中的商机?', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => removeOpportunity(this.ids))
        .then(() => {
          this.$message.success('删除成功');
          this.selectionList = [];
          this.loadStatistics();
          this.onLoad(this.page);
        });
    },
    normalizeOpportunityStatus(status) {
      return ['DRAFT', 'AUDIT'].includes(status) ? 'INITIAL' : status;
    },
    setFollowRoute(opportunityId) {
      if (!opportunityId) return;
      const query = {
        ...this.$route.query,
        id: opportunityId,
        mode: 'follow',
      };
      if (
        String(this.$route.query.id || '') === String(opportunityId) &&
        this.$route.query.mode === 'follow'
      ) {
        return;
      }
      this.$router.push({ path: this.$route.path, query }).catch(() => {});
    },
    clearFollowRoute() {
      const { id, mode, ...query } = this.$route.query || {};
      if (mode !== 'follow' && id === undefined) return;
      this.$router.replace({ path: this.$route.path, query }).catch(() => {});
    },
    syncFollowRoute() {
      const { id, mode } = this.$route.query || {};
      if (mode === 'follow' && id) {
        const currentId =
          (this.form && this.form.opportunityId) ||
          (this.currentRecord && this.currentRecord.opportunityId);
        if (this.followPageVisible && String(currentId || '') === String(id)) return;
        this.openFollowPage({ opportunityId: id });
        return;
      }
      if (this.followPageVisible && mode !== 'follow') {
        this.resetFollowPage();
      }
    },
    openFollow(row) {
      this.setFollowRoute(row.opportunityId);
      this.openFollowPage(row);
    },
    openFollowPage(row) {
      this.currentRecord = row;
      const status = this.normalizeOpportunityStatus(row.opportunityStatus);
      this.followForm = {
        opportunityStatus: status || 'INITIAL',
        followContent: '',
        nextFollowTime: '',
      };
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.idCardFileList = [];
      this.industryCheckResult = null;
      this.followPageVisible = true;
      getOpportunityDetail(row.opportunityId).then(res => {
        const detail = res.data.data || {};
        const detailStatus = this.normalizeOpportunityStatus(detail.opportunityStatus);
        this.form = {
          ...createDefaultForm(),
          ...detail,
          followUserId: detail.followUserId ? String(detail.followUserId) : '',
          opportunityStatus: status || detailStatus || 'INITIAL',
          carrierTypeArray: detail.carrierTypeArray || this.splitCarrierTypes(detail.carrierTypes),
          tagIds: detail.tagIds || (detail.tags || []).map(tag => tag.tagId),
        };
        this.idCardFileList = this.parseFileList(this.form.idCardFiles);
        this.followList = detail.followList || [];
        this.fileList = detail.fileList || [];
      });
    },
    closeFollowPage() {
      this.resetFollowPage();
      this.clearFollowRoute();
    },
    resetFollowPage() {
      this.followPageVisible = false;
      this.currentRecord = null;
      this.form = createDefaultForm();
      this.followList = [];
      this.fileList = [];
      this.idCardFileList = [];
      this.industryCheckResult = null;
      this.followForm = {
        opportunityStatus: 'INITIAL',
        followContent: '',
        nextFollowTime: '',
      };
      this.$nextTick(() => {
        this.$refs.followPageFormRef && this.$refs.followPageFormRef.clearValidate();
      });
    },
    handleSaveFollowPage() {
      if (!this.followForm.followContent || !this.followForm.followContent.trim()) {
        this.$message.warning('请输入跟进内容');
        return;
      }
      this.$refs.followPageFormRef.validate(valid => {
        if (!valid || !this.currentRecord || !this.form.opportunityId) return;
        this.followLoading = true;
        const payload = this.buildSubmitPayload();
        const followPayload = {
          ...this.followForm,
          followUser: payload.followUser,
          followUserId: payload.followUserId,
          opportunityStatus: payload.opportunityStatus,
        };
        updateOpportunity(payload)
          .then(() => addOpportunityFollow(payload.opportunityId, followPayload))
          .then(() => {
            this.$message.success('跟进成功');
            this.closeFollowPage();
            this.loadStatistics();
            this.onLoad(this.page);
          })
          .finally(() => {
            this.followLoading = false;
          });
      });
    },
    handleUploadFile({ file }) {
      const formData = new FormData();
      formData.append('file', file);
      formData.append('opportunityId', this.form.opportunityId);
      return uploadOpportunityFile(formData).then(() => {
        this.$message.success('上传成功');
        return getOpportunityFileList(this.form.opportunityId).then(res => {
          this.fileList = res.data.data || [];
        });
      });
    },
    handleBackgroundCheck() {
      if (!this.form.enterpriseName) {
        this.$message.warning('请先填写企业名称');
        return;
      }
      this.backgroundData = { enterpriseName: this.form.enterpriseName };
      this.backgroundVisible = true;
    },
    handleBackgroundPlaceholder(row = {}) {
      this.backgroundData = { enterpriseName: row.enterpriseName };
      this.backgroundVisible = true;
    },
    handleIndustryCheck(showMessage = true) {
      const shouldMessage = showMessage !== false;
      const industry = (this.form.industryType || '').trim();
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
      checkIndustryRule(industry)
        .then(res => {
          this.industryCheckResult = res.data.data || {};
          if (shouldMessage) {
            this.$message.success('行业准入检测完成');
          }
        })
        .finally(() => {
          this.industryChecking = false;
        });
    },
    openIndustryRuleDialog() {
      this.$refs.industryRuleDialog && this.$refs.industryRuleDialog.open();
    },
    handleIndustryRulesChanged() {
      if (this.form.industryType) {
        this.handleIndustryCheck(false);
      }
    },
    formatCarrierTypes(value) {
      return value ? String(value).split(',').filter(Boolean).join('、') : '-';
    },
    formatTags(row = {}) {
      const tags = row.tags || [];
      if (tags.length) {
        return tags
          .map(tag => ({
            tagId: tag.tagId || tag.id || tag.tagName || tag.name,
            tagName: tag.tagName || tag.name,
          }))
          .filter(tag => tag.tagName);
      }
      if (row.tagNames) {
        return String(row.tagNames)
          .split(',')
          .filter(Boolean)
          .map(name => ({
            tagId: name,
            tagName: name,
          }));
      }
      return [];
    },
    splitCarrierTypes(value) {
      return value ? String(value).split(',').filter(Boolean) : [];
    },
  },
};
</script>

<style scoped>
.opportunity-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.opportunity-search {
  padding: 16px 18px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.opportunity-search :deep(.el-form-item) {
  margin: 0 22px 12px 0;
}

.opportunity-search :deep(.el-form-item__label) {
  height: 36px;
  line-height: 36px;
  color: #303133;
}

.opportunity-search :deep(.el-input),
.opportunity-search :deep(.el-select) {
  width: 168px;
}

.opportunity-search :deep(.el-input__wrapper),
.opportunity-search :deep(.el-select__wrapper) {
  min-height: 36px;
}

.opportunity-search :deep(.el-button) {
  height: 36px;
  padding: 0 18px;
}

.opportunity-toolbar {
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
  gap: 8px;
}

.opportunity-table {
  width: 100%;
}

.opportunity-table :deep(.el-table__cell) {
  text-align: center;
}

.opportunity-table :deep(.cell) {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 32px;
}

.opportunity-table :deep(.opportunity-no-column .cell) {
  white-space: nowrap;
  word-break: keep-all;
}

.opportunity-table :deep(.enterprise-name-column .cell) {
  overflow: hidden;
  flex-wrap: nowrap;
  white-space: nowrap;
  word-break: keep-all;
}

.follow-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.follow-page__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 86px;
  box-sizing: border-box;
  margin: 0 20px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.follow-page__header h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
  line-height: 1.4;
}

.follow-page__header span {
  display: block;
  margin-top: 4px;
  color: #6b7280;
  font-size: 13px;
}

.follow-page__actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.follow-page__form {
  padding: 0;
}

.enterprise-link,
.table-link {
  padding: 0;
  font-weight: 400;
}

.enterprise-link {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.opportunity-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.opportunity-form {
  padding: 0 20px 20px;
}

.opportunity-form :deep(.el-input-group__append) {
  padding: 0;
}

.opportunity-form :deep(.el-input-group__append .el-button) {
  min-width: 124px;
  border: 0;
}

.industry-input-actions {
  display: inline-flex;
  height: 100%;
  align-items: center;
}

.industry-input-actions .el-button {
  min-width: 124px;
}

.industry-input-actions .el-button + .el-button {
  border-left: 1px solid #dcdfe6;
}

.industry-check-alert {
  margin-bottom: 18px;
}

.form-section {
  margin-bottom: 18px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: #fff;
}

.form-section header {
  margin-bottom: 14px;
  color: #1f2937;
  font-size: 15px;
  font-weight: 600;
}

.follow-record-section {
  border-color: #edf0f5;
  background: #f7f8fa;
}

.follow-record-timeline {
  padding: 2px 0 0;
}

.follow-record-section :deep(.el-timeline-item__node) {
  border: 1px solid #2f75e8;
  background: #fff;
}

.follow-record-section :deep(.el-timeline-item__tail) {
  border-left-color: #e0e5ee;
}

.follow-record-section :deep(.el-timeline-item__wrapper) {
  top: -1px;
}

.follow-record-section :deep(.el-timeline-item) {
  padding-bottom: 18px;
}

.follow-record-section :deep(.el-timeline-item:last-child) {
  padding-bottom: 0;
}

.drawer-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 10px 20px;
}

.timeline-title {
  color: #1f2937;
  font-weight: 600;
}

.follow-record-meta {
  color: #64748b;
  font-size: 13px;
  font-weight: 400;
  line-height: 1.35;
}

.timeline-text {
  margin-top: 4px;
  color: #4b5563;
}

.follow-record-section .timeline-text {
  color: #374151;
  font-size: 14px;
  line-height: 1.5;
}

.file-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 12px;
}

.background-tabs {
  margin-top: 14px;
}

@media (max-width: 900px) {
  .opportunity-form :deep(.el-col) {
    max-width: 100%;
    flex: 0 0 100%;
  }
}
</style>
