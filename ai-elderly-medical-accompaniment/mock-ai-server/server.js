// ================================================================
// Mock AI Server - Zero-dependency Node.js HTTP mock
// Ports: 9001 (OCR), 9002 (LLM)
// All medical text is generic reference; NOT medical advice.
// ================================================================

const http = require('http');

// ============ Helper ============
function containsAny(text, keywords) {
  if (!text) return false;
  return keywords.some(k => text.indexOf(k) !== -1);
}

// ============ OCR Mock (port 9001) ============
let ocrCallCount = 0;

const ocrServer = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'application/json; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
  if (req.method !== 'POST' || !req.url.startsWith('/ocr/')) {
    res.writeHead(404); res.end(JSON.stringify({ error: 'Not Found' })); return;
  }

  let body = '';
  req.on('data', c => body += c);
  req.on('end', () => {
    ocrCallCount++;
    const isHighConf = ocrCallCount % 2 === 1;
    const confidence = isHighConf ? 0.95 : 0.72;
    res.writeHead(200);
    res.end(JSON.stringify({
      result: {
        patientName: 'Zhang**', gender: 'Male', age: 71,
        hospitalName: 'YiNianKang Hospital', department: 'Cardiology',
        examItems: [
          { name: 'WBC', value: '6.8', unit: '10^9/L', referenceRange: '4.0-10.0', abnormal: false },
          { name: 'HGB', value: '132', unit: 'g/L', referenceRange: '120-160', abnormal: false },
          { name: 'FBG', value: '7.2', unit: 'mmol/L', referenceRange: '3.9-6.1', abnormal: true },
          { name: 'TC', value: '5.8', unit: 'mmol/L', referenceRange: '2.8-5.2', abnormal: true }
        ],
        conclusion: 'Blood routine normal; FBG and TC elevated. Recommend endocrinology follow-up.',
        date: '2026-10-28'
      },
      confidence: confidence
    }));
    console.log('[OCR:9001] #' + ocrCallCount + ' conf=' + confidence);
  });
});

// ============ LLM Mock (port 9002) ============
let llmCallCount = 0;

