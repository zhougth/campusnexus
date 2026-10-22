<script setup lang="ts">
const props = defineProps<{
  total: number
  page: number
  size: number
}>()

const emit = defineEmits<{
  change: [page: number, size: number]
}>()

function handlePage(page: number): void {
  emit('change', page, props.size)
}

function handleSize(size: number): void {
  emit('change', 1, size)
}
</script>

<template>
  <div v-if="total > 0" class="pager">
    <el-pagination
      background
      layout="total, sizes, prev, pager, next"
      :total="total"
      :current-page="page"
      :page-size="size"
      :page-sizes="[10, 20, 50, 100]"
      @current-change="handlePage"
      @size-change="handleSize"
    />
  </div>
</template>
