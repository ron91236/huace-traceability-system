<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" content="订单详情" style="margin-bottom:16px" />
    <el-row :gutter="16">
      <el-col :span="16">
        <el-card>
          <template #header>
            <span>订单信息</span>
          </template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="statusMap[order.status]?.type">{{ statusMap[order.status]?.label }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="关联证书">{{ order.certName || '暂无' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ formatTime(order.createdAt) }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 订单明细 -->
        <el-card style="margin-top:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>商品明细</span>
              <el-button v-if="order.status === 'DRAFT'" type="primary" size="small" @click="openItemForm()">
                <el-icon style="margin-right:4px"><Plus /></el-icon>添加
              </el-button>
            </div>
          </template>
          <el-table :data="orderItems" stripe>
            <el-table-column prop="batchName" label="计划批次" min-width="160" />
            <el-table-column prop="goodsName" label="商品名称" width="100" />
            <el-table-column prop="goodsSpec" label="规格" width="90" />
            <el-table-column prop="goodsWeight" label="重量" width="90" />
            <el-table-column prop="labelSpecMaterial" label="材质" min-width="140" show-overflow-tooltip />
            <el-table-column prop="labelSpecType" label="类型" width="70" />
            <el-table-column prop="labelSpecName" label="标签规格" min-width="120" show-overflow-tooltip />
            <el-table-column prop="price" label="价格" width="80" align="right">
              <template #default="{ row }">
                <span style="color:#d97706;font-weight:600">{{ row.price != null ? Number(row.price).toFixed(4) : '-' }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="quantity" label="订购数量" width="90" align="center" />
            <el-table-column prop="totalPrice" label="总价" width="90" align="right">
              <template #default="{ row }">{{ row.totalPrice ? Number(row.totalPrice).toFixed(2) : '-' }}</template>
            </el-table-column>
            <el-table-column v-if="order.status === 'DRAFT'" label="操作" width="120" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="primary" link @click="openItemForm(row)">编辑</el-button>
                <el-button size="small" type="danger" link @click="handleDeleteItem(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div v-if="orderItems.length === 0" style="text-align:center;padding:20px;color:#999">暂无商品明细，请点击"添加"按钮</div>
        </el-card>

        <!-- 标签管理 -->
        <el-card style="margin-top:16px">
          <template #header>标签管理</template>
          <el-table :data="orderCodes" stripe>
            <el-table-column prop="productName" label="产品名称" />
            <el-table-column prop="specName" label="标签规格" width="120" />
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="traceTemplate" label="溯源模板" width="120" />
          </el-table>
          <div v-if="orderCodes.length === 0" style="text-align:center;padding:20px;color:#999">暂无标签数据</div>
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <template #header>审核历史</template>
          <el-timeline>
            <el-timeline-item v-for="(item, i) in auditHistory" :key="i" :timestamp="formatTime(item.createdAt)" :type="actionTypeMap[item.action] || 'primary'">
              {{ item.operatorName || '系统' }} - {{ actionLabelMap[item.action] || item.action }}
              <div v-if="item.note" style="color:#999;font-size:12px;margin-top:4px">{{ item.note }}</div>
            </el-timeline-item>
          </el-timeline>
          <div v-if="auditHistory.length === 0" style="text-align:center;padding:20px;color:#999">暂无审核记录</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/编辑商品明细对话框 -->
    <el-dialog v-model="itemDialogVisible" :title="editItemId ? '编辑' : '添加'" width="520px" top="8vh">
      <el-form ref="itemFormRef" :model="itemForm" :rules="itemRules" label-width="90px" label-position="left">
        <!-- 计划批次 -->
        <el-form-item label="计划批次" prop="batchId">
          <el-select v-model="itemForm.batchId" filterable placeholder="请选择批次" style="width:100%" @change="onBatchChange">
            <el-option v-for="b in batches" :key="b.id" :label="batchLabel(b)" :value="b.id" />
          </el-select>
        </el-form-item>

        <!-- 商品信息（只读） -->
        <el-form-item label="商品名称">
          <el-input v-model="itemForm.goodsName" disabled />
        </el-form-item>
        <el-form-item label="规格">
          <el-input v-model="itemForm.goodsSpec" disabled />
        </el-form-item>
        <el-form-item label="重量">
          <el-input v-model="itemForm.goodsWeight" disabled />
        </el-form-item>

        <!-- 标签规格 -->
        <el-form-item label="标签规格" prop="labelSpecId">
          <el-select v-model="itemForm.labelSpecId" filterable placeholder="请选择标签规格" style="width:100%" @change="onLabelSpecChange">
            <el-option v-for="ls in labelSpecs" :key="ls.id" :label="ls.specName" :value="ls.id" />
          </el-select>
        </el-form-item>

        <!-- 标签规格详情（只读，自动带出） -->
        <el-form-item label="材质">
          <el-input v-model="itemForm.labelSpecMaterial" disabled />
        </el-form-item>
        <el-form-item label="类型">
          <el-input v-model="itemForm.labelSpecType" disabled />
        </el-form-item>
        <el-form-item label="价格">
          <el-input v-model="displayPrice" disabled>
            <template #append>元/枚</template>
          </el-input>
        </el-form-item>

        <!-- 订购数量 -->
        <el-form-item label="订购数量" prop="quantity">
          <el-input-number v-model="itemForm.quantity" :min="1" :precision="0" controls-position="right" style="width:100%" @change="calcTotal" />
        </el-form-item>

        <!-- 总价（自动计算） -->
        <el-form-item label="总价">
          <el-input :model-value="displayTotal" disabled>
            <template #append>元</template>
          </el-input>
        </el-form-item>

        <el-form-item label="备注">
          <el-input v-model="itemForm.remark" type="textarea" :rows="2" placeholder="可选备注信息" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="itemDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="itemSubmitting" @click="handleItemSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getOrderDetail, getAuditHistory, addOrderItem, updateOrderItem, deleteOrderItem, getGoods, getBatches, getEnterpriseLabelSpecs } from '@/api/enterprise'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const route = useRoute()
const order = ref<any>({})
const orderCodes = ref<any[]>([])
const orderItems = ref<any[]>([])
const auditHistory = ref<any[]>([])
const goodsList = ref<any[]>([])
const batches = ref<any[]>([])
const labelSpecs = ref<any[]>([])

const itemDialogVisible = ref(false)
const itemSubmitting = ref(false)
const editItemId = ref<number | null>(null)
const itemFormRef = ref<FormInstance>()

const defaultItemForm = {
  batchId: null as number | null,
  goodsId: null as number | null,
  goodsName: '', goodsSpec: '', goodsWeight: '',
  labelSpecId: null as number | null,
  labelSpecName: '', labelSpecMaterial: '', labelSpecType: '',
  price: null as number | null,
  quantity: null as number | null,
  remark: ''
}
const itemForm = reactive<Record<string, any>>({ ...defaultItemForm })

const itemRules: FormRules = {
  batchId: [{ required: true, message: '请选择计划批次', trigger: 'change' }],
  labelSpecId: [{ required: true, message: '请选择标签规格', trigger: 'change' }],
  quantity: [{ required: true, message: '请输入订购数量', trigger: 'blur' }]
}

const displayPrice = computed(() => itemForm.price != null ? Number(itemForm.price).toFixed(4) : '')
const displayTotal = computed(() => {
  if (itemForm.price != null && itemForm.quantity != null) {
    return (Number(itemForm.price) * Number(itemForm.quantity)).toFixed(2)
  }
  return ''
})

const actionTypeMap: Record<string, string> = { SUBMIT: 'primary', APPROVE: 'success', REJECT: 'danger' }
const actionLabelMap: Record<string, string> = { SUBMIT: '提交审核', APPROVE: '审核通过', REJECT: '审核驳回' }

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 19)
}

