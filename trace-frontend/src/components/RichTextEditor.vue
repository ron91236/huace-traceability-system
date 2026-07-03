<template>
  <div class="rich-text-editor">
    <div class="rich-text-toolbar">
      <el-button
        size="small"
        :type="editor?.isActive('bold') ? 'primary' : 'default'"
        @click="editor?.chain().focus().toggleBold().run()"
      >
        <b>B</b>
      </el-button>
      <el-button
        size="small"
        :type="editor?.isActive('italic') ? 'primary' : 'default'"
        @click="editor?.chain().focus().toggleItalic().run()"
      >
        <i>I</i>
      </el-button>
      <el-button
        size="small"
        :type="editor?.isActive('underline') ? 'primary' : 'default'"
        @click="editor?.chain().focus().toggleUnderline().run()"
      >
        <u>U</u>
      </el-button>
      <el-divider direction="vertical" />
      <el-color-picker
        size="small"
        v-model="textColor"
        @change="applyColor"
      />
      <el-input-number
        size="small"
        v-model="fontSize"
        :min="10"
        :max="36"
        controls-position="right"
        style="width: 80px"
        @change="applyFontSize"
      />
      <el-divider direction="vertical" />
      <el-upload
        :show-file-list="false"
        :http-request="insertImage"
        accept="image/*"
        style="display: inline-block"
      >
        <el-button size="small" type="primary" plain>插入图片</el-button>
      </el-upload>
    </div>
    <editor-content :editor="editor" class="rich-text-content" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount } from 'vue'
import { useEditor, EditorContent } from '@tiptap/vue-3'
import StarterKit from '@tiptap/starter-kit'
import Image from '@tiptap/extension-image'
import Color from '@tiptap/extension-color'
import { TextStyle } from '@tiptap/extension-text-style'
import Underline from '@tiptap/extension-underline'

const props = defineProps<{
  modelValue: string
  uploadImage?: (file: File) => Promise<string>
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

const textColor = ref('#333333')
const fontSize = ref(14)

const editor = useEditor({
  content: props.modelValue || '<p>请输入富文本内容</p>',
  extensions: [
    StarterKit,
    Underline,
    TextStyle as any,
    Color,
    Image.configure({
      HTMLAttributes: {
        style: 'max-width:100%;height:auto;display:block;'
      }
    }) as any
  ],
  onUpdate: ({ editor: e }) => {
    emit('update:modelValue', e.getHTML())
  },
  editorProps: {
    attributes: {
      class: 'rich-text-prose'
    }
  }
})

watch(
  () => props.modelValue,
  (val) => {
    if (!editor.value) return
    const current = editor.value.getHTML()
    if (val !== current) {
      editor.value.commands.setContent(val || '<p>请输入富文本内容</p>', { emitUpdate: false })
    }
  }
)

function applyColor(color: string | null) {
  if (!color || !editor.value) return
  editor.value.chain().focus().setColor(color).run()
}

function applyFontSize(size: number | undefined) {
  if (!size || !editor.value) return
  editor.value.chain().focus().setMark('textStyle', { fontSize: `${size}px` }).run()
}

async function insertImage(options: any) {
  if (!props.uploadImage) {
    return
  }
  try {
    const url = await props.uploadImage(options.file)
    editor.value?.chain().focus().setImage({ src: url }).run()
  } catch (e) {
    // 上传失败由调用方处理
  }
}

onBeforeUnmount(() => {
  editor.value?.destroy()
})
</script>

<style scoped lang="scss">
.rich-text-editor {
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  overflow: hidden;
  background: #fff;
}
.rich-text-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-bottom: 1px solid var(--el-border-color);
  background: var(--el-fill-color-light);
  flex-wrap: wrap;
}
.rich-text-content {
  min-height: 120px;
  max-height: 300px;
  overflow-y: auto;
  padding: 10px;
}
.rich-text-content :deep(.rich-text-prose) {
  outline: none;
  min-height: 100px;
}
.rich-text-content :deep(.rich-text-prose p) {
  margin: 0 0 0.6em;
  line-height: 1.6;
}
.rich-text-content :deep(.rich-text-prose img) {
  max-width: 100%;
  height: auto;
  border-radius: 4px;
}
</style>
