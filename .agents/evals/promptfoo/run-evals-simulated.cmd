@echo off
setlocal
cd /d "%~dp0"

if not exist "node_modules\promptfoo\package.json" (
    echo [FAIL-CLOSED] Run npm.cmd ci in .agents/evals/promptfoo first.
    exit /b 1
)
node -e "if(require('./node_modules/promptfoo/package.json').version !== '0.123.0') process.exit(1)"
if errorlevel 1 exit /b 1

echo ===================================================================
echo   VEGETARIAN SUPPORT SYSTEM - HARNESS SELF-TEST (SIMULATED MODE)
echo   [WARNING] THIS IS A HARNESS SELF-TEST, NOT REAL AGENT EVIDENCE!
echo ===================================================================
echo.

set PROMPTFOO_EVAL_MODE=HARNESS_SELF_TEST
set CONFIG_FILE=promptfooconfig.yaml

if not exist "%CONFIG_FILE%" (
    echo [ERROR] Configuration file not found: %CONFIG_FILE%
    exit /b 1
)

echo Running Promptfoo HARNESS SELF-TEST (A01 - A10)...
echo Configuration: %CONFIG_FILE%
echo Mode: %PROMPTFOO_EVAL_MODE%
echo.

call node_modules\.bin\promptfoo.cmd eval -c "%CONFIG_FILE%" --no-cache

if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================================
    echo   [PASS] Harness self-test completed successfully!
    echo   [NOTE] This verified harness assertions only, NOT real agent.
    echo ===================================================================
    echo.
    echo Viewer is optional: rerun with --view to open it.
    if /i "%~1"=="--view" call node_modules\.bin\promptfoo.cmd view
    exit /b 0
) else (
    echo.
    echo ===================================================================
    echo   [FAIL] Harness self-test failed!
    echo ===================================================================
    echo Error code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
