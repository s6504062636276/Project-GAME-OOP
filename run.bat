@echo off
chcp 65001 >nul
cd /d "%~dp0"
if not exist out mkdir out
javac -encoding UTF-8 -d out src/*.java
if errorlevel 1 pause & exit /b
java -cp out Main