const llmServer = http.createServer((req, res) => {
  res.setHeader('Content-Type', 'text/html; charset=utf-8');
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'POST, GET, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
  if (req.method !== 'POST' || !req.url.startsWith('/llm/')) {
    res.writeHead(404); res.end('<p>Not Found</p>'); return;
  }

  let body = '';
  req.on('data', c => body += c);
  req.on('end', () => {
    llmCallCount++;
    let parsed = {};
    try { parsed = JSON.parse(body) || {}; } catch(e) {}

    // ---- /llm/chat/completions (DeepSeek/OpenAI compatible) ----
    if (req.url === '/llm/chat/completions') {
      res.setHeader('Content-Type', 'application/json; charset=utf-8');
      let messages = (parsed.messages && Array.isArray(parsed.messages)) ? parsed.messages : [];
      let userMsg = '';
      for (let i = messages.length - 1; i >= 0; i--) {
        if (messages[i].role === 'user') { userMsg = messages[i].content || ''; break; }
      }
      if (userMsg.indexOf('TRIGGER_500') !== -1) {
        res.writeHead(500); res.end(JSON.stringify({error:'Internal Server Error'}));
        console.log('[LLM:9002/completions] #' + llmCallCount + ' -> 500');
        return;
      }
      let content = chatReply(userMsg);
      let openaiResp = {
        id: 'chatcmpl-mock-' + llmCallCount,
        object: 'chat.completion',
        created: Math.floor(Date.now() / 1000),
        model: 'deepseek-chat',
        choices: [{ index: 0, message: { role: 'assistant', content: content }, finish_reason: 'stop' }],
        usage: { prompt_tokens: 50, completion_tokens: Math.ceil(content.length / 4), total_tokens: 50 + Math.ceil(content.length / 4) }
      };
      res.writeHead(200); res.end(JSON.stringify(openaiResp));
      console.log('[LLM:9002/completions] #' + llmCallCount + ' -> 200 len=' + content.length);
      return;
    }

    // ---- /llm/chat: auto-detect messages[] vs prompt ----
    if (req.url === '/llm/chat') {
      res.setHeader('Content-Type', 'text/plain; charset=utf-8');

      // New multi-turn conversation: {"messages":[...]}
      if (parsed.messages && Array.isArray(parsed.messages)) {
        let userMsg = '';
        for (let i = parsed.messages.length - 1; i >= 0; i--) {
          if (parsed.messages[i].role === 'user') { userMsg = parsed.messages[i].content || ''; break; }
        }
        if (userMsg.indexOf('TRIGGER_500') !== -1) {
          res.writeHead(500); res.end('Internal Server Error (mock)');
          console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 500');
          return;
        }
        if (userMsg.indexOf('TRIGGER_429') !== -1) {
          res.writeHead(429); res.end('Too Many Requests (mock)');
          console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 429');
          return;
        }
        let resp = chatReply(userMsg);
        res.writeHead(200); res.end(resp);
        console.log('[LLM:9002/conv] #' + llmCallCount + ' -> 200 len=' + resp.length);
        return;
      }

      // Legacy: {"prompt":"..."}
      let prompt = parsed.prompt || '';
      legacyReply(res, prompt);
      return;
    }

    // ---- Other /llm/* paths: legacy ----
    legacyReply(res, parsed.prompt || '');
  });

  function legacyReply(res, prompt) {
    if (prompt.indexOf('TRIGGER_500') !== -1) {
      res.writeHead(500); res.end('Internal Server Error (mock)');
      console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 500');
      return;
    }
    if (prompt.indexOf('TRIGGER_429') !== -1) {
      res.writeHead(429); res.end('Too Many Requests (mock)');
      console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 429');
      return;
    }
    let content;
    if (containsAny(prompt, ['NOTICE','notice','gaozhishu','告知书'])) {
      content = noticeTemplate(pickFields(prompt, ['科室','预约时间','行动能力','过敏史']));
    } else if (containsAny(prompt, ['EXPLAIN','explain','jiedu','解读'])) {
      content = explainTemplate(pickFields(prompt, ['诊断','用药建议','备注']));
    } else {
      content = '<p>AI-generated content is for reference only. NOT medical advice.</p>';
    }
    res.writeHead(200); res.end(content);
    console.log('[LLM:9002/legacy] #' + llmCallCount + ' -> 200 len=' + content.length);
  }
});

