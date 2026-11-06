// T-06 实测脚本（陪诊端必测 7-10）
// 用法（后端 8080、mock 9001/9002、MySQL 已启动）：
//   node mock-ai-server/evidence-t06-escort.js
// 说明：
//   1. 仅使用 Node 内置模块（fetch/FormData/Blob/child_process），所有中文请求体由 Node UTF-8 发送。
//   2. 造数据一律走既有 API；SQL 仅执行只读 SELECT（ai_task OCR 行），不写 UPDATE/DELETE/ALTER。
//   3. 必测9 连续执行 OCR，mock 9001 按调用次数交替返回 0.95 / 0.72；
//      脚本最多执行 4 次，直到同时覆盖 >=0.85 与 <0.85 两种场景（正常两次即可覆盖）。
//   4. 无需参数；如需覆盖数据库连接，可设置 DB_USER / DB_PASSWORD / DB_NAME / MYSQL_BIN 环境变量。

const BASE = 'http://127.0.0.1:8080';
const { spawnSync } = require('child_process');

function ts() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}.${String(d.getMilliseconds()).padStart(3, '0')}`;
}
function fmtLocal(d) {
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`;
}
function today() {
  const d = new Date();
  const p = n => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())}`;
}
function shortToken(t) { return t ? t.slice(0, 24) + '...(' + t.length + ' chars)' : String(t); }
function out(label) { console.log(`\n===== ${label} =====`); }
function sleep(ms) { return new Promise(resolve => setTimeout(resolve, ms)); }

async function call(method, path, body, token, opts = {}) {
  const headers = { 'Content-Type': 'application/json; charset=utf-8' };
  if (token) headers.Authorization = 'Bearer ' + token;
  const started = Date.now();
  const t0 = ts();
  const res = await fetch(BASE + path, {
    method, headers,
    body: body === undefined ? undefined : JSON.stringify(body)
  });
  const text = await res.text();
  const ms = Date.now() - started;
  let json = null;
  try { json = JSON.parse(text); } catch (e) {}
  console.log(`[${t0}] ${method} ${path}`);
  if (body !== undefined) console.log('  request body: ' + JSON.stringify(body));
  console.log(`  -> HTTP ${res.status}  (${ms} ms)`);
  const limit = opts.full ? 100000 : 5000;
  console.log('  body: ' + (text.length > limit ? text.slice(0, limit) + '...[truncated,total=' + text.length + ']' : text));
  return { status: res.status, text, json, ms, t0 };
}

async function login(username, password, role) {
  return call('POST', '/api/auth/login', {
    username, password, captcha: '0000', captchaKey: 'test-bypass', role
  });
}

async function uploadFile(token, filename, bytes, mime) {
  const fd = new FormData();
  fd.append('file', new Blob([bytes], { type: mime }), filename);
  const started = Date.now();
  const t0 = ts();
  const res = await fetch(BASE + '/api/file/upload', {
    method: 'POST',
    headers: { Authorization: 'Bearer ' + token },
    body: fd
  });
  const text = await res.text();
  console.log(`[${t0}] POST /api/file/upload`);
  console.log(`  request: multipart/form-data file=${filename} (${bytes.length} bytes, ${mime})`);
  console.log(`  -> HTTP ${res.status}  (${Date.now() - started} ms)`);
  console.log('  body: ' + text);
  let json = null; try { json = JSON.parse(text); } catch (e) {}
  return { status: res.status, text, json };
}

// ===== 只读 SQL（仅 SELECT ai_task OCR 行） =====
let MYSQL_BIN_CACHE;
function resolveMysql() {
  if (MYSQL_BIN_CACHE !== undefined) return MYSQL_BIN_CACHE;
  const candidates = [];
  if (process.env.MYSQL_BIN) candidates.push(process.env.MYSQL_BIN);
  candidates.push('D:/MySQL/mysql-8.0.40-winx64/bin/mysql.exe');
  candidates.push('mysql');
  for (const bin of candidates) {
    const r = spawnSync(bin, ['--version'], { encoding: 'utf8' });
    if (r.status === 0) {
      MYSQL_BIN_CACHE = bin;
      console.log('[SQL] 使用 mysql 客户端: ' + bin + ' -> ' + (r.stdout || '').trim());
      return bin;
    }
  }
  MYSQL_BIN_CACHE = null;
  console.log('[SQL-UNAVAILABLE] 未找到可用 mysql 客户端，已尝试: ' + candidates.join(' | '));
  return null;
}
function sql(query) {
  const bin = resolveMysql();
  if (!bin) return null;
  const r = spawnSync(bin, [
    '-u' + (process.env.DB_USER || 'root'),
    '--default-character-set=utf8mb4',
    '-t', '-e', query,
    process.env.DB_NAME || 'escort_db'
  ], {
    encoding: 'utf8',
    env: Object.assign({}, process.env, { MYSQL_PWD: process.env.DB_PASSWORD || 'root' })
  });
  console.log('SQL> ' + query);
  if (r.status !== 0) {
    console.log('[SQL-ERROR] status=' + r.status);
    console.log((r.stdout || '') + (r.stderr || ''));
    return null;
  }
  const text = (r.stdout || '').trim();
  console.log(text || '[SQL-EMPTY] 查询成功但无输出');
  return text;
}

async function getOrder(orderId, token, label) {
  const r = await call('GET', '/api/order/' + orderId, undefined, token);
  const d = (r.json && r.json.data) || {};
  console.log(`  [${label}] status=${d.status} payStatus=${d.payStatus} escortId=${d.escortId}`);
  return d;
}

async function createOrder(token, elderlyId, amount, department, appointmentTime) {
  const body = {
    requestId: 'T06-' + Date.now() + '-' + Math.floor(Math.random() * 100000),
    elderlyId, hospitalId: 1, department,
    serviceType: 'BASIC', appointmentTime, duration: 2, amount
  };
  const r = await call('POST', '/api/order', body, token);
  return (r.json && r.json.data) || {};
}

async function getDocuments(recordId, token) {
  const r = await call('GET', '/api/record/' + recordId + '/documents', undefined, token);
  return (r.json && r.json.data) || [];
}
async function findDoc(recordId, docId, token, label) {
  const list = await getDocuments(recordId, token);
  const d = list.find(x => x.id === docId) || null;
  console.log(`  [${label}] document id=${d && d.id} ocrStatus=${d && d.ocrStatus}` +
              ` ocrConfidence=${d && d.ocrConfidence} manualConfirmed=${d && d.manualConfirmed}`);
  return d;
}

async function pollOcr(taskId, token, maxMs = 30000) {
  const t0 = Date.now();
  let last = null;
  while (Date.now() - t0 < maxMs) {
    const r = await call('GET', '/api/ai/ocr/task/' + taskId, undefined, token);
    last = r;
    const d = (r.json && r.json.data) || {};
    console.log('  [poll] task.status=' + d.status + ' ocrConfidence=' + d.ocrConfidence +
                ' manualConfirmed=' + d.manualConfirmed + ' errorMsg=' + d.errorMsg);
    if (d.status === 'SUCCESS' || d.status === 'FAILED' || d.status === 'DEGRADED') return last;
    await sleep(500);
  }
  console.log('  [poll] 轮询超时（' + maxMs + 'ms），最后一次响应已打印');
  return last;
}

(async () => {
  // ========== 登录准备 ==========
  out('必测7 陪诊师登录 POST /api/auth/login（13800002222 / Test1234 / ESCORT）');
  let r = await login('13800002222', 'Test1234', 'ESCORT');
  const escortToken = r.json && r.json.data && r.json.data.token;
  const escortUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) +
              ' data.role=' + (r.json && r.json.data && r.json.data.role) +
              ' data.userId=' + escortUserId +
              ' token(first24)=' + shortToken(escortToken));
  if (!escortToken) throw new Error('陪诊师登录失败，停止后续步骤');

  out('前置：家属登录（13800001111）');
  r = await login('13800001111', 'Test1234', 'FAMILY');
  const familyToken = r.json && r.json.data && r.json.data.token;
  console.log('  code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' token(first24)=' + shortToken(familyToken));
  if (!familyToken) throw new Error('家属登录失败，停止后续步骤');

  out('前置：管理员登录（admin）');
  r = await login('admin', 'admin123', 'ADMIN');
  const adminToken = r.json && r.json.data && r.json.data.token;
  console.log('  code=' + (r.json && r.json.code) + ' role=' + (r.json && r.json.data && r.json.data.role) +
              ' token(first24)=' + shortToken(adminToken));
  if (!adminToken) throw new Error('管理员登录失败，停止后续步骤');

  out('前置：老人档案列表 GET /api/elderly/page?current=1&size=10');
  r = await call('GET', '/api/elderly/page?current=1&size=10', undefined, familyToken);
  let elderlyId = r.json && r.json.data && r.json.data.records && r.json.data.records[0] && r.json.data.records[0].id;
  console.log('  使用老人档案 id=' + elderlyId);
  if (!elderlyId) {
    out('前置：无老人档案，走 API 新增 POST /api/elderly');
    const elderName = 'T06测试老人' + Date.now().toString().slice(-6);
    await call('POST', '/api/elderly', {
      name: elderName, gender: 'MALE', birthDate: '1952-06-18',
      idCard: '330102195206181234', phone: '13900002222',
      address: '广州市测试路T06号', emergencyContact: '13800001111',
      emergencyName: '测试家属', mobility: 'ASSIST',
      allergyHistory: '青霉素过敏', chronicDisease: '高血压', remark: 'T06实测新增'
    }, familyToken);
    r = await call('GET', '/api/elderly/page?current=1&size=20', undefined, familyToken);
    const list = (r.json && r.json.data && r.json.data.records) || [];
    const created = list.find(x => x.name === elderName);
    elderlyId = created && created.id;
    console.log('  新增老人回查 id=' + elderlyId);
  }

  out('前置：费用试算 POST /api/order/estimate');
  r = await call('POST', '/api/order/estimate', { serviceType: 'BASIC', duration: 2 }, familyToken);
  const amount = r.json && r.json.data && r.json.data.totalAmount;
  console.log('  totalAmount=' + amount);
  if (amount === undefined || amount === null) throw new Error('费用试算失败，停止后续步骤');

  // ========== 必测8：接单池 -> 接单 -> 开始 -> 结束 ==========
  out('必测8 造单并完成 DISPATCHED -> ACCEPTED -> IN_SERVICE -> SERVICE_FINISHED');
  const appt8 = fmtLocal(new Date(Date.now() + 10 * 60 * 1000));
  const orderA = await createOrder(familyToken, elderlyId, amount, '心内科', appt8);
  console.log('  orderA.id=' + orderA.id + ' orderNo=' + orderA.orderNo +
              ' appointmentTime=' + appt8 + ' amount=' + orderA.amount);
  if (!orderA.id) throw new Error('orderA 创建失败，停止必测8');

  await call('POST', '/api/order/' + orderA.id + '/dispatch?escortId=' + escortUserId, undefined, adminToken);
  await getOrder(orderA.id, familyToken, '派单后');

  out('必测8a 接单池 GET /api/escort/order/pool');
  r = await call('GET', '/api/escort/order/pool', undefined, escortToken);
  const pool = (r.json && r.json.data) || [];
  const inPool = Array.isArray(pool) && pool.some(x => x.id === orderA.id);
  console.log('  接单池包含 orderA=' + inPool + ' 池内条数=' + (Array.isArray(pool) ? pool.length : 'N/A'));

  out('必测8b 陪诊师接单 POST /api/order/{id}/accept');
  await call('POST', '/api/order/' + orderA.id + '/accept', undefined, escortToken);
  await getOrder(orderA.id, familyToken, '接单后');

  out('必测8c 开始服务 POST /api/order/{id}/start');
  await call('POST', '/api/order/' + orderA.id + '/start', undefined, escortToken);
  await getOrder(orderA.id, familyToken, '开始服务后');

  out('必测8d 结束服务 POST /api/order/{id}/finish');
  await call('POST', '/api/order/' + orderA.id + '/finish', undefined, escortToken);
  const orderAAfterFinish = await getOrder(orderA.id, familyToken, '结束服务后');

  // ========== 必测9：上传单据 -> OCR -> 轮询 -> 确认 ==========
  out('必测9 造一个 IN_SERVICE 订单（供 OCR 可见性校验）');
  const appt9 = fmtLocal(new Date(Date.now() + 10 * 60 * 1000));
  const orderB = await createOrder(familyToken, elderlyId, amount, '心内科', appt9);
  console.log('  orderB.id=' + orderB.id + ' orderNo=' + orderB.orderNo +
              ' appointmentTime=' + appt9 + ' amount=' + orderB.amount);
  if (!orderB.id) throw new Error('orderB 创建失败，停止必测9');
  await call('POST', '/api/order/' + orderB.id + '/dispatch?escortId=' + escortUserId, undefined, adminToken);
  await call('POST', '/api/order/' + orderB.id + '/accept', undefined, escortToken);
  await call('POST', '/api/order/' + orderB.id + '/start', undefined, escortToken);
  const orderBAfterStart = await getOrder(orderB.id, familyToken, 'OCR前置订单开始服务后');

  out('必测9a 家属创建就医档案 POST /api/record');
  r = await call('POST', '/api/record', {
    elderlyId,
    orderId: orderB.id,
    hospitalId: 1,
    department: '心内科',
    visitDate: today(),
    chiefComplaint: 'T06 陪诊端 OCR 实测',
    diagnosis: '高血压',
    prescription: '按医嘱服药',
    remark: 'T06 OCR 取证'
  }, familyToken);
  const recordId = r.json && r.json.data && r.json.data.id;
  console.log('  就医档案 id=' + recordId);
  if (!recordId) throw new Error('就医档案创建失败，停止必测9');

  out('必测9b 上传单据图片 POST /api/file/upload（multipart）');
  const png = Buffer.from(
    'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Y9Zl1sAAAAASUVORK5CYII=',
    'base64'
  );
  const up = await uploadFile(escortToken, 't06-ocr.png', png, 'image/png');
  const imageUrl = up.json && up.json.data;
  console.log('  imageUrl=' + imageUrl);
  if (!imageUrl) throw new Error('单据图片上传失败，停止 OCR 流程');

  const seen = { high: null, low: null };
  const attempts = [];
  for (let attempt = 1; attempt <= 4 && (!seen.high || !seen.low); attempt++) {
    out('必测9 OCR 第 ' + attempt + ' 次：上传单据 -> 发起识别 -> 轮询 -> 确认');
    r = await call('POST', '/api/record/' + recordId + '/document?docType=EXAM&imageUrl=' +
      encodeURIComponent(imageUrl), undefined, escortToken);
    const documentId = r.json && r.json.data && r.json.data.id;
    console.log('  documentId=' + documentId + ' 初始 ocrStatus=' + (r.json && r.json.data && r.json.data.ocrStatus));
    if (!documentId) { console.log('  [跳过] 单据上传失败，本次不计入覆盖'); continue; }

    r = await call('POST', '/api/ai/ocr/recognize', { documentId }, escortToken);
    const taskId = r.json && r.json.data && r.json.data.taskId;
    console.log('  OCR taskId=' + taskId + ' 初始 task.status=' + (r.json && r.json.data && r.json.data.status));
    if (!taskId) { console.log('  [跳过] OCR 任务提交失败，本次不计入覆盖'); continue; }

    const poll = await pollOcr(taskId, escortToken);
    const pollData = (poll && poll.json && poll.json.data) || {};
    await sleep(500);

    const doc = await findDoc(recordId, documentId, escortToken, 'OCR后');
    const beforeConfirmed = doc && doc.manualConfirmed;
    const confidence = Number(doc && doc.ocrConfidence);

    out('必测9 确认 OCR POST /api/ai/ocr/task/{taskId}/confirm');
    await call('POST', '/api/ai/ocr/task/' + taskId + '/confirm', {}, escortToken);
    const docAfter = await findDoc(recordId, documentId, escortToken, '确认后');

    out('必测9 ai_task OCR 行只读 SQL（taskId=' + taskId + '）');
    const sqlText = sql(
      'SELECT id, task_type, biz_id, status, retry_count, cost_ms, response_digest, error_msg ' +
      'FROM ai_task WHERE id = ' + Number(taskId) + ';'
    );

    const item = {
      attempt,
      documentId,
      taskId,
      ocrStatus: doc && doc.ocrStatus,
      ocrConfidence: doc && doc.ocrConfidence,
      manualConfirmedBeforeConfirm: beforeConfirmed,
      manualConfirmedAfterConfirm: docAfter && docAfter.manualConfirmed,
      taskStatus: pollData.status,
      taskErrorMsg: pollData.errorMsg,
      aiTaskRow: sqlText
    };
    attempts.push(item);
    // 仅当 OCR 真正 SUCCESS 且置信度有效时才计入覆盖统计，避免失败场景被误分类
    if (doc && doc.ocrStatus === 'SUCCESS' && Number.isFinite(confidence)) {
      if (confidence >= 0.85) seen.high = item;
      else seen.low = item;
    } else {
      console.log('  本次 OCR 未达到 SUCCESS 或置信度缺失，不计入覆盖统计');
    }
  }
  console.log('\n  置信度覆盖汇总：');
  console.log('  >=0.85 场景 = ' + (seen.high ? 'attempt#' + seen.high.attempt + ' confidence=' + seen.high.ocrConfidence : '未覆盖'));
  console.log('  <0.85 场景  = ' + (seen.low ? 'attempt#' + seen.low.attempt + ' confidence=' + seen.low.ocrConfidence : '未覆盖'));

  // ========== 必测10：待审核账号认证申请流程 ==========
  out('必测10 待审核账号登录 POST /api/auth/login（13800003333 / Test1234 / ESCORT）');
  r = await login('13800003333', 'Test1234', 'ESCORT');
  const pendingToken = r.json && r.json.data && r.json.data.token;
  const pendingUserId = r.json && r.json.data && r.json.data.userId;
  console.log('  code=' + (r.json && r.json.code) +
              ' data.role=' + (r.json && r.json.data && r.json.data.role) +
              ' data.userId=' + pendingUserId +
              ' token(first24)=' + shortToken(pendingToken));
  if (!pendingToken) {
    console.log('  待审核账号登录失败，跳过认证申请提交与回查');
  } else {
    out('必测10a 提交资质认证 POST /api/escort/apply');
    await call('POST', '/api/escort/apply', {
      realName: '王芳',
      gender: 'FEMALE',
      birthDate: '1992-07-20',
      idCard: '440301199207201234',
      phone: '13700137001',
      serviceCity: '深圳',
      serviceArea: '南浦区,福安区',
      intro: '2年护理经验，持护士资格证',
      materials: '[]'
    }, pendingToken);

    out('必测10b 回查认证状态 GET /api/escort/apply/status');
    r = await call('GET', '/api/escort/apply/status', undefined, pendingToken);
    const applyStatus = (r.json && r.json.data) || {};
    console.log('  关键字段: auditStatus=' + applyStatus.auditStatus +
                ' auditRemark=' + applyStatus.auditRemark +
                ' createTime=' + applyStatus.createTime);
  }

  console.log('\n===== 关键ID汇总 =====');
  console.log(JSON.stringify({
    escortUserId,
    elderlyId,
    orderA: { id: orderA.id, orderNo: orderA.orderNo, finalStatus: orderAAfterFinish && orderAAfterFinish.status },
    orderB: { id: orderB.id, orderNo: orderB.orderNo, status: orderBAfterStart && orderBAfterStart.status },
    recordId,
    imageUrl,
    ocrAttempts: attempts.map(x => ({
      attempt: x.attempt,
      documentId: x.documentId,
      taskId: x.taskId,
      ocrStatus: x.ocrStatus,
      ocrConfidence: x.ocrConfidence,
      manualConfirmedBeforeConfirm: x.manualConfirmedBeforeConfirm,
      manualConfirmedAfterConfirm: x.manualConfirmedAfterConfirm
    }))
  }, null, 2));
})().catch(e => { console.error('实测脚本异常: ' + (e && e.stack || e)); process.exit(1); });