<script setup lang="ts">
import { ref } from 'vue'
import { ApiError, workerApi } from '../api'
import { workerStatuses, type EndDateGroup, type Worker, type WorkerPage, type WorkerStatus } from '../types'

const props = defineProps<{ workerBaseUrl: string }>()
const emit = defineEmits<{ error: [message: string] }>()

const sum = ref<number | null>(null)
const salaryThreshold = ref('50000')
const salaryCount = ref<number | null>(null)
const endDate = ref('')
const nullEndDate = ref(false)
const endDateCount = ref<number | null>(null)
const groups = ref<EndDateGroup[]>([])
const namePrefix = ref('')
const nameResults = ref<WorkerPage | null>(null)
const status = ref<WorkerStatus>('FIRED')
const statusResults = ref<WorkerPage | null>(null)
const busy = ref('')

function report(error: unknown) {
  emit('error', error instanceof ApiError ? error.message : 'Не удалось выполнить операцию')
}

async function run(key: string, action: () => Promise<unknown>) {
  busy.value = key
  try {
    await action()
  } catch (error) {
    report(error)
  } finally {
    busy.value = ''
  }
}

function localToIso(value: string): string {
  return new Date(value).toISOString()
}

function displayDate(value: string | null): string {
  return value ? new Date(value).toLocaleString('ru-RU') : 'Не указана'
}

function displayWorker(worker: Worker): string {
  return worker.salary == null ? 'Зарплата не указана' : `${worker.salary.toLocaleString('ru-RU')} ₽`
}
</script>

<template>
  <section class="operations-grid">
    <article class="operation-card operation-card--accent">
      <p class="eyebrow">Агрегация</p>
      <h3>Сумма зарплат</h3>
      <p class="operation-card__description">Сумма всех ненулевых зарплат в коллекции.</p>
      <strong v-if="sum !== null" class="metric">{{ sum.toLocaleString('ru-RU') }} ₽</strong>
      <button class="button button--primary" :disabled="busy === 'sum'" @click="run('sum', async () => sum = (await workerApi.sumSalary(workerBaseUrl)).sum)">
        Рассчитать
      </button>
    </article>

    <article class="operation-card">
      <p class="eyebrow">Подсчёт</p>
      <h3>Зарплата меньше порога</h3>
      <label class="field">
        <span>Порог</span>
        <input v-model="salaryThreshold" type="number" min="1" step="1" />
      </label>
      <strong v-if="salaryCount !== null" class="metric">{{ salaryCount }}</strong>
      <button class="button button--secondary" :disabled="busy === 'salary-count' || Number(salaryThreshold) < 1" @click="run('salary-count', async () => salaryCount = (await workerApi.countSalaryLess(workerBaseUrl, Number(salaryThreshold))).count)">
        Посчитать
      </button>
    </article>

    <article class="operation-card">
      <p class="eyebrow">Подсчёт</p>
      <h3>Точная дата окончания</h3>
      <label class="field">
        <span>Дата и время</span>
        <input v-model="endDate" type="datetime-local" :disabled="nullEndDate" />
      </label>
      <label class="check">
        <input v-model="nullEndDate" type="checkbox" />
        <span>Без даты окончания</span>
      </label>
      <strong v-if="endDateCount !== null" class="metric">{{ endDateCount }}</strong>
      <button class="button button--secondary" :disabled="busy === 'end-count' || (!nullEndDate && !endDate)" @click="run('end-count', async () => endDateCount = (await workerApi.countByEndDate(workerBaseUrl, nullEndDate ? null : localToIso(endDate))).count)">
        Посчитать
      </button>
    </article>

    <article class="operation-card operation-card--wide">
      <div class="operation-card__heading">
        <div>
          <p class="eyebrow">Группировка</p>
          <h3>Работники по дате окончания</h3>
        </div>
        <button class="button button--secondary" :disabled="busy === 'groups'" @click="run('groups', async () => groups = await workerApi.groupByEndDate(workerBaseUrl))">
          Получить группы
        </button>
      </div>
      <div v-if="groups.length" class="result-list">
        <div v-for="group in groups" :key="group.endDate ?? 'null'" class="result-row">
          <span>{{ displayDate(group.endDate) }}</span>
          <strong>{{ group.count }}</strong>
        </div>
      </div>
      <p v-else class="muted">Результат появится здесь.</p>
    </article>

    <article class="operation-card operation-card--wide">
      <p class="eyebrow">Поиск</p>
      <h3>Имя начинается с…</h3>
      <form class="inline-form" @submit.prevent="run('name', async () => nameResults = await workerApi.searchByName(workerBaseUrl, namePrefix, 1, 20, ['name,asc']))">
        <label class="field grow">
          <span>Префикс имени</span>
          <input v-model="namePrefix" required placeholder="Например, Ан" />
        </label>
        <button class="button button--secondary" :disabled="busy === 'name'">Найти</button>
      </form>
      <div v-if="nameResults" class="result-list">
        <div v-for="worker in nameResults.items" :key="worker.id" class="result-row result-row--worker">
          <span><b>#{{ worker.id }}</b> {{ worker.name }}</span>
          <span>{{ displayWorker(worker) }}</span>
        </div>
        <p v-if="!nameResults.items.length" class="muted">Совпадений нет.</p>
      </div>
    </article>

    <article class="operation-card operation-card--wide">
      <p class="eyebrow">Поиск</p>
      <h3>Статус выше заданного</h3>
      <form class="inline-form" @submit.prevent="run('status', async () => statusResults = await workerApi.searchByStatus(workerBaseUrl, status, 1, 20, ['status,asc']))">
        <label class="field grow">
          <span>Пороговый статус</span>
          <select v-model="status">
            <option v-for="item in workerStatuses" :key="item" :value="item">{{ item }}</option>
          </select>
        </label>
        <button class="button button--secondary" :disabled="busy === 'status'">Найти</button>
      </form>
      <div v-if="statusResults" class="result-list">
        <div v-for="worker in statusResults.items" :key="worker.id" class="result-row result-row--worker">
          <span><b>#{{ worker.id }}</b> {{ worker.name }}</span>
          <span class="status-pill">{{ worker.status }}</span>
        </div>
        <p v-if="!statusResults.items.length" class="muted">Совпадений нет.</p>
      </div>
    </article>
  </section>
</template>
