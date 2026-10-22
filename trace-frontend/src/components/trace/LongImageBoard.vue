<template>
  <div class="long-board">
    <div class="long-board-body" ref="bodyRef">
      <template v-if="activeKey === REPORT_KEY">
        <div v-for="r in reports" :key="r.id" class="lb-card" @click="$emit('open-report', r)">
          <div class="lb-card-head">
            <span class="lb-card-name">{{ r.reportName }}</span>
            <el-tag v-if="r.testResult" :type="r.testResult === '合格' ? 'success' : 'danger'" size="small">{{ r.testResult }}</el-tag>
          </div>
          <div class="lb-card-body">
            <div v-if="r.testTime" class="lb-field">检测时间：{{ r.testTime }}</div>
            <div v-if="r.testOrg" class="lb-field">检测机构：{{ r.testOrg }}</div>
          </div>
          <div class="lb-card-link">查看报告 ›</div>
        </div>
        <div v-if="!reports.length" class="lb-blank">该批次暂无检测报告</div>
      </template>

      <template v-else-if="activeKey === HGZ_KEY">
        <div class="lb-card">
          <div class="lb-card-head"><span class="lb-card-name">承诺达标合格证</span><span class="lb-card-code">{{ hgz?.code }}</span></div>
          <div class="lb-card-body">
            <div v-if="hgz?.productName" class="lb-field">产品名称：{{ hgz.productName }}</div>
            <div v-if="hgz?.number" class="lb-field">数量(重量)：{{ hgz.number }}</div>
            <div v-if="hgz?.placeOfOrigin" class="lb-field">产地：{{ hgz.placeOfOrigin }}</div>
            <div v-if="hgz?.promiseUser" class="lb-field">承诺主体：{{ hgz.promiseUser }}</div>
            <div v-if="hgz?.useTime" class="lb-field">开具日期：{{ hgz.useTime }}</div>
          </div>
          <a class="lb-btn" :href="hgz?.qrUrl" target="_blank">查看合格证 ›</a>
        </div>
      </template>

      <template v-else>
        <img v-for="(p, i) in activePages" :key="p + i" :src="p" class="lb-page" :loading="i > 1 ? 'lazy' : 'eager'" alt="" />
        <div v-if="!activePages.length" class="lb-blank">该模块暂无内容</div>
      </template>
    </div>

    <nav class="lb-nav">
      <button v-for="item in navItems" :key="item.key"
        :class="['lb-nav-item', { active: item.key === activeKey }]"
        @click="select(item.key)">{{ item.name }}</button>
    </nav>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

const REPORT_KEY = '__report'
const HGZ_KEY = '__hgz'

const props = defineProps<{
  modules: Array<{ key: string; name: string; pages: string[] }>
  reports: any[]
  hgz: any | null
}>()

defineEmits<{ (e: 'open-report', report: any): void }>()

const navItems = computed(() => {
  const items = (props.modules || []).map(m => ({ key: m.key, name: m.name }))
  items.push({ key: REPORT_KEY, name: '检测报告' })
  if (props.hgz) items.push({ key: HGZ_KEY, name: '合格证' })
  return items
})

const activeKey = ref('')
watch(navItems, (items) => {
  if (!items.some(i => i.key === activeKey.value)) activeKey.value = items[0]?.key || ''
}, { immediate: true })

const activePages = computed(() =>
  props.modules?.find(m => m.key === activeKey.value)?.pages || [])

const bodyRef = ref<HTMLElement>()
function select(key: string) {
  if (activeKey.value === key) return
  activeKey.value = key
  bodyRef.value?.scrollIntoView({ block: 'start' })
}
</script>

<style scoped lang="scss">
.long-board { padding-bottom: 76px; }
.long-board-body { min-height: 60vh; }
.lb-page { display: block; width: 100%; }
.lb-card { background: #fff; border-radius: 12px; padding: 14px 16px; margin: 12px; box-shadow: 0 2px 10px rgba(0,0,0,.05); }
.lb-card-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.lb-card-name { font-weight: 600; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.lb-card-code { font-size: 12px; color: #999; }
.lb-card-body { margin-top: 8px; font-size: 13px; color: #666; line-height: 1.9; }
.lb-card-link, .lb-btn { margin-top: 10px; font-size: 13px; color: var(--trace-accent, #059669); text-decoration: none; display: inline-block; }
.lb-blank { padding: 40px 16px; text-align: center; color: #909399; font-size: 13px; }
.lb-nav {
  position: fixed; left: 0; right: 0; bottom: 0; z-index: 20;
  display: flex; gap: 8px; padding: 10px 12px calc(10px + env(safe-area-inset-bottom, 0px));
  background: rgba(255,255,255,.94); backdrop-filter: blur(8px);
  overflow-x: auto; scrollbar-width: none;
}
.lb-nav::-webkit-scrollbar { display: none; }
.lb-nav-item {
  flex: none; padding: 8px 16px; border: none; border-radius: 999px;
  background: #f2f3f5; color: #333; font-size: 13px; white-space: nowrap;
}
.lb-nav-item.active { background: var(--trace-accent, #059669); color: #fff; }
</style>
