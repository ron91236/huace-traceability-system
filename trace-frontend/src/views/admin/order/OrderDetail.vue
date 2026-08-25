<template>
  <div class="page-container">
    <el-page-header @back="$router.back()" content="订单详情" style="margin-bottom:16px" />
    <el-row :gutter="16">
      <el-col :span="16">
        <!-- 订单信息 -->
        <el-card>
          <template #header>订单信息</template>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="订单编号">{{ order.orderNo }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag v-if="order.status" :type="statusMap[order.status]?.type">{{ statusMap[order.status]?.label }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="所属企业">{{ order.enterpriseName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="关联证书">{{ order.certName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="提交时间">{{ order.submitTime || '-' }}</el-descriptions-item>
            <el-descriptions-item label="创建时间">{{ order.createdAt || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 商品明细 -->
        <el-card style="margin-top:16px" v-if="orderItems.length > 0">
          <template #header>商品明细</template>
          <el-table :data="orderItems" border stripe size="small">
            <el-table-column prop="batchName" label="批次" min-width="100" />
            <el-table-column prop="goodsName" label="商品名称" min-width="100" />
            <el-table-column prop="goodsSpec" label="规格" width="80" />
            <el-table-column prop="goodsWeight" label="重量" width="80" />
            <el-table-column prop="labelSpecName" label="标签规格" width="100" />
            <el-table-column prop="labelSpecMaterial" label="材质" width="80" />
            <el-table-column prop="price" label="单价(元)" width="80">
              <template #default="{ row }">{{ row.price != null ? Number(row.price).toFixed(4) : '-' }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column prop="totalPrice" label="总价(元)" width="90">
              <template #default="{ row }">{{ row.totalPrice != null ? Number(row.totalPrice).toFixed(2) : '-' }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 条码信息 -->
        <el-card style="margin-top:16px">
          <template #header>
            <div style="display:flex;justify-content:space-between;align-items:center">
              <span>条码信息</span>
              <div>
                <el-button type="success" size="small" @click="handleExportBarcodes" :loading="exportingBarcodes">导出条码</el-button>
                <el-button type="primary" size="small" @click="openBindDialog">绑定条码</el-button>
              </div>
            </div>
          </template>
          <el-table :data="orderCodes" border stripe size="small">
            <el-table-column prop="productName" label="产品名称" min-width="100" />
            <el-table-column prop="productDescription" label="产品描述" min-width="100">
              <template #default="{ row }">{{ row.productDescription || '-' }}</template>
            </el-table-column>
            <el-table-column prop="goodsName" label="商品名称" min-width="100">
              <template #default="{ row }">{{ row.goodsName || '-' }}</template>
            </el-table-column>
            <el-table-column prop="labelSpecName" label="标签规格" width="100" />
            <el-table-column prop="serialStart" label="开始码" width="100" />
            <el-table-column prop="serialEnd" label="结束码" width="100" />
            <el-table-column prop="quantity" label="数量" width="70" />
            <el-table-column prop="wasteCount" label="作废" width="60" />
            <el-table-column prop="bindCount" label="绑定数" width="70" />
            <el-table-column prop="productionTime" label="生产时间" width="160">
              <template #default="{ row }">{{ row.productionTime || '-' }}</template>
            </el-table-column>
            <el-table-column prop="traceTemplate" label="溯源模板" width="100">
              <template #default="{ row }">{{ row.templateName || '-' }}</template>
            </el-table-column>
            <el-table-column label="预览码" width="80">
              <template #default="{ row }">
                <el-button type="success" link size="small" @click="showPreviewQrcode(row)">预览</el-button>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-popconfirm title="确认解绑该码段？解绑后码段内的条码将失效并释放回条码库" @confirm="handleUnbindCode(row.id)">
                  <template #reference><el-button type="danger" link size="small">解绑</el-button></template>
                </el-popconfirm>
              </template>
            </el-table-column>
            <template #empty><el-empty description="暂无条码数据" :image-size="60" /></template>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="8">
        <!-- 审核历史 -->
        <el-card>
          <template #header>审核历史</template>
          <el-timeline v-if="auditHistory.length > 0">
            <el-timeline-item v-for="(item, i) in auditHistory" :key="i" :timestamp="item.createdAt" :type="item.action === 'APPROVE' ? 'success' : item.action === 'REJECT' ? 'danger' : 'primary'">
              {{ item.operatorName || '系统' }} - {{ item.action === 'APPROVE' ? '审核通过' : item.action === 'REJECT' ? '驳回' : item.action }} {{ item.note ? `(${item.note})` : '' }}
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无审核记录" :image-size="60" />
        </el-card>
      </el-col>
    </el-row>

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
            <el-option v-for="item in orderItems" :key="item.id"
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
            <el-option v-for="tpl in traceTemplates" :key="tpl.templateKey"
              :label="tpl.templateName" :value="tpl.templateKey" />
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
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAdminOrderDetail, getOrderCodes, bindOrderCode, deleteOrderCode, getAllCodePackages, getVoidedCount, previewOrderCodeQrcode, getLastSerial, exportOrderBarcodes } from '@/api/admin'
import { getTraceTemplateOptions } from '@/api/common'
import { orderStatusMap } from '@/utils/constants'

const statusMap = orderStatusMap
const route = useRoute()
const order = ref<any>({})
const orderCodes = ref<any[]>([])
const orderItems = ref<any[]>([])
const auditHistory = ref<any[]>([])
const codePackages = ref<any[]>([])
const traceTemplates = ref<any[]>([])
const calcResult = ref({ totalCount: 0, voidedCount: 0, bindCount: 0 })
const previewQrVisible = ref(false)
const previewQrData = ref<any>({})
const exportingBarcodes = ref(false)

// 绑定表单
const showBindDialog = ref(false)
const binding = ref(false)
const bindForm = ref<any>({
  codePackageId: null,
  labelSpecId: null,
  serialStart: '',
  serialEnd: '',
  wasteCount: 0,
  productName: '',
  traceTemplate: '',
  productionTime: '',
  remark: ''
})

let calcTimer: ReturnType<typeof setTimeout> | null = null

// 选中码包的流水号位数
const selectedPkgDigits = computed(() => {
  if (!bindForm.value.codePackageId) return 0
  const pkg = codePackages.value.find((p: any) => p.id === bindForm.value.codePackageId)
  return pkg?.serialDigits || 0
})

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
  } catch (e) {
    calcResult.value = { totalCount: 0, voidedCount: 0, bindCount: 0 }
  }
}

