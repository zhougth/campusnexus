<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type InputInstance } from 'element-plus'

import { fetchHistory, fetchSessions, createSession } from '@/api/chat'
import { clickHotTopic, fetchHotTopics } from '@/api/common'
import { submitFeedback } from '@/api/feedback'
import MarkdownView from '@/components/MarkdownView.vue'
import type { NotReadyInfo } from '@/composables/useAsync'
import { useAsync } from '@/composables/useAsync'
import { useAuthStore } from '@/stores/auth'
import type {
  ChatEvent,
  ChatRole,
  ConversationSessionVO,
} from '@/types/api'
import {
  ApiError,
  BackendNotImplementedError,
  BackendUnreachableError,
} from '@/utils/errors'
import { confidenceTagType, formatConfidence, formatSource } from '@/utils/format'
import { streamChat } from '@/utils/sse'

type DoneEvent = Extract<ChatEvent, { type: 'done' }>

interface ChatMessage {
  id: string
  role: ChatRole
  content: string
  statusText: string
  followUp: string
  streaming: boolean
  stopped: boolean
  error: string
  notReady: NotReadyInfo | null
  done: DoneEvent | null
  feedbackDone: boolean
}

const router = useRouter()
const auth = useAuthStore()

const messages = ref<ChatMessage[]>([])
const input = ref('')
const streaming = ref(false)
const currentSessionId = ref<string | null>(null)
const listRef = ref<HTMLElement | null>(null)
const inputRef = ref<InputInstance>()

const historyLoading = ref(false)
const historyNotReady = ref<NotReadyInfo | null>(null)
const autoScroll = ref(true)
let abortController: AbortController | null = null
let scrollScheduled = false
let seq = 0

const {
  data: sessions,
  loading: sessionsLoading,
  notReady: sessionsNotReady,
  unreachable: sessionsUnreachable,
  run: loadSessions,
} = useAsync(() => fetchSessions(1, 50))

const {
  data: hotTopics,
  loading: hotLoading,
  notReady: hotNotReady,
  unreachable: hotUnreachable,
  run: loadHotTopics,
} = useAsync(() => fetchHotTopics(10))

const hasMessages = computed(() => messages.value.length > 0)

const sampleQuestions = [
  '食堂几点关门？',
  '图书馆的开放时间是什么？',
  '校车时刻表在哪里看？',
  '期末成绩什么时候出？',
]

const feedbackDialog = reactive({
  visible: false,
  type: 'dislike' as 'dislike' | 'report',
  content: '',
  correctAnswer: '',
  message: null as ChatMessage | null,
})

function uid(): string {
  seq += 1
  return `msg-${Date.now()}-${seq}`
}

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

function createEmptyAssistant(): ChatMessage {
  return reactive<ChatMessage>({
    id: uid(),
    role: 'assistant',
    content: '',
    statusText: '正在准备会话...',
    followUp: '',
    streaming: true,
    stopped: false,
    error: '',
    notReady: null,
    done: null,
    feedbackDone: false,
  })
}

