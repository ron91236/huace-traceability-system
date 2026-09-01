<template>
  <el-dialog :model-value="modelValue" @update:model-value="$emit('update:modelValue', $event)"
    :title="templateData?.id ? '编辑溯源模板' : '新增溯源模板'" width="1200px" top="2vh" destroy-on-close :close-on-click-modal="false">
    <TemplateGallery v-model="galleryVisible" @select="applyGalleryItem" />
    <!-- 顶部：基本信息 -->
    <div class="top-bar">
      <el-form :inline="true" size="small">
        <el-form-item label="模板名称"><el-input v-model="form.templateName" placeholder="模板名称" style="width:160px" /></el-form-item>
        <el-form-item label="模板类型"><el-input v-model="form.templateType" placeholder="类型" style="width:100px" /></el-form-item>
        <el-form-item label="布局">
          <el-select v-model="form.layout" style="width:130px" @change="onLayoutChange">
            <el-option v-for="p in LAYOUT_PRESETS" :key="p.key" :label="p.label" :value="p.key">
              <div style="font-size:13px">{{ p.label }}</div>
              <div style="font-size:11px;color:#999;line-height:1.2">{{ p.description }}</div>
            </el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="主题">
          <el-select v-model="form.themeKey" style="width:110px">
            <el-option v-for="t in themes" :key="t.key" :label="t.label" :value="t.key" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-upload :show-file-list="false" :http-request="uploadBackground" accept="image/*" style="display:inline-block">
            <el-button size="small">背景图</el-button>
          </el-upload>
          <el-button v-if="form.backgroundImage" size="small" type="danger" link @click="form.backgroundImage = ''" style="margin-left:4px">移除背景</el-button>
          <img v-if="form.backgroundImage" :src="form.backgroundImage" class="bg-preview" @error="(e: any) => e.target.style.display='none'" />
        </el-form-item>
        <el-form-item label="页面背景">
          <el-color-picker v-model="form.pageBackgroundColor" show-alpha />
        </el-form-item>
        <el-form-item label="切换动画">
          <el-select v-model="form.pageTransition" style="width:110px">
            <el-option label="无" value="" />
            <el-option label="淡入" value="fade" />
            <el-option label="左滑" value="slide-left" />
            <el-option label="右滑" value="slide-right" />
            <el-option label="上滑" value="slide-up" />
            <el-option label="下滑" value="slide-down" />
            <el-option label="缩放" value="zoom" />
            <el-option label="翻转" value="flip" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-dropdown @command="loadPreset" style="margin-left:8px">
            <el-button size="small" type="success" plain>预设模板</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="pig-trace">生猪溯源模板</el-dropdown-item>
                <el-dropdown-item command="blank">空白模板</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
          <el-button size="small" type="primary" plain style="margin-left:8px" @click="galleryVisible = true">模板画廊</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 页面标签 -->
    <div class="page-tabs">
      <div v-for="(p, i) in form.pages" :key="p.id"
        :class="['page-tab', { active: currentPageIdx === i }]" @click="currentPageIdx = i" @dblclick.stop="startRenamePage(i)">
        <input v-if="renamingPageIdx === i" v-model="p.name" class="tab-rename-input"
          @blur="renamingPageIdx = null" @keyup.enter="renamingPageIdx = null" @click.stop ref="tabRenameRef" />
        <template v-else>{{ p.name }}</template>
        <el-icon v-if="form.pages.length > 1" class="tab-close" @click.stop="removePage(i)"><Close /></el-icon>
      </div>
      <div class="page-tab add-tab" @click="addPage">+ 新页面</div>
    </div>

    <!-- 编辑器主体 -->
    <div class="editor-main">
      <!-- 左侧：元素面板 -->
      <div class="palette">
        <div class="palette-title">元素面板</div>
        <div class="palette-group" v-for="group in elementPalette" :key="group.label">
          <div class="group-label">{{ group.label }}</div>
          <div class="palette-items">
            <div v-for="el in group.items" :key="el.type" class="palette-item" @click="addElement(el.type)">
              <el-icon :size="16"><component :is="el.icon" /></el-icon>
              <span>{{ el.label }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 中间：手机预览画布 -->
      <div class="canvas-area">
        <div class="phone-frame" :data-theme="form.themeKey" :style="currentPhoneThemeStyle">
          <div class="serial-badge">溯源码：00000001</div>
          <div ref="phoneBodyRef" class="phone-body" style="overflow-y: auto; flex: 1;">
            <div v-if="currentPage.elements.length === 0" class="empty-hint">
              点击左侧元素添加到页面
            </div>
            <div v-for="(el, idx) in currentPage.elements" :key="el.id"
              :class="['canvas-element', { selected: selectedId === el.id }, { 'has-bg-btn': el.type === 'button' && el.bgImage }]"
              :style="el.type === 'button' && el.style?.width && el.style.width !== '100%' ? { width: getButtonWidth(el), boxSizing: 'border-box' } : {}"
              @click="selectElement(el)">
              <!-- 元素预览 -->
              <template v-if="el.type === 'text'">
                <div class="el-text" :style="el.style || {}">{{ el.content || '文本内容' }}</div>
              </template>
              <template v-else-if="el.type === 'image'">
                <el-image v-if="el.src" :src="el.src" fit="cover" class="el-image" :style="{ width: el.style?.width || '100%', height: el.style?.height || 'auto', borderRadius: (el.style?.borderRadius || 0) + 'px' }">
                  <template #error><div class="el-placeholder">加载失败</div></template>
                </el-image>
                <div v-else class="el-placeholder">[图片: {{ el.label || '点击配置' }}]</div>
              </template>
              <template v-else-if="el.type === 'video'">
                <div class="el-placeholder video">▶ [视频: {{ el.label || '点击配置' }}]</div>
              </template>
              <template v-else-if="el.type === 'divider'">
                <el-divider />
              </template>
              <template v-else-if="isInfoSection(el.type)">
                <div class="info-section-preview">
                  <div class="section-title">{{ el.label }}</div>
                  <div class="section-fields">
                    <div v-for="f in getSectionPreviewFields(el)" :key="f" class="field-tag">{{ f }}</div>
                  </div>
                </div>
              </template>
              <template v-else-if="el.type === 'button'">
                <div class="btn-wrapper">
                  <div v-if="el.bgImage && el.label" class="btn-label-above" :style="{ fontSize: (el.style?.fontSize || 14) + 'px', color: el.style?.color || '#fff' }">{{ el.label }}</div>
                  <div class="el-button-preview" :style="buttonPreviewStyle(el)">{{ el.bgImage ? '' : el.label }}</div>
                </div>
              </template>
              <template v-else-if="el.type === 'custom-field'">
                <div class="el-placeholder">[自定义字段: {{ el.label || '配置' }}]</div>
              </template>
              <template v-else-if="el.type === 'rich-text'">
                <div class="el-rich-text-preview" :style="el.style || {}" v-html="el.content || '<p>富文本内容</p>'"></div>
              </template>
              <template v-else-if="el.type === 'anti-counterfeit'">
                <div class="el-anti-fake-preview">
                  <div class="af-title">防伪验证</div>
                  <div class="af-input-row">
                    <span class="af-input-mock">请输入防伪码查询验证</span>
                    <span class="af-btn-mock">点击验证</span>
                  </div>
                </div>
              </template>
              <template v-else-if="el.type === 'map'">
                <div class="el-map-preview" :style="el.style || {}">
                  <el-icon :size="24" color="#059669"><MapLocation /></el-icon>
                  <span class="map-label">地图: {{ el.center || '未配置坐标' }}</span>
                  <span class="map-hint">缩放: {{ el.zoom || 10 }} | 标记: {{ (el.markers || []).length }}个</span>
                </div>
              </template>
              <!-- 操作按钮 -->
              <div class="element-actions" v-if="selectedId === el.id" @click.stop>
                <el-button size="small" circle :disabled="idx === 0" @click.stop="moveElement(idx, -1)"><el-icon><Top /></el-icon></el-button>
                <el-button size="small" circle :disabled="idx === currentPage.elements.length - 1" @click.stop="moveElement(idx, 1)"><el-icon><Bottom /></el-icon></el-button>
                <el-button size="small" circle type="danger" @click.stop="removeElement(idx)"><el-icon><Delete /></el-icon></el-button>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右侧：属性面板 -->
      <div class="props-panel">
        <div class="props-title">属性配置</div>
        <div v-if="!selectedElement" class="props-empty">点击画布中的元素进行配置</div>
        <div v-else class="props-form">
          <!-- 通用属性 -->
          <el-form label-width="70px" size="small">
            <el-form-item label="元素名称">
              <el-input v-model="selectedElement.label" />
            </el-form-item>

            <!-- 文本元素 -->
            <template v-if="selectedElement.type === 'text'">
              <el-form-item label="文本内容">
                <el-input v-model="selectedElement.content" type="textarea" :rows="3" />
              </el-form-item>
              <el-form-item label="字号">
                <el-input-number v-model="selectedElement.style.fontSize" :min="10" :max="36" controls-position="right" />
              </el-form-item>
              <el-form-item label="颜色">
                <el-color-picker v-model="selectedElement.style.color" />
              </el-form-item>
              <el-form-item label="加粗">
                <el-switch v-model="selectedElement.style.fontWeight" active-value="bold" inactive-value="normal" />
              </el-form-item>
              <el-form-item label="对齐">
                <el-radio-group v-model="selectedElement.style.textAlign">
                  <el-radio-button value="left">左</el-radio-button>
                  <el-radio-button value="center">中</el-radio-button>
                  <el-radio-button value="right">右</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="行高">
                <el-slider v-model="selectedElement.style.lineHeight" :min="1" :max="2.5" :step="0.1" show-stops />
              </el-form-item>
              <el-form-item label="字间距">
                <el-slider v-model="selectedElement.style.letterSpacing" :min="-1" :max="5" :step="0.5" show-stops />
              </el-form-item>
              <el-form-item label="文本阴影">
                <el-input v-model="selectedElement.style.textShadow" placeholder="如 1px 1px 2px rgba(0,0,0,0.3)" />
              </el-form-item>
            </template>

            <!-- 图片元素 -->
            <template v-else-if="selectedElement.type === 'image'">
              <el-form-item label="图片上传">
                <el-upload :show-file-list="false" :http-request="(opt: any) => uploadElementImage(opt, selectedElement!)" accept="image/*">
                  <el-button size="small">选择图片</el-button>
                </el-upload>
              </el-form-item>
              <el-form-item label="图片URL">
                <el-input v-model="selectedElement.src" placeholder="或输入URL" />
              </el-form-item>
              <el-form-item label="宽度">
                <el-input v-model="selectedElement.style.width" placeholder="如 100% 或 200px" />
              </el-form-item>
              <el-form-item label="高度">
                <el-input v-model="selectedElement.style.height" placeholder="如 auto 或 150px" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedElement.style.borderRadius" :min="0" :max="30" controls-position="right" />
              </el-form-item>
              <el-form-item label="外边距">
                <el-input v-model="selectedElement.style.margin" placeholder="如 10px 0" />
              </el-form-item>
              <el-form-item label="滤镜">
                <el-select v-model="selectedElement.style.filter" style="width:100%" clearable placeholder="无">
                  <el-option label="无" value="" />
                  <el-option label="灰度" value="grayscale(100%)" />
                  <el-option label="模糊" value="blur(2px)" />
                  <el-option label="复古" value="sepia(60%)" />
                  <el-option label="高对比" value="contrast(120%)" />
                  <el-option label="亮度" value="brightness(110%)" />
                </el-select>
              </el-form-item>
            </template>

            <!-- 视频元素 -->
            <template v-else-if="selectedElement.type === 'video'">
              <el-form-item label="视频上传">
                <el-upload :show-file-list="false" :http-request="(opt: any) => uploadElementImage(opt, selectedElement!, 'src')" accept="video/*">
                  <el-button size="small">选择视频</el-button>
                </el-upload>
              </el-form-item>
              <el-form-item label="视频URL">
                <el-input v-model="selectedElement.src" placeholder="或输入视频链接" />
              </el-form-item>
              <el-form-item label="封面图">
                <el-upload :show-file-list="false" :http-request="(opt: any) => uploadElementImage(opt, selectedElement!, 'poster')" accept="image/*" style="margin-bottom:4px">
                  <el-button size="small">选择封面图</el-button>
                </el-upload>
                <el-input v-model="selectedElement.poster" placeholder="或输入封面图URL" />
              </el-form-item>
            </template>

            <!-- 按钮元素 -->
            <template v-else-if="selectedElement.type === 'button'">
              <el-form-item label="按钮文字">
                <el-input v-model="selectedElement.label" />
              </el-form-item>
              <el-form-item label="图标">
                <el-select v-model="selectedElement.icon" style="width:100%" clearable placeholder="无">
                  <el-option label="无" value="" />
                  <el-option label="链接" value="Link" />
                  <el-option label="电话" value="Phone" />
                  <el-option label="箭头" value="ArrowRight" />
                  <el-option label="搜索" value="Search" />
                  <el-option label="主页" value="House" />
                  <el-option label="地图" value="MapLocation" />
                  <el-option label="认证" value="Medal" />
                  <el-option label="安全" value="Lock" />
                </el-select>
              </el-form-item>
              <el-form-item label="动画">
                <el-select v-model="selectedElement.animation" style="width:100%" clearable placeholder="无">
                  <el-option label="无" value="" />
                  <el-option label="脉冲" value="pulse" />
                  <el-option label="闪光" value="shine" />
                  <el-option label="弹跳" value="bounce" />
                </el-select>
              </el-form-item>
              <el-form-item label="按钮类型">
                <el-select v-model="selectedElement.buttonType" style="width:100%">
                  <el-option label="链接跳转" value="link" />
                  <el-option label="页面跳转" value="page" />
                  <el-option label="电话拨打" value="phone" />
                </el-select>
              </el-form-item>
              <el-form-item label="跳转链接" v-if="selectedElement.buttonType === 'link'">
                <el-input v-model="selectedElement.link" placeholder="https://..." />
              </el-form-item>
              <el-form-item label="目标页面" v-if="selectedElement.buttonType === 'page'">
                <el-select v-model="selectedElement.targetPageId" style="width:100%" placeholder="选择目标页面">
                  <el-option v-for="(p, i) in form.pages" :key="p.id" :label="p.name" :value="p.id" :disabled="p.id === currentPage.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="电话号码" v-if="selectedElement.buttonType === 'phone'">
                <el-input v-model="selectedElement.phone" placeholder="电话号码" />
              </el-form-item>
              <el-form-item label="按钮图片">
                <el-upload :show-file-list="false" :http-request="(opt: any) => uploadElementImage(opt, selectedElement!, 'bgImage')" accept="image/*">
                  <el-button size="small">选择图片</el-button>
                </el-upload>
                <el-input v-model="selectedElement.bgImage" placeholder="或输入图片URL" style="margin-top:4px" />
              </el-form-item>
              <el-form-item label="图片尺寸" v-if="selectedElement.bgImage">
                <el-radio-group v-model="selectedElement.imageFit" size="small">
                  <el-radio-button value="cover">填满</el-radio-button>
                  <el-radio-button value="contain">适应</el-radio-button>
                  <el-radio-button value="custom">自定义</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="图片宽度" v-if="selectedElement.bgImage && selectedElement.imageFit === 'custom'">
                <el-input v-model="selectedElement.imageWidth" placeholder="如 80% 或 120px" />
              </el-form-item>
              <el-form-item label="图片高度" v-if="selectedElement.bgImage && selectedElement.imageFit === 'custom'">
                <el-input v-model="selectedElement.imageHeight" placeholder="如 100% 或 120px" />
              </el-form-item>
              <el-form-item label="宽度">
                <div class="width-presets">
                  <el-button size="small" :type="selectedElement.style.width === '100%' ? 'primary' : 'default'" @click="selectedElement.style.width = '100%'">整行</el-button>
                  <el-button size="small" :type="selectedElement.style.width === '50%' ? 'primary' : 'default'" @click="selectedElement.style.width = '50%'">二等分</el-button>
                  <el-button size="small" :type="selectedElement.style.width === '33%' ? 'primary' : 'default'" @click="selectedElement.style.width = '33%'">三等分</el-button>
                </div>
                <el-input v-model="selectedElement.style.width" placeholder="或自定义 如 200px" style="margin-top:4px" />
              </el-form-item>
              <el-form-item label="高度">
                <el-input v-model="selectedElement.style.height" placeholder="如 auto 或 60px" />
              </el-form-item>
              <el-form-item label="字号">
                <el-input-number v-model="selectedElement.style.fontSize" :min="10" :max="36" controls-position="right" />
              </el-form-item>
              <el-form-item label="背景色">
                <el-color-picker v-model="selectedElement.style.backgroundColor" />
              </el-form-item>
              <el-form-item label="文字色">
                <el-color-picker v-model="selectedElement.style.color" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedElement.style.borderRadius" :min="0" :max="30" controls-position="right" />
              </el-form-item>
              <el-form-item label="外边距">
                <el-input v-model="selectedElement.style.margin" placeholder="如 10px 0" />
              </el-form-item>
            </template>

            <!-- 自定义字段 -->
            <template v-else-if="selectedElement.type === 'custom-field'">
              <el-form-item label="字段标识">
                <el-input v-model="selectedElement.fieldKey" placeholder="如 customField1" />
              </el-form-item>
              <el-form-item label="字段类型">
                <el-select v-model="selectedElement.fieldType" style="width:100%">
                  <el-option label="文本" value="text" />
                  <el-option label="图片" value="image" />
                  <el-option label="文件" value="file" />
                  <el-option label="数字" value="number" />
                </el-select>
              </el-form-item>
              <el-form-item label="必填">
                <el-switch v-model="selectedElement.required" />
              </el-form-item>
            </template>

            <!-- 分割线 -->
            <template v-else-if="selectedElement.type === 'divider'">
              <el-form-item label="颜色">
                <el-color-picker v-model="selectedElement.style.borderColor" />
              </el-form-item>
              <el-form-item label="外边距">
                <el-input v-model="selectedElement.style.margin" placeholder="如 10px 0" />
              </el-form-item>
            </template>

            <!-- 富文本 -->
            <template v-else-if="selectedElement.type === 'rich-text'">
              <el-form-item label="富文本内容">
                <RichTextEditor v-model="selectedElement.content" :upload-image="uploadRichImage" />
              </el-form-item>
              <el-form-item label="背景色">
                <el-color-picker v-model="selectedElement.style.backgroundColor" />
              </el-form-item>
              <el-form-item label="内边距">
                <el-input v-model="selectedElement.style.padding" placeholder="如 12px" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedElement.style.borderRadius" :min="0" :max="30" controls-position="right" />
              </el-form-item>
            </template>

            <!-- 信息模块样式 -->
            <template v-else-if="isInfoSection(selectedElement.type)">
              <el-form-item label="显示字段">
                <el-checkbox-group v-model="selectedElement.selectedFields">
                  <el-checkbox v-for="f in getSectionAllFields(selectedElement.type)" :key="f.field" :label="f.field" class="field-cb">
                    {{ f.label }}
                    <el-tag v-if="f.type" size="small" type="info" style="margin-left:2px">{{ f.type }}</el-tag>
                  </el-checkbox>
                </el-checkbox-group>
              </el-form-item>
              <el-form-item label="卡片样式">
                <el-radio-group v-model="selectedElement.style.cardStyle" size="small">
                  <el-radio-button value="flat">扁平</el-radio-button>
                  <el-radio-button value="elevated">浮起</el-radio-button>
                  <el-radio-button value="glass">玻璃</el-radio-button>
                  <el-radio-button value="bordered">描边</el-radio-button>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="背景色">
                <el-color-picker v-model="selectedElement.style.backgroundColor" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedElement.style.borderRadius" :min="0" :max="20" controls-position="right" />
              </el-form-item>
              <el-form-item label="内边距">
                <el-input v-model="selectedElement.style.padding" placeholder="如 12px" />
              </el-form-item>
              <el-form-item label="阴影">
                <el-input v-model="selectedElement.style.boxShadow" placeholder="如 0 4px 12px rgba(0,0,0,0.1)" />
              </el-form-item>
            </template>

            <!-- 地图元素 -->
            <template v-else-if="selectedElement.type === 'map'">
              <el-form-item label="API Key">
                <el-input v-model="selectedElement.mapKey" placeholder="腾讯地图 API Key" />
                <div style="font-size:11px;color:#999;margin-top:2px">在 <a href="https://lbs.qq.com/" target="_blank">lbs.qq.com</a> 申请</div>
              </el-form-item>
              <el-form-item label="中心坐标">
                <el-input v-model="selectedElement.center" placeholder="经度,纬度 如 104.0657,30.6595" />
              </el-form-item>
              <el-form-item label="缩放级别">
                <el-input-number v-model="selectedElement.zoom" :min="3" :max="18" controls-position="right" />
              </el-form-item>
              <el-form-item label="宽度">
                <el-input v-model="selectedElement.style.width" placeholder="如 100% 或 300px" />
              </el-form-item>
              <el-form-item label="高度">
                <el-input v-model="selectedElement.style.height" placeholder="如 200px" />
              </el-form-item>
              <el-form-item label="圆角">
                <el-input-number v-model="selectedElement.style.borderRadius" :min="0" :max="20" controls-position="right" />
              </el-form-item>
              <el-form-item label="标记点">
                <div v-for="(m, mi) in (selectedElement.markers || [])" :key="mi" class="marker-item">
                  <el-input v-model="m.label" placeholder="名称" size="small" style="width:80px" />
                  <el-input v-model="m.lat" placeholder="纬度" size="small" style="width:70px" />
                  <el-input v-model="m.lng" placeholder="经度" size="small" style="width:70px" />
                  <el-button size="small" circle type="danger" @click="selectedElement.markers.splice(mi, 1)"><el-icon><Delete /></el-icon></el-button>
                </div>
                <el-button size="small" @click="(selectedElement.markers = selectedElement.markers || []).push({ label: '', lat: 0, lng: 0 })">+ 添加标记</el-button>
              </el-form-item>
            </template>
          </el-form>
        </div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import Sortable from 'sortablejs'
import { ElMessage } from 'element-plus'
import { createTraceTemplate, updateTraceTemplate } from '@/api/admin'
import { uploadFile } from '@/api/common'
import { SECTION_FIELDS, isInfoSection, getSectionAllFields } from '@/constants/section-fields'
import { LAYOUT_PRESETS, type LayoutType, createPagesByLayout, getDefaultLayout } from '@/constants/layout-presets'
import TemplateGallery from './TemplateGallery.vue'
import RichTextEditor from '@/components/RichTextEditor.vue'
import {
  Document, PictureFilled, VideoCamera, OfficeBuilding, Goods, Medal, Tickets, Location, Link, Edit, Grid, Minus, Reading, Lock,
  Notebook, House, Van, MapLocation, Crop
} from '@element-plus/icons-vue'

const props = defineProps<{ modelValue: boolean; templateData: any }>()
const emit = defineEmits<{ 'update:modelValue': [val: boolean]; saved: [] }>()

const saving = ref(false)
const galleryVisible = ref(false)
let idCounter = 0
const genId = (prefix: string) => `${prefix}_${++idCounter}_${Date.now()}`

// ==================== 主题 ====================
const themes = [
  {
    key: 'standard-green', label: '标准绿',
    primaryColor: '#059669', gradient: null,
    cssVars: {
      '--trace-bg': '#f5f5f5',
      '--trace-accent': '#059669',
      '--trace-accent-light': '#10b981',
      '--trace-accent-dark': '#047857',
      '--trace-section-bg': '#ffffff',
      '--trace-section-shadow': '0 2px 8px rgba(0, 0, 0, 0.06)',
      '--trace-text-primary': '#333333',
      '--trace-text-secondary': '#666666',
      '--trace-text-muted': '#999999',
      '--trace-border-color': '#f0f0f0',
    }
  },
  {
    key: 'tech-blue', label: '科技蓝',
    primaryColor: '#1e40af', gradient: 'linear-gradient(135deg, #1e40af 0%, #3b82f6 100%)',
    cssVars: {
      '--trace-bg': '#eef2ff',
      '--trace-accent': '#3b82f6',
      '--trace-accent-light': '#60a5fa',
      '--trace-accent-dark': '#1e40af',
      '--trace-section-bg': '#ffffff',
      '--trace-section-shadow': '0 2px 12px rgba(30, 64, 175, 0.08)',
      '--trace-text-primary': '#1e40af',
      '--trace-text-secondary': '#475569',
      '--trace-text-muted': '#64748b',
      '--trace-border-color': '#dbeafe',
    }
  },
  {
    key: 'premium-gold', label: '品质金',
    primaryColor: '#92400e', gradient: 'linear-gradient(135deg, #92400e 0%, #b45309 50%, #d97706 100%)',
    cssVars: {
      '--trace-bg': '#fefcf3',
      '--trace-accent': '#d97706',
      '--trace-accent-light': '#f59e0b',
      '--trace-accent-dark': '#92400e',
      '--trace-section-bg': '#fffdf7',
      '--trace-section-shadow': '0 2px 8px rgba(146, 64, 14, 0.06)',
      '--trace-text-primary': '#92400e',
      '--trace-text-secondary': '#785c38',
      '--trace-text-muted': '#a08050',
      '--trace-border-color': '#f0e6d2',
    }
  },
]

const currentThemeStyle = computed(() => {
  const t = themes.find(t => t.key === form.value.themeKey)
  return t?.gradient || t?.primaryColor || '#059669'
})

function getSectionPreviewFields(el: any) {
  const all = getSectionAllFields(el.type)
  const selected = el.selectedFields || all.slice(0, 3).map((f: any) => f.field)
  return selected.slice(0, 4).map((sf: string) => all.find((f: any) => f.field === sf)?.label || sf)
}

// ==================== 元素面板 ====================
const elementPalette = [
  { label: '基础元素', items: [
    { type: 'text', label: '文本', icon: 'Document' },
    { type: 'rich-text', label: '富文本', icon: 'Reading' },
    { type: 'image', label: '图片', icon: 'PictureFilled' },
    { type: 'video', label: '视频', icon: 'VideoCamera' },
    { type: 'divider', label: '分割线', icon: 'Minus' },
    { type: 'button', label: '按钮', icon: 'Link' },
    { type: 'custom-field', label: '自定义字段', icon: 'Edit' },
    { type: 'anti-counterfeit', label: '防伪验证', icon: 'Lock' },
  ]},
  { label: '信息模块', items: [
    { type: 'enterprise-info', label: '企业信息', icon: 'OfficeBuilding' },
    { type: 'product-info', label: '产品信息', icon: 'Goods' },
    { type: 'cert-info', label: '认证信息', icon: 'Medal' },
    { type: 'test-info', label: '检测信息', icon: 'Tickets' },
    { type: 'base-info', label: '基地信息', icon: 'Location' },
  ]},
  { label: '扩展元素', items: [
    { type: 'map', label: '地图', icon: 'MapLocation' },
    { type: 'video-monitor', label: '视频监控', icon: 'VideoCamera' },
    { type: 'iot-environment', label: '环境监测', icon: 'Monitor' },
    { type: 'vr-panorama', label: 'VR全景导览', icon: 'View' },
  ]},
]

// ==================== 数据模型 ====================
interface PageData {
  id: string
  name: string
  elements: ElementData[]
}
interface ElementData {
  id: string
  type: string
  label: string
  [key: string]: any
}

const form = ref({
  templateName: '',
  templateType: '',
  themeKey: 'standard-green',
  layout: getDefaultLayout() as LayoutType,
  backgroundImage: '',
  pageBackgroundColor: '',
  pageTransition: '',
  pages: [] as PageData[],
})

const currentPageIdx = ref(0)
const selectedId = ref<string | null>(null)
const renamingPageIdx = ref<number | null>(null)
const tabRenameRef = ref<HTMLInputElement[] | null>(null)
const phoneBodyRef = ref<HTMLElement | null>(null)
let sortableInstance: Sortable | null = null

function startRenamePage(idx: number) {
  renamingPageIdx.value = idx
  nextTick(() => {
    const inputs = tabRenameRef.value
    if (inputs && inputs.length > 0) {
      inputs[0].focus()
      inputs[0].select()
    }
  })
}

const currentPage = computed(() => form.value.pages[currentPageIdx.value] || { id: '', name: '', elements: [] })
const selectedElement = computed(() => {
  if (!selectedId.value) return null
  return currentPage.value.elements.find(e => e.id === selectedId.value) || null
})

const currentPhoneThemeStyle = computed(() => {
  const style: any = {}
  const theme = themes.find(t => t.key === form.value.themeKey)
  if (theme?.cssVars) {
    Object.assign(style, theme.cssVars)
  }
  if (form.value.backgroundImage) {
    style.backgroundImage = `url(${form.value.backgroundImage})`
    style.backgroundSize = 'cover'
    style.backgroundPosition = 'center top'
    style.backgroundRepeat = 'no-repeat'
  }
  return style
})

// ==================== 创建元素 ====================
function createElementData(type: string): ElementData {
  const id = genId('el')
  const base: ElementData = { id, type, label: '' }
  switch (type) {
    case 'text':
      return { ...base, label: '文本', content: '', style: { fontSize: 14, color: '#333', fontWeight: 'normal', textAlign: 'left' } }
    case 'rich-text':
      return { ...base, label: '富文本', content: '<p>请输入富文本内容</p>', style: { backgroundColor: '#fff', padding: '12px', borderRadius: 8 } }
    case 'image':
      return { ...base, label: '图片', src: '', style: { width: '100%', height: 'auto', borderRadius: 8, margin: '' } }
    case 'video':
      return { ...base, label: '视频', src: '', poster: '', style: {} }
    case 'divider':
      return { ...base, label: '分割线', style: { borderColor: '#e5e7eb', margin: '8px 0' } }
    case 'button':
      return { ...base, label: '按钮', buttonType: 'link', link: '', targetPageId: '', phone: '', bgImage: '', imageFit: 'cover', imageWidth: '', imageHeight: '', style: { backgroundColor: '#059669', color: '#fff', borderRadius: 20, fontSize: 14, width: '100%', height: 'auto', margin: '' } }
    case 'custom-field':
      return { ...base, label: '自定义字段', fieldKey: '', fieldType: 'text', required: false, style: {} }
    case 'anti-counterfeit':
      return { ...base, label: '防伪验证', style: {} }
    case 'map':
      return { ...base, label: '地图', mapKey: '', center: '104.0657,30.6595', zoom: 10, markers: [] as { label: string; lat: number; lng: number }[], style: { width: '100%', height: '200px', borderRadius: 8 } }
    default:
      if (isInfoSection(type)) {
        const sec = SECTION_FIELDS[type]
        return { ...base, label: sec.title, selectedFields: sec.fields.slice(0, 3).map(f => f.field), style: {} }
      }
      return { ...base, label: type, style: {} }
  }
}

// ==================== 操作函数 ====================
function addElement(type: string) {
  const el = createElementData(type)
  form.value.pages[currentPageIdx.value].elements.push(el)
  selectedId.value = el.id
}

function getButtonWidth(el: any): string {
  const w = el.style?.width
  if (!w || w === '100%') return '100%'
  if (w === '50%') return '49%'
  if (w === '33%') return '32.5%'
  return w
}

function buttonPreviewStyle(el: any) {
  const s: any = { ...(el.style || {}) }
  delete s.width  // 按钮预览始终100%填满容器，宽度由canvas-element控制
  if (el.bgImage) {
    s.backgroundImage = `url(${el.bgImage})`
    s.backgroundRepeat = 'no-repeat'
    s.overflow = 'hidden'
    if (el.imageFit === 'contain') {
      s.backgroundSize = 'contain'
      s.backgroundPosition = 'center'
    } else if (el.imageFit === 'custom') {
      const w = el.imageWidth || '100%'
      const h = el.imageHeight || '100%'
      s.backgroundSize = `${w} ${h}`
      s.backgroundPosition = 'center'
    } else {
      s.backgroundSize = 'cover'
      s.backgroundPosition = 'center'
    }
  }
  return s
}

function selectElement(el: ElementData) {
  selectedId.value = selectedId.value === el.id ? null : el.id
}

function moveElement(idx: number, dir: number) {
  const arr = currentPage.value.elements
  const target = idx + dir
  if (target < 0 || target >= arr.length) return
  const temp = arr[idx]
  arr[idx] = arr[target]
  arr[target] = temp
  // trigger reactivity
  form.value.pages[currentPageIdx.value].elements = [...arr]
}

function removeElement(idx: number) {
  const el = currentPage.value.elements[idx]
  if (selectedId.value === el.id) selectedId.value = null
  form.value.pages[currentPageIdx.value].elements.splice(idx, 1)
}

function addPage() {
  const page: PageData = { id: genId('page'), name: `副页${form.value.pages.length}`, elements: [] }
  form.value.pages.push(page)
  currentPageIdx.value = form.value.pages.length - 1
  selectedId.value = null
}

function removePage(idx: number) {
  if (form.value.pages.length <= 1) return
  form.value.pages.splice(idx, 1)
  if (currentPageIdx.value >= form.value.pages.length) {
    currentPageIdx.value = form.value.pages.length - 1
  }
  selectedId.value = null
}

function onLayoutChange(layout: LayoutType) {
  if (layout === 'free') return
  ElMessage.warning('应用布局将覆盖当前页面元素，请确认后重新选择')
  form.value.pages = createPagesByLayout(layout, genId)
  currentPageIdx.value = 0
  selectedId.value = null
  initSortable()
}

function initSortable() {
  nextTick(() => {
    if (!phoneBodyRef.value) return
    if (sortableInstance) {
      sortableInstance.destroy()
      sortableInstance = null
    }
    sortableInstance = new Sortable(phoneBodyRef.value, {
      animation: 200,
      handle: '.canvas-element',
      ghostClass: 'sortable-ghost',
      chosenClass: 'sortable-chosen',
      dragClass: 'sortable-drag',
      touchStartThreshold: 3,
      onEnd: (evt: Sortable.SortableEvent) => {
        const page = form.value.pages[currentPageIdx.value]
        if (!page || evt.oldIndex === undefined || evt.newIndex === undefined) return
        const elements = page.elements
        const [moved] = elements.splice(evt.oldIndex, 1)
        elements.splice(evt.newIndex, 0, moved)
        selectedId.value = null
      },
    })
  })
}

onMounted(() => {
  initSortable()
})

watch(currentPageIdx, () => {
  initSortable()
})

onUnmounted(() => {
  if (sortableInstance) {
    sortableInstance.destroy()
    sortableInstance = null
  }
})

function applyGalleryItem(item: any) {
  form.value.layout = item.layout
  form.value.themeKey = item.themeKey
  form.value.pages = createPagesByLayout(item.layout, genId)
  currentPageIdx.value = 0
  selectedId.value = null
  ElMessage.success(`已应用模板：${item.title}`)
}

// ==================== 上传 ====================
async function uploadBackground(options: any) {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    form.value.backgroundImage = url
    ElMessage.success('背景图上传成功')
  } catch (e) { ElMessage.error('上传失败') }
}

