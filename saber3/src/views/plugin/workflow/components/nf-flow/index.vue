<template>
  <el-row type="flex">
    <el-timeline>
      <template v-for="(item, index) in flowList" :key="index">
        <el-timeline-item
          v-if="!['candidate', 'sequenceFlow'].includes(item.historyActivityType)"
          :key="item.id"
          :timestamp="item.endTime || item.createTime"
          placement="top"
        >
          <el-card shadow="never">
            <p>
              {{ item.assigneeName }} 在 [{{ item.createTime }}] 开始处理 [{{
                item.historyActivityType == 'endEvent'
                  ? '结束'
                  : item.historyActivityName || '未命名'
              }}] 环节
            </p>
            <p v-if="item.historyActivityDurationTime">
              任务历时 [{{ item.historyActivityDurationTime }}]
            </p>
            <template v-if="item.comments && item.comments.length > 0">
              <p
                v-for="(comment, index) in item.comments.filter(c => c.action === 'AddComment')"
                :key="index"
              >
                <template v-if="index < 1">
                  <span v-if="commentMap[comment.type]">
                    {{ commentMap[comment.type] }}: [{{ comment.fullMessage }}]
                  </span>
                  <span
                    style="color: #1989fa; float: right"
                    v-if="item.comments.filter(c => c.action === 'AddComment').length > 1"
                    @click="handleClick"
                  >
                    {{ toggleText }}
                    <i :class="[isFlag ? 'el-icon-arrow-up' : 'el-icon-arrow-down']"></i>
                    <el-icon>
                      <el-icon-arrow-up v-if="isFlag" />
                      <el-icon-arrow-down v-else />
                    </el-icon>
                  </span>
                  <p style="color: gray; font-size: 12px" v-if="comment.time">{{ comment.time }}</p>
                </template>
                <template v-if="index > 0 && isFlag">
                  <span v-if="commentMap[comment.type]">
                    {{ commentMap[comment.type] }}: [{{ comment.fullMessage }}]
                  </span>
                  <p style="color: gray; font-size: 12px" v-if="comment.time">{{ comment.time }}</p>
                </template>
              </p>
              <template v-if="item.attachments && item.attachments.length > 0">
                <div style="display: flex; justify-content: space-between">
                  <span>附件: </span>
                  <div style="flex: 1; margin: -15px 0 0 4px">
                    <nf-upload
                      :model-value="item.attachments"
                      :props="{ label: 'name', value: 'url' }"
                      :upload-preview="handleUploadPreview"
                      disabled
                    ></nf-upload>
                  </div>
                </div>
              </template>
            </template>
            <p v-if="item.endTime">结束时间: [{{ item.endTime }}]</p>
          </el-card>
        </el-timeline-item>
      </template>
    </el-timeline>
  </el-row>
</template>

<script>
import exForm from '../../mixins/ex-form';
export default {
  name: 'nf-flow',
  mixins: [exForm],
  props: {
    flowList: {
      type: Array,
      default: () => {
        return [];
      },
    },
  },
  data() {
    return {
      isFlag: false,
      toggleText: '展开',
      commentMap: {
        assigneeComment: '变更审核人',
        dispatchComment: '调度',
        transferComment: '转办',
        delegateComment: '委托',
        rollbackComment: '驳回意见',
        terminateComment: '终止意见',
        addMultiInstanceComment: '加签',
        deleteMultiInstanceComment: '减签',
        withdrawComment: '撤销',
        recallComment: '撤回',
        deleteProcessComment: '删除流程',
        skipComment: '跳过',
        comment: '审批意见',
      },
    };
  },
  methods: {
    handleClick() {
      this.isFlag = !this.isFlag;
      this.toggleText = this.isFlag ? '收起' : '展开';
    },
  },
};
</script>

<style></style>
