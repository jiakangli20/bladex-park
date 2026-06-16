<template>
  <el-select
    :model-value="modelValue"
    filterable
    allow-create
    default-first-option
    placeholder="请选择或输入指派人"
    style="width: 100%"
    @update:modelValue="value => $emit('update:modelValue', value)"
  >
    <el-option
      v-for="user in users"
      :key="user.id || user.userId || user.account"
      :label="userLabel(user)"
      :value="userLabel(user)"
    />
  </el-select>
</template>

<script>
export default {
  name: 'AssigneeSelect',
  props: {
    modelValue: { type: String, default: '' },
    users: { type: Array, default: () => [] },
  },
  emits: ['update:modelValue'],
  methods: {
    userLabel(user) {
      return user.realName || user.name || user.account || user.userName || user.id;
    },
  },
};
</script>
