import { defineStore } from 'pinia'
import { ref } from 'vue'
import api from '../api/request'
export const useUserStore = defineStore('user', () => {
  // sessionStorage：标签页隔离，三端可同时登录互不干扰
  const token = ref(sessionStorage.getItem('token')||'')
  const user = ref(JSON.parse(sessionStorage.getItem('user')||'null'))
  const login = async d => { const r=await api.post('/auth/login',d); token.value=r.data.token; sessionStorage.setItem('token',r.data.token); user.value={id:r.data.userId,username:r.data.username,nickname:r.data.nickname,role:r.data.role,avatar:r.data.avatar}; sessionStorage.setItem('user',JSON.stringify(user.value)); return r.data }
  const logout = async () => { try{await api.post('/auth/logout')}catch{} token.value=''; user.value=null; sessionStorage.clear() }
  const fetchInfo = async () => { const r=await api.get('/auth/info'); user.value=r.data; sessionStorage.setItem('user',JSON.stringify(r.data)) }
  const setToken = (t) => { token.value = t; sessionStorage.setItem('token', t) }
  const setUser = (u) => { user.value = u; sessionStorage.setItem('user', JSON.stringify(u)) }
  return { token, user, login, logout, fetchInfo, setToken, setUser }
})