function scrollToBottom(force = false): void {
  if (!force && !autoScroll.value) return
  void nextTick(() => {
    const el = listRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function scheduleScroll(): void {
  if (!autoScroll.value || scrollScheduled) return
  scrollScheduled = true
  void nextTick(() => {
    scrollScheduled = false
    const el = listRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function onListScroll(): void {
  const el = listRef.value
  if (!el) return
  autoScroll.value = el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

function handleEvent(message: ChatMessage, event: ChatEvent): void {
  if (event.type === 'status') {
    message.statusText = event.content
    return
  }
  if (event.type === 'token') {
    message.statusText = ''
    message.content += event.content
    scheduleScroll()
    return
  }
  if (event.type === 'followUp') {
    message.followUp = event.content
    return
  }
  if (event.type === 'done') {
    message.done = event
    message.streaming = false
    message.statusText = ''
    scheduleScroll()
    if (auth.isLoggedIn) {
      void loadSessions()
    }
    return
  }
  message.error = event.content
  message.streaming = false
}

function handleSendError(message: ChatMessage, error: unknown): void {
  message.streaming = false
  message.statusText = ''
  if (isAbortError(error)) {
    message.stopped = true
    return
  }
  if (error instanceof BackendNotImplementedError) {
    message.notReady = { method: error.method, path: error.path }
    return
  }
  if (error instanceof BackendUnreachableError) {
    message.error = '无法连接后端服务，请确认 Spring Boot 已启动'
    return
  }
  if (error instanceof ApiError) {
    message.error = error.message
    return
  }
  message.error = '回答生成失败，请重试'
}

async function send(): Promise<void> {
  const question = input.value.trim()
  if (!question || streaming.value) return
  if (question.length > 500) {
    ElMessage.warning('问题不能超过 500 个字符')
    return
  }

  input.value = ''
  autoScroll.value = true
  messages.value.push(
    reactive<ChatMessage>({
      id: uid(),
      role: 'user',
      content: question,
      statusText: '',
      followUp: '',
      streaming: false,
      stopped: false,
      error: '',
      notReady: null,
      done: null,
      feedbackDone: false,
    }),
  )

  const assistant = createEmptyAssistant()
  messages.value.push(assistant)
  scrollToBottom(true)

  const controller = new AbortController()
  abortController = controller
  streaming.value = true
  historyNotReady.value = null

  try {
    let sessionId = currentSessionId.value
    if (!sessionId) {
      const session = await createSession(question.slice(0, 100))
      sessionId = session.sessionId
      currentSessionId.value = sessionId
      if (auth.isLoggedIn) {
        void loadSessions()
      }
    }
    assistant.statusText = '正在思考...'
    await streamChat(
      question,
      sessionId,
      {
        onEvent: (event) => handleEvent(assistant, event),
      },
      controller.signal,
    )
    if (!assistant.done && !assistant.error && !assistant.stopped) {
      assistant.streaming = false
      assistant.statusText = ''
      assistant.error = '连接意外中断，请重试'
    }
  } catch (error) {
    handleSendError(assistant, error)
  } finally {
    streaming.value = false
    abortController = null
    scheduleScroll()
  }
}

function stopGeneration(): void {
  abortController?.abort()
  const last = messages.value[messages.value.length - 1]
  if (last && last.role === 'assistant') {
    last.streaming = false
    last.stopped = true
    last.statusText = ''
  }
}

function newSession(): void {
  if (streaming.value) {
    ElMessage.warning('请先停止当前回答')
    return
  }
  currentSessionId.value = null
  messages.value = []
  historyNotReady.value = null
  autoScroll.value = true
  inputRef.value?.focus()
}

async function openSession(session: ConversationSessionVO): Promise<void> {
  if (streaming.value) {
    ElMessage.warning('请先停止当前回答')
    return
  }
  currentSessionId.value = session.sessionId
  messages.value = []
  historyNotReady.value = null
  historyLoading.value = true
  try {
    const history = await fetchHistory(session.sessionId, 50)
    messages.value = history
      .filter((item) => item.role !== 'system')
      .map((item) =>
        reactive<ChatMessage>({
          id: `h-${item.id}`,
          role: item.role,
          content: item.content,
          statusText: '',
          followUp: '',
          streaming: false,
          stopped: false,
          error: '',
          notReady: null,
          done: null,
          feedbackDone: false,
        }),
      )
    autoScroll.value = true
    scrollToBottom(true)
  } catch (error) {
    if (error instanceof BackendNotImplementedError) {
      historyNotReady.value = { method: error.method, path: error.path }
    } else if (error instanceof BackendUnreachableError) {
      ElMessage.error('无法连接后端服务')
    } else if (error instanceof ApiError) {
      ElMessage.error(error.message)
    }
  } finally {
    historyLoading.value = false
  }
}

function openTopic(id: number): void {
  void clickHotTopic(id).catch(() => undefined)
  void router.push(`/knowledge/${id}`)
}

function useSample(question: string): void {
  input.value = question
  inputRef.value?.focus()
}

async function copyAnswer(content: string): Promise<void> {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择内容')
  }
}

async function sendLike(message: ChatMessage): Promise<void> {
  if (!message.done) return
  try {
    await submitFeedback({
      logId: message.done.logId,
      feedbackType: 'like',
      sessionId: currentSessionId.value ?? undefined,
    })
    message.feedbackDone = true
    ElMessage.success('感谢你的反馈')
  } catch {
    // 错误已由拦截器提示
  }
}

function openFeedback(message: ChatMessage, type: 'dislike' | 'report'): void {
  feedbackDialog.visible = true
  feedbackDialog.type = type
  feedbackDialog.content = ''
  feedbackDialog.correctAnswer = ''
  feedbackDialog.message = message
}

async function submitFeedbackDialog(): Promise<void> {
  const message = feedbackDialog.message
  if (!message?.done) return
  try {
    await submitFeedback({
      logId: message.done.logId,
      feedbackType: feedbackDialog.type,
      sessionId: currentSessionId.value ?? undefined,
      content: feedbackDialog.content.trim() || undefined,
      correctAnswer:
        feedbackDialog.type === 'dislike'
          ? feedbackDialog.correctAnswer.trim() || undefined
          : undefined,
    })
    message.feedbackDone = true
    feedbackDialog.visible = false
    ElMessage.success(
      feedbackDialog.type === 'dislike' ? '已提交点踩反馈' : '已提交纠错反馈',
    )
  } catch {
    // 错误已由拦截器提示
  }
}

function onEnter(event: KeyboardEvent): void {
  if (event.isComposing) return
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    void send()
  }
}

onMounted(() => {
  void loadHotTopics()
  if (auth.isLoggedIn) {
    void loadSessions()
  }
})

watch(
  () => auth.isLoggedIn,
  (loggedIn) => {
    if (loggedIn) {
      void loadSessions()
    } else {
      currentSessionId.value = null
      messages.value = []
      historyNotReady.value = null
    }
  },
)
</script>

<template>
  <div class="chat-page">
    <aside class="chat-sessions">
      <div class="sessions-head">
        <span>会话记录</span>
        <el-button size="small" @click="newSession">
          <el-icon><Plus /></el-icon>
          新对话
        </el-button>
      </div>

      <div class="sessions-body">
        <template v-if="auth.isLoggedIn">
          <div v-if="sessionsNotReady" class="backend-mini">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>会话接口后端未实现</strong>
              <span class="mono">{{ sessionsNotReady?.method }} {{ sessionsNotReady?.path }}</span>
            </div>
          </div>
          <div v-else-if="sessionsUnreachable" class="backend-mini">
            <el-icon><WarningFilled /></el-icon>
            <div>
              <strong>无法连接后端</strong>
              <span>请确认服务已启动</span>
            </div>
          </div>
          <el-skeleton v-else-if="sessionsLoading" :rows="4" animated />
          <el-empty
            v-else-if="!sessions || sessions.records.length === 0"
            description="还没有会话，开始提问吧"
            :image-size="56"
          />
          <template v-else>
            <button
              v-for="session in sessions.records"
              :key="session.sessionId"
              type="button"
              class="session-item"
              :class="{ active: session.sessionId === currentSessionId }"
              @click="openSession(session)"
            >
              <span class="session-title">{{ session.title }}</span>
              <span class="session-meta">{{ session.startTime.slice(5, 16) }}</span>
            </button>
          </template>
        </template>

        <div v-else class="login-cta">
          <p>登录后可保存会话记录、查看历史消息</p>
          <el-button type="primary" size="small" @click="router.push('/login?redirect=/')">
            去登录
          </el-button>
        </div>
      </div>
    </aside>

    <section class="chat-main">
      <div class="chat-topbar">
        <div class="topbar-title">
          <span class="dot" />
          <strong>校园生活助手</strong>
          <span class="topbar-sub">基于校园知识库回答</span>
        </div>
        <el-tag v-if="!auth.isLoggedIn" type="info" effect="plain" size="small">匿名会话</el-tag>
        <el-tag v-else effect="plain" size="small">{{ auth.user?.username }}</el-tag>
      </div>

      <div ref="listRef" class="message-list" @scroll="onListScroll">
        <div v-if="!hasMessages && !historyLoading && !historyNotReady" class="welcome">
          <div class="welcome-mark">C</div>
          <h1>你好，我是校园生活助手</h1>
          <p>可以问我食堂、图书馆、教务、宿舍、交通等问题，答案来自校园知识库。</p>
          <div class="sample-grid">
            <button
              v-for="question in sampleQuestions"
              :key="question"
              type="button"
              class="sample-item"
              @click="useSample(question)"
            >
              {{ question }}
            </button>
          </div>
        </div>

        <div v-if="historyLoading" class="history-skeleton">
          <el-skeleton :rows="5" animated />
        </div>

        <div v-else-if="historyNotReady" class="history-not-ready">
          <el-result icon="warning" title="历史消息接口后端尚未实现" :sub-title="`${historyNotReady?.method} ${historyNotReady?.path}`">
            <template #extra>
              <span class="text-muted text-small">后端完成后刷新即可查看该会话的历史消息。</span>
            </template>
          </el-result>
        </div>

        <div
          v-for="message in messages"
          :key="message.id"
          class="message-row"
          :class="message.role"
        >
          <div v-if="message.role === 'assistant'" class="avatar assistant-avatar">AI</div>
          <div class="bubble" :class="message.role">
            <template v-if="message.role === 'user'">
              <div class="user-text">{{ message.content }}</div>
            </template>
            <template v-else>
              <div v-if="message.notReady" class="answer-error">
                <el-alert
                  type="warning"
                  :closable="false"
                  title="后端接口尚未实现"
                  :description="`${message.notReady?.method} ${message.notReady?.path}，后端完成后刷新即可直接使用。`"
                  show-icon
                />
              </div>
              <div v-else-if="message.error" class="answer-error">
                <el-alert type="error" :closable="false" :title="message.error" show-icon />
              </div>
              <template v-else>
                <div v-if="message.statusText" class="status-line">
                  <span class="typing-dot" />
                  <span class="typing-dot" />
                  <span class="typing-dot" />
                  <span>{{ message.statusText }}</span>
                </div>
                <MarkdownView v-if="message.content" :content="message.content" />
                <div v-if="message.streaming && !message.content && !message.statusText" class="status-line">
                  <span class="typing-dot" />
                  <span class="typing-dot" />
                  <span class="typing-dot" />
                </div>
                <div v-if="message.stopped" class="stopped-line">已停止生成</div>
                <div v-if="message.followUp" class="follow-up">
                  <el-icon><MagicStick /></el-icon>
                  <span>{{ message.followUp }}</span>
                </div>
                <div v-if="message.done" class="answer-meta">
                  <el-tag size="small" effect="plain" :type="confidenceTagType(message.done?.confidence ?? null)">
                    {{ formatConfidence(message.done?.confidence ?? null) }}
                  </el-tag>
                  <el-tag size="small" effect="plain" type="info">
                    {{ formatSource(message.done?.source ?? null) }}
                  </el-tag>
                  <span class="meta-text">命中 {{ message.done?.matchCount }} 条知识</span>
                  <div class="meta-actions">
                    <el-button link size="small" @click="copyAnswer(message.content)">复制</el-button>
                    <template v-if="auth.isLoggedIn && !message.feedbackDone">
                      <el-button link size="small" @click="sendLike(message)">赞</el-button>
                      <el-button link size="small" @click="openFeedback(message, 'dislike')">踩</el-button>
                      <el-button link size="small" @click="openFeedback(message, 'report')">纠错</el-button>
                    </template>
                    <span v-else-if="message.feedbackDone" class="text-muted text-small">已反馈</span>
                  </div>
                </div>
                <div v-if="message.done?.recommendations?.length" class="recommendations">
                  <span class="text-muted text-small">你可能还想了解：</span>
                  <el-button
                    v-for="item in message.done?.recommendations ?? []"
                    :key="item.knowledgeId"
                    link
                    size="small"
                    @click="router.push(`/knowledge/${item.knowledgeId}`)"
                  >
                    {{ item.title }}
                  </el-button>
                </div>
              </template>
            </template>
          </div>
          <div v-if="message.role === 'user'" class="avatar user-avatar">
            {{ auth.user?.username?.slice(0, 1).toUpperCase() ?? '我' }}
          </div>
        </div>
      </div>

      <div class="composer">
        <el-input
          ref="inputRef"
          v-model="input"
          type="textarea"
          :rows="1"
          :autosize="{ minRows: 1, maxRows: 5 }"
          maxlength="500"
          resize="none"
          placeholder="输入你的问题，例如：一食堂几点关门？"
          @keydown="onEnter"
        />
        <div class="composer-actions">
          <span class="composer-hint">Enter 发送 · Shift + Enter 换行</span>
          <el-button v-if="streaming" @click="stopGeneration">停止生成</el-button>
          <el-button v-else type="primary" :disabled="!input.trim()" @click="send">发送</el-button>
        </div>
      </div>
    </section>

    <aside class="chat-topics">
      <div class="topics-head">
        <span>热门话题</span>
        <el-button link size="small" :loading="hotLoading" @click="loadHotTopics">刷新</el-button>
      </div>

      <div v-if="hotNotReady" class="backend-mini">
        <el-icon><WarningFilled /></el-icon>
        <div>
          <strong>热点接口后端未实现</strong>
          <span class="mono">{{ hotNotReady?.method }} {{ hotNotReady?.path }}</span>
        </div>
      </div>
      <div v-else-if="hotUnreachable" class="backend-mini">
        <el-icon><WarningFilled /></el-icon>
        <div>
          <strong>无法连接后端</strong>
          <span>请确认服务已启动</span>
        </div>
      </div>
      <el-skeleton v-else-if="hotLoading" :rows="6" animated />
      <el-empty
        v-else-if="!hotTopics || hotTopics.length === 0"
        description="暂无热门话题"
        :image-size="56"
      />
      <ol v-else class="topic-list">
        <li v-for="(topic, index) in hotTopics" :key="topic.id" @click="openTopic(topic.id)">
          <span class="rank" :class="{ top: index < 3 }">{{ index + 1 }}</span>
          <span class="topic-title">{{ topic.title }}</span>
          <span class="topic-count">{{ topic.viewCount }}</span>
        </li>
      </ol>
      <p class="topics-foot">点击话题查看知识详情</p>
    </aside>

    <el-dialog
      v-model="feedbackDialog.visible"
      :title="feedbackDialog.type === 'dislike' ? '提交点踩反馈' : '提交纠错'"
      width="520px"
    >
      <el-form label-position="top">
        <el-form-item v-if="feedbackDialog.type === 'dislike'" label="你认为正确的答案（可选）">
          <el-input
            v-model="feedbackDialog.correctAnswer"
            type="textarea"
            :rows="3"
            maxlength="2000"
            show-word-limit
            placeholder="补充正确答案，管理员会据此审核"
          />
        </el-form-item>
        <el-form-item :label="feedbackDialog.type === 'dislike' ? '补充说明（可选）' : '问题描述'">
          <el-input
            v-model="feedbackDialog.content"
            type="textarea"
            :rows="3"
            maxlength="1000"
            show-word-limit
            :placeholder="feedbackDialog.type === 'dislike' ? '说明回答哪里有问题' : '说明答案中的错误或需要更新的内容'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="feedbackDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitFeedbackDialog">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.chat-page {
  height: calc(100vh - 64px);
  display: flex;
  min-height: 0;
  background: var(--cn-bg);
}

.chat-sessions {
  width: 264px;
  flex-shrink: 0;
  border-right: 1px solid var(--cn-border);
  background: #fff;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.sessions-head {
  height: 56px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--cn-border);
  font-size: 14px;
  font-weight: 600;
}

.sessions-body {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
}

.session-item {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 12px;
  margin-bottom: 4px;
  border: none;
  border-radius: 8px;
  background: transparent;
  text-align: left;
  cursor: pointer;
  color: var(--cn-text);
}

.session-item:hover {
  background: #f8fafc;
}

.session-item.active {
  background: #eff6ff;
}

.session-title {
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.session-item.active .session-title {
  color: var(--cn-primary);
  font-weight: 500;
}

.session-meta {
  font-size: 11px;
  color: var(--cn-muted);
}

.login-cta {
  padding: 20px 12px;
  text-align: center;
  font-size: 13px;
  color: var(--cn-muted);
  line-height: 1.7;
}

.backend-mini {
  display: flex;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 8px;
  background: #fffbeb;
  border: 1px solid #fde68a;
  color: #92400e;
  font-size: 12px;
  line-height: 1.6;
}

.backend-mini .mono {
  display: block;
  font-size: 11px;
  word-break: break-all;
  margin-top: 2px;
}

.chat-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chat-topbar {
  height: 56px;
  flex-shrink: 0;
  padding: 0 24px;
  background: rgba(255, 255, 255, 0.7);
  border-bottom: 1px solid var(--cn-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.topbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
}

.topbar-sub {
  font-size: 12px;
  color: var(--cn-muted);
}

.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #22c55e;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 28px 24px 12px;
}

.welcome {
  max-width: 640px;
  margin: 6vh auto 0;
  text-align: center;
}

.welcome-mark {
  width: 54px;
  height: 54px;
  margin: 0 auto 18px;
  border-radius: 16px;
  display: grid;
  place-items: center;
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
  font-size: 26px;
  font-weight: 700;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.25);
}

.welcome h1 {
  margin: 0 0 8px;
  font-size: 24px;
}

.welcome p {
  margin: 0 0 24px;
  color: var(--cn-muted);
  font-size: 14px;
}

.sample-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.sample-item {
  padding: 12px 14px;
  border: 1px solid var(--cn-border);
  border-radius: 10px;
  background: #fff;
  color: #374151;
  font-size: 13px;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.sample-item:hover {
  border-color: #bfdbfe;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.08);
}

.message-row {
  max-width: 860px;
  margin: 0 auto 22px;
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.message-row.user {
  justify-content: flex-end;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 10px;
  flex-shrink: 0;
  display: grid;
  place-items: center;
  font-size: 12px;
  font-weight: 600;
}

.assistant-avatar {
  background: linear-gradient(135deg, #2563eb, #38bdf8);
  color: #fff;
}

.user-avatar {
  background: #e2e8f0;
  color: #475569;
}

.bubble {
  max-width: min(720px, 82%);
  border-radius: 12px;
  padding: 12px 16px;
  font-size: 14px;
  line-height: 1.7;
}

.bubble.assistant {
  background: #fff;
  border: 1px solid var(--cn-border);
  color: var(--cn-text);
  min-width: 0;
}

.bubble.user {
  background: #2563eb;
  color: #fff;
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 5px;
  color: var(--cn-muted);
  font-size: 13px;
}

.typing-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #94a3b8;
  animation: blink 1.2s infinite ease-in-out;
}

.typing-dot:nth-child(2) {
  animation-delay: 0.15s;
}

.typing-dot:nth-child(3) {
  animation-delay: 0.3s;
  margin-right: 4px;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.2;
  }
  40% {
    opacity: 1;
  }
}

.stopped-line {
  margin-top: 8px;
  font-size: 12px;
  color: #b45309;
}

.follow-up {
  margin-top: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px dashed #cbd5e1;
  color: #475569;
  font-size: 12px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.answer-meta {
  margin-top: 12px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.meta-text {
  font-size: 12px;
  color: var(--cn-muted);
}

.meta-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 4px;
}

.recommendations {
  margin-top: 10px;
  display: flex;
  align-items: center;
  gap: 4px;
  flex-wrap: wrap;
}

.answer-error {
  min-width: 320px;
}

.history-skeleton,
.history-not-ready {
  max-width: 720px;
  margin: 40px auto;
}

.composer {
  flex-shrink: 0;
  padding: 12px 24px 16px;
  background: linear-gradient(180deg, rgba(245, 247, 251, 0), var(--cn-bg) 30%);
}

.composer :deep(.el-textarea__inner) {
  border-radius: 12px;
  padding: 12px 14px;
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.05);
}

.composer-actions {
  margin-top: 8px;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
}

.composer-hint {
  margin-right: auto;
  font-size: 12px;
  color: #9ca3af;
}

.chat-topics {
  width: 300px;
  flex-shrink: 0;
  background: #fff;
  border-left: 1px solid var(--cn-border);
  padding: 16px;
  overflow-y: auto;
}

.topics-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  font-weight: 600;
  margin-bottom: 12px;
}

.topic-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.topic-list li {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 8px;
  border-radius: 8px;
  cursor: pointer;
}

.topic-list li:hover {
  background: #f8fafc;
}

.rank {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
  border-radius: 6px;
  display: grid;
  place-items: center;
  font-size: 11px;
  color: #64748b;
  background: #f1f5f9;
}

.rank.top {
  background: #fef3c7;
  color: #b45309;
  font-weight: 600;
}

.topic-title {
  flex: 1;
  font-size: 13px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.topic-count {
  font-size: 11px;
  color: #94a3b8;
}

.topics-foot {
  margin-top: 14px;
  font-size: 11px;
  color: #9ca3af;
  text-align: center;
}

@media (max-width: 1180px) {
  .chat-topics {
    display: none;
  }
}

@media (max-width: 820px) {
  .chat-sessions {
    display: none;
  }

  .message-list {
    padding: 20px 14px 8px;
  }

  .composer {
    padding: 10px 14px 14px;
  }
}
</style>
