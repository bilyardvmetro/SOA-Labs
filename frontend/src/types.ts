export const workerStatuses = [
  'FIRED',
  'RECOMMENDED_FOR_PROMOTION',
  'PROBATION',
] as const

export const organizationTypes = [
  'PUBLIC',
  'TRUST',
  'PRIVATE_LIMITED_COMPANY',
  'OPEN_JOINT_STOCK_COMPANY',
] as const

export type WorkerStatus = (typeof workerStatuses)[number]
export type OrganizationType = (typeof organizationTypes)[number]

export interface Coordinates {
  x: number
  y: number
}

export interface Organization {
  fullName: string
  annualTurnover: number
  type: OrganizationType
}

export interface Worker {
  id: number
  name: string
  coordinates: Coordinates
  creationDate: string
  salary: number | null
  startDate: string
  endDate: string | null
  status: WorkerStatus
  organization: Organization | null
}

export type WorkerInput = Omit<Worker, 'id' | 'creationDate'>

export interface WorkerPage {
  items: Worker[]
  page: number
  size: number
  totalItems: number
  totalPages: number
}

export interface ErrorResponse {
  timestamp?: string
  status?: number
  error?: string
  message?: string
  path?: string
}

export interface EndDateGroup {
  endDate: string | null
  count: number
}

export interface CountResult {
  count: number
}

export interface SumResult {
  sum: number
}

export interface FilterRow {
  id: number
  field: string
  operator: string
  value: string
}

export interface SortRow {
  id: number
  field: string
  direction: 'asc' | 'desc'
}
