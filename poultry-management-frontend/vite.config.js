import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// strictPort: if 5173 is busy, stop with an error instead of silently using another port.
// (The backend only allows requests from http://localhost:5173 - see its CORS setting.)
export default defineConfig({
  plugins: [react()],
  server: { port: 5173, strictPort: true },
  preview: { port: 4173, strictPort: true },
});