async function uploadElementImage(options: any, el: ElementData, field: string = 'src') {
  try {
    const res = await uploadFile(options.file)
    const url = res.data?.url || res.data || ''
    el[field] = url
    ElMessage.success('图片上传成功')
  } catch (e) { ElMessage.error('上传失败') }
}

// ==================== 富文本图片上传 ====================
async function uploadRichImage(file: File): Promise<string> {
  try {
    const res = await uploadFile(file)
    const url = res.data?.url || res.data || ''
    ElMessage.success('图片插入成功')
    return url
  } catch (e) {
    ElMessage.error('图片上传失败')
    throw e
  }
}

// ==================== 序列化 ====================
function buildConfigJson() {
  const theme = themes.find(t => t.key === form.value.themeKey)
  const pages = form.value.pages.map(p => ({
    id: p.id,
    name: p.name,
    elements: p.elements.map(el => {
      const base: any = { id: el.id, type: el.type, label: el.label }
      if (el.type === 'text') {
        base.content = el.content
        base.style = { ...el.style }
      } else if (el.type === 'rich-text') {
        base.content = el.content
        base.style = { ...el.style }
      } else if (el.type === 'image') {
        base.src = el.src
        base.style = { ...el.style }
      } else if (el.type === 'video') {
        base.src = el.src
        base.poster = el.poster
      } else if (el.type === 'button') {
        base.buttonType = el.buttonType
        base.link = el.link
        base.targetPageId = el.targetPageId
        base.phone = el.phone
        base.bgImage = el.bgImage
        base.imageFit = el.imageFit || 'cover'
        base.imageWidth = el.imageWidth || ''
        base.imageHeight = el.imageHeight || ''
        base.style = { ...el.style }
      } else if (el.type === 'custom-field') {
        base.fieldKey = el.fieldKey
        base.fieldType = el.fieldType
        base.required = el.required
      } else if (el.type === 'anti-counterfeit') {
        // no extra fields
      } else if (isInfoSection(el.type)) {
        base.selectedFields = el.selectedFields
        base.style = { ...el.style }
      } else if (el.type === 'map') {
        base.mapKey = el.mapKey
        base.center = el.center
        base.zoom = el.zoom
        base.markers = el.markers
        base.style = { ...el.style }
      }
      return base
    }),
  }))

  const config: any = {
    theme: { key: theme?.key || 'standard-green' },
    layout: form.value.layout || 'free',
    pages,
  }
  if (theme?.primaryColor) config.theme.primaryColor = theme.primaryColor
  if (theme?.gradient) config.theme.gradient = theme.gradient
  if (theme?.cssVars) config.theme.cssVars = { ...theme.cssVars }
  if (form.value.backgroundImage) config.backgroundImage = form.value.backgroundImage
  if (form.value.pageBackgroundColor) config.pageBackgroundColor = form.value.pageBackgroundColor
  if (form.value.pageTransition) config.pageTransition = form.value.pageTransition

  // 兼容旧格式：提取 sections/buttons/customFields
  const sections: any[] = []
  const buttons: any[] = []
  const customFields: any[] = []
  for (const page of form.value.pages) {
    for (const el of page.elements) {
      if (isInfoSection(el.type)) {
        const secDef = SECTION_FIELDS[el.type]
        sections.push({
          key: el.type.replace('-info', ''),
          title: el.label || secDef.title,
          icon: '',
          fields: secDef.fields.filter(f => el.selectedFields?.includes(f.field)).map(f => ({ field: f.field, label: f.label, ...(f.type ? { type: f.type } : {}) })),
        })
      } else if (el.type === 'button') {
        buttons.push({ id: el.id, label: el.label, type: 'icon', icon: 'Link', link: el.link, style: el.style })
      } else if (el.type === 'custom-field') {
        customFields.push({ key: el.fieldKey, label: el.label, type: el.fieldType, required: el.required })
      }
    }
  }
  if (sections.length) config.sections = sections
  if (buttons.length) config.buttons = buttons
  if (customFields.length) config.customFields = customFields

  return JSON.stringify(config)
}

