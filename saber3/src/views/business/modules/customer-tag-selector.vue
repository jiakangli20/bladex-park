<template>
  <div class="customer-tag-selector">
    <el-skeleton :loading="loading" animated :rows="3">
      <template #default>
        <el-empty v-if="visibleTagTypeList.length === 0" :description="emptyText" />
        <div v-else class="tag-group-list">
          <section v-for="type in visibleTagTypeList" :key="type.typeId" class="tag-group-section">
            <div class="tag-group-title">{{ type.typeName }}</div>
            <div class="tag-check-list">
              <button
                v-for="tag in type.tags"
                :key="tag.tagId"
                type="button"
                class="tag-check-item"
                :class="{
                  'is-checked': isChecked(tag.tagId),
                  'is-disabled': disabled,
                }"
                :disabled="disabled"
                @click="toggleTag(tag.tagId)"
              >
                <span class="tag-check-box"></span>
                <span class="tag-name">{{ tag.tagName }}</span>
              </button>
            </div>
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
    isChecked(tagId) {
      return this.modelValue.some(value => String(value) === String(tagId));
    },
    toggleTag(tagId) {
      if (this.disabled) return;
      const nextValue = this.isChecked(tagId)
        ? this.modelValue.filter(value => String(value) !== String(tagId))
        : [...this.modelValue, tagId];
      this.handleChange(nextValue);
    },
  },
};
</script>

<style scoped>
.customer-tag-selector {
  width: 100%;
  padding: 4px 0 6px;
  overflow: visible;
}

.tag-group-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  overflow: visible;
}

.tag-group-section {
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #fff;
}

.tag-group-section:last-child {
  padding-bottom: 12px;
}

.tag-group-title {
  margin-bottom: 10px;
  color: #1f2937;
  font-size: 14px;
  font-weight: 500;
}

.tag-check-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: flex-start;
  overflow: visible;
  padding-bottom: 2px;
}

.tag-check-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  box-sizing: border-box;
  max-width: 100%;
  min-width: 108px;
  height: 34px;
  padding: 0 10px;
  border: 1px solid #d7deea;
  border-radius: 4px;
  background: #f8fafc;
  color: #6b7280;
  cursor: pointer;
  font-size: 13px;
  line-height: 1;
  transition: all 0.18s ease;
}

.tag-check-item:hover {
  border-color: #c4d3ea;
  background: #f2f6ff;
  color: #2f75e8;
}

.tag-check-item.is-checked {
  border-color: #d7dfec;
  background: #f2f6ff;
  color: #2f75e8;
}

.tag-check-item.is-disabled {
  cursor: default;
  opacity: 0.95;
}

.tag-check-item.is-disabled:not(.is-checked) {
  background: #fafafa;
  color: #9ca3af;
}

.tag-check-item.is-disabled.is-checked {
  border-color: #d7dfec;
  background: #f2f6ff;
  color: #2f75e8;
}

.tag-check-box {
  position: relative;
  display: inline-flex;
  width: 14px;
  height: 14px;
  flex: 0 0 14px;
  align-items: center;
  justify-content: center;
  border: 1px solid #d4dce8;
  border-radius: 2px;
  background: #fff;
}

.tag-check-item.is-checked .tag-check-box {
  border-color: #2f75e8;
  background: #2f75e8;
}

.tag-check-item.is-checked .tag-check-box::after {
  width: 4px;
  height: 8px;
  border: solid #fff;
  border-width: 0 1px 1px 0;
  content: '';
  transform: rotate(45deg) translateY(-1px);
}

.tag-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
