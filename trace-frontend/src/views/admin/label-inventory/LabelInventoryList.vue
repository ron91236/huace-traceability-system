<template>
  <div class="page-container">
    <el-tabs v-model="activeTab" type="border-card">
      <el-tab-pane label="码包库存明细" name="package">
        <el-table :data="packageList" v-loading="packageLoading" border stripe>
          <el-table-column prop="packageNo" label="码包编号" min-width="150" />
          <el-table-column prop="labelSpecName" label="标签规格" min-width="120" />
          <el-table-column prop="totalIn" label="入库数量" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #67c23a">{{ row.totalIn }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalVoid" label="作废扣减" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #f56c6c">{{ row.totalVoid }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalShip" label="发货扣减" width="100" align="right">
            <template #default="{ row }">
              <span style="color: #e6a23c">{{ row.totalShip }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentStock" label="当前库存" width="100" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.currentStock > 0 ? '#67c23a' : row.currentStock < 0 ? '#f56c6c' : '#909399', fontWeight: 'bold' }">
                {{ row.currentStock }}
              </span>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button type="primary" link size="small" @click="showLogs(row.codePackageId)">查看日志</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="标签规格汇总" name="spec">
        <el-table :data="specList" v-loading="specLoading" border stripe>
          <el-table-column prop="labelSpecName" label="标签规格" min-width="150" />
          <el-table-column prop="totalIn" label="入库总数" width="120" align="right">
            <template #default="{ row }">
              <span style="color: #67c23a">{{ row.totalIn }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalVoid" label="作废总数" width="120" align="right">
            <template #default="{ row }">
              <span style="color: #f56c6c">{{ row.totalVoid }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="totalShip" label="发货总数" width="120" align="right">
            <template #default="{ row }">
              <span style="color: #e6a23c">{{ row.totalShip }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="currentStock" label="当前库存" width="120" align="right">
            <template #default="{ row }">
              <span :style="{ color: row.currentStock > 0 ? '#67c23a' : row.currentStock < 0 ? '#f56c6c' : '#909399', fontWeight: 'bold' }">
                {{ row.currentStock }}
              </span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 库存日志弹窗 -->
    <el-dialog v-model="showLogDialog" title="库存变动日志" width="900px" destroy-on-close>
      <el-table :data="logList" v-loading="logLoading" border stripe max-height="500">
        <el-table-column prop="createdAt" label="时间" width="160">
          <template #default="{ row }">
            {{ row.createdAt ? row.createdAt.replace('T', ' ').substring(0, 19) : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="changeType" label="类型" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getChangeTypeTag(row.changeType)" size="small">
              {{ getChangeTypeLabel(row.changeType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" align="right" />
        <el-table-column prop="serialStart" label="起始码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="serialEnd" label="结束码" min-width="120" show-overflow-tooltip />
        <el-table-column prop="labelSpecName" label="标签规格" min-width="120" show-overflow-tooltip />
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="remark" label="备注" min-width="150" show-overflow-tooltip />
      </el-table>
      <el-pagination
        v-if="logTotal > logSize"
        style="margin-top: 16px; justify-content: flex-end"
        background
        layout="prev, pager, next"
        :total="logTotal"
        :page-size="logSize"
        v-model:current-page="logPage"
        @current-change="loadLogs"
      />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch } from 'vue'
import { getPackageInventory, getSpecInventory, getInventoryLogs } from '@/api/admin'

const activeTab = ref('package')

const packageList = ref<any[]>([])
const packageLoading = ref(false)

const specList = ref<any[]>([])
const specLoading = ref(false)

const showLogDialog = ref(false)
const logList = ref<any[]>([])
const logLoading = ref(false)
const logTotal = ref(0)
const logPage = ref(1)
const logSize = 20
const currentPackageId = ref<number | null>(null)

const loadPackageInventory = async () => {
  packageLoading.value = true
  try {
    const res = await getPackageInventory()
    packageList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    packageLoading.value = false
  }
}

const loadSpecInventory = async () => {
  specLoading.value = true
  try {
    const res = await getSpecInventory()
    specList.value = res.data || []
  } catch (e) {
    console.error(e)
  } finally {
    specLoading.value = false
  }
}

const loadLogs = async () => {
  logLoading.value = true
  try {
    const res = await getInventoryLogs({
      page: logPage.value,
      size: logSize,
      packageId: currentPackageId.value
    })
    logList.value = res.data?.records || []
    logTotal.value = res.data?.total || 0
  } catch (e) {
    console.error(e)
  } finally {
    logLoading.value = false
  }
}

const showLogs = (packageId: number) => {
  currentPackageId.value = packageId
  logPage.value = 1
  showLogDialog.value = true
  loadLogs()
}

const getChangeTypeLabel = (type: string) => {
  const map: Record<string, string> = {
    'IN': '入库',
    'VOID': '作废',
    'SHIP': '发货'
  }
  return map[type] || type
}

const getChangeTypeTag = (type: string) => {
  const map: Record<string, string> = {
    'IN': 'success',
    'VOID': 'danger',
    'SHIP': 'warning'
  }
  return map[type] || 'info'
}

watch(activeTab, (tab) => {
  if (tab === 'package' && packageList.value.length === 0) {
    loadPackageInventory()
  } else if (tab === 'spec' && specList.value.length === 0) {
    loadSpecInventory()
  }
})

onMounted(() => {
  loadPackageInventory()
})
</script>

<style scoped lang="scss">
.page-container {
  background: #fff;
  padding: 20px;
  border-radius: 8px;
}
</style>
