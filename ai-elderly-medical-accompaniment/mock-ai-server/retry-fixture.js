// T-05 测试辅助：固定状态码响应器（仅用于验证“非 5xx/429/超时不自旋重试”）
// 用法（占用 9002，需先停止 mock-ai-server/server.js）：
//   node mock-ai-server/retry-fixture.js 9002 400
// 说明：这是测试夹具，不是业务代码；不修改 server.js。

const http = require('http');
const port = Number(process.argv[2] || 9002);
const status = Number(process.argv[3] || 400);

const server = http.createServer((req, res) => {
  let body = '';
  req.on('data', c => body += c);
  req.on('end', () => {
    const stamp = new Date().toISOString();
    console.log(`[fixture:${port}] ${stamp} ${req.method} ${req.url} -> ${status} bodyLen=${body.length}`);
    res.writeHead(status, { 'Content-Type': 'text/plain; charset=utf-8' });
    res.end('fixture status ' + status);
  });
});
server.listen(port, () => console.log('[fixture] listening on :' + port + ' returning HTTP ' + status));