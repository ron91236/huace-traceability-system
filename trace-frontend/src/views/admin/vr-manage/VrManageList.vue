<template>
  <div class="page-container">
    <el-card>
      <template #header>
        <div class="table-toolbar">
          <el-select v-model="filterEnterpriseId" placeholder="筛选企业" clearable style="width:200px" @change="loadData">
            <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
          <el-button type="primary" @click="openSceneDialog()">
            <el-icon><Plus /></el-icon> 新增场景
          </el-button>
        </div>
      </template>

      <el-table :data="scenes" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="enterpriseName" label="企业" min-width="120" />
        <el-table-column prop="baseName" label="基地" min-width="100">
          <template #default="{ row }">{{ row.baseName || '-' }}</template>
        </el-table-column>
        <el-table-column prop="name" label="场景名称" min-width="140" />
        <el-table-column label="全景图" width="120">
          <template #default="{ row }">
            <el-image :src="row.panoramaUrl" fit="cover" style="width:80px;height:45px;border-radius:4px" :preview-src-list="[row.panoramaUrl]" />
          </template>
        </el-table-column>
        <el-table-column prop="isDefault" label="入口" width="70">
          <template #default="{ row }"><el-tag v-if="row.isDefault === 1" type="success" size="small">入口</el-tag></template>
        </el-table-column>
        <el-table-column prop="sortOrder" label="排序" width="70" />
        <el-table-column label="热点数" width="70">
          <template #default="{ row }">{{ row.hotspots?.length || 0 }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openHotspotEditor(row)">热点</el-button>
            <el-button size="small" type="primary" link @click="openSceneDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除此场景？（热点也会一并删除）" @confirm="handleDeleteScene(row.id)">
              <template #reference><el-button size="small" type="danger" link>删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 场景编辑弹窗 -->
    <el-dialog v-model="sceneDialogVisible" :title="editingScene?.id ? '编辑场景' : '新增场景'" width="600px" destroy-on-close>
      <el-form :model="sceneForm" label-width="90px">
        <el-form-item label="企业" required>
          <el-select v-model="sceneForm.enterpriseId" placeholder="选择企业" style="width:100%">
            <el-option v-for="e in enterprises" :key="e.id" :label="e.name" :value="e.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="关联基地">
          <el-select v-model="sceneForm.baseId" placeholder="选择基地（可选）" clearable style="width:100%">
            <el-option v-for="b in bases" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="场景名称" required>
          <el-input v-model="sceneForm.name" placeholder="如：生产车间入口" />
        </el-form-item>
        <el-form-item label="全景图" required>
          <div v-if="sceneForm.panoramaUrl" class="panorama-preview">
            <el-image :src="sceneForm.panoramaUrl" fit="cover" style="width:200px;height:100px;border-radius:6px" />
            <el-button size="small" type="danger" link @click="sceneForm.panoramaUrl = ''">移除</el-button>
          </div>
          <el-upload v-else :show-file-list="false" :http-request="handlePanoramaUpload" accept="image/*">
            <el-button type="primary"><el-icon><Upload /></el-icon> 上传全景图</el-button>
            <template #tip><div class="el-upload__tip">等距柱状投影图(Equirectangular)，JPG/PNG，建议4096x2048以上</div></template>
          </el-upload>
        </el-form-item>
        <el-form-item label="水平视角">
          <el-slider v-model="sceneForm.hfov" :min="60" :max="180" show-input />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="sceneForm.sortOrder" :min="0" />
        </el-form-item>
        <el-form-item label="入口场景">
          <el-switch v-model="sceneForm.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="sceneDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveScene">保存</el-button>
      </template>
    </el-dialog>

    <!-- 热点编辑器弹窗 -->
    <el-dialog v-model="hotspotEditorVisible" :title="`热点编辑 - ${hotspotScene?.name || ''}`" width="90%" top="2vh" destroy-on-close class="hotspot-editor-dialog">
      <div class="hotspot-editor-layout">
        <!-- 左侧：全景图标注区 -->
        <div class="panorama-edit-area">
          <div class="panorama-img-wrap" ref="panoramaWrapRef" @click="handlePanoramaClick">
            <img :src="hotspotScene?.panoramaUrl" class="panorama-full" @load="onPanoramaLoad" />
            <!-- 热点标记 -->
            <div v-for="(hs, idx) in hotspotList" :key="hs.id || idx"
              class="hotspot-marker"
              :class="{ 'hs-scene': hs.type === 'scene', 'hs-info': hs.type === 'info', selected: selectedHotspotIdx === idx }"
              :style="hotspotMarkerStyle(hs)"
              @click.stop="selectHotspot(idx)"
            >
              <span class="hs-icon">{{ hs.type === 'scene' ? '🔗' : 'ℹ️' }}</span>
              <span class="hs-label">{{ hs.label || (hs.type === 'scene' ? '跳转' : '信息') }}</span>
            </div>
            <div class="panorama-hint">点击全景图添加热点位置</div>
          </div>
        </div>
        <!-- 右侧：热点列表和编辑 -->
        <div class="hotspot-list-area">
          <div class="hs-list-header">
            <span>热点列表 ({{ hotspotList.length }})</span>
            <el-button size="small" type="primary" @click="addNewHotspot">
              <el-icon><Plus /></el-icon> 添加
            </el-button>
          </div>
          <div class="hs-list">
            <div v-for="(hs, idx) in hotspotList" :key="hs.id || idx"
              class="hs-item" :class="{ active: selectedHotspotIdx === idx }"
              @click="selectHotspot(idx)">
              <span class="hs-type-badge">{{ hs.type === 'scene' ? '🔗' : 'ℹ️' }}</span>
              <span class="hs-item-label">{{ hs.label || '未命名' }}</span>
              <el-button size="small" type="danger" link @click.stop="removeHotspot(idx)">删除</el-button>
            </div>
          </div>
          <!-- 选中热点编辑 -->
          <div v-if="selectedHotspot" class="hs-edit-form">
            <el-divider content-position="left">编辑热点</el-divider>
            <el-form label-width="80px" size="small">
              <el-form-item label="类型">
                <el-radio-group v-model="selectedHotspot.type">
                  <el-radio value="scene">跳转场景</el-radio>
                  <el-radio value="info">信息标注</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="标签">
                <el-input v-model="selectedHotspot.label" placeholder="热点显示文字" />
              </el-form-item>
              <template v-if="selectedHotspot.type === 'scene'">
                <el-form-item label="目标场景">
                  <el-select v-model="selectedHotspot.targetSceneId" placeholder="选择跳转场景" style="width:100%">
                    <el-option v-for="s in scenes" :key="s.id" :label="s.name || `场景${s.id}`" :value="s.id" :disabled="s.id === hotspotScene?.id" />
                  </el-select>
                </el-form-item>
              </template>
              <template v-if="selectedHotspot.type === 'info'">
                <el-form-item label="信息内容">
                  <el-input v-model="selectedHotspot.tooltip" type="textarea" :rows="3" placeholder="点击热点后显示的信息" />
                </el-form-item>
              </template>
              <el-form-item label="水平角">
                <el-input-number v-model="selectedHotspot.hYaw" :precision="2" :step="1" style="width:100%" />
              </el-form-item>
              <el-form-item label="垂直角">
                <el-input-number v-model="selectedHotspot.vPitch" :precision="2" :step="1" style="width:100%" />
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
      <template #footer>
        <el-button @click="hotspotEditorVisible = false">关闭</el-button>
        <el-button type="primary" :loading="savingHotspots" @click="saveAllHotspots">保存所有热点</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick } from 'vue'
