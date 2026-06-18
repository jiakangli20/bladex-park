<template>
  <el-dialog
    ref="nf-dialog"
    class="nf-dialog"
    v-model="visible"
    title="权限配置"
    width="60%"
    :before-close="handleClose"
    append-to-body
  >
    <p v-if="all" style="padding: 0 20px">
      不限制权限，所有人均可发起：<el-switch v-model="switchAll"></el-switch>（平台需单独配置）
    </p>
    <el-table v-if="visible" :data="data" border size="default">
      <el-table-column align="center" header-align="center" width="80">
        <template #header>
          <el-button
            circle
            type="primary"
            size="default"
            icon="el-icon-plus"
            @click="data.push({})"
          ></el-button>
        </template>
        <template #default="{ $index }">
          <el-button
            circle
            type="danger"
            size="default"
            icon="el-icon-delete"
            @click="data.splice($index, 1)"
          ></el-button>
        </template>
      </el-table-column>
      <el-table-column label="类型" prop="type" align="center" header-align="center">
        <template #default="{ row, $index }">
          <el-select
            v-model="row.type"
            size="default"
            placeholder="类型"
            @change="handleTypeChange($index)"
          >
            <el-option
              v-for="item in typeList"
              :key="item.value"
              :label="item.label"
              :value="item.value"
              :disabled="
                Boolean(data.find(d => d.type == item.value)) ||
                (switchAll && !['platform'].includes(item.value))
              "
            ></el-option>
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="值" prop="text" align="center" header-align="center">
        <template #default="{ row, $index }">
          <template v-if="row.type == 'user'">
            <el-input
              v-model="row.text"
              size="default"
              placeholder="用户"
              readonly
              @click="handleSelect($index, 'user-select')"
            >
              <template #append>
                <el-button icon="el-icon-plus"></el-button>
              </template>
            </el-input>
          </template>
          <template v-else-if="row.type == 'role'">
            <el-tree-select
              :ref="`role_${$index}`"
              v-model="row.value"
              size="default"
              multiple
              clearable
              placeholder="角色"
              :data="roleList"
              show-checkbox
              check-strictly
              @change="handleChange($index, `role_${$index}`)"
            >
            </el-tree-select>
          </template>
          <template v-else-if="row.type == 'dept'">
            <el-input
              v-model="row.text"
              size="default"
              placeholder="部门"
              readonly
              @click="handleSelect($index, 'dept-select')"
            >
              <template #append>
                <el-button icon="el-icon-plus"></el-button>
              </template>
            </el-input>
          </template>
          <template v-else-if="row.type == 'post'">
            <el-tree-select
              :ref="`post_${$index}`"
              v-model="row.value"
              size="default"
              multiple
              clearable
              placeholder="职位"
              :data="postList"
              show-checkbox
              check-strictly
              @change="handleChange($index, `post_${$index}`)"
            >
            </el-tree-select>
          </template>
          <template v-else-if="row.type == 'platform'">
            <el-tree-select
              :ref="`platform_${$index}`"
              v-model="row.value"
              size="default"
              multiple
              clearable
              placeholder="平台"
              :data="platformList"
              show-checkbox
              check-strictly
              @change="handleChange($index, `platform_${$index}`)"
            >
            </el-tree-select>
          </template>
        </template>
      </el-table-column>
    </el-table>
    <template #footer>
      <el-button @click="handleClose" size="default">取消</el-button>
      <el-button type="primary" @click="handleSubmit" size="default">确定</el-button>
    </template>

    <nf-user-select
      ref="user-select"
      check-type="checkbox"
      :user-url="userUrl"
      :custom-option="customOption"
      :default-checked="defaultChecked"
      @onConfirm="handleSelectConfirm"
    ></nf-user-select>

    <nf-dept-select
      ref="dept-select"
      :url="deptUrl"
      :custom-option="customOption"
      :default-checked="defaultChecked"
      @confirm="handleSelectConfirm"
    ></nf-dept-select>
  </el-dialog>
</template>
<script>
import NfUserSelect from '../nf-user-select/index.vue';
import NfDeptSelect from '../nf-dept-select/index.vue';

