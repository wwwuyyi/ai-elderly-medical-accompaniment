import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
const api = axios.create({ baseURL: '/api', timeout: 15000 })
api.interceptors.request.use(c => { const t = sessionStorage.getItem('token'); if(t) c.headers.Authorization='Bearer '+t; return c })
api.interceptors.response.use(r => { if(r.data.code!==200){ElMessage.error(r.data.message||'失败');return Promise.reject(r.data)} return r.data }, e => { if(e.response?.status===401){sessionStorage.clear();router.push('/login')} const msg = e.response?.data?.message || '网络错误'; ElMessage.error(msg); return Promise.reject(e) })
export default api