import { Plus, Upload } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getVrScenes, createVrScene, updateVrScene, deleteVrScene, createVrHotspot, updateVrHotspot, deleteVrHotspot, getAllEnterprises } from '@/api/admin'
import { getBases } from '@/api/admin'
import { uploadFile } from '@/api/common'

const loading = ref(false)
const saving = ref(false)
const savingHotspots = ref(false)
const scenes = ref<any[]>([])
const enterprises = ref<any[]>([])
const bases = ref<any[]>([])
const filterEnterpriseId = ref<number | undefined>()

// 场景编辑
const sceneDialogVisible = ref(false)
const editingScene = ref<any>(null)
const sceneForm = ref<any>({
  enterpriseId: null, baseId: null, name: '', panoramaUrl: '', hfov: 120, sortOrder: 0, isDefault: 0,
})

// 热点编辑
const hotspotEditorVisible = ref(false)
const hotspotScene = ref<any>(null)
const hotspotList = ref<any[]>([])
const selectedHotspotIdx = ref(-1)
const panoramaWrapRef = ref<HTMLElement | null>(null)
const panoramaNaturalWidth = ref(1)
const panoramaNaturalHeight = ref(1)

const selectedHotspot = computed(() => {
  if (selectedHotspotIdx.value >= 0 && selectedHotspotIdx.value < hotspotList.value.length) {
    return hotspotList.value[selectedHotspotIdx.value]
  }
  return null
})

