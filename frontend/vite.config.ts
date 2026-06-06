import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

function viteEsToolkitPlugin() {
  return {
    name: 'es-toolkit-compat-vite',
    enforce: 'pre',
    resolveId(source: string) {
      if (source.startsWith('es-toolkit/compat/')) {
        const name = source.split('/').pop() || '';
        return `\0virtual:es-toolkit-compat:${name}`;
      }
      return null;
    },
    load(id: string) {
      if (id.startsWith('\0virtual:es-toolkit-compat:')) {
        const name = id.replace('\0virtual:es-toolkit-compat:', '');
        return `export { ${name} as default } from 'es-toolkit/compat';`;
      }
      return null;
    }
  };
}

export default defineConfig({
  plugins: [
    viteEsToolkitPlugin(),
    react(),
    tailwindcss()
  ],
  optimizeDeps: {
    rolldownOptions: {
      plugins: [viteEsToolkitPlugin()]
    }
  },
  build: {
    minify: 'esbuild'
  }
})
