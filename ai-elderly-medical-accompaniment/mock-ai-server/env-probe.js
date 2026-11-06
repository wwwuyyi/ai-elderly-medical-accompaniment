// T-05 取证脚本：环境探测（只读）
// 用途：采集 MySQL/后端 8080/mock 9001+9002/前端 5173 的监听 PID、进程命令行、
//       mock 代码版本（sha256 + node --check）、NOTICE 中文探测。
// 用法：node mock-ai-server/env-probe.js
// 说明：仅使用 Node 内置能力；不修改任何业务代码与配置。

const { execSync } = require('child_process');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

function sh(cmd) {
  try {
    return execSync(cmd, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'pipe'] });
  } catch (e) {
    return String((e.stdout || '') + (e.stderr ? '\n[stderr] ' + e.stderr : ''));
  }
}

function line(s) { console.log(s); }
function section(t) { line('\n===== ' + t + ' ====='); }

(async () => {
  section('采集时间');
  line(new Date().toString());

  section('netstat -ano（仅 LISTENING 且端口 3306/8080/9001/9002/5173）');
  const ns = sh('netstat -ano');
  const ports = ['3306', '8080', '9001', '9002', '5173'];
  const hitLines = ns.split(/\r?\n/).filter(l => /LISTENING/.test(l) &&
    ports.some(p => new RegExp(':' + p + '\\s').test(l)));
  line(hitLines.length ? hitLines.join('\n') : '(无匹配)');
  const pidSet = new Set();
  for (const l of hitLines) {
    const m = l.trim().match(/(\d+)$/);
    if (m) pidSet.add(m[1]);
  }

  section('进程信息（tasklist / wmic）');
  for (const pid of pidSet) {
    line('--- PID ' + pid + ' ---');
    line(sh('tasklist /FI "PID eq ' + pid + '" /FO CSV /NH').trim());
    const w = sh('wmic process where processid=' + pid + ' get CommandLine /format:list');
    line(w.trim() || '(wmic 无输出)');
  }

  section('mock 代码版本');
  const serverPath = path.join(__dirname, 'server.js');
  const buf = fs.readFileSync(serverPath);
  line('server.js path = ' + serverPath);
  line('server.js sha256 = ' + crypto.createHash('sha256').update(buf).digest('hex'));
  line('server.js mtime  = ' + fs.statSync(serverPath).mtime.toString());
  line('node --check 结果：' + (sh('node --check "' + serverPath + '"').trim() || '(无输出=通过)'));

  section('HTTP 连通性探测');
  async function probe(name, url, opts) {
    const t0 = Date.now();
    try {
      const r = await fetch(url, opts);
      const body = await r.text();
      line(name + ' -> HTTP ' + r.status + ' (' + (Date.now() - t0) + ' ms), body[0..160]=' +
        JSON.stringify(body.slice(0, 160)));
      return { status: r.status, body };
    } catch (e) {
      line(name + ' -> 连接失败: ' + e.message + ' (' + (Date.now() - t0) + ' ms)');
      return { status: 0, body: '' };
    }
  }

  await probe('GET  http://127.0.0.1:8080/api/auth/captcha（后端存活）',
    'http://127.0.0.1:8080/api/auth/captcha');
  await probe('GET  http://127.0.0.1:5173/（前端存活）', 'http://127.0.0.1:5173/');
  await probe('GET  http://127.0.0.1:9001/（OCR mock 端口存活）', 'http://127.0.0.1:9001/');

  section('NOTICE 中文探测（Node UTF-8 客户端，禁止 Git Bash 直传中文）');
  const noticePrompt = 'NOTICE 科室=心内科,预约时间=2026-11-07 09:30:00,行动能力=需要搀扶,过敏史=青霉素过敏';
  line('request body = ' + JSON.stringify({ prompt: noticePrompt }));
  const nr = await probe('POST http://127.0.0.1:9002/llm/chat', 'http://127.0.0.1:9002/llm/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json; charset=utf-8' },
    body: JSON.stringify({ prompt: noticePrompt })
  });
  line('NOTICE 探测含中文标题《诊前就诊告知书》= ' + nr.body.includes('诊前就诊告知书'));
  line('NOTICE 探测含科室回填 = ' + nr.body.includes('心内科'));
  line('NOTICE 探测含预约时间回填 = ' + nr.body.includes('2026-11-07 09:30:00'));
  line('NOTICE 探测含行动能力回填 = ' + nr.body.includes('需要搀扶'));
  line('NOTICE 探测含过敏史回填 = ' + nr.body.includes('青霉素过敏'));
  line('NOTICE 探测含字符串 null = ' + nr.body.includes('null'));
})().catch(e => { console.error('env-probe 异常: ' + (e && e.stack || e)); process.exit(1); });