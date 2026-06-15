<template>
  <div class="customer-tag-selector">
    <el-skeleton :loading="loading" animated :rows="3">
      <template #default>
        <el-empty v-if="visibleTagTypeList.length === 0" :description="emptyText" />
        <div v-else class="tag-group-list">
          <section v-for="type in visibleTagTypeList" :key="type.typeId" class="tag-group-section">
            <div class="tag-group-title">{{ type.typeName }}</div>
            <el-checkbox-group
              :model-value="modelValue"
              :disabled="disabled"
              class="tag-check-list"
              @change="handleChange"
            >
              <el-checkbox-button v-for="tag in type.tags" :key="tag.tagId" :label="tag.tagId">
                <span class="tag-color" :style="{ backgroundColor: tag.tagColor || '#1059C6' }"></span>
                <span>{{ tag.tagName }}</span>
              </el-checkbox-button>
            </el-checkbox-group>
          </section>
        </div>
      </template>
    </el-skeleton>
  </div>
</template>

<script>
import { getAllTags, getTagTypeList } from '@/api/business/tag';

export default {
  name: 'CustomerTagSelector',
  props: {
    modelValue: {
      type: Array,
      default: () => [],
    },
    disabled: {
      type: Boolean,
      default: false,
    },
    emptyText: {
      type: String,
      default: '暂无可用客户标签',
    },
  },
  emits: ['update:modelValue', 'change'],
  data() {
    return {
      loading: false,
      tagTypeList: [],
    };
  },
  computed: {
    visibleTagTypeList() {
      return this.tagTypeList.filter(type => type.tags && type.tags.length > 0);
    },
  },
  created() {
    this.loadTags();
  },
  methods: {
    loadTags() {
      this.loading = true;
      Promise.all([getTagTypeList({ status: '0' }), getAllTags({ status: '0' })])
        .then(([typeRes, tagRes]) => {
          const types = typeRes.data.data || [];
          const tags = tagRes.data.data || [];
          this.tagTypeList = types.map(type => ({
            typeId: type.typeId,
            typeName: type.typeName,
            tags: tags.filter(tag => String(tag.tagType) === String(type.typeId)),
          }));
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleChange(values) {
      this.$emit('update:modelValue', values);
      this.$emit('change', values);
    },
  },
};
</script>

<style scoped>
.customer-tag-selector {
  width: 100%;
}

.tag-group-list {
  display: flex;
  max-height: 56vh;
  flex-direction: column;
  gap: 16px;
  overflow-y: auto;
}

.tag-group-section {
  padding-bottom: 14px;
  border-bottom: 1px solid #e5e7eb;
}

.tag-group-section:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.tag-group-title {
  margin-bottom: 10px;
  color: #1f2937;
  font-weight: 600;
}

.tag-check-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-check-list :deep(.el-checkbox-button__inner) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 32px;
  border-left: 1px solid var(--el-border-color);
  border-radius: 4px;
}

.tag-color {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}
</style>
