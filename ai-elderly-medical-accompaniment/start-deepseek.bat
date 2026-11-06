@echo off
REM ============================================
REM 启动后端（DeepSeek真实API模式）
REM 使用方式：双击此bat文件，或在终端中运行
REM Key仅存在于内存中，不会写入任何文件
REM ============================================

REM 填入你的DeepSeek API Key（sk-开头）
set AI_API_KEY=sk-a40f8ac8505440b089a2ade7f7e0453c

REM DeepSeek API地址（一般不用改）
set AI_LLM_BASE_URL=https://api.deepseek.com/v1

REM OCR服务地址（毕设演示用Mock即可，不用改）
set AI_OCR_BASE_URL=http://localhost:9001/ocr

REM JVM网络参数（强制IPv4，避免OkHttp连接超时）
set MAVEN_OPTS=-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false

echo ========================================
echo  启动陪诊助医系统后端 (DeepSeek模式)
echo ========================================
echo AI_API_KEY: %AI_API_KEY:~0,10%***
echo LLM: %AI_LLM_BASE_URL%
echo OCR: %AI_OCR_BASE_URL%
echo ========================================

cd /d "%~dp0"
mvn spring-boot:run
pause