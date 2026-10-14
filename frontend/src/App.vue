<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ApiError, hrApi, workerApi } from './api'
import OperationsPanel from './components/OperationsPanel.vue'
import WorkerFormModal from './components/WorkerFormModal.vue'
import type { FilterRow, SortRow, Worker, WorkerInput, WorkerPage } from './types'

type Tab = 'workers' | 'operations'
type WorkerAction = 'organization' | 'fire' | 'index'

const filterFields = [
  ['id', 'ID'],
  ['name', 'Имя'],
  ['coordinates.x', 'Координата X'],
  ['coordinates.y', 'Координата Y'],
  ['creationDate', 'Дата создания'],
  ['salary', 'Зарплата'],
  ['startDate', 'Начало работы'],
  ['endDate', 'Окончание работы'],
  ['status', 'Статус'],
  ['organization.fullName', 'Название организации'],
  ['organization.annualTurnover', 'Оборот организации'],
  ['organization.type', 'Тип организации'],
] as const

const operators = [
  ['eq', 'равно'],
  ['ne', 'не равно'],
  ['gt', 'больше'],
  ['gte', 'не меньше'],
  ['lt', 'меньше'],
  ['lte', 'не больше'],
  ['contains', 'содержит'],
  ['startsWith', 'начинается с'],
  ['endsWith', 'заканчивается на'],
  ['in', 'одно из (через запятую)'],
  ['isNull', 'не задано'],
  ['notNull', 'задано'],
] as const

const statusLabels: Record<string, string> = {
  FIRED: 'Уволен',
  RECOMMENDED_FOR_PROMOTION: 'Рекомендован к повышению',
  PROBATION: 'Испытательный срок',
}

const organizationLabels: Record<string, string> = {
  PUBLIC: 'Публичная',
  TRUST: 'Траст',
  PRIVATE_LIMITED_COMPANY: 'Частная компания',
  OPEN_JOINT_STOCK_COMPANY: 'Открытое АО',
}

const hostname = window.location.hostname || 'localhost'
const defaultWorkerUrl = `https://${hostname}:18443`
const defaultHrUrl = window.location.port === '5173' ? `https://${hostname}:19443` : window.location.origin

const workerBaseUrl = ref(localStorage.getItem('workerBaseUrl') || defaultWorkerUrl)
const hrBaseUrl = ref(localStorage.getItem('hrBaseUrl') || defaultHrUrl)
const settingsOpen = ref(false)
const activeTab = ref<Tab>('workers')
const loading = ref(false)
const mutationBusy = ref(false)
const error = ref('')
const notice = ref('')
const pageData = ref<WorkerPage>({ items: [], page: 1, size: 20, totalItems: 0, totalPages: 0 })
const query = reactive({ page: 1, size: 20 })
const filters = ref<FilterRow[]>([])
const sorts = ref<SortRow[]>([{ id: 1, field: 'id', direction: 'asc' }])
let rowSequence = 2

const formOpen = ref(false)
const editingWorker = ref<Worker>()
const action = ref<WorkerAction | null>(null)
const actionWorker = ref<Worker>()
const organizationName = ref('')
const removeOrganization = ref(false)
const coefficient = ref('1.1')

const rangeLabel = computed(() => {
  if (!pageData.value.totalItems) return 'Нет записей'
  const first = (pageData.value.page - 1) * pageData.value.size + 1
  const last = Math.min(pageData.value.page * pageData.value.size, pageData.value.totalItems)
  return `${first}–${last} из ${pageData.value.totalItems}`
})

function normalizeUrl(value: string): string {
  return value.trim().replace(/\/+$/, '')
}

