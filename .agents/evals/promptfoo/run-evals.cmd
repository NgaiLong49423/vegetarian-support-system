@echo off
setlocal
cd /d "%~dp0"

if not exist "node_modules\promptfoo\package.json" (
    echo [FAIL-CLOSED] Run npm.cmd ci in .agents/evals/promptfoo first.
    exit /b 1
)
node -e "if(require('./node_modules/promptfoo/package.json').version !== '0.123.0') process.exit(1)"
if errorlevel 1 exit /b 1

echo =======================================================
echo   VEGETARIAN SUPPORT SYSTEM - REAL AGENT ACCEPTANCE EVAL
echo =======================================================
echo.

:: 1. Fail closed: Check verified real provider (Codex CLI)
set CODEX_EXE=C:\Users\Lenovo\AppData\Local\Programs\OpenAI\Codex\bin\codex.exe

if not exist "%CODEX_EXE%" (
    where codex.exe >nul 2>nul
    if %ERRORLEVEL% neq 0 (
        echo [FAIL-CLOSED] No verified real tested-agent provider found!
        echo Codex CLI was not found at %CODEX_EXE% or in PATH.
        echo Acceptance evaluation refused. Will not run simulated mode for real acceptance results.
        exit /b 1
    )
    set CODEX_EXE=codex.exe
)

echo [OK] Verified real tested-agent provider: %CODEX_EXE%

:: 2. Run deterministic preflight test (bounded workspace-write, external isolation, safety)
echo Running preflight check for bounded isolation and safety...
python preflight_real_agent.py
if %ERRORLEVEL% neq 0 (
    echo [FAIL-CLOSED] Preflight check failed. Bounded workspace-write isolation is not verified on this host.
    echo Real acceptance evaluation aborted.
    exit /b 1
)

if /i "%~1"=="--preflight-only" exit /b 0

echo Setting evaluation mode to REAL...
set PROMPTFOO_EVAL_MODE=REAL

set CONFIG_FILE=promptfooconfig.yaml

if not exist "%CONFIG_FILE%" (
    echo [ERROR] Configuration file not found: %CONFIG_FILE%
    exit /b 1
)

echo.
echo Running Promptfoo REAL evaluations (A01 - A10)...
echo Configuration: %CONFIG_FILE%
echo.

call node_modules\.bin\promptfoo.cmd eval -c "%CONFIG_FILE%" --no-cache

if %ERRORLEVEL% equ 0 (
    echo.
    echo =======================================================
    echo   [PASS] All agent acceptance evaluations passed!
    echo =======================================================
    echo.
    echo Viewer is optional: rerun with --view to open it.
    if /i "%~1"=="--view" call node_modules\.bin\promptfoo.cmd view
    exit /b 0
) else (
    echo.
    echo =======================================================
    echo   [FAIL] One or more acceptance evaluations failed!
    echo =======================================================
    echo Error code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
