# AP.iptv 2026 — Proyecto Android

Aplicación nativa que empaqueta el reproductor AP.iptv 2026 en un WebView
con soporte completo para Phone, Tablet, Android TV y **Amazon Fire TV**.

---

## Requisitos para compilar

| Herramienta | Versión mínima |
|---|---|
| Android Studio | Hedgehog 2023.1.1+ |
| JDK | 17+ (incluido en Android Studio) |
| Android SDK | API 34 (se instala automáticamente) |
| Gradle | 8.4 (se descarga automáticamente) |

---

## Pasos para compilar el APK

### Opción A — Android Studio (recomendado)

1. Abre Android Studio
2. **File → Open** → selecciona la carpeta `APiptv2026`
3. Espera a que Gradle sincronice (primera vez: 2-5 min)
4. Menú **Build → Build Bundle(s)/APK(s) → Build APK(s)**
5. El APK se genera en:
   `app/build/outputs/apk/debug/app-debug.apk`

### Opción B — Línea de comandos

```bash
# En la carpeta del proyecto:
chmod +x gradlew
./gradlew assembleDebug

# APK generado en:
app/build/outputs/apk/debug/app-debug.apk
```

---

## Instalar en el dispositivo

### Android Phone/Tablet
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Amazon Fire TV / Fire Stick
```bash
# 1. Habilitar "Aplicaciones de fuentes desconocidas" en Settings → My Fire TV → Developer Options
# 2. Conectar Fire TV a la misma red Wi-Fi que tu PC
# 3. Activar ADB: Settings → My Fire TV → Developer Options → ADB Debugging ON
# 4. Obtener la IP del Fire TV en Settings → My Fire TV → About → Network
# 5. Conectar y instalar:
adb connect <IP_DEL_FIRE_TV>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Android TV (NVIDIA Shield, etc.)
```bash
adb connect <IP_DEL_TV>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Estructura del proyecto

```
APiptv2026/
├── app/src/main/
│   ├── java/com/apiptv/app/
│   │   ├── MainActivity.java       ← Phone / Tablet
│   │   ├── TvActivity.java         ← Android TV / FireTV
│   │   ├── VideoFullscreenHelper.java
│   │   └── AndroidBridge.java      ← JS ↔ Android nativo
│   ├── assets/
│   │   └── index.html              ← Toda la app web aquí
│   ├── res/
│   │   ├── drawable/
│   │   │   ├── ic_launcher.xml     ← Icono de la app
│   │   │   └── tv_banner.xml       ← Banner para TV Home
│   │   ├── values/
│   │   │   ├── themes.xml
│   │   │   └── strings.xml
│   │   └── xml/
│   │       └── network_security_config.xml  ← Permite HTTP
│   └── AndroidManifest.xml
└── app/build.gradle
```

---

## Funcionalidades por plataforma

| Característica | Phone | Tablet | Android TV | Fire TV |
|---|:---:|:---:|:---:|:---:|
| Reproductor HLS | ✅ | ✅ | ✅ | ✅ |
| localStorage (favoritos) | ✅ | ✅ | ✅ | ✅ |
| Pantalla completa (video) | ✅ | ✅ | ✅ | ✅ |
| Streams HTTP | ✅ | ✅ | ✅ | ✅ |
| Control remoto D-pad | — | — | ✅ | ✅ |
| Modo TV automático | — | — | ✅ | ✅ |
| Banner en TV Home | — | — | ✅ | ✅ |
| Leanback launcher | — | — | ✅ | ✅ |
| Mantener pantalla ON | ✅ | ✅ | ✅ | ✅ |
| Detección de plataforma | ✅ | ✅ | ✅ | ✅ |

---

## Actualizar el contenido web

Para actualizar la app web sin recompilar todo:
1. Edita `app/src/main/assets/index.html`
2. Vuelve a compilar: `./gradlew assembleDebug`

---

## Generar APK firmado (para distribuir)

1. Android Studio → **Build → Generate Signed Bundle/APK**
2. Selecciona **APK**
3. Crea o selecciona tu keystore
4. Selecciona `release`
5. El APK firmado estará en `app/build/outputs/apk/release/`
