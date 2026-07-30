<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <div class="search-group">
            <el-input v-model="search.orderNo" placeholder="订单编号" clearable style="width:140px" />
            <el-select v-model="search.status" placeholder="状态" clearable style="width:120px">
              <el-option label="待审核" value="PENDING" /><el-option label="已通过" value="APPROVED" /><el-option label="已驳回" value="REJECTED" />
            </el-select>
            <el-button type="primary" @click="loadData"><el-icon><Search /></el-icon>搜索</el-button>
            <el-button type="success" :loading="exporting" @click="handleExport"><el-icon><Download /></el-icon>导出</el-button>
          </div>
        </div>
      </template>
      <el-table :data="list" v-loading="loading" stripe row-key="id" @expand-change="handleExpandChange">
        <el-table-column type="expand">
          <template #default="{ row }">
            <div style="padding:12px 24px">
              <div v-if="row._expanding" style="text-align:center;color:#999">加载中...</div>
              <div v-else-if="row._expanded">
                <h4 style="margin:0 0 8px">商品明细</h4>
                <el-table :data="row._orderItems" border stripe size="small" style="margin-bottom:16px">
                  <el-table-column prop="batchName" label="批次" min-width="80" />
                  <el-table-column prop="goodsName" label="商品名称" min-width="100" />
                  <el-table-column prop="goodsSpec" label="规格" width="80" />
                  <el-table-column prop="goodsWeight" label="重量" width="80" />
                  <el-table-column prop="labelSpecName" label="标签规格" width="100" />
                  <el-table-column prop="price" label="单价(元)" width="80">
                    <template #default="{ row: ri }">{{ ri.price != null ? Number(ri.price).toFixed(4) : '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="quantity" label="数量" width="70" />
                  <el-table-column prop="totalPrice" label="总价(元)" width="90">
                    <template #default="{ row: ri }">{{ ri.totalPrice != null ? Number(ri.totalPrice).toFixed(2) : '-' }}</template>
                  </el-table-column>
                </el-table>
                <div style="display:flex;justify-content:space-between;align-items:center;margin:0 0 8px">
                  <h4 style="margin:0">条码信息</h4>
                  <div>
                    <el-button type="success" size="small" @click="handleExportBarcodes(row)" :loading="row._exportingBarcodes">导出条码</el-button>
                    <el-button type="primary" size="small" @click="openBindDialog(row)">绑定条码</el-button>
                  </div>
                </div>
                <el-table :data="row._orderCodes" border stripe size="small">
                  <el-table-column prop="productName" label="产品名称" min-width="100" />
                  <el-table-column prop="productDescription" label="产品描述" min-width="100">
                    <template #default="{ row: oc }">{{ oc.productDescription || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="goodsName" label="商品名称" min-width="80">
                    <template #default="{ row: oc }">{{ oc.goodsName || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="labelSpecName" label="标签规格" width="100" />
                  <el-table-column prop="serialStart" label="开始码" width="100" />
                  <el-table-column prop="serialEnd" label="结束码" width="100" />
                  <el-table-column prop="quantity" label="数量" width="70" />
                  <el-table-column prop="wasteCount" label="作废" width="60" />
                  <el-table-column prop="bindCount" label="绑定数" width="70" />
                  <el-table-column prop="productionTime" label="生产时间" width="160">
                    <template #default="{ row: oc }">{{ oc.productionTime || '-' }}</template>
                  </el-table-column>
                  <el-table-column prop="traceTemplate" label="溯源模板" width="100" />
                  <el-table-column label="预览码" width="80">
                    <template #default="{ row: oc }">
                      <el-button type="success" link size="small" @click="showPreviewQrcode(oc)">预览</el-button>
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" fixed="right">
                    <template #default="{ row: oc }">
                      <el-popconfirm title="确认删除?" @confirm="handleDeleteCode(oc.id, row)">
                        <template #reference><el-button type="danger" link size="small">删除</el-button></template>
                      </el-popconfirm>
                    </template>
                  </el-table-column>
                  <template #empty><el-empty description="暂无条码数据" :image-size="60" /></template>
                </el-table>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="orderNo" label="订单编号" width="180" />
        <el-table-column prop="enterpriseName" label="所属企业" width="120" />
        <el-table-column prop="certName" label="关联证书" width="120" />
        <el-table-column prop="totalBarcodeCount" label="订购条码数" width="100">
          <template #default="{ row }">{{ row.totalBarcodeCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="allocatedBarcodeCount" label="分配条码数" width="100">
          <template #default="{ row }">{{ row.allocatedBarcodeCount || 0 }}</template>
        </el-table-column>
        <el-table-column prop="totalPrice" label="订单总价" width="100">
          <template #default="{ row }">{{ row.totalPrice != null ? Number(row.totalPrice).toFixed(2) : '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type" size="small">{{ statusMap[row.status]?.label }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt ? row.createdAt.replace('T',' ').substring(0,19) : '' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="success" link @click="handleExportSingle(row)" :loading="row._exporting">导出</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="success" link @click="handleApprove(row)">通过</el-button>
            <el-button v-if="row.status === 'PENDING'" size="small" type="danger" link @click="openReject(row)">驳回</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrap">
        <el-pagination v-model:current-page="page" v-model:page-size="size" :total="total" layout="total, prev, pager, next" @change="loadData" />
      </div>
    </el-card>

    <el-dialog v-model="rejectVisible" title="驳回原因" width="400px">
      <el-input v-model="rejectNote" type="textarea" :rows="3" placeholder="请输入驳回原因" />
      <template #footer>
        <el-button @click="rejectVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReject">确认驳回</el-button>
      </template>
    </el-dialog>

    <!-- 绑定条码对话框 -->
    <el-dialog v-model="showBindDialog" title="绑定条码" width="520px">
      <el-form :model="bindForm" label-width="100px">
        <el-form-item label="条码库" required>
          <el-select v-model="bindForm.codePackageId" placeholder="请选择条码库" style="width:100%" @change="onPackageChange">
            <el-option v-for="cp in codePackages" :key="cp.id" :value="cp.id">
              <div style="display:flex;justify-content:space-between;align-items:center">
                <span>{{ cp.ruleName || cp.packageNo }}</span>
                <span style="font-size:12px;color:#999">可用 {{ cp.available }} 枚</span>
              </div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="订单标签号">
          <el-select v-model="bindForm.labelSpecId" placeholder="请选择标签规格" style="width:100%" @change="onLabelSpecChange">
            <el-option v-for="item in currentOrderItems" :key="item.id"
              :label="`${item.goodsName} - ${item.labelSpecName}`" :value="item.labelSpecId" />
          </el-select>
        </el-form-item>
        <el-form-item label="开始身份码" required>
          <el-input v-model="bindForm.serialStart" placeholder="请输入开始身份码" @input="triggerCalc" />
          <div v-if="selectedPkgDigits" class="form-tip">需输入 {{ selectedPkgDigits }} 位数字，不足请前补0</div>
        </el-form-item>
        <el-form-item label="结束身份码" required>
          <el-input v-model="bindForm.serialEnd" placeholder="请输入结束身份码" @input="triggerCalc" />
          <div v-if="selectedPkgDigits" class="form-tip">需输入 {{ selectedPkgDigits }} 位数字，不足请前补0</div>
        </el-form-item>
        <el-form-item label="总数量">
          <el-input :model-value="calcResult.totalCount || ''" disabled placeholder="自动计算" />
        </el-form-item>
        <el-form-item label="作废数量">
          <el-input :model-value="calcResult.voidedCount || ''" disabled placeholder="自动计算" />
        </el-form-item>
        <el-form-item label="绑定数量">
          <el-input :model-value="calcResult.bindCount || ''" disabled placeholder="自动计算" />
        </el-form-item>
        <el-form-item label="产品名称">
          <el-input v-model="bindForm.productName" placeholder="请输入产品名称" />
        </el-form-item>
        <el-form-item label="溯源模板">
          <el-select v-model="bindForm.traceTemplate" placeholder="请选择溯源模板" style="width:100%" clearable>
            <el-option v-for="tpl in traceTemplates" :key="tpl.templateKey" :label="tpl.templateName" :value="tpl.templateKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="生产时间">
          <el-date-picker v-model="bindForm.productionTime" type="datetime" placeholder="请选择生产时间" style="width:100%" value-format="YYYY-MM-DD HH:mm:ss" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="bindForm.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBindDialog = false">取消</el-button>
        <el-button type="primary" @click="handleBindCode" :loading="binding">保存</el-button>
      </template>
    </el-dialog>

    <!-- 预览码弹窗 -->
    <el-dialog v-model="previewQrVisible" title="预览溯源二维码" width="460px" align-center>
      <div style="text-align:center">
        <img v-if="previewQrData.qrcode" :src="previewQrData.qrcode" style="width:300px;height:300px" />
        <p v-if="previewQrData.traceUrl" style="margin-top:16px;font-size:13px;color:#666;word-break:break-all">溯源链接：{{ previewQrData.traceUrl }}</p>
        <p v-if="previewQrData.serialNo" style="font-size:12px;color:#999;margin-top:4px">流水号：{{ previewQrData.serialNo }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search, Download } from '@element-plus/icons-vue'
