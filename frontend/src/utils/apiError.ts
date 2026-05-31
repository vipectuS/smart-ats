export interface ApiErrorPayload {
  status?: number
  message?: string
  code?: string
  retryable?: boolean
  userHint?: string
  traceId?: string
}

export interface ResolvedApiError {
  message: string
  code?: string
  retryable: boolean
  userHint?: string
  traceId?: string
  summary: string
}

export const resolveApiError = (error: unknown, fallbackMessage: string): ResolvedApiError => {
  const payload = (error as any)?.response?.data as ApiErrorPayload | undefined
  const message = payload?.message || (error as any)?.message || fallbackMessage
  const code = payload?.code
  const retryable = Boolean(payload?.retryable)
  const userHint = payload?.userHint
  const traceId = payload?.traceId
  const summaryParts = [message]

  if (userHint && userHint !== message) {
    summaryParts.push(userHint)
  }

  if (traceId) {
    summaryParts.push(`追踪号: ${traceId}`)
  }

  return {
    message,
    code,
    retryable,
    userHint,
    traceId,
    summary: summaryParts.join(' '),
  }
}