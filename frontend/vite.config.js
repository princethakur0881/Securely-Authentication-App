
import react from '@vitejs/plugin-react'
import { defineConfig } from 'vite'

export default defineConfig({
  plugins: [react()],

  server: {
    port: 5173,
    proxy: {
      '/auth': 'http://localhost:8080',
      '/api': 'http://localhost:8080',
      '/profile': 'http://localhost:8080',
    },
  },

  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
})