import { getAdminOrders, approveOrder, rejectOrder, exportOrders, exportSingleOrder, getAdminOrderDetail, getOrderCodes, bindOrderCode, deleteOrderCode, getAllCodePackages, getVoidedCount, previewOrderCodeQrcode, getLastSerial, exportOrderBarcodes } from '@/api/admin'
import { getTraceTemplateOptions } from '@/api/common'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const loading = ref(false)
const submitting = ref(false)
const exporting = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const page = ref(1)
const size = ref(20)
const search = reactive({ orderNo: '', status: '' })
const rejectVisible = ref(false)
const rejectNote = ref('')
const rejectId = ref(0)

// 条码绑定相关
const showBindDialog = ref(false)
const binding = ref(false)
const currentOrderId = ref(0)
const currentOrderItems = ref<any[]>([])
const codePackages = ref<any[]>([])
const traceTemplates = ref<any[]>([])
const calcResult = ref({ totalCount: 0, voidedCount: 0, bindCount: 0 })
const bindForm = ref<any>({
  codePackageId: null, labelSpecId: null, serialStart: '', serialEnd: '',
  wasteCount: 0, productName: '', traceTemplate: '', productionTime: '', remark: ''
})
let calcTimer: ReturnType<typeof setTimeout> | null = null

const selectedPkgDigits = computed(() => {
  if (!bindForm.value.codePackageId) return 0
  const pkg = codePackages.value.find((p: any) => p.id === bindForm.value.codePackageId)
  return pkg?.serialDigits || 0
})

