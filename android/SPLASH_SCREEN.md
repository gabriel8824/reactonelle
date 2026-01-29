# Splash Screen Customizável

Guia para configurar splash screens personalizadas no Reactonelle.

---

## Início Rápido

1. Edite `android/app/src/main/assets/splash_config.json`
2. Coloque seus assets em `android/app/src/main/assets/splash/`
3. Build e teste

---

## Configuração (`splash_config.json`)

```json
{
  "type": "lottie",
  "backgroundColor": "#0F172A",
  "logo": {
    "src": "meu_logo.png",
    "width": 150,
    "height": 150,
    "animation": "fade-scale"
  },
  "lottie": {
    "src": "loading.json",
    "width": 300,
    "height": 300,
    "loop": false
  },
  "text": {
    "content": "Carregando...",
    "color": "#FFFFFF",
    "fontSize": 16,
    "animation": "pulse"
  },
  "duration": 3000,
  "transition": "fade",
  "autoHide": false
}
```

---

## Tipos de Splash

| Tipo | Descrição |
|------|-----------|
| `static` | Apenas logo com animação de entrada |
| `lottie` | Animação Lottie |
| `animated` | Logo + animações CSS |

---

## Animações Disponíveis

### Entrada (logo/text animation)
- `none` - Sem animação
- `fade` - Fade in
- `fade-scale` - Fade + escala (padrão)
- `bounce` - Entrada com bounce
- `slide-up` - Desliza de baixo
- `pulse` - Pulso contínuo (ideal para texto)

### Transição para WebView
- `fade` - Fade out (padrão)
- `slide` - Desliza para cima
- `zoom` - Zoom out

---

## Exemplos

### Splash Simples (Logo Estático)

```json
{
  "type": "static",
  "backgroundColor": "#1E40AF",
  "logo": {
    "src": "logo.png",
    "width": 180,
    "height": 180,
    "animation": "fade-scale"
  },
  "duration": 2000,
  "autoHide": true
}
```

### Splash com Animação Lottie

1. Baixe animação de [lottiefiles.com](https://lottiefiles.com)
2. Coloque em `assets/splash/loading.json`

```json
{
  "type": "lottie",
  "backgroundColor": "#0F172A",
  "lottie": {
    "src": "loading.json",
    "width": 250,
    "height": 250,
    "loop": true
  },
  "autoHide": false
}
```

3. No JavaScript, esconda quando pronto:

```javascript
Reactonelle.call('splash.hide');
```

### Splash com Logo + Texto

```json
{
  "type": "static",
  "backgroundColor": "#111827",
  "logo": {
    "src": "icon.png",
    "width": 120,
    "height": 120,
    "animation": "bounce"
  },
  "text": {
    "content": "Carregando...",
    "color": "#9CA3AF",
    "fontSize": 14,
    "animation": "pulse"
  },
  "autoHide": false
}
```

---

## Bridge JavaScript

### Esconder Splash Manualmente

```javascript
// Quando seu app estiver pronto
Reactonelle.call('splash.hide')
  .then(() => console.log('Splash escondida'))
  .catch(err => console.error(err));
```

### Auto-Hide

Se `autoHide: true`, a splash será escondida automaticamente após `duration` ms.

---

## Estrutura de Arquivos

```
android/app/src/main/assets/
├── splash_config.json       # Configuração
└── splash/
    ├── logo.png             # Seu logo
    └── loading.json         # Animação Lottie (opcional)
```

---

## Cores

Use formato hexadecimal:
- `#FFFFFF` - Branco
- `#0F172A` - Slate escuro (padrão)
- `#1E40AF` - Azul
- `#059669` - Verde

---

## Troubleshooting

| Problema | Solução |
|----------|---------|
| Logo não aparece | Verifique se está em `assets/splash/` |
| Animação Lottie não roda | Confirme que é arquivo `.json` válido |
| Splash não esconde | Configure `autoHide: true` ou chame `splash.hide` |
