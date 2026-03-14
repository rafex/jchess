@echo off
set SCRIPT_DIR=%~dp0
call "%SCRIPT_DIR%..\mvnw.cmd" -f "%SCRIPT_DIR%pom.xml" %*

