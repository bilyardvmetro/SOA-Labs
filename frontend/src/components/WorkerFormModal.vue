<script setup lang="ts">
import { reactive, ref } from 'vue'
import {
  organizationTypes,
  type OrganizationType,
  type Worker,
  type WorkerInput,
  type WorkerStatus,
} from '../types'

const props = defineProps<{
  worker?: Worker
  busy: boolean
}>()

const emit = defineEmits<{
  cancel: []
  submit: [input: WorkerInput]
}>()

function toLocalInput(value: string | null): string {
  if (!value) return ''
  const date = new Date(value)
  const offset = date.getTimezoneOffset() * 60_000
  return new Date(date.getTime() - offset).toISOString().slice(0, 16)
}

const hasOrganization = ref(props.worker?.organization != null)
const minimumDateTime = toLocalInput(new Date(Date.now() + 60_000).toISOString())
const formError = ref('')
const editableWorkerStatuses: WorkerStatus[] = [
  'RECOMMENDED_FOR_PROMOTION',
  'PROBATION',
]
const form = reactive({
  name: props.worker?.name ?? '',
  x: props.worker?.coordinates.x.toString() ?? '0',
  y: props.worker?.coordinates.y.toString() ?? '0',
  salary: props.worker?.salary?.toString() ?? '',
  startDate: toLocalInput(props.worker?.startDate ?? new Date().toISOString()),
  endDate: toLocalInput(props.worker?.endDate ?? null),
  status: (props.worker?.status ?? 'PROBATION') as WorkerStatus,
  organizationName: props.worker?.organization?.fullName ?? '',
  annualTurnover: props.worker?.organization?.annualTurnover.toString() ?? '',
  organizationType: (props.worker?.organization?.type ?? 'PUBLIC') as OrganizationType,
})

function submit() {
  formError.value = ''

  if (isChangedPastDate(form.startDate, props.worker?.startDate)) {
    formError.value = 'Дата начала работы не может находиться в прошлом.'
    return
  }
  if (form.endDate !== '' && isChangedPastDate(form.endDate, props.worker?.endDate ?? undefined)) {
    formError.value = 'Дата окончания работы не может находиться в прошлом.'
    return
  }

  const input: WorkerInput = {
    name: form.name.trim(),
    coordinates: { x: Number(form.x), y: Number(form.y) },
    salary: form.salary === '' ? null : Number(form.salary),
    startDate: new Date(form.startDate).toISOString(),
    endDate: form.endDate === '' ? null : new Date(form.endDate).toISOString(),
    status: form.status,
    organization: hasOrganization.value
      ? {
          fullName: form.organizationName.trim(),
          annualTurnover: Number(form.annualTurnover),
          type: form.organizationType,
        }
      : null,
  }
  emit('submit', input)
}

function isChangedPastDate(value: string, original?: string): boolean {
  const unchanged = original != null && value === toLocalInput(original)
  return !unchanged && new Date(value).getTime() < Date.now()
}
</script>

<template>
  <div class="modal-backdrop" @mousedown.self="emit('cancel')">
    <section class="modal" role="dialog" aria-modal="true" aria-labelledby="worker-form-title">
      <header class="modal__header">
        <div>
          <p class="eyebrow">{{ worker ? `Работник #${worker.id}` : 'Новая запись' }}</p>
          <h2 id="worker-form-title">{{ worker ? 'Редактирование' : 'Добавление работника' }}</h2>
        </div>
        <button class="icon-button" type="button" aria-label="Закрыть" @click="emit('cancel')">×</button>
      </header>

      <form class="worker-form" @submit.prevent="submit">
        <label class="field field--wide">
          <span>Имя</span>
          <input v-model="form.name" required minlength="1" autocomplete="off" />
        </label>

        <label class="field">
          <span>Координата X ≤ 202</span>
          <input v-model="form.x" required type="number" max="202" step="1" />
        </label>
        <label class="field">
          <span>Координата Y &gt; −992</span>
          <input v-model="form.y" required type="number" min="-991.999999" step="any" />
        </label>

        <label class="field">
          <span>Зарплата</span>
          <input v-model="form.salary" type="number" min="1" step="1" placeholder="Не указана" />
        </label>
        <label class="field">
          <span>Статус</span>
          <select v-model="form.status" required>
            <option v-for="status in editableWorkerStatuses" :key="status" :value="status">{{ status }}</option>
          </select>
          <small>Статус FIRED назначается только действием «Уволить» в меню работника.</small>
        </label>

        <label class="field">
          <span>Начало работы</span>
          <input v-model="form.startDate" required type="datetime-local" :min="worker ? undefined : minimumDateTime" />
        </label>
        <label class="field">
          <span>Окончание работы</span>
          <input v-model="form.endDate" type="datetime-local" :min="worker ? undefined : minimumDateTime" />
        </label>

        <label class="check field--wide">
          <input v-model="hasOrganization" type="checkbox" />
          <span>Работник относится к организации</span>
        </label>

        <template v-if="hasOrganization">
          <label class="field field--wide">
            <span>Название организации</span>
            <input v-model="form.organizationName" required maxlength="1804" />
          </label>
          <label class="field">
            <span>Годовой оборот</span>
            <input v-model="form.annualTurnover" required type="number" min="1" step="1" />
          </label>
          <label class="field">
            <span>Тип организации</span>
            <select v-model="form.organizationType" required>
              <option v-for="type in organizationTypes" :key="type" :value="type">{{ type }}</option>
            </select>
          </label>
        </template>

        <p v-if="formError" class="form-error field--wide" role="alert">{{ formError }}</p>

        <footer class="modal__actions field--wide">
          <button class="button button--ghost" type="button" :disabled="busy" @click="emit('cancel')">
            Отмена
          </button>
          <button class="button button--primary" type="submit" :disabled="busy">
            {{ busy ? 'Сохраняем…' : 'Сохранить' }}
          </button>
        </footer>
      </form>
    </section>
  </div>
</template>