// ============ Chat reply: keyword matching (CN+EN) ============
function chatReply(text) {
  var t = text || '';

  // Blood sugar / glucose
  if (containsAny(t, ['blood','glucose','sugar','Blood','Glucose','Sugar','xuetang','XUETANG'])) {
    return 'About blood glucose:\n\n' +
      '1. Normal fasting: 3.9-6.1 mmol/L\n' +
      '2. Impaired (6.1-7.0): Control diet, increase exercise, monitor regularly\n' +
      '3. Diabetic range (>=7.0): See endocrinologist\n\n' +
      'Lifestyle: reduce carbs and sweets, walk after meals, monitor regularly.\n' +
      'See a doctor if you experience dizziness, fatigue, or blurred vision.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Blood pressure
  if (containsAny(t, ['blood pressure','BP','bp','hypertension','Hypertension','xueya','XUEYA'])) {
    return 'About blood pressure:\n\n' +
      '1. Normal: <120/<80 mmHg\n' +
      '2. Elevated: 120-129/<80 mmHg\n' +
      '3. Stage 1 HTN: 130-139/80-89 mmHg\n' +
      '4. Stage 2 HTN: >=140/>=90 mmHg\n\n' +
      'Tips: low-sodium diet, regular sleep, moderate exercise, take meds as prescribed.\n' +
      'Seek emergency care for severe headache, chest tightness, or blurred vision.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Medication
  if (containsAny(t, ['medication','medicine','drug','pill','prescription','Medication','Medicine','Drug','Pill','Prescription','chiyao','CHIYAO','fuyao','FUYAO'])) {
    return 'General medication reminders (NOT prescription advice):\n\n' +
      '1. Take medication as prescribed or per label\n' +
      '2. Do NOT self-adjust dosage or stop without consulting your doctor\n' +
      '3. Store medication properly (cool, dry place; some need refrigeration)\n' +
      '4. Tell your doctor about all medications you take\n' +
      '5. Report side effects to your doctor promptly\n\n' +
      'Chronic disease meds require long-term regular use. Do NOT stop just because you feel better.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Hospital visit process
  if (containsAny(t, ['appointment','visit','hospital','process','registration','doctor','Appointment','Visit','Hospital','Process','Registration','Doctor','guahao','GUHAO','jiuzhen','JIUZHEN','liucheng','LIUCHENG','kanbing','KANBING'])) {
    return 'Hospital visit process:\n\n' +
      '1. Registration: Book via hospital app/website or at counter\n' +
      '2. Check-in: Arrive early, check in at kiosk or reception\n' +
      '3. Waiting: Wait in designated area for your turn\n' +
      '4. Consultation: Describe symptoms and history to doctor\n' +
      '5. Tests/Prescription: Doctor orders tests or medication\n' +
      '6. Payment: Pay at counter or kiosk\n' +
      '7. Tests/Pharmacy: Complete tests or pick up medication\n\n' +
      'Bring: ID card, insurance card, past records. Fast 8-12h before blood tests. Arrive 30 min early.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Pain / symptoms
  if (containsAny(t, ['pain','ache','hurt','discomfort','symptom','Pain','Ache','Hurt','Discomfort','Symptom','teng','TENG','tong','TONG','bushufu','BUSHU','nanshou','NANSHOU'])) {
    return 'IMPORTANT: AI cannot provide medical diagnosis.\n\n' +
      'Seek medical attention for: severe/worsening pain, pain with fever/vomiting, pain after injury, pain affecting daily life.\n\n' +
      'When seeing a doctor, describe: location, onset time/duration, pain type (sharp/dull/throbbing), triggers/relief factors, other symptoms.\n\n' +
      'Before seeing a doctor: rest comfortably. Do NOT self-medicate with painkillers.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Lab reports / test results
  if (containsAny(t, ['report','test','lab','result','indicator','Report','Test','Lab','Result','Indicator','baogao','BAOGAO','jiancha','JIANCHA','zhibiao','ZHIBIAO','jieguo','JIEGUO'])) {
    return 'About lab reports:\n\n' +
      '1. Reference range: Values for most healthy people; slight deviations may be normal\n' +
      '2. Arrows: Up = above range; Down = below range\n' +
      '3. Single abnormal value: Evaluate with other indicators and symptoms\n\n' +
      'Common tests: CBC (anemia/infection), Liver (ALT/AST), Kidney (creatinine/BUN), Lipids (cholesterol/triglycerides), Glucose (sugar metabolism)\n\n' +
      'Specific interpretation should be done by your doctor.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Heart / cardiac
  if (containsAny(t, ['heart','cardiac','chest','Heart','Cardiac','Chest','xinzang','XINZANG','xinneike','XINNEIKE'])) {
    return 'For cardiology-related questions:\n\n' +
      'Common cardiology conditions: hypertension, coronary artery disease, arrhythmia, heart failure.\n' +
      'Common tests: ECG, echocardiogram, stress test, Holter monitor, cardiac biomarkers.\n\n' +
      'Preparation: Bring previous ECG/echo reports. Wear comfortable clothing. Inform doctor of all medications.\n' +
      'Emergency signs: Severe chest pain, shortness of breath, palpitations with dizziness - go to ER immediately.\n\n' +
      '---\n*AI content is for reference only. NOT medical advice.*';
  }

  // Generic fallback
  return 'Thank you for your question. As your AI health assistant, I can help with:\n\n' +
    '- Hospital visit processes and preparation\n' +
    '- Understanding common lab test indicators\n' +
    '- General medication precautions\n' +
    '- Choosing the right department for your symptoms\n' +
    '- Healthy lifestyle recommendations\n\n' +
    'Please describe your question in more detail.\n\n' +
    '---\n*AI content is for reference only. NOT medical advice.*';
}

// ============ Legacy templates ============
// 从后端中文 prompt 中提取“键=值”字段（值截至下一个英文逗号）
function pickFields(prompt, labels) {
  const fields = {};
  for (const label of labels) {
    const match = prompt.match(new RegExp(label + '=([^,]*)'));
    if (match) fields[label] = match[1].trim();
  }
  return fields;
}

// 将已解析字段按既有模板句式（<p>）原样回填
function fieldLines(fields, labels) {
  return labels
    .filter(label => fields[label] !== undefined && fields[label] !== '')
    .map(label => '<p><strong>' + label + '：</strong>' + fields[label] + '</p>')
    .join('');
}

function noticeTemplate(fields) {
  fields = fields || {};
  return '<h3>诊前就诊告知书</h3>' +
    '<p><strong>亲爱的患者及家属：</strong></p>' +
    '<p>为保证就诊过程顺利，请您阅读以下提示：</p>' +
    fieldLines(fields, ['科室','预约时间','行动能力','过敏史']) +
    '<p>1. <strong>就诊准备：</strong>请携带身份证、医保卡及既往病历资料。</p>' +
    '<p>2. <strong>空腹要求：</strong>抽血检查前需空腹8-12小时（可少量饮水）。</p>' +
    '<p>3. <strong>用药提示：</strong>请勿在未咨询医生的情况下自行停用长期服用的药物。</p>' +
    '<p>4. <strong>陪同提示：</strong>老年人或行动不便者建议有人陪同就诊。</p>' +
    '<p>5. <strong>就诊流程：</strong>挂号 -> 候诊 -> 就诊 -> 检查/处方 -> 缴费 -> 检查/取药。</p>' +
    '<p>6. <strong>出行提示：</strong>建议提前30分钟到达，优先选择公共交通。</p>' +
    '<p>7. <strong>隐私说明：</strong>您的信息仅用于本次就诊。</p>' +
    '<p style="color:#E6A23C;font-weight:bold;">AI生成内容仅供参考，不构成任何诊疗建议</p>';
}

function explainTemplate(fields) {
  fields = fields || {};
  return '<h3>医嘱通俗解读</h3>' +
    '<p><strong>说明：</strong>以下为对医生医嘱的通俗解释。</p>' +
    fieldLines(fields, ['诊断','用药建议','备注']) +
    '<p>1. <strong>诊断：</strong>医生根据您的症状和检查结果作出的专业判断。</p>' +
    '<p>2. <strong>用药：</strong>请按医嘱服药，请勿自行调整用药；如出现副作用请及时告知医生。</p>' +
    '<p>3. <strong>检查：</strong>用于明确诊断或评估治疗效果，请按检查要求做好准备。</p>' +
    '<p>4. <strong>复诊：</strong>请按预约时间复诊，并携带本次就诊的全部检查报告。</p>' +
    '<p>5. <strong>日常照护：</strong>保持均衡饮食、适度运动和充足睡眠，并关注慢性病相关指标。</p>' +
    '<p>6. <strong>紧急情况：</strong>如出现剧烈胸痛、呼吸困难或意识不清，请立即前往急诊就医。</p>' +
    '<p style="color:#E6A23C;font-weight:bold;">AI生成内容仅供参考，不构成任何诊疗建议</p>';
}

// ============ Start ============
ocrServer.listen(9001, () => console.log('[Mock AI] OCR on :9001'));
llmServer.listen(9002, () => console.log('[Mock AI] LLM on :9002'));
console.log('[Mock AI] Ready. /llm/chat supports conversation (messages[]) + legacy (prompt).');