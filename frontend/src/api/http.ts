export type ApiResponse<T> = {
  code: number
  message: string
  data: T
  traceId: string
  timestamp: string
}

export class ApiError extends Error {
  public readonly status: number
  public readonly code?: number
  public readonly traceId?: string

  constructor(opts: { status: number; message: string; code?: number; traceId?: string }) {
    super(opts.message)
    this.status = opts.status
    this.code = opts.code
    this.traceId = opts.traceId
  }
}

const API_BASE = (import.meta.env.VITE_API_BASE_URL as string | undefined) ?? 'http://localhost:8080'

function buildUrl(path: string): string {
  if (path.startsWith('http://') || path.startsWith('https://')) return path
  const base = API_BASE.endsWith('/') ? API_BASE.slice(0, -1) : API_BASE
  return base + path
}

async function parseJsonOrThrow<T>(res: Response): Promise<ApiResponse<T>> {
  const text = await res.text()
  const contentType = res.headers.get('content-type') ?? ''
  if (!contentType.includes('application/json')) {
    throw new ApiError({ status: res.status, message: `Unexpected content-type: ${contentType}` })
  }
  const body = (text ? JSON.parse(text) : {}) as ApiResponse<T>
  if (!res.ok || (typeof body.code === 'number' && body.code !== 0)) {
    throw new ApiError({
      status: res.status,
      message: body.message || `HTTP ${res.status}`,
      code: body.code,
      traceId: body.traceId,
    })
  }
  return body
}

export async function apiData<T>(
  path: string,
  init: { method: string; body?: unknown },
  token?: string,
): Promise<T> {
  const headers: Record<string, string> = {}
  if (token) headers.Authorization = `Bearer ${token}`

  let body: string | undefined
  if (init.body !== undefined) {
    headers['Content-Type'] = 'application/json; charset=utf-8'
    body = JSON.stringify(init.body)
  }

  const res = await fetch(buildUrl(path), {
    method: init.method,
    headers,
    body,
  })
  const json = await parseJsonOrThrow<T>(res)
  return json.data
}

function parseFilenameFromContentDisposition(value: string | null): string | null {
  if (!value) return null
  // attachment; filename="xxx.csv"
  const m = /filename=\"([^\"]+)\"/i.exec(value)
  if (m?.[1]) return m[1]
  return null
}

export async function downloadBlob(
  path: string,
  opts: { token: string; fallbackFilename: string },
): Promise<{ filename: string }> {
  const res = await fetch(buildUrl(path), {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${opts.token}`,
    },
  })

  if (!res.ok) {
    await parseJsonOrThrow(res)
    throw new ApiError({ status: res.status, message: `HTTP ${res.status}` })
  }

  const blob = await res.blob()
  const filename =
    parseFilenameFromContentDisposition(res.headers.get('content-disposition')) ?? opts.fallbackFilename

  const url = URL.createObjectURL(blob)
  try {
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
  } finally {
    URL.revokeObjectURL(url)
  }

  return { filename }
}

export async function fetchBlob(
  path: string,
  opts: { token: string },
): Promise<{ blob: Blob; filename: string | null; contentType: string | null }> {
  const res = await fetch(buildUrl(path), {
    method: 'GET',
    headers: {
      Authorization: `Bearer ${opts.token}`,
    },
  })

  if (!res.ok) {
    await parseJsonOrThrow(res)
    throw new ApiError({ status: res.status, message: `HTTP ${res.status}` })
  }

  const blob = await res.blob()
  return {
    blob,
    filename: parseFilenameFromContentDisposition(res.headers.get('content-disposition')),
    contentType: res.headers.get('content-type'),
  }
}

export async function uploadFormData<T>(
  path: string,
  opts: { token: string; formData: FormData },
): Promise<T> {
  const res = await fetch(buildUrl(path), {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${opts.token}`,
      // Do not set Content-Type manually; browser will set boundary.
    },
    body: opts.formData,
  })
  const json = await parseJsonOrThrow<T>(res)
  return json.data
}
