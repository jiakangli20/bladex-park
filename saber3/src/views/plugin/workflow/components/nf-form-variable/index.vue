<template>
  <div class="nf-form-variable">
    <el-button
      text
      icon="el-icon-clock"
      title="表单历史"
      style="color: black; font-size: 18px"
      @click="visible = true"
    ></el-button>
    <el-drawer v-model="visible" size="60%" append-to-body destroy-on-close>
      <template #header>
        <span> 流程表单提交历史 <el-tag v-if="isDev">@nutflow1.8.1+</el-tag> </span>
      </template>
      <el-tabs v-model="active" class="nf-theme-custom">
        <el-tab-pane v-for="(item, index) in list" :name="index" :key="index" lazy>
          <template #label>
            <el-popover placement="top" width="auto" trigger="hover">
              {{ item.createUsername }} 于 {{ item.createTime }} 提交
              <template #reference>{{ item.taskName }}</template>
            </el-popover>
          </template>
          <nf-form
            v-model="item.formVariable"
            :option="item.formOption"
            :style="themeCustomStyle"
          ></nf-form>
        </el-tab-pane>
      </el-tabs>
    </el-drawer>
  </div>
</template>

<script>
import { getList } from '../../api/design/form-variable';
import theme from '../../mixins/theme';
export default {
  name: 'nf-form-variable',
  mixins: [theme],
  props: {
    processInsId: {
      type: String,
      default: '',
    },
  },
  computed: {
    isDev() {
      return process.env.NODE_ENV === 'development';
    },
  },
  watch: {
    processInsId: {
      handler(val) {
        this.getFormVariable(val);
      },
      immediate: true,
    },
  },
  data() {
    return {
      visible: false,
      active: 0,
      list: [],
    };
  },
  methods: {
    getFormVariable(processInsId) {
      if (this.validatenull(processInsId)) return;

      getList(1, -1, { processInsId }).then(res => {
        const list = res.data.data.records;
        list.forEach(item => {
          item.formVariable = JSON.parse(item.formVariable);
          item.formOption = JSON.parse(item.formOption);
          item.formOption.detail = true;
        });
        this.list = list;
      });
    },
  },
};
</script>

<style lang="scss">
.nf-form-variable {
  .el-button.is-text {
    padding: 0;
  }
}
</style>