// ==================== 反序列化（从保存的数据加载） ====================
function loadFromConfig(config: any, templateData: any) {
  form.value.templateName = templateData?.templateName || ''
  form.value.templateType = templateData?.templateType || ''
  form.value.themeKey = config?.theme?.key || 'standard-green'
  form.value.layout = config?.layout || 'free'
  form.value.backgroundImage = templateData?.backgroundImage || config?.backgroundImage || ''
  form.value.pageBackgroundColor = config?.pageBackgroundColor || ''
  form.value.pageTransition = config?.pageTransition || ''

  if (config?.pages && config.pages.length > 0) {
    // 从新版多页面格式加载
    form.value.pages = config.pages.map((p: any) => ({
      id: p.id || genId('page'),
      name: p.name || '首页',
      elements: (p.elements || []).map((el: any) => {
        const base = createElementData(el.type)
        Object.assign(base, { id: el.id || base.id, label: el.label || base.label })
        if (el.type === 'text') { base.content = el.content; base.style = el.style || base.style }
        else if (el.type === 'rich-text') { base.content = el.content || ''; base.style = el.style || base.style }
        else if (el.type === 'image') { base.src = el.src; base.style = el.style || base.style }
        else if (el.type === 'video') { base.src = el.src; base.poster = el.poster }
        else if (el.type === 'button') { base.buttonType = el.buttonType; base.link = el.link; base.targetPageId = el.targetPageId; base.phone = el.phone; base.bgImage = el.bgImage || ''; base.imageFit = el.imageFit || 'cover'; base.imageWidth = el.imageWidth || ''; base.imageHeight = el.imageHeight || ''; base.style = el.style || base.style }
        else if (el.type === 'custom-field') { base.fieldKey = el.fieldKey; base.fieldType = el.fieldType; base.required = el.required }
        else if (isInfoSection(el.type)) { base.selectedFields = el.selectedFields || base.selectedFields; base.style = el.style || base.style }
        else if (el.type === 'map') { base.mapKey = el.mapKey || ''; base.center = el.center || base.center; base.zoom = el.zoom || base.zoom; base.markers = el.markers || []; base.style = el.style || base.style }
        return base
      }),
    }))
  } else if (config?.sections) {
    // 从旧版格式迁移
    const elements: ElementData[] = []
    for (const sec of config.sections) {
      const type = sec.key + '-info'
      if (SECTION_FIELDS[type] || SECTION_FIELDS[sec.key]) {
        const elType = SECTION_FIELDS[type] ? type : sec.key
        const el = createElementData(elType)
        el.label = sec.title || el.label
        el.selectedFields = sec.fields?.map((f: any) => f.field) || el.selectedFields
        elements.push(el)
      }
    }
    // 迁移按钮
    if (config.buttons) {
      for (const btn of config.buttons) {
        const el = createElementData('button')
        el.label = btn.label || el.label
        el.link = btn.link || ''
        el.style = { ...el.style, ...(btn.style || {}) }
        elements.push(el)
      }
    }
    // 迁移自定义字段
    if (config.customFields) {
      for (const cf of config.customFields) {
        const el = createElementData('custom-field')
        el.label = cf.label || ''
        el.fieldKey = cf.key || ''
        el.fieldType = cf.type || 'text'
        el.required = cf.required || false
        elements.push(el)
      }
    }
    form.value.pages = [{ id: genId('page'), name: '首页', elements }]
  } else {
    // 空模板
    form.value.pages = [{ id: genId('page'), name: '首页', elements: [] }]
  }

  currentPageIdx.value = 0
  selectedId.value = null
}

