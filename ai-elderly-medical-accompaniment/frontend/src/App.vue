<template>
  <router-view />
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from './store/user'

const u = useUserStore()
const r = useRouter()

onMounted(async () => {
  if (u.token) {
    try {
      await u.fetchInfo()
    } catch {
      u.logout()
      r.push('/login')
    }
  }
})
</script>