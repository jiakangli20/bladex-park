<template>
  <div>
    <div style="height: 80px"></div>
    <el-affix position="bottom" :offset="20">
      <el-row
        v-loading="loading"
        :class="isDialog && dialogType == 'dialog' ? 'foot-item-dialog' : 'foot-item'"
        :style="{
          width: isDialog
            ? dialogType == 'dialog'
              ? '100%'
              : `calc(${dialogWidth} - 10px)`
            : isCollapse
            ? 'calc(100% - 86px)'
            : 'calc(100% - 256px)',
        }"
      >
        <template v-if="process.status == 'todo'">
          <el-button type="primary" :disabled="loading" @click="$emit('draft')"> 暂存 </el-button>
          <el-button
            v-if="getButton('wf_pass')"
            type="success"
            size="default"
            :disabled="loading"
            @click="$emit('examine', true)"
          >
            <span v-if="['recall', 'reject'].includes(process.processIsFinished)">重新提交</span>
            <span v-else>{{ getButton('wf_pass').name }}</span>
          </el-button>
          <el-button
            v-if="getButton('wf_reject')"
            type="danger"
            size="default"
            :disabled="loading"
            @click="$emit('examine', false)"
          >
            <span v-if="backNodes.length > 1">{{ getButton('wf_reject').name }}</span>
            <span v-else>取消申请</span>
          </el-button>
          <el-button
            v-if="getButton('wf_transfer')"
            type="primary"
            size="default"
            :disabled="loading"
            @click="$emit('user-select', { type: 'transfer', checkType: 'radio' })"
            >{{ getButton('wf_transfer').name }}</el-button
          >
          <el-button
            v-if="getButton('wf_delegate')"
            type="warning"
            size="default"
            :disabled="loading"
            @click="$emit('user-select', { type: 'delegate', checkType: 'radio' })"
          >
            {{ getButton('wf_delegate').name }}
          </el-button>
          <el-button
            v-if="getButton('wf_rollback')"
            type="success"
            size="default"
            :disabled="loading"
            @click="handleRollback"
          >
            {{ getButton('wf_rollback').name }}
          </el-button>
          <el-button
            v-if="getButton('wf_terminate')"
            type="danger"
            size="default"
            :disabled="loading"
            @click="$emit('terminate')"
          >
            {{ getButton('wf_terminate').name }}
          </el-button>
          <el-button
            v-if="process.isMultiInstance && getButton('wf_add_instance')"
            type="primary"
            size="default"
            :disabled="loading"
            @click="
              $emit('user-select', {
                type: 'addInstance',
                checkType: 'checkbox',
              })
            "
          >
            {{ getButton('wf_add_instance').name }}
          </el-button>
        </template>
        <el-button
          v-if="
            permission.wf_process_withdraw &&
            process.isOwner &&
            process.isReturnable &&
            !['recall', 'reject'].includes(process.processIsFinished)
          "
          type="warning"
          size="default"
          :disabled="loading"
          @click="$emit('withdraw')"
        >
          撤销
        </el-button>
        <el-button
          v-if="getButton('wf_print')"
          type="info"
          size="default"
          :disabled="loading"
          @click="$emit('print')"
        >
          {{ getButton('wf_print').name }}
        </el-button>
      </el-row>
      <el-dialog v-model="nodeVisible" append-to-body title="选择回退节点">
        <nf-form
          v-if="nodeVisible"
          v-model="nodeForm"
          :option="nodeOption"
          @submit="handleNodeSubmit"
        ></nf-form>
      </el-dialog>
    </el-affix>
  </div>
</template>

<script>
import { backNodes } from '../../api/process/process';

import { mapGetters } from 'vuex';

export default {
  name: 'nf-button',
  computed: {
    ...mapGetters(['isCollapse', 'permission']),
  },
  props: {
    loading: {
      type: Boolean,
      default: false,
    },
    buttonList: {
      type: Array,
      default: () => {
        return [];
      },
    },
    process: Object,
    comment: String,
    isDialog: {
      type: Boolean,
      default: false,
    },
    dialogType: {
      type: String,
      default: 'drawer',
    },
    dialogWidth: {
      type: String,
      default: '80%',
    },
  },
  data() {
    return {
      nodeVisible: false,
      nodeForm: {},
      nodeOption: {
        column: [
          {
            label: '节点',
            prop: 'nodeId',
            type: 'select',
            props: {
              label: 'nodeName',
              value: 'nodeId',
            },
            span: 24,
            rules: [{ required: true, message: '请选择回退节点', trigger: 'change' }],
          },
        ],
      },
      backNodes: [],
    };
  },
  watch: {
    'process.taskId': {
      handler(val) {
        if (val) {
          backNodes({ taskId: val }).then(res => {
            const list = res.data.data;
            this.backNodes = list;
            this.findObject(this.nodeOption.column, 'nodeId').dicData = list;
          });
        }
      },
      immediate: true,
    },
  },
  methods: {
    // 指定回退
    handleRollback() {
      if (!this.comment) {
        this.$message.error('请填写批复意见');
        return;
      }
      this.nodeVisible = true;
    },
    // 指定回退确定
    handleNodeSubmit() {
      const { nodeId } = this.nodeForm;
      this.$emit('rollback', nodeId);
    },
    getButton(key) {
      return this.buttonList.find(b => b.buttonKey == key);
    },
  },
};
</script>

<style lang="scss" scoped>
.foot-item {
  position: fixed;
  bottom: 0;
  margin-left: -20px;
  // right: 0;
  z-index: 101;
  height: 66px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  -webkit-transition: 0.3s;
  transition: 0.3s;
  -webkit-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
.foot-item-dialog {
  z-index: 101;
  height: 66px;
  background-color: #fff;
  display: flex;
  justify-content: center;
  align-items: center;
  -webkit-box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}
</style>
