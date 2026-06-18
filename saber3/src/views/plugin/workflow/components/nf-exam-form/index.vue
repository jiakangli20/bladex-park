<template>
  <nf-form v-model="examineForm" ref="examineForm" :option="examineOption"></nf-form>
</template>

<script>
export default {
  name: 'nf-exam-form',
  props: {
    process: {
      type: Object,
      default: () => {
        return {};
      },
    },
  },
  watch: {
    process: {
      handler(val) {
        if (!val) return;
        const { hideComment, hideAttachment, hideCopy, hideExamine, copyUser, copyUserName } = val;
        if (hideComment) this.findObject(this.examineOption.column, 'comment').display = false; // 隐藏评论
        if (hideComment || hideAttachment)
          this.findObject(this.examineOption.column, 'attachment').display = false; // 隐藏评论附件
        if (hideCopy) this.findObject(this.examineOption.column, '$copyUser').display = false; // 隐藏抄送人
        if (hideExamine) this.findObject(this.examineOption.column, '$assignee').display = false; // 隐藏指定下一步审核人
        if (copyUser) this.examineForm.copyUser = val.copyUser; // 默认抄送人
        if (copyUserName) this.examineForm.$copyUser = val.copyUserName; // 默认抄送人
      },
      deep: true,
      immediate: true,
    },
  },
  data() {
    return {
      examineForm: {},
      examineOption: {
        menuBtn: false,
        labelWidth: '100px',
        column: [
          {
            label: '批复意见',
            prop: 'comment',
            type: 'textarea',
            span: 24,
            event: {
              change: val => {
                val = typeof val == 'object' ? val.value : val;
                this.$emit('update:comment', val);
              },
            },
            display: true,
          },
          {
            label: '附件',
            prop: 'attachment',
            type: 'upload',
            propsHttp: {
              res: 'data',
              url: 'link',
              name: 'originalName',
            },
            props: {
              label: 'name',
              value: 'url',
            },
            action: '/blade-resource/oss/endpoint/put-file',
            span: 24,
            display: true,
          },
          {
            label: '抄送人',
            prop: '$copyUser',
            placeholder: '请选择 抄送人',
            readonly: true,
            append: '+',
            span: 24,
            event: {
              click: () => {
                this.$emit('user-select', { type: 'copy', checkType: 'checkbox' });
              },
            },
            display: true,
          },
          {
            label: '指定审批人',
            prop: '$assignee',
            placeholder:
              '指定下一级审批人，如不选择则使用默认处理人，驳回时无效。多选时若下一节点为多实例并行则会进行动态加减签，若不是则只有第一个生效。',
            readonly: true,
            append: '+',
            span: 24,
            event: {
              click: () => {
                this.$emit('user-select', { type: 'assignee', checkType: 'checkbox' });
              },
            },
            display: true,
          },
        ],
      },
    };
  },
  methods: {
    getComment() {
      return this.examineForm.comment;
    },
  },
};
</script>

<style></style>
