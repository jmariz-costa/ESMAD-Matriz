import { defineConfig, devices } from '@playwright/test'

// Testes E2E (Ficha PL 04, Exercício 4). Correm contra a aplicação já ligada:
// base de dados, Backend (8081) e Frontend (5173). Ver README.md, secção "Testes E2E".
export default defineConfig({
  testDir: './e2e',
  // Um teste de cada vez: todos usam a mesma conta de demonstração
  workers: 1,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  reporter: [['list'], ['html', { open: 'never', outputFolder: 'playwright-report' }]],
  use: {
    baseURL: process.env.MATRIZ_E2E_URL ?? 'http://localhost:5173',
    // Só para diagnosticar falhas (Ficha PL 04, Ex. 4.3)
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [{ name: 'firefox', use: { ...devices['Desktop Firefox'] } }],
})
