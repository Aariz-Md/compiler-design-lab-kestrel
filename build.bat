@echo off
REM build.bat — Compile Kestrel into out/
echo Compiling Kestrel...
if not exist out mkdir out
javac -d out src\kestrel\*.java
if %ERRORLEVEL%==0 (
    echo Build successful.
) else (
    echo Build failed.
    exit /b 1
)
