import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ command, mode }) => {
  const isProdBuild = command === 'build' && mode === 'production'

  return {
    base: '/',
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(
        isProdBuild ? 'https://appcompras-backend-vfh75ywbwq-ue.a.run.app' : ''
      )
    },
    plugins: [react()],
    test: {
      environment: 'jsdom',
      setupFiles: './src/test/setup.js'
    },
    server: {
      port: 5173,
      proxy: {
        '/api': { target: 'http://localhost:8080', changeOrigin: true },
        '/actuator': { target: 'http://localhost:8080', changeOrigin: true },
        '/v3/api-docs': { target: 'http://localhost:8080', changeOrigin: true },
        '/swagger-ui': { target: 'http://localhost:8080', changeOrigin: true }
      }
    }
  }
})