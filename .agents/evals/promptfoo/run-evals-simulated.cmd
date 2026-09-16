@echo off
setlocal
cd /d "%~dp0"

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

call npx.cmd promptfoo@latest eval -c "%CONFIG_FILE%" --no-cache

if %ERRORLEVEL% equ 0 (
    echo.
    echo ===================================================================
    echo   [PASS] Harness self-test completed successfully!
    echo   [NOTE] This verified harness assertions only, NOT real agent.
    echo ===================================================================
    echo.
    echo Launching Promptfoo web viewer...
    call npx.cmd promptfoo@latest view
    exit /b 0
) else (
    echo.
    echo ===================================================================
    echo   [FAIL] Harness self-test failed!
    echo ===================================================================
    echo Error code: %ERRORLEVEL%
    exit /b %ERRORLEVEL%
)
