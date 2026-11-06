<template>
  <el-form
    ref="studentForm"
    :model="studentBeingEdited"
    :rules="rules"
    label-width="110px"
    style="padding-right: 30px"
    label-position="left"
  >
    <el-form-item label="Id:">
      <el-input v-model="studentBeingEdited.id" disabled></el-input>
    </el-form-item>
    <el-form-item label="Username:" prop="username">
      <el-input v-model="studentBeingEdited.username"></el-input>
    </el-form-item>
    <el-form-item label="Status:" prop="enabled">
      <!-- An active student is deactivated through its own confirmed action (UC-STU-deactivate-student). The switch
           stays only while she is deactivated, as the temporary way to reactivate her until UC-STU-reactivate-student
           is built and replaces it. -->
      <el-switch
        v-if="wasDeactivated === true"
        v-model="studentBeingEdited.enabled"
        inline-prompt
        active-text="Enabled"
        inactive-text="Disabled"
        style="--el-switch-on-color: #13ce66; --el-switch-off-color: #ff4949"
      />
      <template v-else-if="wasDeactivated === false">
        <el-tag type="success">Enabled</el-tag>
        <el-button
          type="danger"
          plain
          style="margin-left: 12px"
          :loading="deactivating"
          data-cy="student-deactivate"
          @click="confirmAndDeactivate()"
        >
          Deactivate
        </el-button>
      </template>
    </el-form-item>
    <el-form-item label="First Name:" prop="firstName">
      <el-input v-model="studentBeingEdited.firstName"></el-input>
    </el-form-item>
    <el-form-item label="Last Name:" prop="lastName">
      <el-input v-model="studentBeingEdited.lastName"></el-input>
    </el-form-item>
    <el-form-item label="Email:" prop="email">
      <el-input v-model="studentBeingEdited.email"></el-input>
    </el-form-item>
    <el-form-item label="Team:" prop="teamName">
      <el-input v-model="studentBeingEdited.teamName" disabled></el-input>
    </el-form-item>
    <div style="display: flex; justify-content: end">
      <el-button @click="$emit('close-dialog')"> Cancel </el-button>
      <el-button type="primary" @click="updateExistingStudent()"> Confirm </el-button>
    </div>
  </el-form>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus'
import type { Student } from '@/apis/student/types'
import type { FormInstance } from 'element-plus'
import { deactivateStudent, findStudentById, updateStudent } from '@/apis/student'
import { h, ref, onMounted } from 'vue'

const { studentId } = defineProps(['studentId'])
const emit = defineEmits(['close-dialog'])

// Note that the structure of the userData is NOT the same as the User interface since the roles are stored as an array
const studentBeingEdited = ref<Student>({
  // Data of the user being added or edited
  id: NaN,
  username: '',
  enabled: false,
  firstName: '',
  lastName: '',
  email: ''
})

const studentForm = ref<FormInstance>() // Form reference

// Whether the student was already deactivated when she was loaded: null until she is loaded
const wasDeactivated = ref<boolean | null>(null)
const deactivating = ref(false) // A deactivation request is in flight

// Validation rules
const rules = {
  username: [
    { required: true, message: 'Please input your email as your username', trigger: 'blur' },
    {
      pattern: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
      message: 'Enter your email as username.',
      trigger: 'blur'
    }
  ],
  enabled: [{ required: true, message: 'Please select the status of the user.', trigger: 'blur' }],
  firstName: [
    { required: true, message: 'Please provide the first name of the user.', trigger: 'blur' }
  ],
  lastName: [
    { required: true, message: 'Please provide the last name of the user.', trigger: 'blur' }
  ],
  email: [
    { required: true, message: 'Please provide the email of the user.', trigger: 'blur' },
    { type: 'email', message: 'Please provide a valid email.', trigger: 'blur' }
  ]
}

async function loadStudent(studentId: number) {
  // Load the user to be edited
  const result = await findStudentById(studentId)

  studentBeingEdited.value.id = result.data.id as number
  studentBeingEdited.value.username = result.data.username
  studentBeingEdited.value.enabled = result.data.enabled
  studentBeingEdited.value.firstName = result.data.firstName
  studentBeingEdited.value.lastName = result.data.lastName
  studentBeingEdited.value.email = result.data.email
  studentBeingEdited.value.teamName = result.data.teamName
  wasDeactivated.value = !result.data.enabled
}

// Step 4 of UC-STU-deactivate-student: the consequences, the warning, and the question
function consequencesOfDeactivation() {
  return h('div', [
    h(
      'p',
      `You are about to deactivate ${studentBeingEdited.value.firstName} ${studentBeingEdited.value.lastName}.`
    ),
    h('ul', { style: 'padding-left: 20px; margin: 8px 0' }, [
      h(
        'li',
        'She will no longer be able to log in, or to submit weekly activity reports and peer evaluations.'
      ),
      h('li', 'Her teammates will no longer be able to submit new peer evaluations of her.'),
      h('li', 'Her account and her previously submitted work are kept in the system.'),
      h('li', 'This is not a deletion: her account can be reactivated later.')
    ]),
    h('p', { style: 'font-weight: bold' }, 'Do you want to deactivate this student?')
  ])
}

async function confirmAndDeactivate() {
  try {
    await ElMessageBox.confirm(consequencesOfDeactivation(), 'Deactivate student', {
      type: 'warning',
      confirmButtonText: 'Deactivate',
      cancelButtonText: 'Cancel',
      confirmButtonClass: 'el-button--danger deactivate-student-confirm',
      cancelButtonClass: 'deactivate-student-cancel',
      customClass: 'deactivate-student-dialog'
    })
  } catch {
    return // 5a: the actor cancelled, so nothing is sent and the student stays active
  }

  deactivating.value = true
  try {
    await deactivateStudent(studentBeingEdited.value.id as number) // The same success if she was already deactivated (6a)
    ElMessage.success('Student deactivated successfully') // Step 6
    emit('close-dialog')
  } finally {
    deactivating.value = false
  }
}

onMounted(() => {
  loadStudent(studentId) // Load the artifact to be edited
})

async function updateExistingStudent() {
  await studentForm.value!.validate() // Validate the form

  const updatedStudent: Student = {
    id: studentBeingEdited.value.id,
    username: studentBeingEdited.value.username,
    enabled: studentBeingEdited.value.enabled,
    firstName: studentBeingEdited.value.firstName,
    lastName: studentBeingEdited.value.lastName,
    email: studentBeingEdited.value.email
  }
  console.log(updatedStudent)
  // Call the API to update the user
  await updateStudent(updatedStudent)

  ElMessage.success('Student updated successfully')
  emit('close-dialog')
}
</script>

<style scoped lang="scss"></style>