// ==================== 预设模板 ====================
function loadPreset(key: string) {
  if (key === 'blank') {
    form.value = { templateName: '', templateType: '', themeKey: 'standard-green', layout: 'free', backgroundImage: '', pageBackgroundColor: '', pageTransition: '', pages: [{ id: genId('page'), name: '首页', elements: [] }] }
    currentPageIdx.value = 0; selectedId.value = null
    return
  }
  if (key === 'pig-trace') {
    form.value.templateName = '生猪溯源模板'
    form.value.templateType = '养殖溯源'
    form.value.themeKey = 'premium-gold'
    form.value.backgroundImage = ''

    // 首页：产品主图 + 企业信息 + 内容导航
    const page0Id = genId('page')
    const page1Id = genId('page')
    const page2Id = genId('page')
    const page3Id = genId('page')

    const heroImg = createElementData('image')
    heroImg.label = '产品主图'; heroImg.style = { ...heroImg.style, height: '180px', borderRadius: 0 }

    const titleText = createElementData('text')
    titleText.content = '大河乌猪认证报告'
    titleText.style = { fontSize: 20, color: '#8B2500', fontWeight: 'bold', textAlign: 'center' }

    const subtitleText = createElementData('text')
    subtitleText.content = '后腿肉'
    subtitleText.style = { fontSize: 16, color: '#666', fontWeight: 'normal', textAlign: 'center' }

    const tagsText = createElementData('text')
    tagsText.content = '大河乌猪 | 2025-12-25生产 | 1KG'
    tagsText.style = { fontSize: 12, color: '#999', fontWeight: 'normal', textAlign: 'center' }

    const enterpriseInfo = createElementData('enterprise-info')
    enterpriseInfo.selectedFields = SECTION_FIELDS['enterprise-info'].fields.map(f => f.field)

    const divider1 = createElementData('divider')

    const stageTitle = createElementData('text')
    stageTitle.content = '选择查看内容'
    stageTitle.style = { fontSize: 14, color: '#333', fontWeight: 'bold', textAlign: 'left' }

    const btnTest = createElementData('button')
    btnTest.label = '检测报告'; btnTest.buttonType = 'page'; btnTest.targetPageId = page1Id
    btnTest.style = { ...btnTest.style, backgroundColor: '#8B4513', width: '100%', borderRadius: 8 }

    const btnCert = createElementData('button')
    btnCert.label = '认证信息'; btnCert.buttonType = 'page'; btnCert.targetPageId = page2Id
    btnCert.style = { ...btnCert.style, backgroundColor: '#A0522D', width: '100%', borderRadius: 8 }

    const btnMap = createElementData('button')
    btnMap.label = '位置地图'; btnMap.buttonType = 'page'; btnMap.targetPageId = page3Id
    btnMap.style = { ...btnMap.style, backgroundColor: '#8B2500', width: '100%', borderRadius: 8 }

    // 检测报告页
    const testInfo = createElementData('test-info')
    testInfo.selectedFields = SECTION_FIELDS['test-info'].fields.map(f => f.field)

    const productInfo = createElementData('product-info')
    productInfo.selectedFields = SECTION_FIELDS['product-info'].fields.map(f => f.field)

    // 认证信息页
    const certInfo = createElementData('cert-info')
    certInfo.selectedFields = SECTION_FIELDS['cert-info'].fields.map(f => f.field)

    // 地图页
    const mapEl = createElementData('map')
    mapEl.mapKey = ''
    mapEl.center = '104.2609,25.6745' // 富源县坐标
    mapEl.zoom = 11
    mapEl.markers = [
      { label: '养殖场', lat: 25.6745, lng: 104.2609 },
      { label: '屠宰场', lat: 25.6892, lng: 104.2488 },
    ]

    form.value.pages = [
      { id: page0Id, name: '认证报告', elements: [heroImg, titleText, subtitleText, tagsText, enterpriseInfo, divider1, stageTitle, btnTest, btnCert, btnMap] },
      { id: page1Id, name: '检测报告', elements: [testInfo, productInfo] },
      { id: page2Id, name: '认证信息', elements: [certInfo] },
      { id: page3Id, name: '位置地图', elements: [mapEl] },
    ]
    currentPageIdx.value = 0
    selectedId.value = null
    ElMessage.success('已加载生猪溯源预设模板')
  }
}