function showError(value: unknown) {
  error.value = value instanceof ApiError ? value.message : 'Произошла непредвиденная ошибка'
  notice.value = ''
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function showNotice(message: string) {
  notice.value = message
  error.value = ''
}

function filterExpressions(): string[] {
  return filters.value.map(({ field, operator, value }) =>
    operator === 'isNull' || operator === 'notNull'
      ? `${field}:${operator}`
      : `${field}:${operator}:${value}`,
  )
}

function sortExpressions(): string[] {
  return sorts.value.map(({ field, direction }) => `${field},${direction}`)
}

async function loadWorkers() {
  loading.value = true
  error.value = ''
  try {
    pageData.value = await workerApi.list(
      workerBaseUrl.value,
      query.page,
      query.size,
      sortExpressions(),
      filterExpressions(),
    )
  } catch (value) {
    showError(value)
  } finally {
    loading.value = false
  }
}

function applyQuery() {
  query.page = 1
  void loadWorkers()
}

function resetQuery() {
  filters.value = []
  sorts.value = [{ id: rowSequence++, field: 'id', direction: 'asc' }]
  query.page = 1
  void loadWorkers()
}

function addFilter() {
  filters.value.push({ id: rowSequence++, field: 'name', operator: 'contains', value: '' })
}

function addSort() {
  sorts.value.push({ id: rowSequence++, field: 'name', direction: 'asc' })
}

function changePage(page: number) {
  if (page < 1 || (pageData.value.totalPages > 0 && page > pageData.value.totalPages)) return
  query.page = page
  void loadWorkers()
}

function saveSettings() {
  workerBaseUrl.value = normalizeUrl(workerBaseUrl.value)
  hrBaseUrl.value = normalizeUrl(hrBaseUrl.value)
  localStorage.setItem('workerBaseUrl', workerBaseUrl.value)
  localStorage.setItem('hrBaseUrl', hrBaseUrl.value)
  settingsOpen.value = false
  query.page = 1
  void loadWorkers()
}

function openCreate() {
  editingWorker.value = undefined
  formOpen.value = true
}

function openEdit(worker: Worker) {
  editingWorker.value = worker
  formOpen.value = true
}

async function saveWorker(input: WorkerInput) {
  mutationBusy.value = true
  try {
    if (editingWorker.value) {
      await workerApi.update(workerBaseUrl.value, editingWorker.value.id, input)
      showNotice(`Работник #${editingWorker.value.id} обновлён`)
    } else {
      const created = await workerApi.create(workerBaseUrl.value, input)
      showNotice(`Работник #${created.id} создан`)
    }
    formOpen.value = false
    await loadWorkers()
  } catch (value) {
    showError(value)
  } finally {
    mutationBusy.value = false
  }
}

async function deleteWorker(worker: Worker) {
  if (!window.confirm(`Удалить работника «${worker.name}» (#${worker.id})?`)) return
  mutationBusy.value = true
  try {
    await workerApi.remove(workerBaseUrl.value, worker.id)
    showNotice(`Работник #${worker.id} удалён`)
    if (pageData.value.items.length === 1 && query.page > 1) query.page--
    await loadWorkers()
  } catch (value) {
    showError(value)
  } finally {
    mutationBusy.value = false
  }
}

function openAction(kind: WorkerAction, worker: Worker) {
  action.value = kind
  actionWorker.value = worker
  organizationName.value = worker.organization?.fullName ?? ''
  removeOrganization.value = false
  coefficient.value = '1.1'
}

function closeAction() {
  action.value = null
  actionWorker.value = undefined
}

async function submitAction() {
  const worker = actionWorker.value
  if (!worker || !action.value) return
  mutationBusy.value = true
  try {
    if (action.value === 'organization') {
      await workerApi.patchOrganization(
        workerBaseUrl.value,
        worker.id,
        removeOrganization.value ? null : { fullName: organizationName.value.trim() },
      )
      showNotice(removeOrganization.value ? 'Связь с организацией удалена' : 'Организация переименована')
    } else if (action.value === 'fire') {
      await hrApi.fire(hrBaseUrl.value, worker.id, organizationName.value.trim())
      showNotice(`Работник #${worker.id} уволен`)
    } else {
      await hrApi.indexSalary(hrBaseUrl.value, worker.id, coefficient.value)
      showNotice(`Зарплата работника #${worker.id} проиндексирована`)
    }
    closeAction()
    await loadWorkers()
  } catch (value) {
    showError(value)
  } finally {
    mutationBusy.value = false
  }
}

function formatDate(value: string | null): string {
  return value ? new Date(value).toLocaleString('ru-RU') : '—'
}

function formatMoney(value: number | null): string {
  return value == null ? '—' : `${value.toLocaleString('ru-RU')} ₽`
}

function operatorNeedsValue(operator: string): boolean {
  return operator !== 'isNull' && operator !== 'notNull'
}

onMounted(loadWorkers)
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <div class="brand__mark">W</div>
        <div>
          <p class="eyebrow">SOA · лабораторная №2</p>
          <h1>HR Console</h1>
        </div>
      </div>
      <button class="button button--ghost" @click="settingsOpen = !settingsOpen">
        {{ settingsOpen ? 'Скрыть адреса' : 'Подключение' }}
      </button>
    </header>

    <section v-if="settingsOpen" class="connection-panel">
      <div>
        <p class="eyebrow">Настройки подключения</p>
        <h2>Адреса сервисов</h2>
      </div>
      <label class="field">
        <span>worker-service</span>
        <input v-model="workerBaseUrl" type="url" placeholder="https://localhost:18443" />
      </label>
      <label class="field">
        <span>hr-service</span>
        <input v-model="hrBaseUrl" type="url" placeholder="https://localhost:19443" />
      </label>
      <button class="button button--primary" @click="saveSettings">Сохранить</button>
    </section>

    <div v-if="error" class="alert alert--error" role="alert">
      <div><b>Не удалось выполнить запрос</b><span>{{ error }}</span></div>
      <button class="icon-button" @click="error = ''">×</button>
    </div>
    <div v-if="notice" class="alert alert--success" role="status">
      <div><b>Готово</b><span>{{ notice }}</span></div>
      <button class="icon-button" @click="notice = ''">×</button>
    </div>

    <nav class="tabs" aria-label="Разделы приложения">
      <button :class="['tab', { 'tab--active': activeTab === 'workers' }]" @click="activeTab = 'workers'">
        Работники <span>{{ pageData.totalItems }}</span>
      </button>
      <button :class="['tab', { 'tab--active': activeTab === 'operations' }]" @click="activeTab = 'operations'">
        Операции над коллекцией
      </button>
    </nav>

    <main>
      <template v-if="activeTab === 'workers'">
        <section class="query-panel">
          <div class="query-panel__header">
            <div>
              <p class="eyebrow">Запрос к коллекции</p>
              <h2>Фильтрация и сортировка</h2>
            </div>
            <div class="button-row">
              <button class="button button--ghost" @click="addFilter">+ Фильтр</button>
              <button class="button button--ghost" @click="addSort">+ Сортировка</button>
            </div>
          </div>

          <div v-if="filters.length" class="query-list">
            <div v-for="filter in filters" :key="filter.id" class="query-row query-row--filter">
              <span class="query-row__kind">Где</span>
              <select v-model="filter.field">
                <option v-for="item in filterFields" :key="item[0]" :value="item[0]">{{ item[1] }}</option>
              </select>
              <select v-model="filter.operator">
                <option v-for="item in operators" :key="item[0]" :value="item[0]">{{ item[1] }}</option>
              </select>
              <input v-model="filter.value" :disabled="!operatorNeedsValue(filter.operator)" placeholder="Значение" />
              <button class="icon-button" title="Удалить фильтр" @click="filters = filters.filter(item => item.id !== filter.id)">×</button>
            </div>
          </div>

          <div v-if="sorts.length" class="query-list">
            <div v-for="sort in sorts" :key="sort.id" class="query-row query-row--sort">
              <span class="query-row__kind">Сначала</span>
              <select v-model="sort.field">
                <option v-for="item in filterFields" :key="item[0]" :value="item[0]">{{ item[1] }}</option>
              </select>
              <select v-model="sort.direction">
                <option value="asc">по возрастанию</option>
                <option value="desc">по убыванию</option>
              </select>
              <button class="icon-button" title="Удалить сортировку" @click="sorts = sorts.filter(item => item.id !== sort.id)">×</button>
            </div>
          </div>

          <div class="query-panel__footer">
            <label class="compact-field">
              <span>На странице</span>
              <select v-model.number="query.size" @change="applyQuery">
                <option :value="10">10</option>
                <option :value="20">20</option>
                <option :value="50">50</option>
                <option :value="100">100</option>
              </select>
            </label>
            <div class="button-row">
              <button class="button button--ghost" @click="resetQuery">Сбросить</button>
              <button class="button button--secondary" @click="applyQuery">Применить</button>
            </div>
          </div>
        </section>

        <section class="collection-card">
          <header class="collection-card__header">
            <div>
              <p class="eyebrow">Коллекция</p>
              <h2>Работники</h2>
            </div>
            <div class="button-row">
              <button class="button button--ghost" :disabled="loading" @click="loadWorkers">Обновить</button>
              <button class="button button--primary" @click="openCreate">Добавить работника</button>
            </div>
          </header>

          <div class="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>ID</th><th>Работник</th><th>Зарплата</th><th>Статус</th><th>Организация</th><th>Координаты</th><th>Период</th><th></th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="loading"><td colspan="8" class="empty-state">Загружаем коллекцию…</td></tr>
                <tr v-else-if="!pageData.items.length"><td colspan="8" class="empty-state">По заданным условиям ничего не найдено.</td></tr>
                <tr v-for="worker in pageData.items" v-else :key="worker.id">
                  <td class="mono">#{{ worker.id }}</td>
                  <td><b>{{ worker.name }}</b><small>Создан {{ formatDate(worker.creationDate) }}</small></td>
                  <td class="nowrap">{{ formatMoney(worker.salary) }}</td>
                  <td><span :class="['status-pill', `status-pill--${worker.status.toLowerCase()}`]">{{ statusLabels[worker.status] }}</span></td>
                  <td>
                    <template v-if="worker.organization"><b>{{ worker.organization.fullName }}</b><small>{{ organizationLabels[worker.organization.type] }} · {{ worker.organization.annualTurnover.toLocaleString('ru-RU') }} ₽</small></template>
                    <span v-else class="muted">Не указана</span>
                  </td>
                  <td class="mono">{{ worker.coordinates.x }}; {{ worker.coordinates.y }}</td>
                  <td class="nowrap"><small>с {{ formatDate(worker.startDate) }}</small><small>по {{ formatDate(worker.endDate) }}</small></td>
                  <td>
                    <details class="row-menu">
                      <summary aria-label="Действия">•••</summary>
                      <div class="row-menu__items">
                        <button :disabled="worker.status === 'FIRED'" @click="openEdit(worker)">Редактировать</button>
                        <button :disabled="worker.status === 'FIRED'" @click="openAction('organization', worker)">Изменить организацию</button>
                        <button :disabled="worker.status === 'FIRED' || worker.salary == null" @click="openAction('index', worker)">Индексировать зарплату</button>
                        <button
                          :disabled="!worker.organization"
                          :title="!worker.organization ? 'Для увольнения необходима организация' : ''"
                          @click="openAction('fire', worker)"
                        >
                          {{ !worker.organization ? 'Уволить (нет организации)' : worker.status === 'FIRED' ? 'Повторить увольнение' : 'Уволить' }}
                        </button>
                        <button class="danger" @click="deleteWorker(worker)">Удалить</button>
                      </div>
                    </details>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <footer class="pagination">
            <span>{{ rangeLabel }}</span>
            <div class="button-row">
              <button class="button button--ghost" :disabled="query.page <= 1 || loading" @click="changePage(query.page - 1)">← Назад</button>
              <span class="page-number">Страница {{ pageData.page }}<template v-if="pageData.totalPages"> из {{ pageData.totalPages }}</template></span>
              <button class="button button--ghost" :disabled="query.page >= pageData.totalPages || loading" @click="changePage(query.page + 1)">Вперёд →</button>
            </div>
          </footer>
        </section>
      </template>

      <OperationsPanel v-else :worker-base-url="workerBaseUrl" @error="showError" />
    </main>

    <WorkerFormModal v-if="formOpen" :worker="editingWorker" :busy="mutationBusy" @cancel="formOpen = false" @submit="saveWorker" />

    <div v-if="action && actionWorker" class="modal-backdrop" @mousedown.self="closeAction">
      <section class="modal modal--small" role="dialog" aria-modal="true">
        <header class="modal__header">
          <div>
            <p class="eyebrow">Работник #{{ actionWorker.id }}</p>
            <h2 v-if="action === 'organization'">Организация</h2>
            <h2 v-else-if="action === 'fire'">Увольнение</h2>
            <h2 v-else>Индексация зарплаты</h2>
          </div>
          <button class="icon-button" @click="closeAction">×</button>
        </header>

        <form class="stack" @submit.prevent="submitAction">
          <template v-if="action === 'organization'">
            <label class="check"><input v-model="removeOrganization" type="checkbox" /><span>Удалить связь с организацией</span></label>
            <label class="field"><span>Новое название</span><input v-model="organizationName" :disabled="removeOrganization" required maxlength="1804" /></label>
          </template>
          <template v-else-if="action === 'fire'">
            <p class="muted">Название должно точно совпадать с организацией работника.</p>
            <label class="field"><span>Название организации</span><input v-model="organizationName" required maxlength="1804" /></label>
          </template>
          <template v-else>
            <p class="muted">Текущая зарплата: <b>{{ formatMoney(actionWorker.salary) }}</b></p>
            <label class="field"><span>Положительный коэффициент</span><input v-model="coefficient" required type="number" min="0.000001" step="any" /></label>
          </template>
          <footer class="modal__actions">
            <button class="button button--ghost" type="button" :disabled="mutationBusy" @click="closeAction">Отмена</button>
            <button class="button button--primary" :disabled="mutationBusy">{{ mutationBusy ? 'Выполняем…' : 'Выполнить' }}</button>
          </footer>
        </form>
      </section>
    </div>
  </div>
</template>
