// T-05 取证脚本：必测16 真实 SQL 查询（只读）
// 用法：node mock-ai-server/evidence-sql.js [noticeContentId]
// 说明：通过 mysql 客户端查询 escort_db，输出 ai_task / ai_generated_content 原始行；
//       若传入 noticeContentId，则对该内容计算 SHA-256 前16位，与 ai_task.response_digest 对照。
//       仅使用 Node 内置能力，不修改任何业务数据与配置。

const { spawnSync } = require('child_process');
const crypto = require('crypto');

const DB = process.env.DB_NAME || 'escort_db';
const USER = process.env.DB_USER || 'root';
const PWD = process.env.DB_PASSWORD || 'root';
const noticeContentId = process.argv[2];

// SC-01：优先环境变量 MYSQL_BIN，其次固定绝对路径，最后 PATH 探测；
// 客户端不可用时显式报错退出，禁止静默输出“(无输出)”。
function resolveMysql() {
  const candidates = [];
  if (process.env.MYSQL_BIN) candidates.push(process.env.MYSQL_BIN);
  candidates.push('D:/MySQL/mysql-8.0.40-winx64/bin/mysql.exe');
  candidates.push('mysql');
  for (const bin of candidates) {
    const r = spawnSync(bin, ['--version'], { encoding: 'utf8' });
    if (r.status === 0) {
      console.log('[SC-01] mysql client = ' + bin + ' -> ' + (r.stdout || '').trim());
      return bin;
    }
  }
  console.error('[SC-01] 错误：未找到可用的 mysql 客户端，已尝试：' + candidates.join(' | '));
  process.exit(1);
}
const MYSQL_BIN = resolveMysql();

function mysql(sql) {
  const r = spawnSync(MYSQL_BIN, ['-u' + USER, '--default-character-set=utf8mb4', '-t', '-e', sql, DB], {
    encoding: 'utf8',
    env: Object.assign({}, process.env, { MYSQL_PWD: PWD })
  });
  console.log('SQL> ' + sql);
  if (r.status !== 0) {
    console.error('[SC-01] SQL 执行失败，status=' + r.status);
    console.error((r.stdout || '') + (r.stderr || ''));
    process.exit(1);
  }
  const out = (r.stdout || '').trim();
  console.log(out || '[SQL-EMPTY] 查询成功但无输出');
  console.log('');
  return out;
}

function mysqlRaw(sql) {
  const r = spawnSync(MYSQL_BIN, ['-u' + USER, '--default-character-set=utf8mb4', '-N', '-B', '-e', sql, DB], {
    encoding: 'utf8',
    env: Object.assign({}, process.env, { MYSQL_PWD: PWD })
  });
  if (r.status !== 0) {
    console.error('[SC-01] SQL 执行失败（raw），status=' + r.status);
    console.error((r.stdout || '') + (r.stderr || ''));
    process.exit(1);
  }
  return (r.stdout || '').trim();
}

console.log('===== 数据源 =====');
mysql('SELECT VERSION() AS mysql_version, DATABASE() AS db;');

console.log('===== 必测16-a ai_task 最近 20 行 =====');
mysql("SELECT id, task_type, biz_id, status, retry_count, cost_ms, response_digest, error_msg, start_time, end_time " +
      "FROM ai_task ORDER BY id DESC LIMIT 20;");

console.log('===== 必测16-b ai_task 与 ai_generated_content 关联 =====');
mysql("SELECT t.id AS task_id, t.task_type, t.biz_id, t.status, t.retry_count, t.cost_ms, t.response_digest, " +
      "c.id AS content_id, c.content_type, c.biz_id AS content_biz_id, LEFT(c.content, 80) AS content_head " +
      "FROM ai_task t LEFT JOIN ai_generated_content c ON c.task_id = t.id " +
      "ORDER BY t.id DESC LIMIT 20;");

console.log('===== 必测16-c 失败/降级计数 =====');
mysql("SELECT status, COUNT(*) AS cnt FROM ai_task GROUP BY status;");

console.log('===== 必测16-d 确认无 prompt 明文落库（应为 0 行） =====');
mysql("SELECT id, request_digest, response_digest FROM ai_task " +
      "WHERE request_digest LIKE '%科室=%' OR request_digest LIKE '%生成诊前告知书%' " +
      "OR request_digest LIKE '%过敏史=%' OR response_digest LIKE '%诊前就诊告知书%';");

if (noticeContentId) {
  console.log('===== 必测16-e SHA-256 摘要核对（contentId=' + noticeContentId + '） =====');
  const row = mysqlRaw("SELECT c.content, t.response_digest FROM ai_generated_content c " +
                       "LEFT JOIN ai_task t ON t.id = c.task_id WHERE c.id = " +
                       Number(noticeContentId) + " LIMIT 1;");
  const parts = row.split('\t');
  const content = parts[0] || '';
  const stored = parts[1] || '';
  const calc = crypto.createHash('sha256').update(content, 'utf8').digest('hex').slice(0, 16);
  console.log('  content_id=' + noticeContentId);
  console.log('  stored response_digest = ' + JSON.stringify(stored));
  console.log('  calculated sha256[0..16] = ' + calc);
  console.log('  匹配 = ' + (stored.indexOf(calc) === 0));
  console.log('  content 前 120 字符 = ' + JSON.stringify(content.slice(0, 120)));
}