// 监听 templateData 变化
watch(() => props.templateData, (val) => {
  if (val) {
    let config: any = null
    try { config = typeof val.configJson === 'string' ? JSON.parse(val.configJson) : val.configJson } catch {}
    loadFromConfig(config, val)
  } else {
    form.value = { templateName: '', templateType: '', themeKey: 'standard-green', layout: 'free', backgroundImage: '', pageBackgroundColor: '', pageTransition: '', pages: [{ id: genId('page'), name: '首页', elements: [] }] }
    currentPageIdx.value = 0
    selectedId.value = null
  }
}, { immediate: true })

// ==================== 保存 ====================
async function handleSave() {
  if (!form.value.templateName) return ElMessage.warning('请输入模板名称')
  saving.value = true
  try {
    const data = {
      templateName: form.value.templateName,
      templateType: form.value.templateType,
      backgroundImage: form.value.backgroundImage || null,
      configJson: buildConfigJson(),
    }
    if (props.templateData?.id) {
      await updateTraceTemplate(props.templateData.id, data)
    } else {
      await createTraceTemplate(data)
    }
    ElMessage.success('保存成功')
    emit('saved')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped lang="scss">
.top-bar {
  padding: 8px 0 12px;
  border-bottom: 1px solid #eee;
  margin-bottom: 10px;
  :deep(.el-form-item) { margin-bottom: 0; }
}

.page-tabs {
  display: flex;
  gap: 4px;
  padding: 6px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
  overflow-x: auto;

  .page-tab {
    padding: 6px 16px;
    border-radius: 6px 6px 0 0;
    cursor: pointer;
    font-size: 13px;
    color: #666;
    background: #f5f5f5;
    border: 1px solid #e5e7eb;
    border-bottom: none;
    display: flex;
    align-items: center;
    gap: 6px;
    white-space: nowrap;
    transition: all 0.2s;

    &.active { background: #fff; color: #059669; font-weight: 600; border-color: #059669; }
    &:hover:not(.active) { background: #ecfdf5; }

    .tab-close { font-size: 12px; color: #999; &:hover { color: #f56c6c; } }
  }

  .add-tab { color: #059669; border-style: dashed; background: transparent; }
}

.editor-main {
  display: flex;
  gap: 12px;
  height: 60vh;
  overflow: hidden;
}

// 左侧元素面板
.palette {
  width: 180px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px;

  .palette-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: #333; }
  .palette-group { margin-bottom: 12px; }
  .group-label { font-size: 11px; color: #999; margin-bottom: 6px; text-transform: uppercase; }
  .palette-items { display: flex; flex-direction: column; gap: 3px; }
  .palette-item {
    display: flex; align-items: center; gap: 6px; padding: 6px 8px; border-radius: 6px;
    cursor: pointer; font-size: 12px; color: #555; transition: all 0.15s;
    &:hover { background: #e8f5e9; color: #059669; }
  }
}

// 中间画布
.canvas-area {
  flex: 0 1 380px;
  display: flex;
  justify-content: center;
  overflow-y: auto;
  padding: 0 8px;
}

.phone-frame {
  width: 340px;
  min-height: 500px;
  max-height: 85vh;
  border: 2px solid #333;
  border-radius: 24px;
  overflow: hidden;
  background: var(--trace-bg, #f5f5f5);
  display: flex;
  flex-direction: column;
}

.serial-badge {
  position: absolute; top: 8px; right: 10px; z-index: 5;
  background: rgba(0,0,0,0.45); color: #fff; font-size: 10px;
  padding: 3px 8px; border-radius: 10px; white-space: nowrap;
}

.phone-body { padding: 8px; min-height: 300px; display: flex; flex-wrap: wrap; gap: 4px; align-content: flex-start; overflow-y: auto; flex: 1; }
.phone-body > .canvas-element { width: 100%; box-sizing: border-box; }

.empty-hint {
  text-align: center;
  color: #bbb;
  padding: 60px 20px;
  font-size: 13px;
}

.canvas-element {
  position: relative;
  margin-bottom: 6px;
  border: 2px solid transparent;
  border-radius: 8px;
  padding: 6px;
  cursor: move;
  transition: all 0.15s;
  background: var(--trace-section-bg, #fff);
  overflow: hidden;

  &:hover { border-color: var(--trace-accent, #059669); opacity: 0.8; }
  &.selected { border-color: var(--trace-accent, #059669); box-shadow: 0 0 0 2px rgba(var(--trace-accent-rgb, 5, 150, 105), 0.15); }
  &.has-bg-btn { padding: 0; }
  &.sortable-ghost { opacity: 0.4; background: var(--trace-report-bg, #f0fdf4); }
  &.sortable-chosen { box-shadow: 0 4px 12px rgba(0,0,0,0.15); }
  &.sortable-drag { opacity: 0.9; }

  .element-actions {
    position: absolute;
    right: 4px;
    top: 4px;
    display: flex;
    gap: 2px;
    z-index: 10;
  }
}

.el-text { line-height: 1.5; word-break: break-all; }
.el-image { width: 100%; max-height: 160px; border-radius: 6px; }
.el-placeholder { text-align: center; color: #999; padding: 16px; background: #f9f9f9; border-radius: 6px; font-size: 12px; &.video { color: #666; } }
.btn-wrapper { width: 100%; }
.btn-label-above {
  text-align: center; font-weight: 600;
  margin-bottom: 6px; line-height: 1.3;
}
.el-button-preview {
  text-align: center; padding: 10px 16px; border-radius: 20px;
  background: var(--trace-btn-bg, #059669); color: var(--trace-btn-text, #fff); font-size: 13px;
  background-size: cover; background-position: center;
  display: flex; align-items: center; justify-content: center;
  width: 100%; box-sizing: border-box;
}
.width-presets { display: flex; gap: 4px; }
.width-presets .el-button { flex: 1; }
.el-rich-text-preview {
  line-height: 1.6; word-break: break-all; min-height: 30px;
  :deep(p) { margin: 4px 0; }
  :deep(img) { max-width: 100%; }
}

.bg-preview {
  width: 28px; height: 28px; border-radius: 4px; object-fit: cover;
  margin-left: 6px; vertical-align: middle; border: 1px solid #ddd;
}

.tab-rename-input {
  width: 80px; padding: 2px 6px; font-size: 13px; border: 1px solid #059669;
  border-radius: 4px; outline: none; background: #fff;
}

.info-section-preview {
  .section-title { font-size: 13px; font-weight: 600; color: var(--trace-text-primary, #333); margin-bottom: 6px; padding-bottom: 4px; border-bottom: 1px solid var(--trace-border-color, #f0f0f0); }
  .section-fields { display: flex; flex-wrap: wrap; gap: 4px; }
  .field-tag { font-size: 10px; padding: 2px 6px; background: var(--trace-report-bg, #f0fdf4); color: var(--trace-accent, #059669); border-radius: 4px; border: 1px solid var(--trace-border-color, #d1fae5); }
}

// 防伪验证预览
.el-anti-fake-preview {
  padding: 12px;
  .af-title { font-size: 13px; font-weight: 600; color: var(--trace-text-primary, #333); margin-bottom: 8px; }
  .af-input-row { display: flex; gap: 6px; align-items: center; }
  .af-input-mock { flex: 1; padding: 6px 10px; background: var(--trace-section-bg-alt, #f9f9f9); border: 1px solid var(--trace-border-color, #ddd); border-radius: 6px; font-size: 11px; color: var(--trace-text-muted, #999); }
  .af-btn-mock { padding: 6px 14px; background: var(--trace-btn-bg, #059669); color: var(--trace-btn-text, #fff); border-radius: 6px; font-size: 11px; font-weight: 600; }
}

// 地图预览
.el-map-preview {
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  gap: 4px; background: var(--trace-report-bg, #f0fdf4); border: 1px dashed var(--trace-accent, #059669); border-radius: 8px;
  padding: 16px; min-height: 80px;
  .map-label { font-size: 12px; color: var(--trace-accent, #059669); font-weight: 600; }
  .map-hint { font-size: 10px; color: var(--trace-text-muted, #999); }
}

.marker-item {
  display: flex; align-items: center; gap: 4px; margin-bottom: 4px;
}

// 右侧属性面板
.props-panel {
  width: 250px;
  flex-shrink: 0;
  overflow-y: auto;
  background: #fafafa;
  border-radius: 8px;
  padding: 10px;

  .props-title { font-size: 13px; font-weight: 600; margin-bottom: 10px; color: #333; }
  .props-empty { text-align: center; color: #bbb; padding: 40px 10px; font-size: 12px; }
  .props-form { :deep(.el-form-item) { margin-bottom: 10px; } }

  .field-cb { width: 100%; margin-right: 0 !important; font-size: 12px; }
}
</style>
