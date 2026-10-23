@echo off
REM run_demo.bat -- Run the complete live demo of Kestrel Phase 3
echo Building Kestrel...
if not exist out mkdir out
javac -d out src\kestrel\*.java
if %ERRORLEVEL% NEQ 0 (
    echo Build failed.
    exit /b 1
)
echo.
echo Starting Complete Demonstration...
echo.
java -cp out kestrel.Demo