onMounted(async () => {
  await Promise.all([loadData(), loadEnterprises()])
})

async function loadData() {
  loading.value = true
  try {
    const res = await getVrScenes(filterEnterpriseId.value ? { enterpriseId: filterEnterpriseId.value } : {})
    scenes.value = res.data || []
  } finally { loading.value = false }
}

async function loadEnterprises() {
  try { const res = await getAllEnterprises(); enterprises.value = res.data || [] } catch {}
}

async function loadBases(enterpriseId: number) {
  if (!enterpriseId) { bases.value = []; return }
  try { const res = await getBases({ enterpriseId, page: 1, size: 100 }); bases.value = res.data?.list || res.data || [] } catch {}
}

function openSceneDialog(scene?: any) {
  editingScene.value = scene || null
  if (scene) {
    sceneForm.value = { ...scene }
    loadBases(scene.enterpriseId)
  } else {
    sceneForm.value = { enterpriseId: null, baseId: null, name: '', panoramaUrl: '', hfov: 120, sortOrder: 0, isDefault: 0 }
  }
  sceneDialogVisible.value = true
}

async function handlePanoramaUpload(options: any) {
  try {
    const res = await uploadFile(options.file)
    sceneForm.value.panoramaUrl = res.data
    ElMessage.success('全景图上传成功')
  } catch { ElMessage.error('上传失败') }
}