function batchLabel(b: any) {
  const g = goodsList.value.find((x: any) => x.id === b.goodsId)
  const spec = b.goodsSpec || ''
  const weight = g?.weightSpec || ''
  return `${b.name}${spec ? '(' + g?.name + '-' + spec : ''}${weight ? '-' + weight : ''}${spec || weight ? ')' : ''}`
}

function onBatchChange(batchId: number) {
  const b = batches.value.find((x: any) => x.id === batchId)
  if (!b) return
  const g = goodsList.value.find((x: any) => x.id === b.goodsId)
  itemForm.goodsId = b.goodsId || null
  itemForm.goodsName = g?.name || ''
  itemForm.goodsSpec = b.goodsSpec || g?.packageSpec || ''
  itemForm.goodsWeight = g?.weightSpec || ''
}

function onLabelSpecChange(lsId: number) {
  const ls = labelSpecs.value.find((x: any) => x.id === lsId)
  if (!ls) return
  itemForm.labelSpecName = ls.specName || ''
  itemForm.labelSpecMaterial = ls.material || ''
  itemForm.labelSpecType = ls.usageMethod || ''
  itemForm.price = ls.price || null
  calcTotal()
}

function calcTotal() {
  // totalPrice is computed in displayTotal, no need to store in form
}

function openItemForm(row?: any) {
  editItemId.value = row?.id || null
  if (row) {
    Object.keys(defaultItemForm).forEach(k => {
      itemForm[k] = row[k] ?? defaultItemForm[k as keyof typeof defaultItemForm]
    })
  } else {
    Object.assign(itemForm, { ...defaultItemForm })
  }
  itemDialogVisible.value = true
}

async function handleItemSubmit() {
  if (!itemFormRef.value) return
  const valid = await itemFormRef.value.validate().catch(() => false)
  if (!valid) return
  itemSubmitting.value = true
  try {
    const data: Record<string, any> = { ...itemForm, orderId: Number(route.params.id) }
    if (editItemId.value) {
      await updateOrderItem(editItemId.value, data)
      ElMessage.success('更新成功')
    } else {
      await addOrderItem(data)
      ElMessage.success('添加成功')
    }
    itemDialogVisible.value = false
    loadDetail()
  } finally { itemSubmitting.value = false }
}

async function handleDeleteItem(row: any) {
  await ElMessageBox.confirm('确认删除此商品明细?', '提示', { type: 'warning' })
  await deleteOrderItem(row.id)
  ElMessage.success('删除成功')
  loadDetail()
}

async function loadDetail() {
  const id = Number(route.params.id)
  try {
    const res = await getOrderDetail(id)
    order.value = res.data?.order || {}
    orderCodes.value = res.data?.orderCodes || []
    orderItems.value = res.data?.orderItems || []
  } catch (e) {}
  try {
    const res = await getAuditHistory(id)
    auditHistory.value = res.data || []
  } catch (e) {}
}

onMounted(async () => {
  const [gRes, lsRes, bRes] = await Promise.all([
    getGoods({ page: 1, size: 200 }),
    getEnterpriseLabelSpecs(),
    getBatches({ page: 1, size: 200 })
  ])
  goodsList.value = gRes.data?.list || []
  labelSpecs.value = lsRes.data || []
  batches.value = bRes.data?.list || []
  loadDetail()
})
</script>
