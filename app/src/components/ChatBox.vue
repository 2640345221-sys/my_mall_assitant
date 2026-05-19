<script setup>
import { ref, nextTick, onMounted } from 'vue'

const STORAGE_KEY = 'mall_chat_conversation_id'

const userId = ref('1')
const input = ref('')
const messages = ref([])
const streaming = ref(false)
const conversationId = ref('')

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  conversationId.value = saved || crypto.randomUUID()
  if (!saved) localStorage.setItem(STORAGE_KEY, conversationId.value)
})

function newConversation() {
  messages.value = []
  const id = crypto.randomUUID()
  conversationId.value = id
  localStorage.setItem(STORAGE_KEY, id)
}

async function send() {
  const msg = input.value.trim()
  if (!msg || streaming.value) return
  input.value = ''

  messages.value.push({ role: 'user', content: msg })
  const assistantIndex = messages.value.length // 新消息的索引
  messages.value.push({ role: 'assistant', content: '' })
  streaming.value = true

  try {
    const resp = await fetch('/chat/stream', {
      method: 'POST',  // 注意大写
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        userId: userId.value,
        message: msg,
        conversationId: conversationId.value
      })
    })

    const reader = resp.body.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += decoder.decode(value, { stream: true })

      const events = buffer.split('\n\n')
      buffer = events.pop() || ''

      for (const event of events) {
        const lines = event.split('\n')
            .filter(l => l.startsWith('data:'))
            .map(l => l.slice(5))
        if (lines.length > 0) {
          messages.value[assistantIndex].content += lines.join('\n')
          await nextTick()
        }
      }
    }
  } catch (e) {
    messages.value[assistantIndex].content = '请求失败: ' + e.message
  } finally {
    streaming.value = false
  }
}
</script>

<template>
  <div class="messages" ref="msgList">
    <div v-for="(m, i) in messages" :key="i" :class="['msg', m.role]">
      {{ m.content }}
    </div>
  </div>

  <div class="input-area">
    <input v-model="userId" placeholder="用户ID" style="width:80px;margin-right:4px" />
    <button @click="newConversation" :disabled="streaming" style="background:#999;margin-right:4px">新对话</button>
    <input
      v-model="input"
      @keyup.enter="send"
      placeholder="输入消息..."
      :disabled="streaming"
    />
    <button @click="send" :disabled="streaming || !input.trim()">发送</button>
  </div>
</template>