export default {
  props: {
    userOption: Object,
    all: {
      type: Boolean,
      default: false,
    },
  },
  components: {
    NfUserSelect,
    NfDeptSelect,
  },
  computed: {
    userUrl() {
      return this.userOption.userUrl;
    },
    roleUrl() {
      return this.userOption.roleUrl;
    },
    deptUrl() {
      return this.userOption.deptUrl;
    },
    postUrl() {
      return this.userOption.postUrl;
    },
    customUrl() {
      return this.userOption.customUrl;
    },
    customOption() {
      return this.userOption.customOption;
    },
    roleProps() {
      return this.customOption
        ? this.customOption.roleProps
        : { label: 'roleName', value: 'id', children: 'children', records: 'data.data' };
    },
    deptProps() {
      return this.customOption
        ? this.customOption.deptProps
        : { label: 'deptName', value: 'id', children: 'children', records: 'data.data' };
    },
    postProps() {
      return this.customOption
        ? this.customOption.postProps
        : { label: 'postName', value: 'id', children: 'children', records: 'data.data.records' };
    },
  },
  watch: {
    visible(val) {
      if (!this.init) {
        this.getRoleList();
        // this.getDeptList()
        this.getPostList();
        this.init = true;
      }
      if (val && this.userOption && this.userOption.data) {
        const data = JSON.parse(JSON.stringify(this.userOption.data));
        for (let item of data) {
          if (['role', 'post', 'platform'].includes(item.type) && typeof item.value == 'string')
            item.value = item.value?.split(',') || [];
        }
        this.data = data;
      }
    },
  },
  data() {
    return {
      switchAll: false,
      init: false,
      visible: false,
      data: [],
      roleList: [],
      deptList: [],
      postList: [],
      selectIndex: 0,
      defaultChecked: '',
      typeList: [
        {
          label: '用户',
          value: 'user',
        },
        {
          label: '角色',
          value: 'role',
        },
        {
          label: '部门',
          value: 'dept',
        },
        {
          label: '职位',
          value: 'post',
        },
        {
          label: '平台',
          value: 'platform',
        },
      ],
      typeDic: {
        user: '用户',
        role: '角色',
        dept: '部门',
        post: '职位',
        platform: '平台',
      },
      platformList: [
        // {
        //   label: 'PC',
        //   value: 'pc'
        // },
        {
          label: '移动端H5',
          value: 'h5',
        },
        {
          label: '微信小程序',
          value: 'mp-wx',
        },
        {
          label: 'IOS',
          value: 'ios',
        },
        {
          label: '安卓',
          value: 'android',
        },
      ],
    };
  },
  methods: {
    handleSelect(index, ref) {
      this.selectIndex = index;
      this.defaultChecked = this.data[index].value;
      this.$refs[ref].visible = true;
    },
    handleSelectConfirm(id, name) {
      this.data[this.selectIndex].value = id;
      this.data[this.selectIndex].text = name;
    },
    handleTypeChange(index) {
      const type = this.data[index].type;
      this.data[index] = {
        type,
        value: ['role', 'post', 'platform'].includes(type) ? [] : '',
      };
    },
    handleChange(index, ref) {
      setTimeout(() => {
        const text = this.$refs[ref]
          .getCheckedNodes()
          .map(n => n.label)
          .join(',');
        if (text) this.data[index].text = text;
      });
    },
    getRoleList() {
      this.$axios.get(this.roleUrl).then(res => {
        const { records } = this.roleProps;
        const list = this.getAsVal(res, records);
        this.handleList(list, this.roleProps);
        this.roleList = list;
      });
    },
    getDeptList() {
      this.$axios.get(this.deptUrl).then(res => {
        const { records } = this.deptProps;
        const list = this.getAsVal(res, records);
        this.handleList(list, this.deptProps);
        this.deptList = list;
      });
    },
    getPostList() {
      this.$axios.get(this.postUrl, { params: { size: -1 } }).then(res => {
        const { records } = this.postProps;
        const list = this.getAsVal(res, records);
        this.handleList(list, this.postProps);
        this.postList = list;
      });
    },
    handleSubmit() {
      let result = this.data.filter(d => d.type && d.value);
      result = result.map(item => {
        if (['role', 'post', 'platform'].includes(item.type)) {
          item.value = item.value.join(',');
        }
        return item;
      });
      this.$emit('submit', result, this.switchAll);
      this.visible = false;
    },
    handleClose(done) {
      this.visible = false;
      if (done && typeof done == 'function') done();
    },
    getAsVal(obj, bind = '') {
      let result = this.deepClone(obj);
      if (this.validateNull(bind)) return result;
      bind.split('.').forEach(ele => {
        if (!this.validateNull(result[ele])) {
          result = result[ele];
        } else {
          result = '';
        }
      });
      return result;
    },
    handleList(list, props) {
      for (let item of list) {
        item.label = item[props.label];
        item.value = item[props.value];
        if (item[props.children]) {
          item.children = item[props.children];
          this.handleList(item.children, props);
        }
      }
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

  .el-select {
    width: 100%;
  }
}
</style>
