import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import path from 'path'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import ElementPlus from 'unplugin-element-plus/vite'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  
  return {
    plugins: [
      vue(),
      // Element Plus 自动导入
      AutoImport({
        resolvers: [
          ElementPlusResolver(),
        ],
        imports: [
          'vue',
          'vue-router',
          'pinia',
        ],
        dts: 'src/auto-imports.d.ts',
        eslintrc: {
          enabled: true,
          filepath: './.eslintrc-auto-import.json',
        },
      }),
      Components({
        resolvers: [
          ElementPlusResolver(),
        ],
        dts: 'src/components.d.ts',
      }),
      ElementPlus({
        useSource: true,
      }),
    ],
    
    base: './',
    
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
        '@components': path.resolve(__dirname, 'src/components'),
        '@views': path.resolve(__dirname, 'src/views'),
        '@router': path.resolve(__dirname, 'src/router'),
        '@electron': path.resolve(__dirname, 'electron'),
      },
    },
    
    css: {
      preprocessorOptions: {
        scss: {
          additionalData: `@use "@/styles/variables.scss" as *;`,
        },
      },
    },
    
    server: {
      port: 5173,
      host: '0.0.0.0',
      strictPort: false,
      open: false,
      proxy: {
        '/api': {
          target: env.VITE_API_URL || 'http://localhost:18080',
          changeOrigin: true,
          rewrite: (path) => path.replace(/^\/api/, ''),
        },
        '/ws': {
          target: env.VITE_WS_URL || 'ws://localhost:18080',
          ws: true,
        },
      },
    },
    
    optimizeDeps: {
      include: [
        'vue',
        'vue-router',
        'pinia',
        'axios',
        'element-plus',
        '@element-plus/icons-vue',
      ],
      exclude: ['monaco-editor'],
    },
    
    build: {
      outDir: 'dist',
      emptyOutDir: true,
      target: 'es2022',
      minify: 'esbuild',
      sourcemap: mode === 'development',
      chunkSizeWarningLimit: 1000,
      
      rollupOptions: {
        output: {
          manualChunks: {
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            'element-plus': ['element-plus', '@element-plus/icons-vue'],
            'editor': ['monaco-editor'],
          },
          chunkFileNames: 'js/[name]-[hash].js',
          entryFileNames: 'js/[name]-[hash].js',
          assetFileNames: (assetInfo) => {
            const name = assetInfo.name || ''
            if (name.endsWith('.css')) {
              return 'css/[name]-[hash][extname]'
            }
            if (/\.(png|jpe?g|gif|svg|webp|ico)$/i.test(name)) {
              return 'images/[name]-[hash][extname]'
            }
            if (/\.(woff2?|eot|ttf|otf)$/i.test(name)) {
              return 'fonts/[name]-[hash][extname]'
            }
            return 'assets/[name]-[hash][extname]'
          },
        },
      },
      
      // Monaco Editor 特殊处理
      commonjsOptions: {
        transformMixedEsModules: true,
      },
    },
    
    // 开发环境性能优化
    esbuild: {
      target: 'es2022',
      jsx: 'preserve',
    },
    
    // Worker 配置
    worker: {
      format: 'es',
    },
  }
})