async function handleSaveScene() {
  if (!sceneForm.value.enterpriseId) return ElMessage.warning('请选择企业')
  if (!sceneForm.value.name) return ElMessage.warning('请输入场景名称')
  if (!sceneForm.value.panoramaUrl) return ElMessage.warning('请上传全景图')
  saving.value = true
  try {
    if (editingScene.value?.id) {
      await updateVrScene(editingScene.value.id, sceneForm.value)
    } else {
      await createVrScene(sceneForm.value)
    }
    ElMessage.success('保存成功')
    sceneDialogVisible.value = false
    await loadData()
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}

async function handleDeleteScene(id: number) {
  try {
    await deleteVrScene(id)
    ElMessage.success('删除成功')
    await loadData()
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

// ==================== 热点编辑器 ====================
function openHotspotEditor(scene: any) {
  hotspotScene.value = scene
  hotspotList.value = (scene.hotspots || []).map((h: any) => ({ ...h }))
  selectedHotspotIdx.value = -1
  hotspotEditorVisible.value = true
}

function onPanoramaLoad(e: Event) {
  const img = e.target as HTMLImageElement
  panoramaNaturalWidth.value = img.naturalWidth || 1
  panoramaNaturalHeight.value = img.naturalHeight || 1
}

function handlePanoramaClick(e: MouseEvent) {
  const wrap = panoramaWrapRef.value
  if (!wrap || !hotspotScene.value) return
  const rect = wrap.getBoundingClientRect()
  const img = wrap.querySelector('img') as HTMLImageElement
  if (!img) return

  // 计算点击位置相对于图片的百分比
  const x = (e.clientX - rect.left) / rect.width
  const y = (e.clientY - rect.top) / rect.height

  // 转换为 yaw/pitch（简化映射：全景图水平 360 度，垂直 180 度）
  const hfov = hotspotScene.value.hfov || 120
  const yaw = (x - 0.5) * 360
  const pitch = (0.5 - y) * 180

  // 添加到热点列表或更新选中热点的位置
  if (selectedHotspot.value) {
    selectedHotspot.value.hYaw = Math.round(yaw * 100) / 100
    selectedHotspot.value.vPitch = Math.round(pitch * 100) / 100
  } else {
    hotspotList.value.push({
      type: 'info', label: '', tooltip: '', hYaw: Math.round(yaw * 100) / 100, vPitch: Math.round(pitch * 100) / 100, sortOrder: hotspotList.value.length,
    })
    selectedHotspotIdx.value = hotspotList.value.length - 1
  }
}

function addNewHotspot() {
  hotspotList.value.push({
    type: 'info', label: '新热点', tooltip: '', hYaw: 0, vPitch: 0, sortOrder: hotspotList.value.length,
  })
  selectedHotspotIdx.value = hotspotList.value.length - 1
}

function selectHotspot(idx: number) {
  selectedHotspotIdx.value = selectedHotspotIdx.value === idx ? -1 : idx
}

function removeHotspot(idx: number) {
  hotspotList.value.splice(idx, 1)
  if (selectedHotspotIdx.value >= hotspotList.value.length) selectedHotspotIdx.value = -1
}

function hotspotMarkerStyle(hs: any) {
  // yaw/pitch 转百分比位置
  const x = (Number(hs.hYaw) / 360 + 0.5) * 100
  const y = (0.5 - Number(hs.vPitch) / 180) * 100
  return {
    left: `${Math.max(2, Math.min(98, x))}%`,
    top: `${Math.max(2, Math.min(98, y))}%`,
  }
}

async function saveAllHotspots() {
  if (!hotspotScene.value) return
  savingHotspots.value = true
  try {
    const sceneId = hotspotScene.value.id
    // 找出需要删除的（有id但不在当前列表中的）
    const originalIds = new Set((hotspotScene.value.hotspots || []).map((h: any) => h.id))
    const currentIds = new Set(hotspotList.value.filter(h => h.id).map(h => h.id))
    const toDelete = [...originalIds].filter(id => !currentIds.has(id))

    // 删除
    for (const id of toDelete) {
      await deleteVrHotspot(id as number)
    }

    // 创建或更新
    for (let i = 0; i < hotspotList.value.length; i++) {
      const hs = { ...hotspotList.value[i], sceneId, sortOrder: i }
      if (hs.id) {
        await updateVrHotspot(hs.id, hs)
      } else {
        await createVrHotspot(hs)
      }
    }

    ElMessage.success('热点保存成功')
    await loadData()
    // 更新当前编辑的scene热点数据
    const updated = scenes.value.find(s => s.id === sceneId)
    if (updated) hotspotScene.value = updated
  } catch (e: any) { ElMessage.error(e.message || '热点保存失败') }
  finally { savingHotspots.value = false }
}
</script>

<style scoped lang="scss">
.page-container { padding: 0; }
.table-toolbar { display: flex; gap: 12px; align-items: center; }

.panorama-preview { display: flex; align-items: center; gap: 12px; }

/* 热点编辑器 */
.hotspot-editor-layout {
  display: flex;
  gap: 16px;
  height: 70vh;
  min-height: 400px;
}

.panorama-edit-area {
  flex: 1;
  position: relative;
  overflow: hidden;
  background: #1a1a2e;
  border-radius: 8px;
}

.panorama-img-wrap {
  width: 100%;
  height: 100%;
  position: relative;
  cursor: crosshair;
  img.panorama-full {
    width: 100%;
    height: 100%;
    object-fit: cover;
    display: block;
  }
}

.panorama-hint {
  position: absolute;
  bottom: 8px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0,0,0,0.6);
  color: #fff;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  pointer-events: none;
}

.hotspot-marker {
  position: absolute;
  transform: translate(-50%, -50%);
  z-index: 10;
  display: flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  transition: transform 0.15s;
  &:hover { transform: translate(-50%, -50%) scale(1.15); }
  &.selected { z-index: 20; }

  .hs-icon {
    font-size: 20px;
    filter: drop-shadow(0 1px 3px rgba(0,0,0,0.5));
  }
  .hs-label {
    font-size: 11px;
    background: rgba(0,0,0,0.7);
    color: #fff;
    padding: 2px 6px;
    border-radius: 3px;
    white-space: nowrap;
    max-width: 80px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  &.selected .hs-icon { filter: drop-shadow(0 0 6px #409eff); }
  &.selected .hs-label { background: #409eff; }
}

.hotspot-list-area {
  width: 320px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  overflow: hidden;
}

.hs-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f5f7fa;
  border-bottom: 1px solid #e5e7eb;
  font-weight: 600;
  font-size: 14px;
}

.hs-list {
  flex: 1;
  overflow-y: auto;
  max-height: 200px;
}

.hs-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
  transition: background 0.15s;
  &:hover { background: #f5f7fa; }
  &.active { background: #ecf5ff; border-left: 3px solid #409eff; }
  .hs-type-badge { font-size: 14px; }
  .hs-item-label { flex: 1; font-size: 13px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
}

.hs-edit-form {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px 12px;
}

@media (max-width: 768px) {
  .hotspot-editor-layout {
    flex-direction: column;
    height: auto;
  }
  .panorama-edit-area { height: 300px; }
  .hotspot-list-area { width: 100%; }
}
</style>
