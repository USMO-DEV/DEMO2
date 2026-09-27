@echo off
chcp 65001 >nul
cd /d "%~dp0"

if not exist out mkdir out

echo [1/2] 编译 Java 后端...
javac -encoding UTF-8 -d out src\*.java
if errorlevel 1 (
  echo 编译失败，请确认已安装 JDK 11+
  pause
  exit /b 1
)

echo [2/2] 启动后端 API...
java -cp out Main

pause
