<template>
  <div class="page-container">
    <el-row :gutter="16">
      <el-col :span="6"><el-card shadow="hover"><div class="stat-item"><div class="stat-icon" style="background:#409eff"><el-icon :size="28"><Goods /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.goodsCount || 0 }}</div><div class="stat-label">商品数量</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-item"><div class="stat-icon" style="background:#67c23a"><el-icon :size="28"><Tickets /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.batchCount || 0 }}</div><div class="stat-label">批次数量</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-item"><div class="stat-icon" style="background:#e6a23c"><el-icon :size="28"><Document /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.orderCount || 0 }}</div><div class="stat-label">订单数量</div></div></div></el-card></el-col>
      <el-col :span="6"><el-card shadow="hover"><div class="stat-item"><div class="stat-icon" style="background:#f56c6c"><el-icon :size="28"><Location /></el-icon></div><div class="stat-info"><div class="stat-value">{{ stats.baseCount || 0 }}</div><div class="stat-label">基地数量</div></div></div></el-card></el-col>
    </el-row>
    <el-card style="margin-top:16px">
      <template #header>最近登录</template>
      <p>上次登录时间：{{ stats.lastLoginTime || '暂无记录' }}</p>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getDashboard } from '@/api/enterprise'
const stats = ref<any>({})
onMounted(async () => { try { const res = await getDashboard(); stats.value = res.data || {} } catch (e) {} })
</script>

<style scoped lang="scss">
.stat-item { display:flex;align-items:center;gap:16px }
.stat-icon { width:56px;height:56px;border-radius:12px;display:flex;align-items:center;justify-content:center;color:#fff }
.stat-value { font-size:28px;font-weight:bold;color:#333 }
.stat-label { font-size:13px;color:#999;margin-top:2px }
</style>
