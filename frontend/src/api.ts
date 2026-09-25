import type {
  CountResult,
  EndDateGroup,
  ErrorResponse,
  SumResult,
  Worker,
  WorkerInput,
  WorkerPage,
  WorkerStatus,
} from './types'

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly details?: ErrorResponse,
  ) {
    super(message)
  }
}

function normalizedBase(baseUrl: string): string {
  return baseUrl.trim().replace(/\/+$/, '')
}

async function request<T>(baseUrl: string, path: string, init?: RequestInit): Promise<T> {
  let response: Response
  try {
    response = await fetch(`${normalizedBase(baseUrl)}${path}`, {
      ...init,
      headers: {
        Accept: 'application/json',
        ...(init?.body ? { 'Content-Type': 'application/json' } : {}),
        ...init?.headers,
      },
    })
  } catch {
    throw new ApiError('Сервис недоступен. Проверьте адрес, HTTPS-сертификат и CORS.', 0)
  }

  if (!response.ok) {
    let details: ErrorResponse | undefined
    try {
      details = (await response.json()) as ErrorResponse
    } catch {
      details = undefined
    }
    throw new ApiError(details?.message || `Сервис вернул HTTP ${response.status}`, response.status, details)
  }

  if (response.status === 204) {
    return undefined as T
  }
  return (await response.json()) as T
}

function pageQuery(page: number, size: number, sorts: string[] = []): URLSearchParams {
  const query = new URLSearchParams({ page: String(page), size: String(size) })
  sorts.forEach((sort) => query.append('sort', sort))
  return query
}

export const workerApi = {
  list(baseUrl: string, page: number, size: number, sorts: string[], filters: string[]) {
    const query = pageQuery(page, size, sorts)
    filters.forEach((filter) => query.append('filter', filter))
    return request<WorkerPage>(baseUrl, `/workers?${query}`)
  },
  get(baseUrl: string, id: number) {
    return request<Worker>(baseUrl, `/workers/${id}`)
  },
  create(baseUrl: string, input: WorkerInput) {
    return request<Worker>(baseUrl, '/workers', { method: 'POST', body: JSON.stringify(input) })
  },
  update(baseUrl: string, id: number, input: WorkerInput) {
    return request<Worker>(baseUrl, `/workers/${id}`, { method: 'PUT', body: JSON.stringify(input) })
  },
  patchOrganization(baseUrl: string, id: number, organization: null | { fullName: string }) {
    return request<Worker>(baseUrl, `/workers/${id}`, {
      method: 'PATCH',
      body: JSON.stringify({ organization }),
    })
  },
  remove(baseUrl: string, id: number) {
    return request<void>(baseUrl, `/workers/${id}`, { method: 'DELETE' })
  },
  groupByEndDate(baseUrl: string) {
    return request<EndDateGroup[]>(baseUrl, '/workers/group-by-end-date')
  },
  searchByName(baseUrl: string, prefix: string, page: number, size: number, sorts: string[]) {
    const query = pageQuery(page, size, sorts)
    query.set('prefix', prefix)
    return request<WorkerPage>(baseUrl, `/workers/search/by-name-prefix?${query}`)
  },
  searchByStatus(baseUrl: string, status: WorkerStatus, page: number, size: number, sorts: string[]) {
    const query = pageQuery(page, size, sorts)
    query.set('status', status)
    return request<WorkerPage>(baseUrl, `/workers/search/by-status-greater-than?${query}`)
  },
  sumSalary(baseUrl: string) {
    return request<SumResult>(baseUrl, '/workers/sum/salary')
  },
  countByEndDate(baseUrl: string, endDate: string | null) {
    return request<CountResult>(baseUrl, `/workers/count/endDate/${encodeURIComponent(endDate ?? 'null')}`)
  },
  countSalaryLess(baseUrl: string, threshold: number) {
    return request<CountResult>(baseUrl, `/workers/count/salary/less/${threshold}`)
  },
}

export const hrApi = {
  fire(baseUrl: string, workerId: number, organizationName: string) {
    const query = new URLSearchParams({ organizationName })
    return request<Worker>(baseUrl, `/hr/fire/${workerId}?${query}`, { method: 'POST' })
  },
  indexSalary(baseUrl: string, workerId: number, coefficient: string) {
    return request<Worker>(baseUrl, `/hr/index/${workerId}/${encodeURIComponent(coefficient)}`, {
      method: 'POST',
    })
  },
}