onMounted(async () => {
  const id = Number(route.params.id)
  try {
    const res = await getAdminOrderDetail(id)
    const d = res.data
    order.value = d?.order || {}
    orderCodes.value = d?.orderCodes || []
    orderItems.value = d?.orderItems || []
    auditHistory.value = d?.auditHistory || []
  } catch {
    // request 拦截器已处理错误提示
  }
  try {
    const pkgRes = await getAllCodePackages()
    codePackages.value = pkgRes.data || []
  } catch (e) {}
  try {
    const tplRes = await getTraceTemplateOptions(order.value.enterpriseId)
    traceTemplates.value = tplRes.data || []
  } catch (e) {}
})

function onPackageChange(pkgId: number) {
  const pkg = codePackages.value.find((p: any) => p.id === pkgId)
  if (pkg) {
    bindForm.value.serialStart = pkg.serialStart || ''
    bindForm.value.serialEnd = pkg.serialEnd || ''
  }
}

function onLabelSpecChange(labelSpecId: number) {
  const item = orderItems.value.find((i: any) => i.labelSpecId === labelSpecId)
  if (item) {
    bindForm.value.productName = item.goodsName || ''
  }
}

async function handleBindCode() {
  if (!bindForm.value.codePackageId) return ElMessage.warning('请选择条码库')
  if (!bindForm.value.serialStart || !bindForm.value.serialEnd) return ElMessage.warning('请输入开始和结束身份码')
  // 码段位数校验
  const digits = selectedPkgDigits.value
  if (digits > 0) {
    if (bindForm.value.serialStart.length !== digits) {
      return ElMessage.warning(`开始身份码必须为 ${digits} 位，当前 ${bindForm.value.serialStart.length} 位，不足请前补0`)
    }
    if (bindForm.value.serialEnd.length !== digits) {
      return ElMessage.warning(`结束身份码必须为 ${digits} 位，当前 ${bindForm.value.serialEnd.length} 位，不足请前补0`)
    }
  }
  binding.value = true
  try {
    const id = Number(route.params.id)
    await bindOrderCode(id, { ...bindForm.value, wasteCount: calcResult.value.voidedCount, bindCount: calcResult.value.bindCount })
    ElMessage.success('绑定成功')
    showBindDialog.value = false
    const codeRes = await getOrderCodes(id)
    orderCodes.value = codeRes.data || []
    const pkgRes = await getAllCodePackages()
    codePackages.value = pkgRes.data || []
    bindForm.value = { codePackageId: null, labelSpecId: null, serialStart: '', serialEnd: '', productName: '', traceTemplate: '', productionTime: '', remark: '' }
    calcResult.value = { totalCount: 0, voidedCount: 0, bindCount: 0 }
  } catch (e) {
    ElMessage.error('绑定失败')
  } finally {
    binding.value = false
  }
}

async function handleUnbindCode(codeId: number) {
  try {
    await deleteOrderCode(codeId)
    ElMessage.success('解绑成功')
    const id = Number(route.params.id)
    const codeRes = await getOrderCodes(id)
    orderCodes.value = codeRes.data || []
    // 解绑后重新拉取订单详情，同步绑定数量统计
    const detailRes = await getAdminOrderDetail(id)
    order.value = detailRes.data?.order || {}
  } catch (e) {
    ElMessage.error('解绑失败')
  }
}

async function openBindDialog() {
  showBindDialog.value = true
  // 自动填充上次绑定后的码段流水号+1
  try {
    const id = Number(route.params.id)
    const res = await getLastSerial(id)
    if (res.data?.nextStart) {
      bindForm.value.serialStart = res.data.nextStart
    }
  } catch (e) {}
}

async function showPreviewQrcode(row: any) {
  try {
    const res = await previewOrderCodeQrcode(row.id)
    previewQrData.value = res.data || {}
    previewQrVisible.value = true
  } catch (e) {
    ElMessage.error('获取预览码失败')
  }
}

async function handleExportBarcodes() {
  exportingBarcodes.value = true
  try {
    const id = Number(route.params.id)
    await exportOrderBarcodes(id)
    ElMessage.success('导出成功')
  } catch (e) {
    ElMessage.error('导出失败')
  } finally {
    exportingBarcodes.value = false
  }
}
</script>

<style scoped>
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
</style>
