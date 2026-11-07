/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        console: {
          950: '#070B12',
          900: '#0B101D',
          850: '#0F1626',
          800: '#141D32',
          750: '#19243E',
          700: '#1E2B4A',
          600: '#2A3B66',
          border: '#1F2C47',
          borderSubtle: '#172238',
          text: '#F1F5F9',
          muted: '#94A3B8',
          dim: '#64748B'
        },
        semantic: {
          verified: '#10B981',
          verifiedGlow: 'rgba(16, 185, 129, 0.15)',
          inferred: '#6366F1',
          inferredGlow: 'rgba(99, 102, 241, 0.15)',
          missing: '#F59E0B',
          missingGlow: 'rgba(245, 158, 11, 0.15)',
          conflict: '#F43F5E',
          conflictGlow: 'rgba(244, 63, 94, 0.2)',
          sandbox: '#06B6D4',
          sandboxGlow: 'rgba(6, 182, 212, 0.15)',
          neutral: '#64748B'
        }
      },
      fontFamily: {
        mono: ['"JetBrains Mono"', '"Fira Code"', 'ui-monospace', 'SFMono-Regular', 'Menlo', 'monospace'],
        sans: ['"Inter"', 'system-ui', '-apple-system', 'sans-serif']
      }
    },
  },
  plugins: [],
}

