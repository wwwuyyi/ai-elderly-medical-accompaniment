import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/login', name:'Login', component: ()=>import('../views/Login.vue'), meta:{public:true} },
  { path: '/register', name:'Register', component: ()=>import('../views/Register.vue'), meta:{public:true} },
  { path: '/family/home', name:'FamilyHome', component: ()=>import('../views/family/Home.vue'), meta:{role:'FAMILY'} },
  { path: '/family/elderly', name:'ElderlyList', component: ()=>import('../views/family/ElderlyList.vue'), meta:{role:'FAMILY'} },
  { path: '/family/elderly/add', name:'ElderlyAdd', component: ()=>import('../views/family/ElderlyEdit.vue'), meta:{role:'FAMILY'} },
  { path: '/family/elderly/:id', name:'ElderlyEdit', component: ()=>import('../views/family/ElderlyEdit.vue'), meta:{role:'FAMILY'} },
  { path: '/family/order/create', name:'OrderCreate', component: ()=>import('../views/family/OrderCreate.vue'), meta:{role:'FAMILY'} },
  { path: '/family/orders', name:'FamilyOrders', component: ()=>import('../views/family/Orders.vue'), meta:{role:'FAMILY'} },
  { path: '/family/order/:id', name:'FamilyOrderDetail', component: ()=>import('../views/family/OrderDetail.vue'), meta:{role:'FAMILY'} },
  { path: '/family/evaluate/:id', name:'Evaluate', component: ()=>import('../views/family/Evaluate.vue'), meta:{role:'FAMILY'} },
  { path: '/family/records', name:'FamilyRecords', component: ()=>import('../views/family/Records.vue'), meta:{role:'FAMILY'} },
  { path: '/family/record/:id', name:'RecordDetail', component: ()=>import('../views/family/RecordDetail.vue'), meta:{role:'FAMILY'} },
  { path: '/family/notice/:id', name:'NoticeView', component: ()=>import('../views/family/NoticeView.vue'), meta:{role:'FAMILY'} },
  { path: '/family/ai-chat', name:'AiChat', component: ()=>import('../views/family/AiChat.vue'), meta:{role:'FAMILY'} },
  { path: '/family/ai-chat/:sessionId', name:'AiChatDetail', component: ()=>import('../views/family/AiChatDetail.vue'), meta:{role:'FAMILY'} },
  { path: '/escort/home', name:'EscortHome', component: ()=>import('../views/escort/Home.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/apply', name:'EscortApply', component: ()=>import('../views/escort/Apply.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/pool', name:'OrderPool', component: ()=>import('../views/escort/OrderPool.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/orders', name:'EscortOrders', component: ()=>import('../views/escort/Orders.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/order/:id', name:'EscortOrderDetail', component: ()=>import('../views/escort/OrderDetail.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/ocr/:taskId', name:'OcrConfirm', component: ()=>import('../views/escort/OcrConfirm.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/statistics', name:'EscortStats', component: ()=>import('../views/escort/Statistics.vue'), meta:{role:'ESCORT'} },
  { path: '/escort/profile', name:'EscortProfile', component: ()=>import('../views/Profile.vue'), meta:{role:'ESCORT'} },
  { path: '/family/profile', name:'FamilyProfile', component: ()=>import('../views/Profile.vue'), meta:{role:'FAMILY'} },
  { path: '/admin/profile', name:'AdminProfile', component: ()=>import('../views/Profile.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/dashboard', name:'AdminDashboard', component: ()=>import('../views/admin/Dashboard.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/audit', name:'AdminAudit', component: ()=>import('../views/admin/Audit.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/users', name:'AdminUsers', component: ()=>import('../views/admin/Users.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/orders', name:'AdminOrders', component: ()=>import('../views/admin/Orders.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/evaluations', name:'AdminEvaluations', component: ()=>import('../views/admin/Evaluations.vue'), meta:{role:'ADMIN'} },
  { path: '/admin/record-preview', name:'AdminRecordPreview', component: ()=>import('../views/admin/RecordPreview.vue'), meta:{role:'ADMIN'} },
  { path: '/family/notify', name: 'FamilyNotify', component: () => import('../views/Notify.vue'), meta: { role: 'FAMILY' } },
  { path: '/escort/notify', name: 'EscortNotify', component: () => import('../views/Notify.vue'), meta: { role: 'ESCORT' } },
  { path: '/admin/notify', name: 'AdminNotify', component: () => import('../views/Notify.vue'), meta: { role: 'ADMIN' } },
  { path: '/', redirect: '/login' }
]

const router = createRouter({ history: createWebHistory(), routes })

router.beforeEach((to, from, next) => {
  const t = sessionStorage.getItem('token')
  const u = JSON.parse(sessionStorage.getItem('user')||'null')
  if (to.meta.public) { next(); return }
  if (!t) { next('/login'); return }
  if (to.meta.role && u?.role !== to.meta.role) {
    if (u?.role === 'FAMILY') next('/family/home')
    else if (u?.role === 'ESCORT') next('/escort/home')
    else if (u?.role === 'ADMIN') next('/admin/dashboard')
    return
  }
  next()
})

export default router