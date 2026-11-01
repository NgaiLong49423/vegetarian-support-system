param()
$env:MAMXANX_E2E_FRONTEND_URL = "http://localhost:15174"
$env:MAMXANX_E2E_BACKEND_URL = "http://localhost:18084/api/v1"
$env:MAMXANX_E2E_COMPOSE_PROJECT = "mamxanh-ai-e2e"

Set-Location "app/mamxanh-frontend"
npx playwright test tests/integration/ai-g0-g4-acceptance.spec.ts --config playwright.integration.config.ts
