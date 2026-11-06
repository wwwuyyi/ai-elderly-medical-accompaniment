<template>
<div style="padding:20px;background:#f5f7fa;min-height:100vh">
  <h2 style="font-size:22px;color:#2F5AA8;margin-bottom:20px">消息通知</h2>
  <div v-if="list.length===0" style="text-align:center;padding:60px;color:#999;font-size:18px">暂无通知</div>
  <el-card v-for="n in list" :key="n.id" :style="{marginBottom:'12px',cursor:'pointer',border:n.isRead?'1px solid #ddd':'2px solid #2F5AA8'}" @click="read(n)">
    <div style="display:flex;justify-content:space-between;align-items:center">
      <div><div style="font-size:18px;font-weight:bold">{{n.title}}</div><div style="font-size:16px;color:#666;margin-top:6px">{{n.content}}</div><div style="font-size:14px;color:#999;margin-top:4px">{{n.createTime}}</div></div>
      <el-tag v-if="!n.isRead" type="danger" size="small">未读</el-tag></div>
  </el-card>
  <div style="text-align:center;margin-top:16px"><el-button size="large" @click="markAll" :disabled="list.length===0">全部已读</el-button></div>
  <el-pagination :current-page="current" :total="total" :page-size="size" @current-change="p=>{current=p;load()}" layout="prev,next" style="text-align:center;margin-top:12px"/>
</div></template>
<script setup>
import { ref, onMounted } from 'vue'; import api from '@/api/request'; import { ElMessage } from 'element-plus'
const list=ref([]); const current=ref(1); const size=ref(20); const total=ref(0)
const load=async()=>{const r=await api.get('/notification/page',{current:current.value,size:size.value}).catch(()=>{}); if(r){list.value=r.data.records;total.value=r.data.total}}
const read=async n=>{if(!n.isRead){await api.put(`/notification/${n.id}/read`);n.isRead=1;ElMessage.success('已读')}}
const markAll=async()=>{await api.put('/notification/read-all');list.value.forEach(n=>n.isRead=1);ElMessage.success('全部已读')}
onMounted(load)
</script>