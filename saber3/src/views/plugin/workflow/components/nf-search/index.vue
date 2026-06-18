<template>
  <el-dialog
    ref="nf-dialog"
    class="nf-dialog"
    v-model="visible"
    title="表单搜索条件"
    width="60%"
    append-to-body
  >
    <nf-form v-model="form" :option="option" @submit="handleSubmit"></nf-form>
  </el-dialog>
</template>

<script>
export default {
  name: 'nf-search',
  props: {
    modelValue: [Array, String],
    default: () => {
      return [];
    },
  },
  watch: {
    modelValue: {
      handler(val) {
        if (val) {
          const arr = [];
          this.deepClone(val)
            .split(',')
            .forEach(s => {
              const row = s.split(':');
              const [column, condition, value, type] = row;
              const data = { column, condition };
              if (!['exists', 'notExists'].includes(condition)) {
                data.value = value;
                data.type = type;
              }
              arr.push(data);
            });
          this.form.search = arr;
        }
      },
      immediate: true,
    },
  },
  data() {
    return {
      visible: false,
      form: {},
      option: {
        border: true,
        submitText: '确定',
        column: [
          {
            label: '',
            labelWidth: '0px',
            span: 24,
            prop: 'search',
            type: 'dynamic',
            children: {
              align: 'center',
              column: [
                {
                  label: '字段',
                  prop: 'column',
                },
                {
                  label: '条件',
                  prop: 'condition',
                  type: 'select',
                  dicData: [
                    {
                      label: '等于',
                      value: 'equal',
                    },
                    {
                      label: '不等于',
                      value: 'notEqual',
                    },
                    {
                      label: '包含',
                      value: 'like',
                    },
                    {
                      label: '存在',
                      value: 'exists',
                    },
                    {
                      label: '不存在',
                      value: 'notExists',
                    },
                    {
                      label: '大于',
                      value: 'greaterThan',
                    },
                    {
                      label: '小于',
                      value: 'lessThan',
                    },
                    {
                      label: '大于等于',
                      value: 'greaterThanOrEqual',
                    },
                    {
                      label: '小于等于',
                      value: 'lessThanOrEqual',
                    },
                  ],
                  placeholder: '条件，存在/不存在无需填值',
                  change: ({ value, index }) => {
                    if (['exists', 'notExists'].includes(value)) this.form.search[index].value = '';
                  },
                },
                {
                  label: '值',
                  prop: 'value',
                },
                {
                  label: '类型',
                  prop: 'type',
                  type: 'select',
                  placeholder: '默认string',
                  dicData: [
                    {
                      label: 'string',
                      value: 'string',
                    },
                    {
                      label: 'integer',
                      value: 'integer',
                    },
                    {
                      label: 'double',
                      value: 'double',
                    },
                    {
                      label: 'long',
                      value: 'long',
                    },
                  ],
                },
              ],
            },
          },
        ],
      },
    };
  },
  methods: {
    handleSubmit(form, done) {
      const { search } = form;
      if (search && search.length > 0) {
        const arr = [];
        search.forEach(s => {
          const { column, condition, value, type } = s;
          if (column && (['exists', 'notExists'].includes(condition) || (condition && value)))
            arr.push(`${column}:${condition}:${value}:${type || 'string'}`);
        });
        this.$emit('update:modelValue', arr.join(','));
      } else {
        this.$emit('update:modelValue', '');
      }
      this.visible = false;
      done();
    },
  },
};
</script>

<style lang="scss">
.nf-dialog {
  display: flex;
  flex-direction: column;
  margin: 0 !important;
  position: absolute;
  top: 40%;
  left: 50%;
  transform: translate(-50%, -40%);
  max-height: calc(100% - 30px);
  max-width: calc(100% - 30px);
  .el-dialog__body {
    flex: 1;
    overflow: auto;
  }
}
</style>
