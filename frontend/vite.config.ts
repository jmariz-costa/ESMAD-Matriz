/// <reference types="vitest/config" />
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

// Endereço do Backend usado pelo proxy de desenvolvimento.
// É o único sítio onde está definido; o código da aplicação usa sempre o caminho relativo /api.
const ENDERECO_BACKEND = process.env.MATRIZ_BACKEND_URL ?? 'http://localhost:8081'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      // Frontend e Backend ficam na mesma origem, para o cookie de sessão funcionar sem CORS
      '/api': ENDERECO_BACKEND,
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/test/setup.ts'],
  },
})
