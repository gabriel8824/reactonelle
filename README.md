<p align="center">
  <img src="docs/assets/logo.png" alt="Reactonelle Logo" width="120" />
</p>

<h1 align="center">⚡ Reactonelle</h1>

<p align="center">
  <strong>Transforme qualquer aplicação Web em um App Android nativo com acesso completo às APIs do sistema.</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Bridge-JavaScript-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black" alt="JavaScript" />
  <img src="https://img.shields.io/badge/License-MIT-blue?style=for-the-badge" alt="License" />
</p>

---

## 🚀 O que é o Reactonelle?

**Reactonelle** é um container nativo Android que carrega sua aplicação web (React, Vue, Svelte, HTML puro) e expõe **44 APIs nativas** diretamente para o JavaScript. É a solução perfeita para:

- 🏢 **Empresas** que querem criar apps Android rapidamente usando seu time web
- 🎨 **Whitelabel** - Personalize completamente para diferentes clientes
- 💡 **Startups** que precisam de MVP rápido com funcionalidades nativas
- 🔧 **Desenvolvedores** que preferem web mas precisam de recursos nativos

---

## ✨ Principais Funcionalidades

<table>
  <tr>
    <td align="center" width="25%">
      <h3>📱 Hardware</h3>
      Câmera, Galeria, Lanterna, Vibração, Biometria
    </td>
    <td align="center" width="25%">
      <h3>📍 Sensores</h3>
      GPS, Status de Rede, Bateria, Informações do Dispositivo
    </td>
    <td align="center" width="25%">
      <h3>🎨 UI Nativa</h3>
      Toast, Alertas, Action Sheets, Date Picker, Status Bar
    </td>
    <td align="center" width="25%">
      <h3>🔐 Segurança</h3>
      Autenticação Biométrica, Permissões Granulares
    </td>
  </tr>
</table>

---

## 🎯 44 Bridges Disponíveis

| Categoria | Bridges |
|-----------|---------|
| **Sistema** | `device.info`, `app.version`, `network.status`, `battery.status` |
| **Hardware** | `flashlight.toggle`, `haptic`, `biometric.authenticate` |
| **Câmera/Mídia** | `camera.photo`, `gallery.pick`, `qrcode.scan`, `qrcode.generate` |
| **Áudio** | `microphone.start`, `microphone.stop` |
| **Localização** | `location.current` |
| **UI Nativa** | `toast`, `alert`, `actionsheet.show`, `datepicker.show` |
| **Status Bar** | `statusbar.style`, `statusbar.show`, `statusbar.hide` |
| **Teclado** | `keyboard.show`, `keyboard.hide` |
| **Storage** | `storage.set`, `storage.get` |
| **Clipboard** | `clipboard.write`, `clipboard.read`, `clipboard.hasText` |
| **Compartilhar** | `share`, `url.open`, `url.canOpen` |
| **Contatos** | `contacts.pick`, `contacts.getAll` |
| **Notificações** | `notification.local`, `notification.cancel` |
| **Permissões** | `permission.check`, `permission.request` |

---

## 📦 Início Rápido

### 1. Clone o repositório

```bash
git clone https://github.com/seu-usuario/reactonelle.git
cd reactonelle/android
```

### 2. Abra no Android Studio

```bash
open -a "Android Studio" .
```

### 3. Configure sua aplicação web

Coloque sua build em `android/app/src/main/assets/www/` ou configure uma URL remota.

### 4. Execute!

```bash
./gradlew installDebug
```

---

## 💻 Exemplo de Uso

```javascript
// No seu JavaScript/TypeScript
const result = await Reactonelle.call('camera.photo', {
  quality: 80,
  facing: 'back'
});

if (result.success) {
  console.log('Foto capturada:', result.data.base64);
}
```

```javascript
// Biometria
const auth = await Reactonelle.call('biometric.authenticate', {
  title: 'Confirme sua identidade',
  description: 'Use sua digital para continuar'
});

if (auth.success) {
  // Usuário autenticado!
}
```

```javascript
// QR Code
const scan = await Reactonelle.call('qrcode.scan');
console.log('Código lido:', scan.data.value);
```

---

## 🎨 Personalização Whitelabel

O Reactonelle foi projetado para ser **100% personalizável**:

| O que personalizar | Arquivo |
|-------------------|---------|
| Nome do App | `res/values/strings.xml` |
| Ícone | `res/mipmap-*/ic_launcher.png` |
| Cores do tema | `res/values/colors.xml` |
| Splash Screen | `res/drawable/splash_background.xml` |
| ID do pacote | `build.gradle.kts` |

📖 Veja o [Guia Completo de Personalização](docs/CUSTOMIZATION_GUIDE.md)

---

## 🏗️ Arquitetura

```
android/
├── app/src/main/
│   ├── java/com/reactonelle/
│   │   ├── MainActivity.kt          # Entrada do app
│   │   ├── bridge/                   # Sistema de bridges
│   │   │   ├── ReactonelleBridge.kt  # Orquestrador
│   │   │   └── handlers/             # Handlers por categoria
│   │   ├── webview/                  # WebView customizada
│   │   └── debug/                    # Menu de desenvolvedor
│   ├── assets/www/                   # Sua aplicação web
│   └── res/                          # Recursos Android
└── build.gradle.kts
```

---

## 🛠️ Menu de Desenvolvedor

Agite o dispositivo em modo debug para acessar:

- 🔄 Recarregar aplicação
- 🔗 Trocar URL de desenvolvimento
- 📋 Visualizar logs do console
- 🐛 Inspecionar chamadas de bridge

---

## 📄 Licença

MIT License - Veja o arquivo [LICENSE](LICENSE) para mais detalhes.

---

## 🤝 Contribuindo

Contribuições são bem-vindas! Por favor, leia nosso guia de contribuição antes de enviar PRs.

---

<p align="center">
  <strong>Feito com ❤️ para a comunidade de desenvolvedores</strong>
</p>

<p align="center">
  <a href="docs/CUSTOMIZATION_GUIDE.md">📖 Docs</a> •
  <a href="https://github.com/seu-usuario/reactonelle/issues">🐛 Issues</a> •
  <a href="https://github.com/seu-usuario/reactonelle/discussions">💬 Discussões</a>
</p>
