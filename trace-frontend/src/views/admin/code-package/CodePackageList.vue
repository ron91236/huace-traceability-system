<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-input v-model="keyword" placeholder="搜索码包编号" style="width:240px" clearable @clear="loadData" @keyup.enter="loadData">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-upload :show-file-list="false" accept=".csv,.xlsx,.xls" :before-upload="handleImport">
            <el-button type="primary"><el-icon><Upload /></el-icon>导入码包</el-button>
          </el-upload>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe size="small">
        <el-table-column prop="packageNo" label="码包编号" width="150" />
        <el-table-column prop="packageName" label="码包名称" width="130">
          <template #default="{ row }">{{ row.packageName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="productCode" label="产品代码" width="90">
          <template #default="{ row }">{{ row.productCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="产品名称" width="130">
          <template #default="{ row }">{{ row.codeType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="totalCount" label="总条数" width="80" />
        <el-table-column prop="serialStart" label="流水号起" width="100">
          <template #default="{ row }">{{ row.serialStart || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serialEnd" label="流水号止" width="100">
          <template #default="{ row }">{{ row.serialEnd || '-' }}</template>
        </el-table-column>
        <el-table-column prop="serialDigits" label="流水号位数" width="90">
          <template #default="{ row }">{{ row.serialDigits || '-' }}</template>
        </el-table-column>
        <el-table-column prop="labelSpecName" label="标签规格" width="120">
          <template #default="{ row }">{{ row.labelSpecName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="packagingType" label="包装类别" width="100">
          <template #default="{ row }">{{ row.packagingType || '-' }}</template>
        </el-table-column>
        <el-table-column prop="urlPrefix" label="防伪码前缀" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.urlPrefix || '-' }}</template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" width="120" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column prop="creator" label="创建人" width="80">
          <template #default="{ row }">{{ row.creator || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column label="下发状态" width="90">
          <template #default="{ row }">
            <el-tag v-if="row.dispatched" type="danger" size="small"><el-icon style="vertical-align:middle"><Lock /></el-icon> 已下发</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="importTime" label="导入时间" width="170">
          <template #default="{ row }">{{ row.importTime ? row.importTime.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="viewDetail(row)">查看</el-button>
            <el-popconfirm v-if="!row.dispatched" title="确认将该码包下发至印刷厂?" @confirm="handleDispatch(row)">
              <template #reference><el-button size="small" type="warning" link>下发</el-button></template>
            </el-popconfirm>
            <el-button v-if="row.dispatched" size="small" type="info" link @click="handleUndispatch(row)">解除下发</el-button>
            <el-popconfirm v-if="row.status === 'UNBOUND' && !row.dispatched" title="确认删除该码包?" @confirm="handleDelete(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="码包明细" width="800px">
      <el-descriptions :column="3" border size="small" style="margin-bottom:16px">
        <el-descriptions-item label="码包编号">{{ detailPkg.packageNo }}</el-descriptions-item>
        <el-descriptions-item label="码包名称">{{ detailPkg.packageName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="产品代码">{{ detailPkg.productCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="总条数">{{ detailPkg.totalCount }}</el-descriptions-item>
        <el-descriptions-item label="流水号起">{{ detailPkg.serialStart || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流水号止">{{ detailPkg.serialEnd || '-' }}</el-descriptions-item>
        <el-descriptions-item label="流水号位数">{{ detailPkg.serialDigits || '-' }}</el-descriptions-item>
        <el-descriptions-item label="标签规格">{{ detailPkg.labelSpecName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="包装类别">{{ detailPkg.packagingType || '-' }}</el-descriptions-item>
        <el-descriptions-item label="防伪码前缀">{{ detailPkg.urlPrefix || '-' }}</el-descriptions-item>
        <el-descriptions-item label="创建人">{{ detailPkg.creator || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detailPkg.remark || '-' }}</el-descriptions-item>
        <el-descriptions-item label="下发状态">
          <el-tag v-if="detailPkg.dispatched" type="danger" size="small">已下发</el-tag>
          <el-tag v-else type="info" size="small">未下发</el-tag>
        </el-descriptions-item>
        <el-descriptions-item v-if="detailPkg.dispatched" label="下发时间">{{ detailPkg.dispatchedAt?.replace('T',' ').substring(0,19) || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="detailPkg.dispatched" label="下发操作人">{{ detailPkg.dispatchedBy || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-table :data="detailList" stripe max-height="300" size="small">
        <el-table-column prop="serialNo" label="流水号" />
        <el-table-column prop="antiFakeCode" label="防伪码" />
        <el-table-column prop="url" label="溯源网址" />
        <el-table-column label="绑定状态" width="100">
          <template #default="{ row }"><el-tag :type="row.bindStatus === 'BOUND' ? 'success' : 'info'" size="small">{{ row.bindStatus === 'BOUND' ? '已绑定' : '未绑定' }}</el-tag></template>
        </el-table-column>
      </el-table>
      <div v-if="auditLogs.length > 0" style="margin-top:16px">
        <div style="font-weight:bold;margin-bottom:8px">操作审计日志</div>
        <el-table :data="auditLogs" stripe size="small" max-height="200">
          <el-table-column prop="action" label="操作" width="100">
            <template #default="{ row }">
              <el-tag :type="row.action === 'DISPATCH' ? 'danger' : 'warning'" size="small">{{ row.action === 'DISPATCH' ? '下发' : '解除下发' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="operatorName" label="操作人" width="120" />
          <el-table-column prop="reason" label="原因" show-overflow-tooltip />
          <el-table-column prop="createdAt" label="时间" width="170">
            <template #default="{ row }">{{ row.createdAt?.replace('T',' ').substring(0,19) || '' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Upload, Lock } from '@element-plus/icons-vue'
import { getCodePackages, importCodePackage, getCodePackageDetail, deleteCodePackage, dispatchCodePackage, undispatchCodePackage, getCodePackageAuditHistory } from '@/api/admin'
import { codePackageStatusMap } from '@/utils/constants'

const statusMap = codePackageStatusMap
const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const keyword = ref('')
const detailVisible = ref(false)
const detailList = ref<any[]>([])
const detailPkg = ref<any>({})
const auditLogs = ref<any[]>([])

onMounted(() => loadData())

async function loadData() {
  loading.value = true
  try {
    const res = await getCodePackages({ page: page.value, size: size.value, keyword: keyword.value })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function handleImport(file: File) {
  try {
    await importCodePackage(file)
    ElMessage.success('导入成功')
    loadData()
  } catch (e) {}
  return false
}

async function viewDetail(row: any) {
  detailPkg.value = row
  const res = await getCodePackageDetail(row.id)
  detailList.value = res.data?.items || []
  if (row.dispatched) {
    const auditRes = await getCodePackageAuditHistory(row.id)
    auditLogs.value = auditRes.data || []
  } else {
    auditLogs.value = []
  }
  detailVisible.value = true
}

async function handleDelete(id: number) {
  await deleteCodePackage(id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleDispatch(row: any) {
  await dispatchCodePackage(row.id)
  ElMessage.success('码包已下发')
  loadData()
}

async function handleUndispatch(row: any) {
  try {
    const { value } = await ElMessageBox.prompt('请输入解除下发的原因', '解除下发', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await undispatchCodePackage(row.id, { reason: value })
    ElMessage.success('已解除下发状态')
    loadData()
  } catch {}
}
</script>