// 预览码相关
const previewQrVisible = ref(false)
const previewQrData = ref<any>({})

onMounted(async () => {
  loadData()
  try { const r = await getAllCodePackages(); codePackages.value = r.data || [] } catch {}
  try { const r = await getTraceTemplateOptions(); traceTemplates.value = r.data || [] } catch {}
})

async function loadData() {
  loading.value = true
  try {
    const res = await getAdminOrders({ page: page.value, size: size.value, ...search })
    list.value = (res.data?.list || []).map((item: any) => ({ ...item, _expanded: false, _expanding: false, _orderItems: [], _orderCodes: [], _exportingBarcodes: false }))
    total.value = res.data?.total || 0
  } finally { loading.value = false }
}

async function handleExpandChange(row: any, expandedRows: any[]) {
  const isExpanded = expandedRows.some((r: any) => r.id === row.id)
  if (!isExpanded) return
  if (row._expanded) return // 已加载过
  row._expanding = true
  try {
    const res = await getAdminOrderDetail(row.id)
    const d = res.data
    row._orderItems = d?.orderItems || []
    row._orderCodes = d?.orderCodes || []
    row._expanded = true
  } catch (e) {
    ElMessage.error('加载明细失败')
  } finally {
    row._expanding = false
  }
}

async function handleExportSingle(row: any) {
  row._exporting = true
  try {
    await exportSingleOrder(row.id)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    row._exporting = false
  }
}

async function handleApprove(row: any) {
  submitting.value = true
  try {
    await approveOrder(row.id)
    ElMessage.success('审核通过')
    loadData()
  } finally { submitting.value = false }
}

function openReject(row: any) {
  rejectId.value = row.id
  rejectNote.value = ''
  rejectVisible.value = true
}

async function handleReject() {
  if (!rejectNote.value.trim()) return ElMessage.warning('请输入驳回原因')
  submitting.value = true
  try {
    await rejectOrder(rejectId.value, { reviewNote: rejectNote.value })
    ElMessage.success('已驳回')
    rejectVisible.value = false
    loadData()
  } finally { submitting.value = false }
}

