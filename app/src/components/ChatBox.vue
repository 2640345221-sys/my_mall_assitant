<script setup>
import { ref, nextTick, onMounted, watch } from 'vue'

const STORAGE_KEY = 'mall_chat_conversation_id'
const userId = ref('1')
const input = ref('')
const messages = ref([])
const streaming = ref(false)
const conversationId = ref('')
const msgList = ref(null)

onMounted(() => {
  const saved = localStorage.getItem(STORAGE_KEY)
  conversationId.value = saved || crypto.randomUUID()
  if (!saved) localStorage.setItem(STORAGE_KEY, conversationId.value)
})

watch(messages, () => nextTick(() => {
  if (msgList.value) msgList.value.scrollTop = msgList.value.scrollHeight
}), { deep: true })

function newConversation() {
  messages.value = []
  conversationId.value = crypto.randomUUID()
  localStorage.setItem(STORAGE_KEY, conversationId.value)
}

async function send() {
  const msg = input.value.trim()
  if (!msg || streaming.value) return
  input.value = ''
  messages.value.push({ role: 'user', content: msg })
  const idx = messages.value.length
  messages.value.push({ role: 'assistant', content: '' })
  streaming.value = true

  try {
    const resp = await fetch('/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ userId: userId.value, message: msg, conversationId: conversationId.value })
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
        const lines = event.split('\n').filter(l => l.startsWith('data:')).map(l => l.slice(5))
        if (lines.length > 0) {
          messages.value[idx].content += lines.join('\n')
          await nextTick()
        }
      }
    }
    } catch (e) {
      messages.value[idx].content = '请求失败: ' + e.message
      messages.value[idx].failed = true
      messages.value[idx].lastMsg = msg
    } finally {
      streaming.value = false
    }
  }

  function resend(lastMsg) {
    messages.value.pop()
    input.value = lastMsg
    send()
  }
</script>

<template>
  <div class="header">
    <div class="dot"></div>
    <span>商城智能助手</span>
  </div>

  <div class="messages" ref="msgList">
    <div v-if="messages.length === 0" class="welcome">
      <p>你好！我是商城助手，可以帮您搜索商品、管理购物车、下单</p>
    </div>
    <div v-for="(m, i) in messages" :key="i" :class="['msg-row', m.role]">
      <div class="msg-avatar">{{ m.role === 'user' ? '我' : 'AI' }}</div>
      <div class="msg-bubble">
        {{ m.content }}
        <button v-if="m.failed" @click="resend(m.lastMsg)" class="retry-btn">重新发送</button>
        <div v-if="streaming && i === messages.length-1 && m.content === ''" class="typing">
          <span></span><span></span><span></span>
        </div>
      </div>
    </div>
  </div>

  <div class="input-area">
    <button class="btn-new" @click="newConversation" :disabled="streaming">新对话</button>
    <span style="font-size:13px;color:#888;white-space:nowrap">用户ID:</span>
    <input v-model="userId" placeholder="用户ID" />
    <input v-model="input" @keyup.enter="send" placeholder="输入消息..." :disabled="streaming" />
    <button class="btn-send" @click="send" :disabled="streaming || !input.trim()">发送</button>
  </div>
</template>
