/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      boxShadow: {
        polish: '0 20px 60px rgba(20, 18, 25, 0.12)'
      },
      colors: {
        ink: '#17151c',
        coral: '#ef476f',
        mint: '#0f766e',
        linen: '#f7f3ef'
      }
    }
  },
  plugins: []
}