async function handleExport() {
  exporting.value = true
  try {
    const params: any = {}
    if (search.orderNo) params.keyword = search.orderNo
    if (search.status) params.status = search.status
    await exportOrders(params)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally { exporting.value = false }
}

// ====== 条码绑定功能 ======
function triggerCalc() {
  if (calcTimer) clearTimeout(calcTimer)
  calcTimer = setTimeout(calcBindCount, 300)
}

async function calcBindCount() {
  const { serialStart, serialEnd } = bindForm.value
  if (!serialStart || !serialEnd || !/^\d+$/.test(serialStart) || !/^\d+$/.test(serialEnd)) {
    calcResult.value = { totalCount: 0, voidedCount: 0, bindCount: 0 }
    return
  }
  try {
    const res = await getVoidedCount(serialStart, serialEnd)
    calcResult.value = res.data
  } catch {
    calcResult.value = { totalCount: 0, voidedCount: 0, bindCount: 0 }
  }
}

function onPackageChange(pkgId: number) {
  const pkg = codePackages.value.find((p: any) => p.id === pkgId)
  if (pkg) {
    bindForm.value.serialStart = pkg.serialStart || ''
    bindForm.value.serialEnd = pkg.serialEnd || ''
  }
}

function onLabelSpecChange(labelSpecId: number) {
  const item = currentOrderItems.value.find((i: any) => i.labelSpecId === labelSpecId)
  if (item) bindForm.value.productName = item.goodsName || ''
}

async function openBindDialog(row: any) {
  currentOrderId.value = row.id
  currentOrderItems.value = row._orderItems || []
  bindForm.value = { codePackageId: null, labelSpecId: null, serialStart: '', serialEnd: '', wasteCount: 0, productName: '', traceTemplate: '', productionTime: '', remark: '' }
  calcResult.value = { totalCount: 0, voidedCount: 0, bindCount: 0 }
  showBindDialog.value = true
  try {
    const res = await getLastSerial(row.id)
    if (res.data?.nextStart) bindForm.value.serialStart = res.data.nextStart
  } catch {}
}

async function handleBindCode() {
  if (!bindForm.value.codePackageId) return ElMessage.warning('请选择条码库')
  if (!bindForm.value.serialStart || !bindForm.value.serialEnd) return ElMessage.warning('请输入开始和结束身份码')
  const digits = selectedPkgDigits.value
  if (digits > 0) {
    if (bindForm.value.serialStart.length !== digits) return ElMessage.warning(`开始身份码必须为 ${digits} 位，当前 ${bindForm.value.serialStart.length} 位`)
    if (bindForm.value.serialEnd.length !== digits) return ElMessage.warning(`结束身份码必须为 ${digits} 位，当前 ${bindForm.value.serialEnd.length} 位`)
  }
  binding.value = true
  try {
    await bindOrderCode(currentOrderId.value, { ...bindForm.value, wasteCount: calcResult.value.voidedCount, bindCount: calcResult.value.bindCount })
    ElMessage.success('绑定成功')
    showBindDialog.value = false
    // 刷新该订单的条码列表
    const row = list.value.find(r => r.id === currentOrderId.value)
    if (row) {
      const codeRes = await getOrderCodes(currentOrderId.value)
      row._orderCodes = codeRes.data || []
    }
    const pkgRes = await getAllCodePackages()
    codePackages.value = pkgRes.data || []
  } catch {
    ElMessage.error('绑定失败')
  } finally { binding.value = false }
}

async function handleDeleteCode(codeId: number, row: any) {
  try {
    await deleteOrderCode(codeId)
    ElMessage.success('已删除')
    const codeRes = await getOrderCodes(row.id)
    row._orderCodes = codeRes.data || []
  } catch { ElMessage.error('删除失败') }
}

async function showPreviewQrcode(oc: any) {
  try {
    const res = await previewOrderCodeQrcode(oc.id)
    previewQrData.value = res.data || {}
    previewQrVisible.value = true
  } catch { ElMessage.error('获取预览码失败') }
}

async function handleExportBarcodes(row: any) {
  row._exportingBarcodes = true
  try {
    await exportOrderBarcodes(row.id)
    ElMessage.success('导出成功')
  } catch { ElMessage.error('导出失败') }
  finally { row._exportingBarcodes = false }
}
</script>

<style scoped lang="scss">
.search-group {
  display: flex;
  gap: 8px;
  align-items: center;
}
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
