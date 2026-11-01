import type { AiChatResponse, AiChatTurn, AiExecutionMode } from "../api/recipes"

const SESSION_TTL_MS = 30 * 60 * 1000
const STORAGE_PREFIX = "mamxanh.ai-chat.session.v1"

export interface PersistedAiChatMessage extends AiChatTurn {
  id: number
  recommendations?: AiChatResponse["recommendations"]
  workflowUrl?: string | null
  steps?: AiChatResponse["steps"]
  pendingConfirmation?: AiChatResponse["pendingConfirmation"]
  consentRequest?: AiChatResponse["consentRequest"]
  sources?: AiChatResponse["sources"]
  engine?: string | null
  runStatus?: string | null
  knowledgeRevision?: string | null
  requestQuestion?: string
}

export interface PersistedAiChatSession {
  sessionId: string | null
  messages: PersistedAiChatMessage[]
  executionMode: AiExecutionMode
  sequence: number
  expiresAt: number
}

function storageKey(ownerKey: string) {
  return `${STORAGE_PREFIX}.${ownerKey}`
}

export function readAiChatSession(ownerKey: string): PersistedAiChatSession | null {
  try {
    const key = storageKey(ownerKey)
    const serialized = window.sessionStorage.getItem(key)
    if (!serialized) return null
    const session = JSON.parse(serialized) as PersistedAiChatSession
    if (!session.expiresAt || session.expiresAt <= Date.now() || !Array.isArray(session.messages)) {
      window.sessionStorage.removeItem(key)
      return null
    }
    return session
  } catch {
    return null
  }
}

export function writeAiChatSession(ownerKey: string, session: Omit<PersistedAiChatSession, "expiresAt">) {
  try {
    window.sessionStorage.setItem(storageKey(ownerKey), JSON.stringify({
      ...session,
      expiresAt: Date.now() + SESSION_TTL_MS,
    } satisfies PersistedAiChatSession))
  } catch {
    // Conversation remains usable in memory when browser storage is unavailable.
  }
}

export function clearAiChatSession(ownerKey: string) {
  try {
    window.sessionStorage.removeItem(storageKey(ownerKey))
  } catch {
    // Clearing local state must remain available when storage is unavailable.
  }
}
