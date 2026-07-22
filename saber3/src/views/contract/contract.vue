<template>
  <basic-container>
    <div class="contract-page">
      <template v-if="detailMode">
        <el-skeleton :loading="detailLoading" animated>
          <template #template>
            <section class="contract-profile-page">
              <el-skeleton-item variant="p" style="width: 32%" />
              <el-skeleton-item variant="p" style="width: 68%; margin-top: 20px" />
              <el-skeleton-item variant="p" style="width: 86%; margin-top: 20px" />
            </section>
          </template>
          <template #default>
            <section class="contract-detail-page">
              <header class="contract-detail-header">
                <div class="contract-detail-title-row">
                  <el-button
                    class="contract-detail-back"
                    text
                    icon="el-icon-arrow-left"
                    @click="closeDetailPage"
                  />
                  <h2>{{ detailHeaderTitle }}</h2>
                  <el-tag :type="statusType(detailContract.contractStatus)" effect="plain">
                    {{ detailContract.contractStatusName || statusText(detailContract.contractStatus) }}
                  </el-tag>
                </div>
                <div class="contract-detail-actions">
                  <el-button
                    v-if="detailContract.contractId"
                    type="primary"
                    @click="handleEditContract(detailContract)"
                  >
                    编辑
                  </el-button>
                  <el-button
                    v-if="detailContract.contractId"
                    type="danger"
                    plain
                    @click="handleStartTermination(detailContract)"
                  >
                    退租
                  </el-button>
                  <el-button
                    v-if="detailContract.contractId"
                    type="primary"
                    plain
                    @click="handleRenew(detailContract)"
                  >
                    续租
                  </el-button>
                  <el-button
                    v-if="permission.contract_contract_terminate && detailContract.contractStatus !== '4'"
                    type="danger"
                    @click="handleTerminate(detailContract)"
                  >
                    作废
                  </el-button>
                  <el-dropdown trigger="click" @command="handleDetailMoreCommand">
                    <el-button type="primary" plain>
                      更多
                      <i class="el-icon-arrow-down el-icon--right" />
                    </el-button>
                    <template #dropdown>
                      <el-dropdown-menu>
                        <el-dropdown-item command="downloadContract">下载合同</el-dropdown-item>
                        <el-dropdown-item command="editContract">编辑合同</el-dropdown-item>
                        <el-dropdown-item command="contractChange">合同变更</el-dropdown-item>
                        <el-dropdown-item command="startApproval">发起合同审批</el-dropdown-item>
                        <el-dropdown-item command="signedUpload">上传盖章合同</el-dropdown-item>
                        <el-dropdown-item command="roomReview">发起房屋验收</el-dropdown-item>
                        <el-dropdown-item command="archive">合同归档</el-dropdown-item>
                        <el-dropdown-item v-if="currentCustomerId" command="editCustomer">
                          维护租客
                        </el-dropdown-item>
                        <el-dropdown-item command="addAttachment">新增附件</el-dropdown-item>
                        <el-dropdown-item v-if="currentCustomerId" divided command="deleteCustomer">
                          删除租客
                        </el-dropdown-item>
                      </el-dropdown-menu>
                    </template>
                  </el-dropdown>
                </div>
              </header>

              <el-alert
                class="contract-detail-summary"
                type="info"
                show-icon
                :closable="false"
                :title="contractSummary"
              />

              <el-tabs v-model="profileTab" class="contract-detail-tabs">
                <el-tab-pane label="合同信息" name="info">
                  <section class="contract-info-block">
                    <div class="contract-info-title">基本信息</div>
                    <div class="contract-info-grid">
                      <div
                        v-for="item in baseContractInfoItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>

                  <section class="contract-info-block">
                    <div class="contract-info-title">房源信息</div>
                    <div class="contract-info-grid">
                      <div
                        v-for="item in roomInfoItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>

                  <section class="contract-info-block">
                    <div class="contract-info-title-row">
                      <div class="contract-info-title">租客信息</div>
                      <el-button
                        v-if="currentCustomerId"
                        type="primary"
                        plain
                        size="small"
                        @click="openCustomerEdit"
                      >
                        维护租客
                      </el-button>
                    </div>
                    <div class="contract-info-grid">
                      <div
                        v-for="item in tenantInfoItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>

                  <section class="contract-info-block">
                    <div class="contract-info-title">滞纳金信息</div>
                    <div class="contract-info-grid">
                      <div
                        v-for="item in lateFeeInfoItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>
                </el-tab-pane>

                <el-tab-pane label="合同明细" name="detail">
                  <section class="contract-info-block">
                    <div class="contract-info-title">租赁条款</div>
                    <div class="clause-grid">
                      <div class="clause-panel">
                        <div class="clause-title">基本条款</div>
                        <div class="contract-info-grid two-col">
                          <div
                            v-for="item in rentBasicClauseItems"
                            :key="item.label"
                            class="contract-info-field"
                          >
                            <span>{{ item.label }}</span>
                            <strong>{{ item.value }}</strong>
                          </div>
                        </div>
                      </div>
                      <div class="clause-panel">
                        <div class="clause-title">租金保证金</div>
                        <div class="contract-info-grid two-col">
                          <div
                            v-for="item in depositClauseItems"
                            :key="item.label"
                            class="contract-info-field"
                          >
                            <span>{{ item.label }}</span>
                            <strong>{{ item.value }}</strong>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="clause-band">
                      <div
                        v-for="item in rentPaymentClauseItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>

                  <section v-if="hasPropertyTerms" class="contract-info-block">
                    <div class="contract-info-title">物业条款</div>
                    <div class="clause-grid">
                      <div class="clause-panel">
                        <div class="clause-title">基本条款</div>
                        <div class="contract-info-grid two-col">
                          <div
                            v-for="item in propertyBasicClauseItems"
                            :key="item.label"
                            class="contract-info-field"
                          >
                            <span>{{ item.label }}</span>
                            <strong>{{ item.value }}</strong>
                          </div>
                        </div>
                      </div>
                    </div>
                    <div class="clause-band">
                      <div
                        v-for="item in propertyFeeClauseItems"
                        :key="item.label"
                        class="contract-info-field"
                      >
                        <span>{{ item.label }}</span>
                        <strong>{{ item.value }}</strong>
                      </div>
                    </div>
                  </section>
                </el-tab-pane>

                <el-tab-pane label="合同文本" name="text">
                  <section class="contract-info-block">
                    <div class="contract-info-title">合同文本</div>
                    <div class="contract-text-list">
                      <div class="contract-text-item">
                        <div>
                          <span>合同正文</span>
                          <strong>{{ contractTextTypeText(detailContract) }}</strong>
                        </div>
                        <div class="contract-text-actions">
                          <el-button type="primary" plain @click="handlePreviewContractText(detailContract)">
                            预览
                          </el-button>
                          <el-button
                            type="primary"
                            :loading="contractTextDownloading"
                            @click="handleDownloadContractText(detailContract)"
                          >
                            下载Word
                          </el-button>
                        </div>
                      </div>
                      <div v-if="approvalFileRecord" class="contract-text-item">
                        <div>
                          <span>合同审批表</span>
                          <strong>{{ approvalFileRecord.processName || '合同会签审批表' }}</strong>
                        </div>
                        <div class="contract-text-actions">
                          <el-button
                            type="primary"
                            plain
                            @click="openWorkflowFile(approvalFileRecord.printFileUrl, approvalFileRecord)"
                          >
                            预览
                          </el-button>
                        </div>
                      </div>
                      <div v-if="detailContract.contractFileUrl" class="contract-text-item">
                        <div>
                          <span>盖章合同</span>
                          <strong>{{ detailContract.contractNo || '已盖章合同' }}</strong>
                        </div>
                        <div class="contract-text-actions">
                          <el-button
                            type="primary"
                            plain
                            @click="previewAttachment({
                              fileUrl: detailContract.contractFileUrl,
                              fileName: detailContract.contractNo || '已盖章合同'
                            }, '盖章合同预览')"
                          >
                            预览
                          </el-button>
                        </div>
                      </div>
                    </div>
                  </section>

                  <section class="contract-info-block">
                    <div class="contract-info-title-row">
                      <div class="contract-info-title">附件</div>
                      <el-button type="primary" icon="el-icon-plus" @click="openSupplementDialog">
                        新增附件
                      </el-button>
                    </div>
                    <el-table :data="supplements" border class="contract-detail-table">
                      <el-table-column prop="fileName" label="文件名称" width="240" show-overflow-tooltip>
                        <template #default="{ row }">{{ row.fileName || row.agreementName || '-' }}</template>
                      </el-table-column>
                      <el-table-column prop="contractNo" label="合同编号" min-width="150" show-overflow-tooltip />
                      <el-table-column prop="createBy" label="上传人" width="140" align="center">
                        <template #default="{ row }">{{ row.createBy || row.updateBy || '-' }}</template>
                      </el-table-column>
                      <el-table-column prop="createTime" label="上传时间" width="180" align="center">
                        <template #default="{ row }">{{ row.createTime || row.updateTime || '-' }}</template>
                      </el-table-column>
                      <el-table-column label="操作" width="230" align="center" fixed="right">
                        <template #default="{ row }">
                          <div class="attachment-table-actions">
                            <el-button text type="primary" @click="previewAttachment(row)">预览</el-button>
                            <el-button text type="primary" @click="downloadSupplement(row)">下载</el-button>
                            <el-button text type="danger" @click="removeSupplement(row)">删除</el-button>
                          </div>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-empty v-if="supplements.length === 0" description="暂无附件" />
                  </section>
                </el-tab-pane>

                <el-tab-pane label="账单列表" name="bill">
                  <section class="contract-info-block">
                    <el-table
                      v-loading="paymentLoading"
                      :data="paymentData"
                      border
                      class="contract-detail-table"
                    >
                      <el-table-column prop="contractNo" label="合同编号" min-width="150" show-overflow-tooltip />
                      <el-table-column prop="feeName" label="账单名称" min-width="150" />
                      <el-table-column label="账期" min-width="200" align="center">
                        <template #default="{ row }">
                          {{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}
                        </template>
                      </el-table-column>
                      <el-table-column prop="amountDue" label="应收金额" width="130" align="right">
                        <template #default="{ row }">{{ formatMoneyWithUnit(row.amountDue) }}</template>
                      </el-table-column>
                      <el-table-column prop="amountPaid" label="实收金额" width="130" align="right">
                        <template #default="{ row }">{{ formatMoneyWithUnit(row.amountPaid) }}</template>
                      </el-table-column>
                      <el-table-column prop="payDeadline" label="缴费期限" width="140" align="center">
                        <template #default="{ row }">{{ row.payDeadline || '-' }}</template>
                      </el-table-column>
                      <el-table-column label="账单状态" width="110" align="center">
                        <template #default="{ row }">
                          <el-tag :type="paymentTagType(row)" effect="plain">
                            {{ paymentStatusText(row.payStatus) }}
                          </el-tag>
                        </template>
                      </el-table-column>
                      <el-table-column label="操作" width="270" align="center" fixed="right">
                        <template #default="{ row }">
                          <div class="bill-table-actions">
                            <template v-if="row.payStatus !== '1'">
                              <el-button text type="primary" @click="handleConfirmPayment(row)">
                                确认缴费
                              </el-button>
                              <el-button text type="primary" @click="handleRemind(row)">
                                催缴
                              </el-button>
                              <el-button text type="danger" @click="handleStartOverdueApproval(row)">
                                逾期处理
                              </el-button>
                            </template>
                            <span v-else class="muted">已缴费</span>
                          </div>
                        </template>
                      </el-table-column>
                    </el-table>
                    <el-empty v-if="paymentData.length === 0" description="暂无账单" />
                  </section>
                </el-tab-pane>
              </el-tabs>
            </section>
          </template>
        </el-skeleton>
      </template>

      <template v-else>
      <el-alert
        v-if="expiringData.length"
        type="warning"
        show-icon
        :closable="false"
        :description="expiringSummary"
        class="contract-alert"
      >
        <template #title>
          合同到期提醒：当前有 {{ expiringData.length }} 份合同进入续签提醒周期
        </template>
      </el-alert>

      <section class="summary-grid">
        <div v-for="item in summaryCards" :key="item.key" class="summary-card">
          <span>{{ item.label }}</span>
          <strong>{{ item.value }}</strong>
        </div>
      </section>

      <section class="contract-search">
        <el-form :inline="true" :model="query">
          <el-form-item label="合同编号">
            <el-input
              v-model="query.contractNo"
              clearable
              placeholder="请输入合同编号"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="客户名称">
            <el-input
              v-model="query.customerName"
              clearable
              placeholder="请输入客户名称"
              @keyup.enter="searchChange"
            />
          </el-form-item>
          <el-form-item label="合同状态">
            <el-select v-model="query.contractStatus" clearable placeholder="请选择">
              <el-option
                v-for="item in statusOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="searchChange">查询</el-button>
            <el-button icon="el-icon-delete" @click="searchReset">重置</el-button>
          </el-form-item>
        </el-form>
      </section>

      <section class="contract-toolbar">
        <div class="toolbar-left">
          <el-button type="primary" icon="el-icon-plus" @click="handleAdd">新建合同</el-button>
          <el-button
            type="success"
            plain
            @click="handleStartApprovalFromSelection"
          >
            <el-icon><Promotion /></el-icon>
            发起合同审批
          </el-button>
          <el-button
            v-if="permission.contract_contract_delete"
            type="danger"
            plain
            icon="el-icon-delete"
            :disabled="selectionList.length === 0"
            @click="handleDelete"
          >
            批量删除
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
        row-key="contractId"
        class="contract-table"
        @selection-change="selectionChange"
      >
        <el-table-column type="selection" width="44" align="center" />
        <el-table-column
          prop="customerName"
          label="租客名称"
          min-width="180"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-button text type="primary" class="tenant-name-btn" @click="openDetail(row)">
              {{ row.customerName || '-' }}
            </el-button>
          </template>
        </el-table-column>
        <el-table-column
          prop="contractNo"
          label="合同编号"
          min-width="170"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column prop="contractStatus" label="合同状态" width="150" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.contractStatus)" effect="plain">
              {{ row.contractStatusName || statusText(row.contractStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="approvalStatus"
          label="审批状态"
          width="120"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <el-tag :type="approvalStatusType(row.approvalStatus)" effect="plain">
              {{ approvalStatusText(row.approvalStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column
          prop="parkName"
          label="园区名称"
          width="130"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          prop="roomName"
          label="房源信息"
          width="130"
          align="center"
          show-overflow-tooltip
        >
          <template #default="{ row }">
            <span>{{ row.roomName || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="startDate" label="开始日" width="115" align="center" />
        <el-table-column prop="endDate" label="结束日" width="145" align="center">
          <template #default="{ row }">
            <span>{{ row.endDate || '-' }}</span>
            <el-tag v-if="isExpiringSoon(row)" type="warning" effect="plain" class="inline-tag"
              >即将到期</el-tag
            >
          </template>
        </el-table-column>
        <el-table-column prop="rentPrice" label="租赁单价" width="120" align="center">
          <template #default="{ row }">{{ formatUnitPrice(row.rentPrice) }}</template>
        </el-table-column>
        <el-table-column prop="signDate" label="签订日期" width="120" align="center">
          <template #default="{ row }">{{ row.signDate || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="156" align="center" fixed="right">
          <template #default="{ row }">
            <div class="table-actions">
              <el-button text type="primary" @click="openDetail(row)">详情</el-button>
              <el-button text type="primary" @click="handleArchive(row)">归档</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="contract-pagination">
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

      <el-drawer
        v-model="detailVisible"
        :title="detailTitle"
        size="980px"
        append-to-body
        class="contract-detail-drawer"
      >
        <section class="detail-head">
          <div>
            <h3>{{ detailContract.contractName || '合同详情' }}</h3>
            <span>{{ detailContract.contractNo || '-' }}</span>
          </div>
          <div class="detail-actions">
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              @click="handleEditContract(detailContract)"
            >
              编辑合同
            </el-button>
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              plain
              :loading="contractTextDownloading"
              @click="handleDownloadContractText(detailContract)"
            >
              下载合同
            </el-button>
            <el-button
              v-if="detailContract.contractId"
              type="success"
              plain
              @click="handleStartApproval(detailContract)"
            >
              发起合同审批
            </el-button>
            <el-button type="primary" plain @click="handleArchive(detailContract)"
              >合同归档</el-button
            >
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              plain
              @click="handleRenew(detailContract)"
            >
              合同续租
            </el-button>
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              @click="handleSignedUpload(detailContract)"
            >
              上传盖章合同
            </el-button>
            <el-button
              v-if="detailContract.contractId"
              type="warning"
              plain
              @click="handleStartTermination(detailContract)"
            >
              发起退租审批
            </el-button>
            <el-button
              v-if="detailContract.contractId"
              type="primary"
              plain
              @click="handleStartRoomReview(detailContract)"
            >
              发起房屋验收
            </el-button>
            <el-button
              v-if="permission.contract_contract_terminate && detailContract.contractStatus !== '4'"
              type="danger"
              @click="handleTerminate(detailContract)"
            >
              作废
            </el-button>
          </div>
        </section>

        <el-alert
          class="detail-alert"
          type="info"
          show-icon
          :closable="false"
          :title="contractSummary"
        />

        <el-tabs v-model="detailTab">
          <el-tab-pane label="合同信息" name="info">
            <section class="detail-section">
              <div class="detail-section-title">基本信息</div>
              <div class="contract-field-grid">
                <div v-for="item in basicDetailItems" :key="item.label" class="contract-field">
                  <span>{{ item.label }}</span>
                  <strong>{{ item.value }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">房源信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>房源信息</span>
                  <strong>{{ detailContract.roomName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>面积</span>
                  <strong>{{ formatAreaValue(detailContract.rentArea) }}</strong>
                </div>
                <div class="contract-field">
                  <span>建筑名称</span>
                  <strong>{{ detailContract.buildingName || '-' }}</strong>
                </div>
              </div>
            </section>

            <section class="detail-section">
              <div class="detail-section-title">租客信息</div>
              <div class="contract-field-grid">
                <div class="contract-field">
                  <span>租客</span>
                  <strong>{{ detailContract.customerName || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>跟进人</span>
                  <strong>{{ detailContract.followUser || detailContract.createBy || '-' }}</strong>
                </div>
                <div class="contract-field">
                  <span>备注</span>
                  <strong>{{ detailContract.remark || '-' }}</strong>
                </div>
              </div>
            </section>
          </el-tab-pane>

          <el-tab-pane label="账单列表" name="payment">
            <el-table v-loading="paymentLoading" :data="paymentData" border>
              <el-table-column prop="feeName" label="费用类型" width="110" align="center" />
              <el-table-column label="费用期间" min-width="210" align="center">
                <template #default="{ row }"
                  >{{ row.periodStart || '-' }} ~ {{ row.periodEnd || '-' }}</template
                >
              </el-table-column>
              <el-table-column prop="amountDue" label="应缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
              </el-table-column>
              <el-table-column prop="amountPaid" label="已缴金额" width="120" align="right">
                <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
              </el-table-column>
              <el-table-column prop="payDeadline" label="到期日" width="120" align="center" />
              <el-table-column prop="payStatus" label="状态" width="100" align="center">
                <template #default="{ row }">
                  <el-tag :type="paymentTagType(row)" effect="plain">{{
                    paymentStatusText(row.payStatus)
                  }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="overdueApprovalStatus"
                label="逾期审批"
                width="110"
                align="center"
              >
                <template #default="{ row }">
                  <el-tag
                    v-if="row.overdueApprovalStatus"
                    :type="approvalStatusType(row.overdueApprovalStatus)"
                    effect="plain"
                  >
                    {{ approvalStatusText(row.overdueApprovalStatus) }}
                  </el-tag>
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="270" align="center">
                <template #default="{ row }">
                  <div class="bill-table-actions">
                    <template v-if="row.payStatus !== '1'">
                      <el-button text type="primary" @click="handleConfirmPayment(row)"
                        >确认缴费</el-button
                      >
                      <el-button text type="primary" @click="handleRemind(row)">催缴</el-button>
                      <el-button text type="danger" @click="handleStartOverdueApproval(row)">
                        逾期处理
                      </el-button>
                    </template>
                    <span v-else class="muted">已缴费</span>
                  </div>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>

          <el-tab-pane label="审批记录" name="workflow">
            <el-table v-loading="workflowLoading" :data="workflowData" border>
              <el-table-column prop="businessType" label="业务类型" width="140" align="center">
                <template #default="{ row }">
                  {{ workflowBusinessTypeText(row.businessType) }}
                </template>
              </el-table-column>
              <el-table-column
                prop="processName"
                label="流程名称"
                min-width="160"
                show-overflow-tooltip
              />
              <el-table-column
                prop="processInsId"
                label="实例ID"
                min-width="180"
                show-overflow-tooltip
              />
              <el-table-column prop="processStatus" label="状态" width="110" align="center">
                <template #default="{ row }">
                  <el-tag :type="workflowStatusType(row.processStatus)" effect="plain">
                    {{ workflowStatusText(row.processStatus) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column
                prop="currentNode"
                label="当前节点"
                min-width="140"
                show-overflow-tooltip
              />
              <el-table-column prop="approvalTime" label="完成时间" width="170" align="center">
                <template #default="{ row }">
                  {{ row.approvalTime || row.createTime || '-' }}
                </template>
              </el-table-column>
              <el-table-column prop="printFileUrl" label="文件" width="110" align="center">
                <template #default="{ row }">
                  <el-button
                    v-if="row.printFileUrl"
                    text
                    type="primary"
                    @click="openWorkflowFile(row.printFileUrl, row)"
                  >
                    查看
                  </el-button>
                  <span v-else class="muted">-</span>
                </template>
              </el-table-column>
            </el-table>
            <el-empty v-if="workflowData.length === 0" description="暂无审批记录" />
          </el-tab-pane>

          <el-tab-pane label="操作日志" name="log">
            <el-timeline>
              <el-timeline-item
                v-for="item in logData"
                :key="item.logId"
                :timestamp="item.operateTime"
                placement="top"
              >
                <div class="contract-log">
                  <div class="contract-log__title">{{ item.actionDesc || item.action }}</div>
                  <div class="contract-log__operator">{{ item.operator }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
            <el-empty v-if="logData.length === 0" description="暂无操作记录" />
          </el-tab-pane>
        </el-tabs>
      </el-drawer>

      <el-drawer
        v-model="customerEditVisible"
        title="编辑租客"
        size="860px"
        append-to-body
        class="customer-edit-drawer"
        @close="resetCustomerEdit"
      >
        <el-form
          ref="customerEditFormRef"
          v-loading="customerEditLoading"
          :model="customerEditForm"
          :rules="customerEditRules"
          label-position="top"
          class="customer-edit-form"
        >
          <el-tabs v-model="customerEditTab" class="customer-edit-tabs">
            <el-tab-pane label="基本信息" name="basic">
              <section class="customer-edit-section">
                <div class="customer-edit-section__title">基本信息</div>
                <el-row :gutter="22">
                  <el-col :span="8">
                    <el-form-item label="租客姓名" prop="enterpriseName">
                      <el-input
                        v-model="customerEditForm.enterpriseName"
                        maxlength="200"
                        placeholder="请输入租客名称"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="行业分类">
                      <el-input
                        v-model="customerEditForm.industry"
                        maxlength="50"
                        placeholder="请选择"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="租客标签">
                      <customer-tag-selector
                        v-model="customerEditForm.tagIds"
                        compact
                        empty-text="请选择"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="租客编码">
                      <el-input v-model="customerEditForm.customerId" disabled />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item prop="approvalContactName">
                      <template #label>
                        <span>审批联系人</span>
                        <el-tooltip content="用于审批流表单回填" placement="top">
                          <i class="el-icon-question customer-help-icon" />
                        </el-tooltip>
                      </template>
                      <el-select
                        v-model="customerEditForm.approvalContactName"
                        filterable
                        allow-create
                        default-first-option
                        placeholder="请选择审批联系人"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="item in editContactOptions"
                          :key="`approval-${item}`"
                          :label="item"
                          :value="item"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="账单联系人">
                      <el-select
                        v-model="customerEditForm.billContactName"
                        filterable
                        allow-create
                        default-first-option
                        placeholder="请选择账单联系人"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="item in editContactOptions"
                          :key="`bill-${item}`"
                          :label="item"
                          :value="item"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="合同签署人">
                      <el-select
                        v-model="customerEditForm.contractSigner"
                        filterable
                        allow-create
                        default-first-option
                        placeholder="请选择合同签署人"
                        style="width: 100%"
                      >
                        <el-option
                          v-for="item in editContactOptions"
                          :key="`sign-${item}`"
                          :label="item"
                          :value="item"
                        />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="证件照片">
                      <div class="identity-upload-row">
                        <el-upload
                          action="/api/blade-resource/oss/endpoint/put-file"
                          :headers="uploadHeaders"
                          :show-file-list="false"
                          accept=".png,.jpg,.jpeg"
                          :before-upload="beforeIdentityUpload"
                          :on-success="(response, file) => handleIdentityUploadSuccess('front', response, file)"
                          :on-error="handleIdentityUploadError"
                          class="identity-upload"
                        >
                          <div class="identity-upload-card">
                            <el-image
                              v-if="customerEditForm.identityFrontUrl"
                              :src="customerEditForm.identityFrontUrl"
                              fit="cover"
                              class="identity-upload-image"
                            />
                            <div v-else class="identity-upload-placeholder">
                              <i class="el-icon-camera" />
                              <span>上传身份证人面像</span>
                            </div>
                          </div>
                        </el-upload>
                        <el-upload
                          action="/api/blade-resource/oss/endpoint/put-file"
                          :headers="uploadHeaders"
                          :show-file-list="false"
                          accept=".png,.jpg,.jpeg"
                          :before-upload="beforeIdentityUpload"
                          :on-success="(response, file) => handleIdentityUploadSuccess('back', response, file)"
                          :on-error="handleIdentityUploadError"
                          class="identity-upload"
                        >
                          <div class="identity-upload-card">
                            <el-image
                              v-if="customerEditForm.identityBackUrl"
                              :src="customerEditForm.identityBackUrl"
                              fit="cover"
                              class="identity-upload-image"
                            />
                            <div v-else class="identity-upload-placeholder">
                              <i class="el-icon-camera" />
                              <span>上传身份证国徽像</span>
                            </div>
                          </div>
                        </el-upload>
                      </div>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="介绍人">
                      <el-input
                        v-model="customerEditForm.channel"
                        maxlength="100"
                        placeholder="请输入介绍人"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="16">
                    <el-form-item label="备注">
                      <el-input
                        v-model="customerEditForm.remark"
                        type="textarea"
                        :rows="2"
                        maxlength="500"
                        placeholder="请输入备注"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </section>

              <section class="customer-edit-section">
                <div class="customer-edit-section__title">默认联系人</div>
                <el-row :gutter="22">
                  <el-col :span="8">
                    <el-form-item label="联系人姓名" prop="contactName">
                      <el-input
                        v-model="customerEditForm.contactName"
                        maxlength="50"
                        placeholder="请输入联系人姓名"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="联系电话">
                      <el-input
                        v-model="customerEditForm.contactPhone"
                        maxlength="30"
                        placeholder="请输入联系电话"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="联系邮箱">
                      <el-input
                        v-model="customerEditForm.contactEmail"
                        maxlength="100"
                        placeholder="请输入联系邮箱"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="联系人职务">
                      <el-input
                        v-model="customerEditForm.contactPosition"
                        maxlength="50"
                        placeholder="请输入联系人职务"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="16">
                    <el-form-item label="注册地址">
                      <el-input
                        v-model="customerEditForm.registeredAddress"
                        maxlength="500"
                        placeholder="请输入注册地址"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </section>
            </el-tab-pane>
            <el-tab-pane label="年度信息" name="annual">
              <section class="customer-edit-section">
                <div class="customer-edit-section__title">年度信息</div>
                <el-row :gutter="22">
                  <el-col :span="8">
                    <el-form-item label="上年度营收(万元)">
                      <el-input-number
                        v-model="customerEditForm.lastYearRevenue"
                        :min="0"
                        :precision="2"
                        controls-position="right"
                        style="width: 100%"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="主营业务">
                      <el-input
                        v-model="customerEditForm.mainBusiness"
                        maxlength="200"
                        placeholder="请输入主营业务"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="企业规模">
                      <el-input
                        v-model="customerEditForm.scale"
                        maxlength="50"
                        placeholder="请输入企业规模"
                      />
                    </el-form-item>
                  </el-col>
                  <el-col :span="24">
                    <el-form-item label="主要合作客户">
                      <el-input
                        v-model="customerEditForm.majorClients"
                        type="textarea"
                        :rows="3"
                        maxlength="1000"
                        placeholder="请输入主要合作客户"
                      />
                    </el-form-item>
                  </el-col>
                </el-row>
              </section>
            </el-tab-pane>
          </el-tabs>
        </el-form>
        <template #footer>
          <el-button @click="customerEditVisible = false">取消</el-button>
          <el-button type="primary" :loading="customerEditSaving" @click="submitCustomerEdit">
            确定
          </el-button>
        </template>
      </el-drawer>

      <el-dialog
        v-model="supplementVisible"
        title="新增附件"
        width="620px"
        append-to-body
        @close="resetSupplementForm"
      >
        <div class="supplement-contract">
          <div>
            <span>合同编号</span>
            <strong>{{ detailContract.contractNo || '-' }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ customerTitle }}</strong>
          </div>
        </div>
        <el-form
          ref="supplementFormRef"
          :model="supplementForm"
          :rules="supplementRules"
          label-position="top"
        >
          <el-form-item label="附件名称" prop="agreementName">
            <el-input
              v-model="supplementForm.agreementName"
              maxlength="100"
              show-word-limit
              placeholder="请输入附件名称"
            />
          </el-form-item>
          <el-form-item label="附件文件" prop="fileUrl">
            <el-upload
              drag
              ref="supplementUploadRef"
              action="/api/blade-resource/oss/endpoint/put-file"
              :headers="uploadHeaders"
              :limit="1"
              :file-list="supplementUploadFileList"
              :show-file-list="true"
              accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
              :before-upload="beforeSupplementUpload"
              :on-success="handleSupplementUploadSuccess"
              :on-error="handleSupplementUploadError"
              :on-remove="handleSupplementUploadRemove"
              class="supplement-upload"
            >
              <div class="supplement-upload__text">
                <strong>上传合同归档附件</strong>
                <span>支持 PDF、Word、JPG、PNG，单个文件不超过 20MB</span>
              </div>
            </el-upload>
          </el-form-item>
          <el-form-item label="备注">
            <el-input
              v-model="supplementForm.remark"
              type="textarea"
              :rows="2"
              maxlength="300"
              show-word-limit
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="supplementVisible = false">取消</el-button>
          <el-button type="primary" :loading="supplementLoading" @click="submitSupplement">
            确定上传
          </el-button>
        </template>
      </el-dialog>

      <el-drawer
        v-model="paymentBox"
        title="缴费计划"
        size="760px"
        append-to-body
        class="contract-payment-drawer"
      >
        <el-table v-loading="paymentLoading" :data="paymentData" border>
          <el-table-column prop="feeName" label="费用" width="110" />
          <el-table-column prop="periodStart" label="账期开始" width="110" />
          <el-table-column prop="periodEnd" label="账期结束" width="110" />
          <el-table-column prop="amountDue" label="应收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountDue) }}</template>
          </el-table-column>
          <el-table-column prop="amountPaid" label="实收" width="100" align="right">
            <template #default="{ row }">{{ formatMoney(row.amountPaid) }}</template>
          </el-table-column>
          <el-table-column prop="payDeadline" label="应缴日期" width="110" />
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag :type="paymentTagType(row)" effect="plain">{{
                paymentStatusText(row.payStatus)
              }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="170" fixed="right" align="center">
            <template #default="{ row }">
              <div class="bill-table-actions">
                <template v-if="row.payStatus !== '1'">
                  <el-button type="primary" text @click="handleConfirmPayment(row)">确认</el-button>
                  <el-button type="primary" text @click="handleRemind(row)">催缴</el-button>
                </template>
                <span v-else class="muted">已缴费</span>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </el-drawer>

      <el-dialog
        v-model="precheckVisible"
        :title="precheckTitle"
        width="560px"
        append-to-body
        class="precheck-dialog"
      >
        <div class="precheck-list">
          <div
            v-for="item in precheckItems"
            :key="item.label"
            :class="['precheck-item', { 'is-done': item.done }]"
          >
            <div>
              <strong>{{ item.label }}</strong>
              <span>{{ item.done ? '已满足' : item.pendingText }}</span>
            </div>
            <el-tag :type="item.done ? 'success' : 'warning'" effect="plain">
              {{ item.done ? '完成' : '待处理' }}
            </el-tag>
          </div>
        </div>
        <template #footer>
          <el-button @click="precheckVisible = false">关闭</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="approvalVisible"
        :title="approvalDialogTitle"
        width="760px"
        append-to-body
        @close="resetApprovalDialog"
      >
        <div class="approval-dialog">
          <section class="detail-section">
            <div class="detail-section-title">{{ approvalSummaryTitle }}</div>
            <div class="contract-field-grid">
              <div v-for="item in approvalSummaryItems" :key="item.label" class="contract-field">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
              </div>
            </div>
          </section>

          <section class="detail-section">
            <div class="detail-section-title">审批配置</div>
            <el-form :model="approvalForm" label-width="108px">
              <el-form-item label="审批流程" required>
                <el-select
                  v-model="approvalForm.processDefKey"
                  filterable
                  :loading="approvalLoading"
                  :placeholder="approvalProcessPlaceholder"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in approvalProcessOptions"
                    :key="item.id || item.key"
                    :label="approvalProcessLabel(item)"
                    :value="item.key"
                  >
                    <div class="approval-option">
                      <span>{{ item.name || item.key }}</span>
                      <em
                        >{{ item.key
                        }}<template v-if="item.version"> / v{{ item.version }}</template></em
                      >
                    </div>
                  </el-option>
                </el-select>
              </el-form-item>
            </el-form>
          </section>

          <section v-if="isTerminationWorkflow" class="detail-section">
            <div class="detail-section-title">退租信息</div>
            <el-form :model="approvalExtra" label-width="108px">
              <el-form-item label="退租类型">
                <el-radio-group v-model="approvalExtra.terminationType">
                  <el-radio-button label="normal">正常退租</el-radio-button>
                  <el-radio-button label="early">客户提前退租</el-radio-button>
                  <el-radio-button label="special">特殊情况</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="退租日期">
                <el-date-picker
                  v-model="approvalExtra.expectedTerminationDate"
                  value-format="YYYY-MM-DD"
                  type="date"
                  placeholder="请选择退租日期"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item v-if="approvalExtra.terminationType !== 'normal'" label="违约金">
                <el-input-number
                  v-model="approvalExtra.breachPenalty"
                  :min="0"
                  :precision="2"
                  controls-position="right"
                  style="width: 100%"
                />
              </el-form-item>
              <el-form-item label="退租原因">
                <el-input
                  v-model="approvalExtra.terminationReason"
                  type="textarea"
                  :rows="3"
                  placeholder="请填写退租原因"
                />
              </el-form-item>
            </el-form>
          </section>
        </div>
        <template #footer>
          <el-button @click="approvalVisible = false">取消</el-button>
          <el-button
            type="primary"
            :disabled="!approvalForm.processDefKey || approvalLoading"
            @click="goWorkflowApproval"
          >
            下一步
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="contractChangeVisible"
        title="新增合同变更"
        width="640px"
        append-to-body
        class="contract-change-dialog"
        @closed="resetContractChange"
      >
        <el-form
          ref="contractChangeFormRef"
          :model="contractChangeForm"
          :rules="contractChangeRules"
          label-width="104px"
          class="contract-change-form"
        >
          <el-row :gutter="24">
            <el-col :span="12">
              <el-form-item label="合同" prop="contractLabel">
                <el-input v-model="contractChangeForm.contractLabel" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="变更类型" prop="changeType">
                <el-select
                  v-model="contractChangeForm.changeType"
                  placeholder="请选择变更类型"
                  style="width: 100%"
                >
                  <el-option
                    v-for="item in contractChangeTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新租金单价" prop="newRentPrice">
                <el-input-number
                  v-model="contractChangeForm.newRentPrice"
                  :min="0.01"
                  :precision="2"
                  :controls="false"
                  placeholder="元/㎡/月"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新月租金" prop="newMonthlyRent">
                <el-input-number
                  v-model="contractChangeForm.newMonthlyRent"
                  :min="0.01"
                  :precision="2"
                  :controls="false"
                  placeholder="元/月"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="新结束日期" prop="newEndDate">
                <el-date-picker
                  v-model="contractChangeForm.newEndDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="请选择日期"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="变更原因" prop="reason">
            <el-input
              v-model="contractChangeForm.reason"
              type="textarea"
              :rows="4"
              maxlength="1000"
              show-word-limit
              placeholder="请输入变更原因"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="contractChangeVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="contractChangeSaving"
            @click="submitContractChange"
          >
            确定
          </el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="renewVisible"
        title="合同续租"
        width="552px"
        append-to-body
        class="renew-dialog"
        @close="resetRenewDialog"
      >
        <el-alert
          class="renew-tip"
          title="提示"
          type="info"
          show-icon
          :closable="false"
          description="合同续租，将使用原有条款新建合同，新合同将在租期起日开始执行"
        />
        <el-form ref="renewFormRef" :model="renewForm" :rules="renewRules" label-position="top">
          <el-form-item label="租期起始日期" prop="dateRange" required>
            <el-date-picker
              v-model="renewForm.dateRange"
              type="daterange"
              value-format="YYYY-MM-DD"
              start-placeholder="开始时间"
              end-placeholder="结束时间"
              range-separator="至"
              style="width: 100%"
            />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="renewVisible = false">取消</el-button>
          <el-button type="primary" @click="confirmRenew">确定</el-button>
        </template>
      </el-dialog>

      <el-dialog
        v-model="signedUploadVisible"
        title="上传盖章合同"
        width="560px"
        append-to-body
        @close="resetSignedUpload"
      >
        <section class="signed-upload-summary">
          <div>
            <span>合同编号</span>
            <strong>{{ signedUploadContract.contractNo || '-' }}</strong>
          </div>
          <div>
            <span>企业名称</span>
            <strong>{{ signedUploadContract.customerName || '-' }}</strong>
          </div>
        </section>
        <el-upload
          drag
          ref="signedUploadRef"
          action="/api/blade-resource/oss/endpoint/put-file"
          :headers="uploadHeaders"
          :limit="1"
          :show-file-list="true"
          accept=".pdf,.doc,.docx,.png,.jpg,.jpeg"
          :before-upload="beforeSignedFileUpload"
          :on-success="handleSignedFileSuccess"
          :on-error="handleSignedFileError"
          :on-remove="handleSignedFileRemove"
          class="signed-upload-box"
        >
          <div class="signed-upload-text">
            <strong>上传已盖章合同扫描件</strong>
            <span>支持 PDF、Word、图片等合同归档文件</span>
          </div>
        </el-upload>
        <template #footer>
          <el-button @click="signedUploadVisible = false">取消</el-button>
          <el-button
            type="primary"
            :loading="signedUploadLoading"
            :disabled="!signedUploadForm.contractFileUrl"
            @click="submitSignedUpload"
          >
            确认生效
          </el-button>
        </template>
      </el-dialog>

      <notice-preview-dialog
        v-model="noticePreview.visible"
        :title="noticePreview.title"
        :html="noticePreview.html"
        :loading="noticePreview.loading"
        :download-url="noticePreview.downloadUrl"
        :download-label="noticePreview.downloadLabel"
        @download="downloadNoticePreviewFile"
      />
    </div>
  </basic-container>
</template>

<script>
import { Base64 } from 'js-base64';
import { Promotion } from '@element-plus/icons-vue';
import NoticePreviewDialog from '@/components/contract/notice-preview-dialog.vue';
import {
  applyContractChange,
  confirmPayment,
  getDetail,
  getExpiring,
  getList,
  getLogs,
  getPayment,
  getStats,
  getWorkflowRecords,
  remindPayment,
  remove,
  terminate,
  uploadSignedContract,
} from '@/api/contract/contract';
import {
  getSupplementAgreements,
  removeSupplementAgreement,
  saveSupplementAgreement,
} from '@/api/contract/archive';
import { noticePrintUrl } from '@/api/contract/print';
import {
  getCustomerDetail,
  removeCustomer,
  updateCustomer,
} from '@/api/business/customer';
import { getList as getDeploymentList } from '@/views/plugin/workflow/api/design/deployment';
import { paymentCycleDic, paymentStatusDic, statusDic } from '@/option/contract/contract';
import { mapGetters } from 'vuex';
import { getToken } from '@/utils/auth';
import CustomerTagSelector from '@/views/business/modules/customer-tag-selector.vue';
import {
  createNoticePreviewState,
  downloadNoticeFile,
  openAttachmentPreview,
  openNoticePreview,
} from '@/utils/contract-notice';

const CONTRACT_APPROVAL_BUSINESS_TYPE = 'contract_approval';
const CONTRACT_PAYMENT_BUSINESS_TYPE = 'contract_payment';
const CONTRACT_ROOM_REVIEW_BUSINESS_TYPE = 'contract_room_review';
const CONTRACT_TERMINATION_BUSINESS_TYPE = 'contract_termination';
const CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE = 'contract_overdue_legal';
const WORKFLOW_TYPES = {
  contract_approval: {
    title: '发起合同审批',
    summaryTitle: '合同信息',
    processPlaceholder: '请选择已部署的合同审批流程',
    formKeys: ['contract', 'wf_ex_contract'],
    defaultKeys: ['contract_sign'],
    nameKeywords: ['合同审批', '合同'],
  },
  contract_payment: {
    title: '发起付款流程',
    summaryTitle: '账单信息',
    processPlaceholder: '请选择已部署的付款流程',
    formKeys: ['pay', 'invoice'],
    defaultKeys: ['pay'],
    nameKeywords: ['付款', '缴费', '付款通知'],
  },
  contract_room_review: {
    title: '发起房屋验收',
    summaryTitle: '退租交接信息',
    processPlaceholder: '请选择已部署的房屋验收流程',
    formKeys: ['return'],
    defaultKeys: ['roomreview'],
    nameKeywords: ['房屋验收', '交接验收', '归还载体'],
  },
  contract_termination: {
    title: '发起退租审批',
    summaryTitle: '退租信息',
    processPlaceholder: '请选择已部署的退租审批流程',
    formKeys: ['terminate'],
    defaultKeys: ['termination'],
    nameKeywords: ['退租审批', '退租'],
  },
  contract_overdue_legal: {
    title: '发起逾期律师函审批',
    summaryTitle: '逾期账单信息',
    processPlaceholder: '请选择已部署的逾期律师函流程',
    formKeys: ['project-approval-law'],
    defaultKeys: ['law'],
    nameKeywords: ['律师函', '逾期', '项目审批'],
  },
};

const createDefaultCustomerEditForm = () => ({
  customerId: null,
  enterpriseName: '',
  industry: '',
  tenantType: 'personal',
  tagIds: [],
  creditCode: '',
  approvalContactName: '',
  billContactName: '',
  contractSigner: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  contactPosition: '',
  identityFrontUrl: '',
  identityBackUrl: '',
  registeredAddress: '',
  address: '',
  channel: '',
  remark: '',
  lastYearRevenue: undefined,
  mainBusiness: '',
  scale: '',
  majorClients: '',
});

const createDefaultContractChangeForm = () => ({
  contractId: null,
  contractLabel: '',
  changeType: '',
  newRentPrice: null,
  newMonthlyRent: null,
  newEndDate: '',
  reason: '',
});

export default {
  name: 'ContractList',
  components: {
    CustomerTagSelector,
    NoticePreviewDialog,
    Promotion,
  },
  data() {
    return {
      query: {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      },
      loading: false,
      page: {
        pageSize: 10,
        currentPage: 1,
        total: 0,
      },
      selectionList: [],
      data: [],
      stats: {},
      expiringData: [],
      detailMode: false,
      detailLoading: false,
      profileTab: 'info',
      detailVisible: false,
      detailTab: 'info',
      detailContract: {},
      customerDetail: {},
      customerContracts: [],
      paymentBox: false,
      paymentLoading: false,
      paymentData: [],
      supplements: [],
      workflowLoading: false,
      workflowData: [],
      logData: [],
      contractTextDownloading: false,
      activeDownloadContractId: '',
      customerEditVisible: false,
      customerEditLoading: false,
      customerEditSaving: false,
      customerEditTab: 'basic',
      customerEditForm: createDefaultCustomerEditForm(),
      customerEditRules: {
        enterpriseName: [{ required: true, message: '请输入租客名称', trigger: 'blur' }],
        contactName: [{ required: true, message: '请输入联系人姓名', trigger: 'blur' }],
      },
      supplementVisible: false,
      supplementLoading: false,
      supplementUploadFileList: [],
      supplementForm: {
        agreementName: '',
        changeItem: '合同档案附件',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      },
      supplementRules: {
        agreementName: [{ required: true, message: '请输入附件名称', trigger: 'blur' }],
        fileUrl: [{ required: true, message: '请上传附件文件', trigger: 'change' }],
      },
      uploadHeaders: {
        'Blade-Auth': `bearer ${getToken()}`,
        'Blade-Requested-With': 'BladeHttpRequest',
      },
      signedUploadVisible: false,
      signedUploadLoading: false,
      signedUploadContract: {},
      signedUploadForm: {
        contractFileUrl: '',
      },
      contractChangeVisible: false,
      contractChangeSaving: false,
      contractChangeForm: createDefaultContractChangeForm(),
      contractChangeTypeOptions: [
        { label: '租金变更', value: '租金变更' },
        { label: '租期变更', value: '租期变更' },
        { label: '租金及租期变更', value: '租金及租期变更' },
        { label: '其他', value: '其他' },
      ],
      contractChangeRules: {
        contractLabel: [{ required: true, message: '请选择合同', trigger: 'change' }],
        changeType: [{ required: true, message: '请选择变更类型', trigger: 'change' }],
        reason: [{ required: true, message: '请输入变更原因', trigger: 'blur' }],
      },
      renewVisible: false,
      renewContract: {},
      renewForm: {
        dateRange: [],
      },
      precheckVisible: false,
      precheckTitle: '',
      precheckItems: [],
      renewRules: {
        dateRange: [
          {
            required: true,
            validator: (rule, value, callback) => {
              if (!Array.isArray(value) || value.length !== 2 || !value[0] || !value[1]) {
                callback(new Error('请选择租期起始日期'));
                return;
              }
              if (new Date(value[1]).getTime() <= new Date(value[0]).getTime()) {
                callback(new Error('结束时间必须晚于开始时间'));
                return;
              }
              callback();
            },
            trigger: 'change',
          },
        ],
      },
      routeDetailOpenedContractId: '',
      approvalVisible: false,
      approvalLoading: false,
      approvalType: CONTRACT_APPROVAL_BUSINESS_TYPE,
      approvalContract: {},
      approvalPayment: {},
      approvalProcessOptions: [],
      approvalForm: {
        processDefKey: '',
      },
      approvalExtra: {
        terminationType: 'normal',
        expectedTerminationDate: '',
        breachPenalty: 0,
        terminationReason: '',
      },
      noticePreview: createNoticePreviewState(),
    };
  },
  computed: {
    ...mapGetters(['permission', 'userInfo']),
    statusOptions() {
      return statusDic;
    },
    singleSelectedContract() {
      return this.selectionList.length === 1 ? this.selectionList[0] : null;
    },
    summaryCards() {
      return [
        { key: 'total', label: '合同总数', value: this.stats.totalCount || 0 },
        { key: 'active', label: '生效', value: this.stats.activeCount || 0 },
        { key: 'pending', label: '待审批', value: this.stats.pendingCount || 0 },
        { key: 'rent', label: '月租金合计', value: this.formatMoney(this.stats.monthlyRentTotal) },
      ];
    },
    ids() {
      return this.selectionList.map(item => item.contractId).join(',');
    },
    currentCustomerId() {
      return this.customerDetail.customerId || this.detailContract.customerId || '';
    },
    customerTitle() {
      return (
        this.customerDetail.enterpriseName ||
        this.detailContract.customerName ||
        this.detailContract.contractName ||
        '租客详情'
      );
    },
    customerTags() {
      const tags = this.customerDetail.tags || [];
      if (!Array.isArray(tags)) return [];
      return tags
        .map(item => item.tagName || item.name || item.label || item.tagValue || '')
        .filter(Boolean);
    },
    contractRows() {
      if (this.customerContracts.length) {
        return this.customerContracts;
      }
      return this.detailContract && this.detailContract.contractId ? [this.detailContract] : [];
    },
    detailHeaderTitle() {
      return this.detailContract.contractName || this.customerTitle || '合同详情';
    },
    baseContractInfoItems() {
      const contract = this.detailContract || {};
      return [
        { label: '合同编号', value: this.detailValue(contract.contractNo) },
        { label: '合同名称', value: this.detailValue(contract.contractName) },
        {
          label: '合同状态',
          value: contract.contractStatusName || this.statusText(contract.contractStatus),
        },
        { label: '审批状态', value: this.approvalStatusText(contract.approvalStatus) },
        { label: '经办人', value: this.detailValue(contract.followUser || contract.createBy) },
        { label: '创建人', value: this.detailValue(contract.createBy) },
        { label: '签订日', value: this.detailValue(contract.signDate) },
        { label: '所属公司', value: this.detailValue(contract.parkName) },
        { label: '合同开始日', value: this.detailValue(contract.startDate) },
        { label: '合同结束日', value: this.detailValue(contract.endDate) },
        { label: '合同来源', value: this.contractSourceText(contract) },
        { label: '续签提醒', value: contract.renewalRemindDays ? `${contract.renewalRemindDays}天` : '--' },
      ];
    },
    roomInfoItems() {
      const contract = this.detailContract || {};
      return [
        { label: '房源信息', value: this.detailValue(contract.roomName || contract.buildingName) },
        { label: '楼栋', value: this.detailValue(contract.buildingName) },
        { label: '园区', value: this.detailValue(contract.parkName) },
        { label: '面积', value: this.formatAreaOrDash(contract.rentArea) },
      ];
    },
    tenantInfoItems() {
      const customer = this.customerDetail || {};
      const contract = this.detailContract || {};
      const baseItems = [
        { label: '租客', value: this.detailValue(customer.enterpriseName || contract.customerName) },
        { label: '签订人', value: this.detailValue(customer.contractSigner || contract.customerName) },
        { label: '租客编码', value: this.detailValue(customer.customerId || this.currentCustomerId) },
      ];
      if (!customer.customerId) {
        return baseItems;
      }
      return [
        ...baseItems,
        { label: '租客类型', value: this.tenantTypeText(customer.tenantType) },
        { label: '行业', value: this.detailValue(customer.industry) },
        { label: '联系人', value: this.detailValue(customer.contactName || contract.followUser) },
        { label: '联系电话', value: this.detailValue(customer.contactPhone) },
        { label: '联系邮箱', value: this.detailValue(customer.contactEmail) },
        { label: '联系人职务', value: this.detailValue(customer.contactPosition) },
        { label: '审批联系人', value: this.detailValue(customer.approvalContactName) },
        { label: '账单联系人', value: this.detailValue(customer.billContactName) },
        { label: '租客标签', value: this.customerTags.length ? this.customerTags.join('、') : '--' },
        { label: '统一社会信用代码', value: this.detailValue(customer.creditCode) },
        { label: '企业类型', value: this.detailValue(customer.enterpriseType) },
        { label: '成立日期', value: this.detailValue(customer.establishDate) },
        { label: '注册资本', value: this.detailValue(customer.registeredCapital) },
        { label: '企业规模', value: this.detailValue(customer.scale) },
        { label: '主营业务', value: this.detailValue(customer.mainBusiness) },
        { label: '上年度营收', value: this.detailValue(customer.lastYearRevenue) },
        { label: '主要合作客户', value: this.detailValue(customer.majorClients) },
        { label: '招商渠道', value: this.detailValue(customer.channel) },
        { label: '注册地址', value: this.detailValue(customer.registeredAddress || customer.address) },
        { label: '企业地址', value: this.detailValue(customer.address) },
        { label: '经营范围', value: this.detailValue(customer.businessScope) },
      ];
    },
    lateFeeInfoItems() {
      const contract = this.detailContract || {};
      return [
        { label: '滞纳金规则', value: this.lateFeeText(contract) },
        { label: '滞纳金上限', value: this.formatMoneyWithUnitOrDash(contract.lateFeeCap) },
        { label: '租金递增', value: this.rentIncreaseText(contract) },
      ];
    },
    rentBasicClauseItems() {
      const contract = this.detailContract || {};
      return [
        { label: '房源信息', value: this.detailValue(contract.roomName || contract.buildingName) },
        { label: '计租面积', value: this.formatAreaOrDash(contract.rentArea) },
        { label: '合同类型', value: this.contractTextTypeText(contract) },
      ];
    },
    depositClauseItems() {
      const contract = this.detailContract || {};
      return [
        { label: '保证金金额', value: this.formatMoneyWithUnitOrDash(contract.deposit) },
      ];
    },
    rentPaymentClauseItems() {
      const contract = this.detailContract || {};
      return [
        { label: '计租方式', value: this.contractTextTypeText(contract) },
        { label: '开始时间', value: this.detailValue(contract.startDate) },
        { label: '结束时间', value: this.detailValue(contract.endDate) },
        { label: '合同单价', value: this.formatUnitPrice(contract.rentPrice) },
        { label: '月租金', value: this.formatMoneyWithUnitOrDash(contract.monthlyRent) },
        { label: '年天数', value: this.contractDays(contract) },
        { label: '付款周期', value: this.paymentCycleText(contract.paymentCycle) },
        { label: '租金递增', value: this.rentIncreaseText(contract) },
      ];
    },
    hasPropertyTerms() {
      const contract = this.detailContract || {};
      return Boolean(contract.propertyFee || contract.managementFee || contract.publicFee);
    },
    propertyBasicClauseItems() {
      const contract = this.detailContract || {};
      return [
        { label: '房源信息', value: this.detailValue(contract.roomName || contract.buildingName) },
        { label: '计费面积', value: this.formatAreaOrDash(contract.rentArea) },
        { label: '物业单价', value: this.formatUnitPrice(contract.propertyFee) },
        { label: '管理费', value: this.formatMoneyWithUnitOrDash(contract.managementFee) },
        { label: '公摊费', value: this.formatMoneyWithUnitOrDash(contract.publicFee) },
      ];
    },
    propertyFeeClauseItems() {
      const contract = this.detailContract || {};
      return [
        { label: '开始时间', value: this.detailValue(contract.startDate) },
        { label: '结束时间', value: this.detailValue(contract.endDate) },
        { label: '年天数', value: this.contractDays(contract) },
        { label: '付款周期', value: this.paymentCycleText(contract.paymentCycle) },
      ];
    },
    approvalFileRecord() {
      const record = (this.workflowData || []).find(
        item => item.businessType === CONTRACT_APPROVAL_BUSINESS_TYPE && item.printFileUrl
      );
      if (record) {
        return record;
      }
      if (!this.detailContract.approvalFileUrl) {
        return null;
      }
      return {
        businessType: CONTRACT_APPROVAL_BUSINESS_TYPE,
        contractId: this.detailContract.contractId,
        printFileUrl: this.detailContract.approvalFileUrl,
        processName: '合同会签审批表',
      };
    },
    editContactOptions() {
      return [
        this.customerEditForm.contactName,
        this.customerEditForm.approvalContactName,
        this.customerEditForm.billContactName,
        this.customerEditForm.contractSigner,
      ]
        .map(item => String(item || '').trim())
        .filter((item, index, list) => item && list.indexOf(item) === index);
    },
    expiringSummary() {
      return this.expiringData
        .slice(0, 3)
        .map(item => `${item.customerName || item.contractName || '-'}：${item.endDate || '-'}`)
        .join('；');
    },
    detailTitle() {
      return this.detailContract.contractName || '合同详情';
    },
    approvalConfig() {
      return WORKFLOW_TYPES[this.approvalType] || WORKFLOW_TYPES[CONTRACT_APPROVAL_BUSINESS_TYPE];
    },
    approvalDialogTitle() {
      return this.approvalConfig.title;
    },
    approvalSummaryTitle() {
      return this.approvalConfig.summaryTitle;
    },
    approvalProcessPlaceholder() {
      return this.approvalConfig.processPlaceholder;
    },
    isTerminationWorkflow() {
      return this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE;
    },
    approvalSummaryItems() {
      const contract = this.approvalContract || {};
      const payment = this.approvalPayment || {};
      if (this.approvalType === CONTRACT_PAYMENT_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || payment.customerName || '-' },
          { label: '费用类型', value: payment.feeName || '-' },
          {
            label: '费用期间',
            value: `${payment.periodStart || '--'} 至 ${payment.periodEnd || '--'}`,
          },
          { label: '应缴金额', value: this.formatMoney(payment.amountDue) },
          { label: '缴费截止日', value: payment.payDeadline || '-' },
        ];
      }
      if (this.approvalType === CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || payment.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || payment.customerName || '-' },
          { label: '逾期费用', value: payment.feeName || '-' },
          {
            label: '欠费期间',
            value: `${payment.periodStart || '--'} 至 ${payment.periodEnd || '--'}`,
          },
          { label: '欠费金额', value: this.formatMoney(this.unpaidAmount(payment)) },
          { label: '逾期天数', value: `${this.overdueDays(payment)}天` },
        ];
      }
      if (this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || '-' },
          { label: '租赁房源', value: contract.roomName || contract.buildingName || '-' },
          {
            label: '合同期限',
            value: `${contract.startDate || '--'} 至 ${contract.endDate || '--'}`,
          },
          { label: '保证金', value: this.formatMoney(contract.deposit) },
          { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
        ];
      }
      if (this.approvalType === CONTRACT_ROOM_REVIEW_BUSINESS_TYPE) {
        return [
          { label: '合同编号', value: contract.contractNo || '-' },
          { label: '企业名称', value: contract.customerName || '-' },
          { label: '交接房源', value: contract.roomName || contract.buildingName || '-' },
          { label: '租赁面积', value: this.formatAreaValue(contract.rentArea) },
          { label: '保证金', value: this.formatMoney(contract.deposit) },
          {
            label: '退租状态',
            value: contract.contractStatusName || this.statusText(contract.contractStatus),
          },
        ];
      }
      return [
        { label: '合同编号', value: contract.contractNo || '-' },
        { label: '合同名称', value: contract.contractName || '-' },
        { label: '企业名称', value: contract.customerName || '-' },
        { label: '房源信息', value: contract.roomName || contract.buildingName || '-' },
        {
          label: '合同期限',
          value: `${contract.startDate || '--'} 至 ${contract.endDate || '--'}`,
        },
        { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
      ];
    },
    contractSummary() {
      const contract = this.detailContract || {};
      return `合同摘要：起租日 ${contract.startDate || '--'}，租赁面积 ${this.formatAreaOrDash(
        contract.rentArea
      )}，租金单价 ${this.formatUnitPrice(contract.rentPrice)}，月租金 ${this.formatMoneyWithUnitOrDash(
        contract.monthlyRent
      )}。`;
    },
    basicDetailItems() {
      const contract = this.detailContract || {};
      return [
        { label: '合同编号', value: contract.contractNo || '-' },
        {
          label: '合同状态',
          value: contract.contractStatusName || this.statusText(contract.contractStatus),
        },
        { label: '审批状态', value: this.approvalStatusText(contract.approvalStatus) },
        { label: '当前节点', value: contract.approvalCurrentNodeName || '-' },
        { label: '所属园区', value: contract.parkName || '-' },
        { label: '签订日', value: contract.signDate || '-' },
        { label: '合同开始日', value: contract.startDate || '-' },
        { label: '合同结束日', value: contract.endDate || '-' },
        { label: '租赁面积', value: this.formatAreaValue(contract.rentArea) },
        { label: '月租金', value: this.formatMoney(contract.monthlyRent) },
        { label: '租金单价', value: this.formatUnitPrice(contract.rentPrice) },
        { label: '物业费', value: this.formatUnitPrice(contract.propertyFee) },
        { label: '押金', value: this.formatMoney(contract.deposit) },
        { label: '滞纳金', value: this.lateFeeText(contract) },
        { label: '租金递增', value: contract.rentIncreaseNode || '-' },
        {
          label: '续签提醒',
          value: contract.renewalRemindDays ? `${contract.renewalRemindDays}天` : '-',
        },
      ];
    },
  },
  created() {
    this.applyRouteQuery();
    this.reload();
  },
  watch: {
    '$route.query'() {
      this.applyRouteQuery();
      this.reload();
    },
  },
  methods: {
    applyRouteQuery() {
      const routeQuery = this.$route.query || {};
      if (routeQuery.customerId) {
        this.query.customerId = routeQuery.customerId;
      }
      if (routeQuery.customerName) {
        this.query.customerName = routeQuery.customerName;
      }
      if (routeQuery.contractId) {
        this.query.contractId = routeQuery.contractId;
        this.routeDetailOpenedContractId = '';
      } else {
        delete this.query.contractId;
        this.routeDetailOpenedContractId = '';
      }
    },
    defaultApprovalExtra() {
      return {
        terminationType: 'normal',
        expectedTerminationDate: this.formatDate(this.addDays(new Date(), 30)),
        breachPenalty: 0,
        terminationReason: '',
      };
    },
    terminationTypeText(value) {
      const map = {
        normal: '正常退租',
        early: '客户提前退租',
        special: '特殊情况',
      };
      return map[value] || '正常退租';
    },
    handleAdd() {
      this.$router.push({ path: '/contract/create-template', query: { mode: 'create' } });
    },
    handleStartApprovalFromSelection() {
      if (
        !this.ensurePrerequisites(
          '合同审批前置条件',
          this.selectionContractApprovalPrerequisites()
        )
      ) {
        return;
      }
      this.handleStartApproval(this.singleSelectedContract);
    },
    handleStartApproval(row) {
      if (
        !this.ensurePrerequisites('合同审批前置条件', this.contractApprovalPrerequisites(row))
      ) {
        return;
      }
      this.handleStartWorkflow(CONTRACT_APPROVAL_BUSINESS_TYPE, row);
    },
    handleStartOverdueApproval(row) {
      if (
        !this.ensurePrerequisites(
          '逾期处理前置条件',
          this.overdueApprovalPrerequisites(this.detailContract, row)
        )
      ) {
        return;
      }
      this.handleStartWorkflow(CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE, this.detailContract, row);
    },
    handleStartTermination(row) {
      if (
        !this.ensurePrerequisites('退租审批前置条件', this.terminationPrerequisites(row))
      ) {
        return;
      }
      this.handleStartWorkflow(CONTRACT_TERMINATION_BUSINESS_TYPE, row);
    },
    handleStartRoomReview(row) {
      if (
        !this.ensurePrerequisites('房屋验收前置条件', this.roomReviewPrerequisites(row))
      ) {
        return;
      }
      this.handleStartWorkflow(CONTRACT_ROOM_REVIEW_BUSINESS_TYPE, row);
    },
    handleContractChange(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要变更的合同');
        return;
      }
      this.contractChangeForm = {
        ...createDefaultContractChangeForm(),
        contractId: row.contractId,
        contractLabel: [row.contractNo, row.contractName || row.customerName]
          .filter(Boolean)
          .join(' - '),
      };
      this.contractChangeVisible = true;
      this.$nextTick(() => {
        if (this.$refs.contractChangeFormRef) {
          this.$refs.contractChangeFormRef.clearValidate();
        }
      });
    },
    submitContractChange() {
      this.$refs.contractChangeFormRef.validate(valid => {
        if (!valid) return;
        const form = this.contractChangeForm;
        const contract = this.detailContract || {};
        const rentPriceChanged = this.contractChangeAmountChanged(
          contract.rentPrice,
          form.newRentPrice
        );
        const monthlyRentChanged = this.contractChangeAmountChanged(
          contract.monthlyRent,
          form.newMonthlyRent
        );
        const rentChanged = rentPriceChanged || monthlyRentChanged;
        const endDateChanged =
          Boolean(form.newEndDate) && String(form.newEndDate) !== String(contract.endDate || '');
        if (!rentChanged && !endDateChanged) {
          this.$message.warning('请至少填写一项与原合同不同的变更内容');
          return;
        }
        if (form.changeType === '租金变更' && !rentChanged) {
          this.$message.warning('租金变更需填写新的租金单价或月租金');
          return;
        }
        if (form.changeType === '租期变更' && !endDateChanged) {
          this.$message.warning('租期变更需填写新的合同结束日期');
          return;
        }
        if (form.changeType === '租金及租期变更' && (!rentChanged || !endDateChanged)) {
          this.$message.warning('租金及租期变更需同时填写新租金和新结束日期');
          return;
        }
        this.contractChangeSaving = true;
        applyContractChange({
          contractId: form.contractId,
          changeType: form.changeType,
          newRentPrice: rentPriceChanged ? form.newRentPrice : null,
          newMonthlyRent: monthlyRentChanged ? form.newMonthlyRent : null,
          newEndDate: endDateChanged ? form.newEndDate : null,
          reason: form.reason.trim(),
        })
          .then(() => {
            const contractId = form.contractId;
            this.$message.success('合同变更已登记并生效');
            this.contractChangeVisible = false;
            this.reload();
            this.openDetail({ contractId });
          })
          .finally(() => {
            this.contractChangeSaving = false;
          });
      });
    },
    contractChangeAmountChanged(oldValue, newValue) {
      if (newValue === null || newValue === undefined || newValue === '') return false;
      return Number(oldValue) !== Number(newValue);
    },
    resetContractChange() {
      this.contractChangeSaving = false;
      this.contractChangeForm = createDefaultContractChangeForm();
    },
    handleRenew(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要续租的合同');
        return;
      }
      this.renewContract = { ...(row || {}) };
      const startDate = this.nextDate(row.endDate) || this.formatDate(new Date());
      const endDate = this.defaultRenewEndDate(startDate, row.startDate, row.endDate);
      this.renewForm.dateRange = [startDate, endDate];
      this.renewVisible = true;
      this.$nextTick(() => {
        if (this.$refs.renewFormRef) {
          this.$refs.renewFormRef.clearValidate();
        }
      });
    },
    confirmRenew() {
      this.$refs.renewFormRef.validate(valid => {
        if (!valid) return;
        const [startDate, endDate] = this.renewForm.dateRange || [];
        this.renewVisible = false;
        this.$router.push({
          path: '/contract/create-template',
          query: {
            mode: 'renew',
            sourceContractId: this.renewContract.contractId,
            renewStartDate: startDate,
            renewEndDate: endDate,
          },
        });
      });
    },
    resetRenewDialog() {
      this.renewContract = {};
      this.renewForm = {
        dateRange: [],
      };
    },
    handleStartWorkflow(type, contract = {}, payment = {}) {
      this.approvalType = type || CONTRACT_APPROVAL_BUSINESS_TYPE;
      this.approvalContract = { ...(contract || {}) };
      this.approvalPayment = { ...(payment || {}) };
      this.approvalExtra = this.defaultApprovalExtra();
      this.approvalForm.processDefKey = '';
      this.approvalVisible = true;
      this.loadApprovalProcessOptions();
    },
    loadApprovalProcessOptions() {
      this.approvalLoading = true;
      getDeploymentList(1, -1, { status: 1 })
        .then(res => {
          const records = (res.data.data || {}).records || [];
          this.approvalProcessOptions = records
            .filter(item => this.isWorkflowProcess(item))
            .sort((a, b) => {
              const versionDiff = Number(b.version || 0) - Number(a.version || 0);
              if (versionDiff !== 0) return versionDiff;
              const timeA = new Date(a.deployTime || a.createTime || 0).getTime();
              const timeB = new Date(b.deployTime || b.createTime || 0).getTime();
              return timeB - timeA;
            });
          this.approvalForm.processDefKey = this.resolveApprovalProcessKey(
            this.approvalProcessOptions,
            this.approvalForm.processDefKey
          );
          if (this.approvalProcessOptions.length === 0) {
            this.$message.warning(
              `未找到可用的${this.approvalDialogTitle.replace('发起', '')}，请先在部署管理激活流程`
            );
          }
        })
        .finally(() => {
          this.approvalLoading = false;
        });
    },
    isWorkflowProcess(item = {}) {
      const name = item.name || '';
      const key = item.key || '';
      const formKey = item.formKey || '';
      const config = this.approvalConfig;
      return (
        (config.formKeys || []).includes(formKey) ||
        (config.defaultKeys || []).includes(key) ||
        (config.nameKeywords || []).some(keyword => name.includes(keyword) || key.includes(keyword))
      );
    },
    resolveApprovalProcessKey(processOptions = [], currentKey = '') {
      if (currentKey && processOptions.some(item => item.key === currentKey)) {
        return currentKey;
      }
      const config = this.approvalConfig;
      const preferred =
        processOptions.find(item => (config.formKeys || []).includes(item.formKey)) ||
        processOptions.find(item => (config.defaultKeys || []).includes(item.key)) ||
        processOptions.find(item =>
          (config.nameKeywords || []).some(keyword => (item.name || '').includes(keyword))
        ) ||
        processOptions[0];
      return preferred ? preferred.key : '';
    },
    approvalProcessLabel(item = {}) {
      const name = item.name || item.key || '-';
      const key = item.key ? ` / ${item.key}` : '';
      const version = item.version ? ` v${item.version}` : '';
      return `${name}${version}${key}`;
    },
    canStartApproval(row) {
      return this.contractApprovalPrerequisites(row).every(item => item.done);
    },
    canStartTermination(row) {
      return this.terminationPrerequisites(row).every(item => item.done);
    },
    canStartRoomReview(row) {
      return this.roomReviewPrerequisites(row).every(item => item.done);
    },
    hasRunningWorkflow(businessType) {
      return (this.workflowData || []).some(
        item => item.businessType === businessType && item.processStatus === 'running'
      );
    },
    ensurePrerequisites(title, items = []) {
      if ((items || []).every(item => item.done)) {
        return true;
      }
      this.precheckTitle = title;
      this.precheckItems = items || [];
      this.precheckVisible = true;
      return false;
    },
    baseContractPrerequisites(row) {
      return [
        {
          label: '已选择合同',
          done: Boolean(row && row.contractId),
          pendingText: '请先选择合同',
        },
      ];
    },
    contractApprovalPrerequisites(row) {
      const approvalStatus = String((row && row.approvalStatus) || '');
      return [
        ...this.baseContractPrerequisites(row),
        {
          label: '合同状态为待审批',
          done: String(row?.contractStatus || '') === '0',
          pendingText: `当前状态：${this.statusText(row?.contractStatus)}`,
        },
        {
          label: '合同审批未进行中且未通过',
          done: !['running', 'approved'].includes(approvalStatus),
          pendingText:
            approvalStatus === 'running'
              ? '当前合同审批正在进行中'
              : '合同审批已通过，不需要重复发起',
        },
      ];
    },
    selectionContractApprovalPrerequisites() {
      const selectionCount = this.selectionList.length;
      const selectionItems = [
        {
          label: '已选择合同',
          done: selectionCount > 0,
          pendingText: '请先选择合同',
        },
        {
          label: '仅选择一份合同',
          done: selectionCount <= 1,
          pendingText: '每次只能发起一份合同审批',
        },
      ];
      if (selectionCount !== 1) {
        return selectionItems;
      }
      return [
        ...selectionItems,
        ...this.contractApprovalPrerequisites(this.singleSelectedContract).slice(1),
      ];
    },
    signedUploadPrerequisites(row) {
      return [
        ...this.baseContractPrerequisites(row),
        {
          label: '合同状态为待盖章',
          done: String(row?.contractStatus || '') === '5',
          pendingText: `当前状态：${this.statusText(row?.contractStatus)}`,
        },
      ];
    },
    terminationPrerequisites(row) {
      return [
        ...this.baseContractPrerequisites(row),
        {
          label: '合同已生效或已到期',
          done: ['1', '2'].includes(String(row?.contractStatus || '')),
          pendingText: `当前状态：${this.statusText(row?.contractStatus)}`,
        },
        {
          label: '退租审批未进行中',
          done: !this.hasRunningWorkflow(CONTRACT_TERMINATION_BUSINESS_TYPE),
          pendingText: '该合同已有进行中的退租审批',
        },
      ];
    },
    roomReviewPrerequisites(row) {
      return [
        ...this.baseContractPrerequisites(row),
        {
          label: '进入退租交接阶段',
          done: String(row?.contractStatus || '') === '7',
          pendingText: `当前状态：${this.statusText(row?.contractStatus)}`,
        },
        {
          label: '房屋验收未进行中',
          done: !this.hasRunningWorkflow(CONTRACT_ROOM_REVIEW_BUSINESS_TYPE),
          pendingText: '该合同已有进行中的房屋验收流程',
        },
      ];
    },
    overdueApprovalPrerequisites(contract = {}, payment = {}) {
      return [
        ...this.baseContractPrerequisites(contract),
        {
          label: '已选择账单',
          done: Boolean(payment && payment.paymentId),
          pendingText: '请选择需要发起逾期处理的账单',
        },
        {
          label: '账单未缴清',
          done: String(payment?.payStatus || '') !== '1',
          pendingText: '该账单已完成缴费',
        },
        {
          label: '账单已逾期',
          done: this.isPaymentOverdue(payment),
          pendingText: '仅逾期账单可以发起逾期处理',
        },
        {
          label: '逾期处理未进行中',
          done: payment?.overdueApprovalStatus !== 'running',
          pendingText: '该账单逾期处理流程正在进行中',
        },
      ];
    },
    buildContractApprovalPayload(contract = {}) {
      const selectedContract = contract || {};
      return {
        processDefKey: this.approvalForm.processDefKey,
        params: {
          processDefKey: this.approvalForm.processDefKey,
          businessType: CONTRACT_APPROVAL_BUSINESS_TYPE,
          businessTable: 'biz_contract',
          businessKey: String(selectedContract.contractId || ''),
          contractId: selectedContract.contractId,
          contractNo: selectedContract.contractNo,
          contractName: selectedContract.contractName,
          customerId: selectedContract.customerId,
          customerName: selectedContract.customerName,
          roomId: selectedContract.roomId,
          roomName: selectedContract.roomName,
          buildingId: selectedContract.buildingId,
          buildingName: selectedContract.buildingName,
          parkId: selectedContract.parkId,
          parkName: selectedContract.parkName,
          rentArea: selectedContract.rentArea,
          rentPrice: selectedContract.rentPrice,
          monthlyRent: selectedContract.monthlyRent,
          propertyFee: selectedContract.propertyFee,
          deposit: selectedContract.deposit,
          followUser: selectedContract.followUser,
          signDate: selectedContract.signDate,
          startDate: selectedContract.startDate,
          endDate: selectedContract.endDate,
          paymentCycle: selectedContract.paymentCycle,
          renewalRemindDays: selectedContract.renewalRemindDays,
          contractStatus: selectedContract.contractStatus,
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildPaymentWorkflowPayload(type, contract = {}, payment = {}) {
      const selectedContract = contract || {};
      const selectedPayment = payment || {};
      const processKey = this.approvalForm.processDefKey || '';
      const isInvoiceWorkflow = processKey.includes('invoice');
      return {
        processDefKey: this.approvalForm.processDefKey,
        params: {
          processDefKey: this.approvalForm.processDefKey,
          businessType: type,
          businessTable: 'biz_contract_payment',
          businessKey: String(selectedPayment.paymentId || ''),
          contractId: selectedPayment.contractId || selectedContract.contractId,
          paymentId: selectedPayment.paymentId,
          contractNo: selectedContract.contractNo || selectedPayment.contractNo,
          contractName: selectedContract.contractName,
          customerId: selectedContract.customerId,
          customerName: selectedContract.customerName || selectedPayment.customerName,
          roomId: selectedContract.roomId,
          roomName: selectedContract.roomName,
          buildingId: selectedContract.buildingId,
          buildingName: selectedContract.buildingName,
          parkId: selectedPayment.parkId || selectedContract.parkId,
          parkName: selectedContract.parkName,
          feeType: selectedPayment.feeType,
          feeName: selectedPayment.feeName,
          periodStart: selectedPayment.periodStart,
          periodEnd: selectedPayment.periodEnd,
          amountDue: selectedPayment.amountDue,
          amountPaid: selectedPayment.amountPaid,
          unpaidAmount: this.unpaidAmount(selectedPayment),
          payDeadline: selectedPayment.payDeadline,
          overdueDays: this.overdueDays(selectedPayment),
          templateKey: isInvoiceWorkflow ? 'invoice-apply' : 'payment-notice',
          applicant: this.userInfo.nick_name,
          applicantDept: this.userInfo.dept_name,
          applyTime: this.formatDate(new Date()),
        },
      };
    },
    buildContractFollowupWorkflowPayload(type, contract = {}) {
      const selectedContract = contract || {};
      const unpaidAmount = (this.paymentData || []).reduce(
        (total, item) => total + this.unpaidAmount(item),
        0
      );
      const overdueAmount = (this.paymentData || [])
        .filter(item => this.isPaymentOverdue(item))
        .reduce((total, item) => total + this.unpaidAmount(item), 0);
      const templateKeyMap = {
        [CONTRACT_TERMINATION_BUSINESS_TYPE]: 'termination-approval',
        [CONTRACT_ROOM_REVIEW_BUSINESS_TYPE]: 'room-review',
      };
      const params = {
        processDefKey: this.approvalForm.processDefKey,
        businessType: type,
        businessTable: 'biz_contract',
        businessKey: String(selectedContract.contractId || ''),
        contractId: selectedContract.contractId,
        contractNo: selectedContract.contractNo,
        contractName: selectedContract.contractName,
        customerId: selectedContract.customerId,
        customerName: selectedContract.customerName,
        roomId: selectedContract.roomId,
        roomIds: selectedContract.roomIds,
        roomName: selectedContract.roomName,
        buildingId: selectedContract.buildingId,
        buildingIds: selectedContract.buildingIds,
        buildingName: selectedContract.buildingName,
        parkId: selectedContract.parkId,
        parkName: selectedContract.parkName,
        rentArea: selectedContract.rentArea,
        rentPrice: selectedContract.rentPrice,
        monthlyRent: selectedContract.monthlyRent,
        deposit: selectedContract.deposit,
        startDate: selectedContract.startDate,
        endDate: selectedContract.endDate,
        followUser: selectedContract.followUser,
        unpaidAmount,
        overdueAmount,
        expectedTerminationDate: this.formatDate(new Date()),
        templateKey: templateKeyMap[type] || type,
        applicant: this.userInfo.nick_name,
        applicantDept: this.userInfo.dept_name,
        applyTime: this.formatDate(new Date()),
      };
      if (type === CONTRACT_TERMINATION_BUSINESS_TYPE) {
        Object.assign(params, {
          terminationType: this.approvalExtra.terminationType,
          terminationTypeName: this.terminationTypeText(this.approvalExtra.terminationType),
          expectedTerminationDate:
            this.approvalExtra.expectedTerminationDate || params.expectedTerminationDate,
          breachPenalty: this.approvalExtra.breachPenalty,
          terminationReason: this.approvalExtra.terminationReason,
          offlineSpecialProcess: this.approvalExtra.terminationType === 'special',
        });
      }
      return {
        processDefKey: this.approvalForm.processDefKey,
        params,
      };
    },
    buildWorkflowPayload() {
      if (
        this.approvalType === CONTRACT_PAYMENT_BUSINESS_TYPE ||
        this.approvalType === CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE
      ) {
        return this.buildPaymentWorkflowPayload(
          this.approvalType,
          this.approvalContract,
          this.approvalPayment
        );
      }
      if (
        this.approvalType === CONTRACT_TERMINATION_BUSINESS_TYPE ||
        this.approvalType === CONTRACT_ROOM_REVIEW_BUSINESS_TYPE
      ) {
        return this.buildContractFollowupWorkflowPayload(this.approvalType, this.approvalContract);
      }
      return this.buildContractApprovalPayload(this.approvalContract);
    },
    goWorkflowApproval() {
      if (!this.approvalContract.contractId) {
        this.$message.warning('请选择合同后再发起流程');
        return;
      }
      if (this.isTerminationWorkflow && !this.validateTerminationExtra()) {
        return;
      }
      if (
        [CONTRACT_PAYMENT_BUSINESS_TYPE, CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE].includes(
          this.approvalType
        ) &&
        !this.approvalPayment.paymentId
      ) {
        this.$message.warning('请选择账单后再发起流程');
        return;
      }
      if (!this.approvalForm.processDefKey) {
        this.$message.warning('请选择审批流程');
        return;
      }
      const payload = this.buildWorkflowPayload();
      const encodedParam = encodeURIComponent(Base64.encode(JSON.stringify(payload)));
      this.approvalVisible = false;
      this.$router.push(`/plugin/workflow/pages/process/form/start/${encodedParam}`);
    },
    validateTerminationExtra() {
      if (!this.approvalExtra.expectedTerminationDate) {
        this.$message.warning('请选择退租日期');
        return false;
      }
      if (!this.approvalExtra.terminationReason) {
        this.$message.warning('请填写退租原因');
        return false;
      }
      if (
        this.approvalExtra.terminationType === 'early' &&
        Number(this.approvalExtra.breachPenalty || 0) <= 0
      ) {
        this.$message.warning('客户提前退租请填写违约金');
        return false;
      }
      return true;
    },
    resetApprovalDialog() {
      this.approvalType = CONTRACT_APPROVAL_BUSINESS_TYPE;
      this.approvalContract = {};
      this.approvalPayment = {};
      this.approvalForm.processDefKey = '';
      this.approvalExtra = this.defaultApprovalExtra();
    },
    handleDelete() {
      if (this.selectionList.length === 0) {
        this.$message.warning('请选择至少一条数据');
        return;
      }
      this.$confirm('确定将选择数据删除?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remove(this.ids))
        .then(() => {
          this.reload();
          this.$message.success('操作成功!');
        });
    },
    handleTerminate(row) {
      if (!row || !row.contractId) return;
      this.$confirm('确定作废该合同?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => terminate(row.contractId))
        .then(() => {
          this.reload();
          this.detailVisible = false;
          this.detailMode = false;
          this.$message.success('操作成功!');
        });
    },
    handleSignedUpload(row) {
      if (
        !this.ensurePrerequisites('上传盖章合同前置条件', this.signedUploadPrerequisites(row))
      ) {
        return;
      }
      this.signedUploadContract = { ...row };
      this.signedUploadForm = {
        contractFileUrl: row.contractFileUrl || '',
      };
      this.signedUploadVisible = true;
      this.$nextTick(() => {
        const upload = this.$refs.signedUploadRef;
        if (upload && upload.clearFiles) {
          upload.clearFiles();
        }
      });
    },
    beforeSignedFileUpload(file) {
      const allowTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'image/jpeg',
        'image/png',
      ];
      const isAllowType =
        allowTypes.includes(file.type) || /\.(pdf|doc|docx|jpg|jpeg|png)$/i.test(file.name || '');
      const isLt20M = file.size / 1024 / 1024 < 20;
      if (!isAllowType) {
        this.$message.error('仅支持上传 PDF、Word、JPG、PNG 格式文件');
      }
      if (!isLt20M) {
        this.$message.error('文件大小不能超过 20MB');
      }
      return isAllowType && isLt20M;
    },
    handleSignedFileSuccess(response, file) {
      if (!response || response.success === false) {
        this.signedUploadForm.contractFileUrl = '';
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl =
        data.link || data.url || data.path || response.link || response.url || response.data || '';
      this.signedUploadForm.contractFileUrl = fileUrl || '';
      if (this.signedUploadContract) {
        this.signedUploadContract.contractFileUrl = fileUrl || '';
      }
      this.$message.success(file && file.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleSignedFileError(error) {
      this.signedUploadForm.contractFileUrl = '';
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    handleSignedFileRemove() {
      this.signedUploadForm.contractFileUrl = '';
      if (this.signedUploadContract) {
        this.signedUploadContract.contractFileUrl = '';
      }
    },
    submitSignedUpload() {
      if (!this.signedUploadContract.contractId) {
        this.$message.warning('请选择需要盖章的合同');
        return;
      }
      if (!this.signedUploadForm.contractFileUrl) {
        this.$message.warning('请先上传盖章合同文件');
        return;
      }
      this.signedUploadLoading = true;
      uploadSignedContract(this.signedUploadContract.contractId, {
        contractFileUrl: this.signedUploadForm.contractFileUrl,
      })
        .then(() => {
          this.$message.success('盖章合同已生效');
          const contractId = this.signedUploadContract.contractId;
          this.signedUploadVisible = false;
          this.resetSignedUpload();
          this.reload();
          if (
            (this.detailMode || this.detailVisible) &&
            String(this.detailContract.contractId) === String(contractId)
          ) {
            this.openDetail({ contractId });
          }
        })
        .finally(() => {
          this.signedUploadLoading = false;
        });
    },
    resetSignedUpload() {
      this.signedUploadLoading = false;
      this.signedUploadContract = {};
      this.signedUploadForm = {
        contractFileUrl: '',
      };
      this.$nextTick(() => {
        const upload = this.$refs.signedUploadRef;
        if (upload && upload.clearFiles) {
          upload.clearFiles();
        }
      });
    },
    openDetail(row) {
      if (!row || !row.contractId) return;
      this.detailMode = true;
      this.detailVisible = false;
      this.detailLoading = true;
      this.profileTab = 'info';
      this.paymentData = [];
      this.workflowData = [];
      this.logData = [];
      this.customerDetail = {};
      this.customerContracts = [];
      this.supplements = [];
      getDetail(row.contractId)
        .then(res => {
          const contract = {
            ...row,
            ...((res.data && res.data.data) || {}),
          };
          this.detailContract = contract;
          this.loadCustomerDetail(contract.customerId);
          this.loadCustomerContracts(contract);
        })
        .finally(() => {
          this.detailLoading = false;
        });
      this.loadWorkflow(row.contractId);
      this.loadLogs(row.contractId);
    },
    closeDetailPage() {
      this.detailMode = false;
      this.detailVisible = false;
      this.profileTab = 'info';
      this.detailContract = {};
      this.customerDetail = {};
      this.customerContracts = [];
      this.paymentData = [];
      this.workflowData = [];
      this.logData = [];
      this.supplements = [];
    },
    loadCustomerDetail(customerId) {
      if (!customerId) {
        this.customerDetail = {};
        return;
      }
      getCustomerDetail(customerId)
        .then(res => {
          this.customerDetail = res.data.data || {};
        })
        .catch(() => {
          this.customerDetail = {};
        });
    },
    loadCustomerContracts(contract = {}) {
      if (!contract.customerId) {
        this.customerContracts = contract.contractId ? [contract] : [];
        this.loadPayment(contract.contractId);
        this.loadSupplements(contract.contractId);
        return;
      }
      getList(1, 100, { customerId: contract.customerId })
        .then(res => {
          const result = res.data.data || {};
          const records = result.records || [];
          this.customerContracts = records.length ? records : [contract];
          this.loadPaymentsByContracts(this.customerContracts);
          this.loadSupplementsByContracts(this.customerContracts);
        })
        .catch(() => {
          this.customerContracts = contract.contractId ? [contract] : [];
          this.loadPayment(contract.contractId);
          this.loadSupplements(contract.contractId);
        });
    },
    loadPaymentsByContracts(contracts = []) {
      if (!contracts.length) {
        this.paymentData = [];
        return;
      }
      this.paymentLoading = true;
      Promise.all(
        contracts.map(contract =>
          getPayment(contract.contractId)
            .then(res =>
              (res.data.data || []).map(item => ({
                ...item,
                contractNo: item.contractNo || contract.contractNo,
                contractName: item.contractName || contract.contractName,
                customerName: item.customerName || contract.customerName,
              }))
            )
            .catch(() => [])
        )
      )
        .then(groups => {
          this.paymentData = groups.flat();
        })
        .finally(() => {
          this.paymentLoading = false;
        });
    },
    loadSupplements(contractId) {
      if (!contractId) {
        this.supplements = [];
        return;
      }
      getSupplementAgreements(contractId).then(res => {
        this.supplements = (res.data.data || []).map(item => ({
          ...item,
          contractNo: item.contractNo || this.detailContract.contractNo,
        }));
      });
    },
    loadSupplementsByContracts(contracts = []) {
      if (!contracts.length) {
        this.supplements = [];
        return;
      }
      Promise.all(
        contracts.map(contract =>
          getSupplementAgreements(contract.contractId)
            .then(res =>
              (res.data.data || []).map(item => ({
                ...item,
                contractNo: item.contractNo || contract.contractNo,
              }))
            )
            .catch(() => [])
        )
      ).then(groups => {
        this.supplements = groups.flat();
      });
    },
    openCustomerEdit() {
      const customerId = this.currentCustomerId;
      if (!customerId) {
        this.$message.warning('手工录入企业暂无客户档案，不能在这里编辑');
        return;
      }
      this.customerEditVisible = true;
      this.customerEditLoading = true;
      this.customerEditTab = 'basic';
      getCustomerDetail(customerId)
        .then(res => {
          const detail = res.data.data || {};
          this.customerEditForm = {
            ...createDefaultCustomerEditForm(),
            ...detail,
            tenantType: detail.tenantType || 'personal',
            approvalContactName: detail.approvalContactName || detail.contactName || '',
            billContactName: detail.billContactName || detail.contactName || '',
            contractSigner: detail.contractSigner || detail.contactName || '',
            registeredAddress: detail.registeredAddress || detail.address || '',
            majorClients: detail.majorClients || detail.majorCustomers || '',
            tagIds: this.resolveCustomerTagIds(detail),
          };
        })
        .finally(() => {
          this.customerEditLoading = false;
          this.$nextTick(() => {
            if (this.$refs.customerEditFormRef) {
              this.$refs.customerEditFormRef.clearValidate();
            }
          });
        });
    },
    resetCustomerEdit() {
      this.customerEditLoading = false;
      this.customerEditSaving = false;
      this.customerEditTab = 'basic';
      this.customerEditForm = createDefaultCustomerEditForm();
    },
    submitCustomerEdit() {
      this.$refs.customerEditFormRef.validate(valid => {
        if (!valid) return;
        this.customerEditSaving = true;
        updateCustomer(this.normalizeCustomerPayload(this.customerEditForm))
          .then(() => {
            this.$message.success('租客信息已保存');
            this.customerEditVisible = false;
            const contractId = this.detailContract.contractId;
            this.reload();
            if (contractId) {
              this.openDetail({ contractId });
            }
          })
          .finally(() => {
            this.customerEditSaving = false;
          });
      });
    },
    handleCustomerDelete() {
      const customerId = this.currentCustomerId;
      if (!customerId) {
        this.$message.warning('手工录入企业暂无客户档案，不能在这里删除');
        return;
      }
      this.$confirm(`确定删除租客「${this.customerTitle}」？`, '提示', {
        type: 'warning',
        confirmButtonText: '确定',
        cancelButtonText: '取消',
      })
        .then(() => removeCustomer(customerId))
        .then(() => {
          this.$message.success('租客已删除');
          this.closeDetailPage();
          this.reload();
        });
    },
    resolveCustomerTagIds(detail = {}) {
      if (Array.isArray(detail.tagIds)) {
        return detail.tagIds;
      }
      if (Array.isArray(detail.tags)) {
        return detail.tags.map(tag => tag.tagId || tag.id).filter(Boolean);
      }
      return [];
    },
    normalizeCustomerPayload(row = {}) {
      return {
        ...row,
        address: row.registeredAddress || row.address,
        creditCode: row.creditCode || null,
        tenantType: row.tenantType || 'personal',
        approvalContactName: row.approvalContactName || '',
        billContactName: row.billContactName || '',
        contractSigner: row.contractSigner || '',
        identityFrontUrl: row.identityFrontUrl || '',
        identityBackUrl: row.identityBackUrl || '',
        tagIds: Array.isArray(row.tagIds) ? row.tagIds : [],
      };
    },
    beforeIdentityUpload(file) {
      const isImage =
        ['image/jpeg', 'image/png'].includes(file.type) || /\.(jpg|jpeg|png)$/i.test(file.name || '');
      const isLt10M = file.size / 1024 / 1024 < 10;
      if (!isImage) {
        this.$message.error('仅支持上传 JPG、PNG 格式图片');
      }
      if (!isLt10M) {
        this.$message.error('图片大小不能超过 10MB');
      }
      return isImage && isLt10M;
    },
    handleIdentityUploadSuccess(side, response, file) {
      if (!response || response.success === false) {
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl =
        data.link || data.url || data.path || response.link || response.url || response.data || '';
      if (!fileUrl) {
        this.$message.error('上传成功但未返回文件地址');
        return;
      }
      if (side === 'front') {
        this.customerEditForm.identityFrontUrl = fileUrl;
      } else {
        this.customerEditForm.identityBackUrl = fileUrl;
      }
      this.$message.success(file && file.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleIdentityUploadError(error) {
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    openSupplementDialog() {
      if (!this.detailContract.contractId) {
        this.$message.warning('请先选择合同');
        return;
      }
      const contractNo = this.detailContract.contractNo || '';
      this.supplementForm = {
        agreementName: contractNo ? `${contractNo}附件` : '合同附件',
        changeItem: '合同档案附件',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      };
      this.supplementUploadFileList = [];
      this.supplementVisible = true;
      this.$nextTick(() => {
        if (this.$refs.supplementFormRef) {
          this.$refs.supplementFormRef.clearValidate();
        }
        if (this.$refs.supplementUploadRef && this.$refs.supplementUploadRef.clearFiles) {
          this.$refs.supplementUploadRef.clearFiles();
        }
      });
    },
    resetSupplementForm() {
      this.supplementLoading = false;
      this.supplementUploadFileList = [];
      this.supplementForm = {
        agreementName: '',
        changeItem: '合同档案附件',
        fileName: '',
        fileUrl: '',
        fileType: '',
        remark: '',
      };
      if (this.$refs.supplementFormRef) {
        this.$refs.supplementFormRef.clearValidate();
      }
    },
    beforeSupplementUpload(file) {
      const allowTypes = [
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'image/jpeg',
        'image/png',
      ];
      const isAllowType =
        allowTypes.includes(file.type) || /\.(pdf|doc|docx|jpg|jpeg|png)$/i.test(file.name || '');
      const isLt20M = file.size / 1024 / 1024 < 20;
      if (!isAllowType) {
        this.$message.error('仅支持上传 PDF、Word、JPG、PNG 格式文件');
      }
      if (!isLt20M) {
        this.$message.error('文件大小不能超过 20MB');
      }
      return isAllowType && isLt20M;
    },
    handleSupplementUploadSuccess(response, file) {
      if (!response || response.success === false) {
        this.supplementForm.fileUrl = '';
        this.$message.error((response && response.msg) || '上传失败');
        return;
      }
      const data = response.data || {};
      const fileUrl =
        data.link || data.url || data.path || response.link || response.url || response.data || '';
      this.supplementForm.fileUrl = fileUrl || '';
      this.supplementForm.fileName = (file && file.name) || data.name || data.originalName || '';
      this.supplementForm.fileType = this.getFileType(this.supplementForm.fileName || fileUrl);
      this.supplementUploadFileList = file
        ? [{ name: file.name, url: fileUrl }]
        : this.supplementUploadFileList;
      if (this.$refs.supplementFormRef) {
        this.$refs.supplementFormRef.validateField('fileUrl');
      }
      this.$message.success(file && file.name ? `${file.name} 上传成功` : '上传成功');
    },
    handleSupplementUploadError(error) {
      this.supplementForm.fileUrl = '';
      const message = (error && error.message) || '上传失败，请重试';
      this.$message.error(message);
    },
    handleSupplementUploadRemove() {
      this.supplementForm.fileName = '';
      this.supplementForm.fileUrl = '';
      this.supplementForm.fileType = '';
      this.supplementUploadFileList = [];
    },
    submitSupplement() {
      if (!this.detailContract.contractId) {
        this.$message.warning('请先选择合同');
        return;
      }
      this.$refs.supplementFormRef.validate(valid => {
        if (!valid) return;
        this.supplementLoading = true;
        saveSupplementAgreement({
          ...this.supplementForm,
          contractId: this.detailContract.contractId,
          changeItem: this.supplementForm.changeItem || '合同档案附件',
        })
          .then(() => {
            this.$message.success('附件已上传');
            this.supplementVisible = false;
            this.loadSupplementsByContracts(this.contractRows);
          })
          .finally(() => {
            this.supplementLoading = false;
          });
      });
    },
    removeSupplement(row) {
      this.$confirm('确认删除该附件吗？', '提示', {
        type: 'warning',
      })
        .then(() => removeSupplementAgreement(row.agreementId))
        .then(() => {
          this.$message.success('附件已删除');
          this.loadSupplementsByContracts(this.contractRows);
        });
    },
    downloadSupplement(row) {
      if (!row.fileUrl) {
        this.$message.warning('暂无附件文件');
        return;
      }
      downloadNoticeFile(row.fileUrl, row.fileName || row.agreementName || '附件').catch(() => {
        this.$message.error('附件下载失败，请稍后重试');
      });
    },
    previewAttachment(row, title = '附件预览') {
      if (!row || !row.fileUrl) {
        this.$message.warning('暂无附件文件');
        return;
      }
      openAttachmentPreview(this.noticePreview, row, title);
    },
    getFileType(value) {
      const match = String(value || '').match(/\.([a-z0-9]+)(?:\?|#|$)/i);
      return match ? match[1].toLowerCase() : '';
    },
    handlePayment(row) {
      this.detailContract = row || {};
      this.paymentBox = true;
      this.loadPayment(row.contractId);
    },
    loadPayment(contractId) {
      if (!contractId) return;
      this.paymentLoading = true;
      getPayment(contractId)
        .then(res => {
          this.paymentData = res.data.data || [];
        })
        .finally(() => {
          this.paymentLoading = false;
        });
    },
    loadLogs(contractId) {
      if (!contractId) return;
      getLogs(contractId).then(res => {
        this.logData = res.data.data || [];
      });
    },
    loadWorkflow(contractId) {
      if (!contractId) return;
      this.workflowLoading = true;
      getWorkflowRecords(contractId)
        .then(res => {
          this.workflowData = res.data.data || [];
        })
        .finally(() => {
          this.workflowLoading = false;
        });
    },
    openWorkflowFile(url, row) {
      if (!url || !row) return;
      const noticeType = this.workflowNoticeType(row, url);
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
          paymentId: row.paymentId,
          formDataJson: row.formDataJson || '',
        },
        noticePrintUrl(noticeType, {
          contractId: row.contractId,
          paymentId: row.paymentId,
        }) || url,
        '合同流程文件',
        row.processName || '审批表预览'
      );
    },
    workflowNoticeType(row, url) {
      const currentUrl = String(url || '');
      if (currentUrl.includes('termination-agreement')) return 'termination-agreement';
      if (currentUrl.includes('room-review')) return 'room-review';
      if (currentUrl.includes('project-approval')) return 'project-approval';
      if (currentUrl.includes('payment-notice')) return 'payment-notice';
      if (currentUrl.includes('invoice-apply')) return 'invoice-apply';
      if (currentUrl.includes('overdue-notice')) return 'overdue-notice';
      if (currentUrl.includes('move-out-notice')) return 'move-out-notice';
      if (currentUrl.includes('legal-letter')) return 'legal-letter';
      switch (row.businessType) {
        case CONTRACT_PAYMENT_BUSINESS_TYPE:
          return row.processDefKey && String(row.processDefKey).includes('invoice')
            ? 'invoice-apply'
            : 'payment-notice';
        case CONTRACT_ROOM_REVIEW_BUSINESS_TYPE:
          return 'room-review';
        case CONTRACT_TERMINATION_BUSINESS_TYPE:
          return 'termination-approval';
        case CONTRACT_OVERDUE_LEGAL_BUSINESS_TYPE:
          return 'legal-letter';
        case CONTRACT_APPROVAL_BUSINESS_TYPE:
        default:
          return 'contract-approval';
      }
    },
    downloadNoticePreviewFile() {
      if (!this.noticePreview.downloadUrl) return;
      downloadNoticeFile(this.noticePreview.downloadUrl, this.noticePreview.fallbackName);
    },
    handlePreviewContractText(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要预览的合同');
        return;
      }
      const noticeType = this.resolveContractTextNoticeType(row);
      openNoticePreview(
        this,
        this.noticePreview,
        {
          noticeType,
          contractId: row.contractId,
        },
        noticePrintUrl(noticeType, { contractId: row.contractId }),
        `${row.contractNo || '合同'}正文.docx`,
        noticeType === 'contract-floating' ? '合同正文浮动租金版' : '合同正文固定租金版'
      );
    },
    handleDownloadContractText(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要下载的合同');
        return;
      }
      const noticeType = this.resolveContractTextNoticeType(row);
      const downloadUrl = noticePrintUrl(noticeType, { contractId: row.contractId });
      if (!downloadUrl) {
        this.$message.warning('合同下载地址生成失败');
        return;
      }
      this.activeDownloadContractId = row.contractId;
      this.contractTextDownloading = true;
      downloadNoticeFile(downloadUrl, `${row.contractNo || '合同'}正文.docx`)
        .then(() => {
          this.$message.success('合同下载已开始');
        })
        .catch(() => {
          this.$message.error('合同下载失败，请确认合同字段是否完整后重试');
        })
        .finally(() => {
          this.contractTextDownloading = false;
          this.activeDownloadContractId = '';
        });
    },
    resolveContractTextNoticeType(row = {}) {
      const increaseNode = String(row.rentIncreaseNode || '');
      const remark = String(row.remark || '');
      if (increaseNode && increaseNode !== 'none') return 'contract-floating';
      if (remark.includes('浮动')) return 'contract-floating';
      return 'contract-fixed';
    },
    handleConfirmPayment(row) {
      this.$prompt('请输入实收金额', '确认缴费', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputValue: row.amountDue,
      })
        .then(({ value }) =>
          confirmPayment(row.paymentId, {
            amountPaid: value,
          })
        )
        .then(() => {
          if (this.detailMode) {
            this.loadPaymentsByContracts(this.contractRows);
          } else {
            this.loadPayment(this.currentPaymentContractId(row));
          }
          this.$message.success('操作成功!');
        });
    },
    handleRemind(row) {
      if (!row || !row.paymentId) {
        this.$message.warning('请选择需要催缴的账单');
        return;
      }
      this.$confirm('确定对该账单发起催缴吗？', '催缴确认', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      })
        .then(() => remindPayment(row.paymentId))
        .then(() => {
          if (this.detailMode) {
            this.loadPaymentsByContracts(this.contractRows);
          } else {
            this.loadPayment(this.currentPaymentContractId(row));
          }
          this.$message.success('催缴已发起');
        });
    },
    handleArchive(row) {
      if (!row || !row.contractId) return;
      this.$confirm('确定进入该合同档案归档?', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }).then(() => {
        this.$router.push({
          path: '/contract/archive',
          query: {
            contractId: row.contractId,
          },
        });
      });
    },
    handleEditContract(row) {
      if (!row || !row.contractId) {
        this.$message.warning('请选择需要编辑的合同');
        return;
      }
      this.$router.push({
        path: '/contract/create-template',
        query: {
          mode: 'edit',
          contractId: row.contractId,
        },
      });
    },
    handleDetailMoreCommand(command) {
      const row = this.detailContract || {};
      const actionMap = {
        downloadContract: () => this.handleDownloadContractText(row),
        editContract: () => this.handleEditContract(row),
        contractChange: () => this.handleContractChange(row),
        startApproval: () => this.handleStartApproval(row),
        signedUpload: () => this.handleSignedUpload(row),
        roomReview: () => this.handleStartRoomReview(row),
        archive: () => this.handleArchive(row),
        editCustomer: () => this.openCustomerEdit(),
        addAttachment: () => this.openSupplementDialog(),
        deleteCustomer: () => this.handleCustomerDelete(),
      };
      const action = actionMap[command];
      if (action) {
        action();
      }
    },
    searchReset() {
      this.query = {
        contractNo: '',
        customerName: '',
        contractStatus: '',
      };
      this.page.currentPage = 1;
      this.reload();
    },
    searchChange() {
      this.page.currentPage = 1;
      this.reload();
    },
    selectionChange(list) {
      this.selectionList = list;
    },
    currentChange(currentPage) {
      this.page.currentPage = currentPage;
      this.loadData();
    },
    sizeChange(pageSize) {
      this.page.pageSize = pageSize;
      this.page.currentPage = 1;
      this.loadData();
    },
    reload() {
      this.loadData();
      this.loadStats();
      this.loadExpiring();
    },
    loadData() {
      this.loading = true;
      getList(this.page.currentPage, this.page.pageSize, this.cleanParams(this.query))
        .then(res => {
          const result = res.data.data || {};
          this.page.total = result.total || 0;
          this.data = result.records || [];
          this.openRouteContractDetail();
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadStats() {
      getStats({}).then(res => {
        this.stats = res.data.data || {};
      });
    },
    loadExpiring() {
      getExpiring(1, 20, {}).then(res => {
        const data = res.data.data || {};
        this.expiringData = data.records || [];
      });
    },
    openRouteContractDetail() {
      if (!this.query.contractId) return;
      if (String(this.routeDetailOpenedContractId) === String(this.query.contractId)) return;
      const target = this.data.find(
        item => String(item.contractId) === String(this.query.contractId)
      );
      if (target) {
        this.routeDetailOpenedContractId = String(this.query.contractId);
        this.openDetail(target);
      }
    },
    currentPaymentContractId(row) {
      return (
        row.contractId ||
        this.detailContract.contractId ||
        (this.paymentData[0] && this.paymentData[0].contractId)
      );
    },
    cleanParams(params) {
      return Object.keys(params || {}).reduce((result, key) => {
        const value = params[key];
        if (value !== '' && value !== undefined && value !== null) {
          result[key] = value;
        }
        return result;
      }, {});
    },
    isExpiringSoon(row) {
      if (!row || !row.endDate) return false;
      const remindDays = Number(row.renewalRemindDays || 0);
      if (!remindDays) return false;
      const end = new Date(row.endDate).getTime();
      if (Number.isNaN(end)) return false;
      const days = Math.ceil((end - Date.now()) / 86400000);
      return days >= 0 && days <= remindDays;
    },
    statusText(value) {
      const item = statusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    statusType(value) {
      const map = {
        0: 'info',
        1: 'success',
        2: 'warning',
        3: 'primary',
        4: 'danger',
        5: 'warning',
        6: 'warning',
        7: 'primary',
        8: 'warning',
      };
      return map[String(value)] || 'info';
    },
    tenantTypeText(value) {
      const map = {
        personal: '个人',
        individual: '个人',
        company: '企业',
        enterprise: '企业',
        org: '企业',
      };
      return map[String(value || '')] || this.detailValue(value);
    },
    contractSourceText(row = {}) {
      if (row.parentContractId) return '续签合同';
      if (row.projectId) return '入驻审批转合同';
      return row.contractId ? '新建合同' : '--';
    },
    contractTextTypeText(row = {}) {
      return this.resolveContractTextNoticeType(row) === 'contract-floating'
        ? '浮动租金版'
        : '固定租金版';
    },
    paymentCycleText(value) {
      const item = paymentCycleDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : this.detailValue(value);
    },
    rentIncreaseText(contract = {}) {
      const rate = contract.rentIncreaseRate;
      const node = String(contract.rentIncreaseNode || '');
      if ((rate === null || rate === undefined || rate === '') && (!node || node === 'none')) {
        return '--';
      }
      const unitMap = {
        percent: '%',
        percent_year: '%/年',
        percentPerYear: '%/年',
        yuan: '元',
        yuan_year: '元/年',
      };
      const nodeMap = {
        none: '',
        year1: '第1年',
        year2: '第2年',
        year3: '第3年',
        second_year: '第2年',
        third_year: '第3年',
      };
      const rateText =
        rate === null || rate === undefined || rate === ''
          ? ''
          : `${rate}${unitMap[contract.rentIncreaseUnit] || contract.rentIncreaseUnit || '%'}`;
      const nodeText = nodeMap[node] || node;
      return [nodeText, rateText].filter(Boolean).join('起递增') || '--';
    },
    contractDays(contract = {}) {
      const start = this.parseDate(contract.startDate);
      const end = this.parseDate(contract.endDate);
      if (!start || !end || end.getTime() < start.getTime()) {
        return '--';
      }
      return Math.floor((end.getTime() - start.getTime()) / 86400000) + 1;
    },
    approvalStatusText(value) {
      const map = {
        running: '审批中',
        approved: '已通过',
        rejected: '已驳回',
        canceled: '已取消',
        deleted: '已删除',
      };
      return map[String(value || '')] || '未发起';
    },
    approvalStatusType(value) {
      const map = {
        running: 'warning',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[String(value || '')] || 'info';
    },
    canUploadSignedContract(row) {
      return this.signedUploadPrerequisites(row).every(item => item.done);
    },
    workflowBusinessTypeText(value) {
      const map = {
        contract_approval: '合同审批',
        contract_payment: '付款流程',
        contract_room_review: '房屋验收',
        contract_termination: '退租审批',
        contract_overdue_legal: '逾期律师函',
      };
      return map[String(value || '')] || value || '-';
    },
    workflowStatusText(value) {
      const map = {
        running: '审批中',
        approved: '已通过',
        rejected: '已驳回',
        canceled: '已取消',
        deleted: '已删除',
      };
      return map[String(value || '')] || value || '-';
    },
    workflowStatusType(value) {
      const map = {
        running: 'warning',
        approved: 'success',
        rejected: 'danger',
        canceled: 'info',
        deleted: 'info',
      };
      return map[String(value || '')] || 'info';
    },
    paymentStatusText(value) {
      const item = paymentStatusDic.find(ele => String(ele.value) === String(value));
      return item ? item.label : '未知';
    },
    paymentTagType(row) {
      if (row.payStatus === '1') return 'success';
      if (this.isPaymentOverdue(row)) return 'danger';
      return 'warning';
    },
    isPaymentOverdue(row) {
      if (!row.payDeadline || row.payStatus === '1') return false;
      return new Date(row.payDeadline).getTime() < Date.now();
    },
    overdueDays(row) {
      if (!row || !row.payDeadline || row.payStatus === '1') return 0;
      const deadline = new Date(row.payDeadline).getTime();
      if (Number.isNaN(deadline) || deadline >= Date.now()) return 0;
      return Math.ceil((Date.now() - deadline) / 86400000);
    },
    unpaidAmount(row) {
      const due = Number((row && row.amountDue) || 0);
      const paid = Number((row && row.amountPaid) || 0);
      return Math.max(due - paid, 0);
    },
    lateFeeText(contract) {
      if (!contract || !contract.lateFeeRatio) return '--';
      const unitMap = {
        percent_day: '%/天',
        yuan_day: '元/天',
        percentPerDay: '%/日',
        percentPerMonth: '%/月',
        amountPerDay: '元/日',
      };
      return `${contract.lateFeeRatio}${
        unitMap[contract.lateFeeUnit] || contract.lateFeeUnit || ''
      }`;
    },
    detailValue(value) {
      if (value === null || value === undefined || value === '') {
        return '--';
      }
      return value;
    },
    formatMoneyWithUnitOrDash(value) {
      if (value === null || value === undefined || value === '') {
        return '--';
      }
      return `${this.formatMoney(value)}元`;
    },
    formatMoney(value) {
      const number = Number(value || 0);
      return number.toLocaleString('zh-CN', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2,
      });
    },
    formatMoneyWithUnit(value) {
      if (value === null || value === undefined || value === '') {
        return '-';
      }
      return `${this.formatMoney(value)}元`;
    },
    formatUnitPrice(value) {
      if (value === null || value === undefined || value === '') return '-';
      return `${this.formatMoney(value)}元/㎡/月`;
    },
    formatAreaValue(value) {
      const number = Number(value || 0);
      return `${number.toLocaleString('zh-CN', {
        minimumFractionDigits: 0,
        maximumFractionDigits: 2,
      })}㎡`;
    },
    formatAreaOrDash(value) {
      if (value === null || value === undefined || value === '') {
        return '--';
      }
      return this.formatAreaValue(value);
    },
    openFileUrl(url) {
      if (!url) {
        this.$message.warning('暂无文件');
        return;
      }
      this.previewAttachment({ fileUrl: url, fileName: '合同文件' }, '合同文件预览');
    },
    formatDate(date) {
      const y = date.getFullYear();
      const m = date.getMonth() + 1;
      const d = date.getDate();
      return `${y}-${String(m).padStart(2, '0')}-${String(d).padStart(2, '0')}`;
    },
    parseDate(value) {
      if (!value) return null;
      const date = new Date(`${String(value).slice(0, 10)}T00:00:00`);
      return Number.isNaN(date.getTime()) ? null : date;
    },
    nextDate(value) {
      const date = this.parseDate(value);
      if (!date) return '';
      date.setDate(date.getDate() + 1);
      return this.formatDate(date);
    },
    defaultRenewEndDate(startDate, oldStartDate, oldEndDate) {
      const start = this.parseDate(startDate);
      if (!start) return '';
      const oldStart = this.parseDate(oldStartDate);
      const oldEnd = this.parseDate(oldEndDate);
      const months =
        oldStart && oldEnd
          ? Math.max(
              (oldEnd.getFullYear() - oldStart.getFullYear()) * 12 +
                oldEnd.getMonth() -
                oldStart.getMonth(),
              1
            )
          : 12;
      const end = new Date(start);
      end.setMonth(end.getMonth() + months);
      return this.formatDate(end);
    },
    addDays(date, days) {
      const next = new Date(date);
      next.setDate(next.getDate() + Number(days || 0));
      return next;
    },
  },
};
</script>

<style lang="scss" scoped>
.contract-page {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.contract-profile-page {
  min-height: calc(100vh - 180px);
  padding: 24px 30px 42px;
  border-radius: 10px;
  background: #fff;
}

.contract-profile-header {
  position: relative;
  padding-bottom: 24px;
}

.contract-profile-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 34px;
}

.contract-profile-title-row h2 {
  margin: 0;
  color: #303133;
  font-size: 22px;
  font-weight: 700;
}

.contract-profile-back {
  width: 28px;
  height: 28px;
  padding: 0;
  color: #606266;
}

.contract-profile-actions {
  position: absolute;
  top: 0;
  right: 0;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 12px;
  max-width: calc(100% - 320px);
}

.contract-profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 24px 80px;
  margin-top: 18px;
  color: #303133;
  font-size: 14px;
}

.contract-profile-grid > div {
  min-width: 0;
}

.contract-profile-grid span,
.contract-profile-grid strong {
  color: #303133;
  font-weight: 400;
}

.contract-profile-tag {
  margin-right: 6px;
}

.contract-profile-tabs {
  margin-top: 10px;
}

.contract-profile-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.contract-profile-tabs :deep(.el-tabs__item) {
  height: 44px;
  padding: 0 18px;
  color: #303133;
  font-size: 16px;
  font-weight: 400;
}

.contract-profile-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background-color: #2f80ff;
}

.contract-profile-tabs :deep(.el-tabs__content) {
  padding-top: 42px;
}

.contract-profile-table {
  width: 100%;
}

.contract-profile-table :deep(.el-table__header th) {
  height: 48px;
  background: #fff;
  color: #8c98aa;
  font-weight: 600;
  text-align: center;
}

.contract-profile-table :deep(.el-table__cell) {
  text-align: center;
}

.contract-profile-table :deep(.el-table__row) {
  height: 54px;
}

.contract-profile-table :deep(.el-table__inner-wrapper::before) {
  background-color: #edf0f5;
}

.contract-detail-page {
  min-height: calc(100vh - 180px);
  overflow: hidden;
  border-radius: 10px;
  background: #f4f4f6;
}

.contract-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 22px 24px 16px;
  border: 1px solid #e6eaf2;
  border-radius: 10px 10px 0 0;
  background: #fff;
  box-shadow: 0 2px 10px rgba(16, 89, 198, 0.04);
}

.contract-detail-title-row {
  display: flex;
  min-width: 0;
  align-items: center;
  gap: 10px;
}

.contract-detail-title-row h2 {
  max-width: 520px;
  margin: 0;
  overflow: hidden;
  color: #303133;
  font-size: 20px;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.contract-detail-back {
  width: 30px;
  height: 30px;
  padding: 0;
  color: #606266;
}

.contract-detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
}

.contract-detail-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.contract-detail-summary {
  margin: 0 24px 18px;
  border-color: #2f80ff;
  background: #eef6ff;
}

.contract-detail-tabs {
  background: transparent;
}

.contract-detail-tabs :deep(.el-tabs__header) {
  padding: 0 24px;
  margin: 0;
  border-right: 1px solid #e6eaf2;
  border-left: 1px solid #e6eaf2;
  background: #fff;
}

.contract-detail-tabs :deep(.el-tabs__nav-wrap::after) {
  height: 1px;
  background: #edf0f5;
}

.contract-detail-tabs :deep(.el-tabs__item) {
  height: 54px;
  padding: 0 18px;
  color: #303133;
  font-size: 15px;
  font-weight: 500;
}

.contract-detail-tabs :deep(.el-tabs__item.is-active) {
  color: #2f80ff;
}

.contract-detail-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  background-color: #2f80ff;
}

.contract-detail-tabs :deep(.el-tabs__content) {
  padding: 16px 18px 18px;
  background: #f4f4f6;
}

.contract-info-block {
  padding: 22px 24px 26px;
  margin-bottom: 16px;
  border: 1px solid #e6eaf2;
  border-radius: 10px;
  background: #fff;
  box-shadow: 0 2px 10px rgba(16, 89, 198, 0.04);
}

.contract-info-block:last-child {
  margin-bottom: 0;
}

.contract-info-title,
.contract-info-title-row {
  min-height: 34px;
  padding-bottom: 14px;
  margin-bottom: 22px;
  border-bottom: 1px solid #edf0f5;
  color: #303133;
  font-size: 16px;
  font-weight: 600;
}

.contract-info-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
}

.contract-info-title-row .contract-info-title {
  min-height: 0;
  padding-bottom: 0;
  margin-bottom: 0;
  border-bottom: 0;
}

.contract-info-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px 48px;
}

.contract-info-grid.two-col {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.contract-info-field {
  min-width: 0;
  color: #303133;
  font-size: 14px;
}

.contract-info-field span,
.contract-info-field strong {
  display: block;
  min-width: 0;
}

.contract-info-field span {
  margin-bottom: 8px;
  color: #8c98aa;
}

.contract-info-field strong {
  color: #303133;
  font-weight: 400;
  line-height: 1.55;
  overflow-wrap: anywhere;
}

.clause-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 28px 44px;
}

.clause-panel {
  min-width: 0;
  padding: 0 14px;
}

.clause-title {
  position: relative;
  margin-bottom: 18px;
  padding-left: 12px;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.clause-title::before {
  position: absolute;
  top: 2px;
  bottom: 2px;
  left: 0;
  width: 3px;
  border-radius: 3px;
  background: #2f80ff;
  content: '';
}

.clause-band {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 18px 32px;
  margin-top: 26px;
  padding: 16px 20px;
  background: #eef8ff;
}

.contract-text-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.contract-text-item {
  display: flex;
  min-height: 72px;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.contract-text-item span,
.contract-text-item strong {
  display: block;
}

.contract-text-item span {
  margin-bottom: 6px;
  color: #8c98aa;
  font-size: 13px;
}

.contract-text-item strong {
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.contract-text-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.contract-text-actions :deep(.el-button + .el-button) {
  margin-left: 0;
}

.contract-detail-table {
  width: 100%;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  overflow: hidden;
}

.contract-detail-table :deep(.el-table__header th) {
  height: 48px;
  background: #fff;
  color: #8c98aa;
  font-weight: 600;
  text-align: center;
}

.contract-detail-table :deep(.el-table__cell) {
  text-align: center;
}

.contract-detail-table :deep(.el-table__inner-wrapper::before) {
  background-color: #edf0f5;
}

.attachment-table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  white-space: nowrap;
}

.attachment-table-actions :deep(.el-button) {
  min-width: 0;
  padding: 0;
  margin-left: 0;
}

.bill-table-actions {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  white-space: nowrap;
}

.bill-table-actions :deep(.el-button) {
  min-width: 0;
  padding: 0;
  margin-left: 0;
}

.profile-table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.profile-table-actions :deep(.el-button) {
  min-width: 44px;
  padding: 0 3px;
  margin-left: 0;
}

.contract-attachment-bar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 16px;
}

.customer-edit-form {
  padding: 0 0 70px;
}

.customer-edit-tabs :deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.customer-edit-section {
  padding: 18px 20px 10px;
  margin-bottom: 18px;
  border: 1px solid #edf0f5;
  border-radius: 8px;
  background: #fff;
}

.customer-edit-section__title {
  margin: -18px -20px 18px;
  padding: 16px 20px;
  border-bottom: 1px solid #edf0f5;
  color: #303133;
  font-size: 15px;
  font-weight: 600;
}

.customer-help-icon {
  margin-left: 4px;
  color: #606266;
  cursor: help;
}

.customer-edit-drawer :deep(.el-drawer__body) {
  padding-top: 12px;
}

.customer-edit-drawer :deep(.el-form-item__label) {
  color: #303133;
  font-weight: 400;
}

.customer-edit-drawer :deep(.el-input__wrapper),
.customer-edit-drawer :deep(.el-select__wrapper),
.customer-edit-drawer :deep(.el-textarea__inner) {
  border-radius: 4px;
  box-shadow: 0 0 0 1px #dcdfe6 inset;
}

.identity-upload-row {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
}

.identity-upload {
  width: 160px;
}

.identity-upload-card {
  width: 160px;
  height: 112px;
  overflow: hidden;
  border: 1px dashed #dcdfe6;
  border-radius: 6px;
  background: #fafafa;
}

.identity-upload-image {
  width: 100%;
  height: 100%;
  display: block;
}

.identity-upload-placeholder {
  display: flex;
  height: 100%;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #606266;
  font-size: 14px;
}

.identity-upload-placeholder i {
  display: inline-flex;
  width: 30px;
  height: 30px;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #2f80ff;
  color: #fff;
  font-size: 16px;
}

.supplement-contract {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  padding: 12px 14px;
  margin-bottom: 16px;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  background: #f7f9fc;
}

.supplement-contract span,
.supplement-contract strong {
  display: block;
}

.supplement-contract span {
  margin-bottom: 5px;
  color: #909399;
  font-size: 12px;
}

.supplement-contract strong {
  overflow: hidden;
  color: #303133;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.supplement-upload {
  width: 100%;
}

.supplement-upload__text {
  display: flex;
  flex-direction: column;
  gap: 6px;
  color: #606266;
}

.supplement-upload__text strong {
  color: #303133;
  font-size: 14px;
}

.supplement-upload__text span {
  color: #909399;
  font-size: 12px;
}

.contract-alert,
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
  flex-wrap: wrap;
  gap: 10px;
}

.contract-table {
  width: 100%;
  border-radius: 0;
}

.contract-table :deep(.el-table__header th),
.contract-table :deep(.el-table__cell),
.contract-detail-drawer :deep(.el-table__header th),
.contract-detail-drawer :deep(.el-table__cell),
.contract-payment-drawer :deep(.el-table__header th),
.contract-payment-drawer :deep(.el-table__cell) {
  text-align: center;
}

.tenant-name-btn {
  max-width: 100%;
  display: inline-flex;
  overflow: hidden;
  vertical-align: middle;
}

.tenant-name-btn :deep(span) {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  white-space: nowrap;
}

.table-actions :deep(.el-button) {
  min-width: 40px;
  padding: 0 3px;
  margin-left: 0;
}

.inline-tag {
  margin-left: 6px;
}

.contract-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 12px 0 0;
}

.detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 14px;
  padding: 14px 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-head h3 {
  margin: 0;
  color: #1f2937;
  font-size: 18px;
  font-weight: 600;
}

.detail-head span {
  display: block;
  margin-top: 6px;
  color: #909399;
  font-size: 13px;
}

.detail-actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.approval-dialog {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.approval-option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  width: 100%;
}

.approval-option em {
  color: #909399;
  font-size: 12px;
  font-style: normal;
}

.precheck-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.precheck-item {
  min-height: 54px;
  padding: 10px 12px;
  border: 1px solid #f3d19e;
  border-radius: 8px;
  background: #fdf6ec;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.precheck-item div {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.precheck-item strong {
  color: #303133;
  font-weight: 600;
}

.precheck-item span {
  color: #8c98aa;
  font-size: 12px;
}

.precheck-item.is-done {
  border-color: #b3e19d;
  background: #f0f9eb;
}

.contract-change-dialog :deep(.el-dialog__body) {
  padding: 24px 28px 10px;
}

.contract-change-form :deep(.el-form-item) {
  margin-bottom: 22px;
}

.contract-change-form :deep(.el-input-number .el-input__inner) {
  text-align: left;
}

.contract-change-form :deep(.el-textarea__inner) {
  min-height: 104px !important;
  resize: vertical;
}

.renew-dialog :deep(.el-dialog__body) {
  min-height: 640px;
  padding-top: 0;
}

.renew-tip {
  margin-bottom: 20px;
}

.renew-dialog :deep(.el-form-item__label) {
  color: #606266;
  font-weight: 500;
}

.renew-dialog :deep(.el-range-editor.el-input__wrapper) {
  width: 100%;
}

.detail-alert {
  margin-bottom: 12px;
}

.detail-section {
  margin-bottom: 14px;
  padding: 16px;
  border: 1px solid #edf0f5;
  border-radius: 10px;
  background: #fff;
}

.detail-section-title {
  margin-bottom: 12px;
  color: #1f2937;
  font-weight: 600;
}

.contract-field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.contract-field {
  display: flex;
  min-height: 34px;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 13px;
}

.contract-field span {
  flex: 0 0 104px;
  color: #8c98aa;
}

.contract-field strong {
  min-width: 0;
  color: #303133;
  font-weight: 500;
  overflow-wrap: anywhere;
}

.contract-log {
  padding: 10px 12px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.contract-log__title {
  color: #303133;
  font-weight: 600;
}

.contract-log__operator,
.muted {
  color: #909399;
  font-size: 12px;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-card {
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  background: #fff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.summary-card span {
  color: #606266;
  font-size: 13px;
}

.summary-card strong {
  margin-top: 5px;
  color: #1f2937;
  font-size: 22px;
  font-weight: 600;
}

.contract-page :deep(.el-button),
.contract-page :deep(.el-input__wrapper),
.contract-page :deep(.el-select__wrapper) {
  border-radius: 6px;
}

@media (max-width: 1100px) {
  .contract-profile-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 32px;
  }

  .summary-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .contract-field-grid {
    grid-template-columns: 1fr;
  }

  .contract-info-grid,
  .clause-band {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 18px 28px;
  }

  .clause-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .contract-profile-page {
    padding: 18px 14px 28px;
  }

  .contract-profile-actions {
    position: static;
    margin-top: 14px;
  }

  .contract-profile-grid,
  .supplement-contract {
    grid-template-columns: 1fr;
    gap: 14px;
  }

  .contract-detail-header,
  .contract-info-title-row,
  .contract-text-item {
    align-items: flex-start;
    flex-direction: column;
  }

  .contract-detail-title-row h2 {
    max-width: 100%;
    white-space: normal;
  }

  .contract-detail-summary {
    margin-right: 14px;
    margin-left: 14px;
  }

  .contract-detail-tabs :deep(.el-tabs__header),
  .contract-info-block {
    padding-right: 14px;
    padding-left: 14px;
  }

  .contract-info-grid,
  .contract-info-grid.two-col,
  .clause-band {
    grid-template-columns: 1fr;
  }

  .clause-panel {
    padding: 0;
  }
}
</style>
