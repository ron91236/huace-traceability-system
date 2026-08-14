<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div style="display:flex;gap:8px;flex-wrap:wrap">
            <el-select v-if="isAdmin" v-model="entFilter" placeholder="全部企业" clearable
              style="width:180px" @change="loadData">
              <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
            </el-select>
            <el-input v-model="search.foodName" placeholder="食品名称" clearable style="width:160px" @keyup.enter="loadData" />
            <el-input v-model="search.barcode" placeholder="商品条码" clearable style="width:160px" @keyup.enter="loadData" />
            <el-select v-model="search.hasLabel" placeholder="是否创建标签" clearable style="width:140px">
              <el-option label="已创建" value="yes" />
              <el-option label="未创建" value="no" />
            </el-select>
            <el-date-picker v-model="dateRange" type="daterange" range-separator="至"
              start-placeholder="创建开始日期" end-placeholder="创建结束日期"
              value-format="YYYY-MM-DD" style="width:260px" />
            <el-button type="primary" @click="loadData">搜索</el-button>
          </div>
          <el-button v-if="!isAdmin" type="primary" @click="addVisible = true">
            <el-icon><Plus /></el-icon>新增
          </el-button>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" border stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column v-if="isAdmin" label="所属企业" min-width="140">
          <template #default="{ row }">{{ row.enterpriseName || entName(row.enterpriseId) }}</template>
        </el-table-column>
        <el-table-column prop="foodName" label="食品名称" min-width="160" />
        <el-table-column prop="barcode" label="商品条码" width="150" />
        <el-table-column prop="spec" label="规格" width="120">
          <template #default="{ row }">{{ row.spec || '-' }}</template>
        </el-table-column>
        <el-table-column prop="labelVersionCount" label="数字标签版本数量" width="140" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.labelVersionCount > 0" type="success" size="small">{{ row.labelVersionCount }}</el-tag>
            <span v-else>0</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170" />
        <el-table-column label="操作" width="230">
          <template #default="{ row }">
            <template v-if="row.labelVersionCount > 0">
              <el-button v-if="!isAdmin" size="small" type="primary" link @click="createLabel(row)">新增标签</el-button>
              <el-button size="small" type="primary" link @click="goVersions(row)">{{ isAdmin ? '查看标签版本' : '管理标签版本' }}</el-button>
            </template>
            <el-button v-else-if="!isAdmin" size="small" type="primary" link @click="createLabel(row)">创建标签</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total"
          layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <!-- 新增商品 -->
    <el-dialog v-model="addVisible" title="新增商品" width="460px" :close-on-click-modal="false">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="食品名称" prop="foodName">
          <el-input v-model="addForm.foodName" placeholder="请输入食品名称" />
        </el-form-item>
        <el-form-item label="商品条码" prop="barcode">
          <el-input v-model="addForm.barcode" placeholder="请输入商品条码" />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="addForm.spec" placeholder="请输入规格（选填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleAdd">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance } from 'element-plus'
import { getDlProducts, createDlProduct, createDlVersion } from '@/api/digital-label'
import { useDlAdmin } from '@/composables/useDlAdmin'

const { isAdmin, entFilter, enterprises, entName } = useDlAdmin()
const router = useRouter()
const list = ref<any[]>([])
const loading = ref(false)
const saving = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const search = reactive({ foodName: '', barcode: '', hasLabel: '' })
const dateRange = ref<string[]>([])
const addVisible = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ foodName: '', barcode: '', spec: '' })
const addRules = {
  foodName: [{ required: true, message: '请输入食品名称', trigger: 'blur' }],
  barcode: [{ required: true, message: '请输入商品条码', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res = await getDlProducts({
      page: page.value, size: size.value,
      foodName: search.foodName || undefined,
      barcode: search.barcode || undefined,
      hasLabel: search.hasLabel || undefined,
      startDate: dateRange.value?.[0] || undefined,
      endDate: dateRange.value?.[1] || undefined,
      enterpriseId: entFilter.value,
    })
    list.value = res.data?.list || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

async function handleAdd() {
  await addFormRef.value?.validate()
  saving.value = true
  try {
    await createDlProduct(addForm)
    ElMessage.success('新增成功')
    addVisible.value = false
    addForm.foodName = ''; addForm.barcode = ''; addForm.spec = ''
    loadData()
  } finally {
    saving.value = false
  }
}

async function createLabel(row: any) {
  try {
    const res = await createDlVersion(row.id)
    ElMessage.success('标签版本已创建')
    router.push(`/dl/versions/${res.data.id}/edit`)
  } catch (e) {}
}

function goVersions(row: any) {
  router.push({ path: `/dl/products/${row.id}/versions`, query: { foodName: row.foodName, barcode: row.barcode } })
}

onMounted(loadData)
</script>
