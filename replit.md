# GameNative

## Project Overview

GameNative is an Android application that allows users to play games they own on Steam, Epic, and GOG directly on Android devices with cloud saves. It is a fork of [Pluvia](https://github.com/oxters168/Pluvia), a Steam client for Android.

## Tech Stack

- **Language**: Kotlin
- **Build System**: Gradle (Kotlin DSL) with Gradle Wrapper (8.12.1)
- **Target Platform**: Android (minSdk 26, targetSdk 28, compileSdk 35)
- **UI**: Jetpack Compose
- **DI**: Dagger Hilt
- **Architecture**: Android app modules (:app, :ubuntufs)
- **NDK Version**: 22.1.7171670

## Important Notes

This is a **pure Android mobile application**. It is NOT a web application and cannot be run as a web server. To build and run this project, you need:

1. Android Studio
2. Android SDK (compileSdk 35)
3. Android NDK (22.1.7171670)

The Replit environment does not include the Android SDK, so full APK compilation is not possible in this environment. The project can be used for code editing, reviewing, and version control purposes.

## Building Locally

1. Clone the repository
2. Open in Android Studio
3. (Optional) Add `STEAMGRIDDB_API_KEY=your_api_key_here` to `local.properties`
4. Build and run on an Android device or emulator

## Project Structure

- `app/` - Main Android application module
- `ubuntufs/` - Ubuntu filesystem module (for running Linux games)
- `gradle/` - Gradle wrapper and version catalog
- `keyvalues/` - Key-value configuration files
- `media/` - Media assets
- `tools/` - Build tools

## Otimizações para Moto G35 (4GB RAM)

As seguintes otimizações foram aplicadas para dispositivos com 4GB de RAM:

### Container.java
- **MESA_SHADER_CACHE_MAX_SIZE**: reduzido de `512MB` para `256MB` — economiza ~256MB de RAM do sistema
- **videoMemorySize (DXVK)**: reduzido de `2048` para `1024` MB — evita estouro de memória em dispositivos com 4GB
- **maxDeviceMemory (DXVK)**: definido para `1024` MB para limitar consumo de memória do driver gráfico
- **WINEDEBUG=-all**: adicionado para desativar log de debug do Wine e reduzir sobrecarga de CPU/RAM
- **STAGING_SHARED_MEMORY=1**: melhora a gestão de memória compartilhada em dispositivos com pouca RAM
- **Resolução padrão**: mantida em `1280x720` (ótima para o Moto G35 com tela 1600x720)

### PrefManager.kt
- **videoMemorySize padrão**: reduzido de `2048` para `1024` MB

### gradle.properties
- **JVM Heap do Gradle**: reduzido de `8192m` para `4096m` (para o processo de build)

### Observação importante
O Moto G35 possui **4GB de RAM físico** — não é possível alocar mais do que isso. As otimizações garantem que o app usa a memória de forma eficiente dentro do limite disponível.

## Otimizações para Jogos Pesados

### PrefManager.kt — Novos defaults de emulação
- **Box86 / Box64 preset**: alterado de `COMPATIBILITY` para `PERFORMANCE`
  - Blocos JIT maiores (BIGBLOCK=3): menos recompilações = mais FPS
  - CALLRET=1: menor overhead nas chamadas de função
  - FORWARD=512: lookahead maior para o compilador JIT
  - FASTNAN/FASTROUND=1: operações de ponto flutuante mais rápidas
- **FEXCore preset**: alterado de `INTERMEDIATE` para `PERFORMANCE`
  - Desativa barreiras de memória desnecessárias (TSO) — ganho significativo de velocidade

### DefaultVersion.java — Cache de shaders habilitado
- **ASYNC_CACHE**: ativado (`0 → 1`) — shaders compilados de forma assíncrona agora ficam salvos em disco. **Elimina travamentos** ao reabrir jogos que já foram jogados antes.

### Container.java — Variável de ambiente adicional
- **DXVK_ASYNC=1**: ativa compilação assíncrona de shaders explicitamente — evita engasgos durante o jogo enquanto novos shaders são compilados.

### Box64rc (default/light/ultralight) — Configurações globais
- Adicionada seção `[*]` que aplica as configurações de performance do PERFORMANCE preset a **todos os jogos automaticamente**, sem precisar configurar manualmente cada um.

## Key Dependencies

- JavaSteam (Steam protocol library)
- Jetpack Compose (UI)
- Dagger Hilt (dependency injection)
- Room (local database)
- Coil / Landscapist (image loading)
- PostHog (analytics)
