<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Widget · Dock · Launcher e-ink · Android</strong><br/>Apps · bridge athistory.koplugin · l’Atelier</p>
</td>
</tr>
</table>

<a href="https://atelier.atechnologie.fr/" title="l'Atelier – asociación de fabricación de libros y herramientas de investigación"><img src="https://raw.githubusercontent.com/Atelier-Recherche/.github/main/profile/logo.jpg" alt="l'Atelier" width="200" /></a>  
<sub>Desarrollado por <a href="https://atelier.atechnologie.fr/">l'Atelier</a> — fabricación de libros y herramientas de investigación (EHESS)</sub>

<p>
🇫🇷 <a href="README.md">Français</a> ·
🇬🇧 <a href="README.en.md">English</a> ·
🇩🇪 <a href="README.de.md">Deutsch</a> ·
🇪🇸 <a href="README.es.md"><b>Español</b></a>
</p>

<p>
<a href="https://atelier.atechnologie.fr/"><img src="https://img.shields.io/badge/🌐_l'Atelier-atelier.atechnologie.fr-2d5016?style=for-the-badge" alt="Sitio l'Atelier" /></a>

</p>

</div>

---

## 📸 Vista previa

| Widget | Dock |
| :---: | :---: |
| <img src="readme-media/screen1.jpg" alt="KOReader History — Widget" width="400" /> | <img src="readme-media/screen2.jpg" alt="KOReader History — Dock" width="400" /> |

## 📖 Acerca de

Monorepo Android (por l’Atelier) con **dos aplicaciones independientes** y un bridge KOReader compartido:

1. **KOReader History** — widget de historial de lectura + dock de acciones  
2. **Atelier e-ink Launcher** — launcher a pantalla completa pensado para e-ink / Boox (escritorio, dock, cajón, notificaciones…)

Ambas apps comparten `core/` y el plugin **Atelier History Bridge** (`athistory.koplugin`). El dock del **widget** y el del **launcher** son **separados** (prefs y slots distintos): se puede instalar solo el widget, solo el launcher, o ambos.

## 🧩 Componentes

| Elemento | Package / ruta | Función |
|----------|----------------|---------|
| App widget | `fr.atelier.koreaderhistory` (`app/`) | Widget de historial + config + dock del widget |
| Launcher | `fr.atelier.einklauncher` (`launcher/`) | Home e-ink: escritorio, dock, apps, notifs |
| Core | `core/` | Prefs, historial, dock, iconos, helpers del bridge |
| `athistory.koplugin` | plugins KOReader | Bridge: lee `settings/athistory/pending.lua` y ejecuta la acción |
| `deploy.ps1` | raíz | Build + install de ambos APKs + grants ADB + push del plugin |

## ⬇️ Instalación

### Con `deploy.ps1` (recomendado)

```powershell
.\deploy.ps1            # build si hace falta + install widget + launcher + plugin
.\deploy.ps1 -Force     # fuerza rebuild
.\deploy.ps1 -SkipInstall
```

### Manual

1. Abrir el proyecto Gradle (`ATkoreaderhistory`)
2. Compilar: `gradlew.bat :app:assembleDebug :launcher:assembleDebug`
3. Instalar los APKs en el dispositivo
4. Conceder acceso a **todos los archivos** (lectura del historial KOReader)
5. **Widget**: añadir **KOReader History** a la pantalla de inicio y configurar  
6. **Launcher** (opcional): definir **Atelier e-ink Launcher** como app de inicio; activar notificaciones / accesibilidad / uso según haga falta

### Bridge KOReader

Desde la app widget: **Instalar el bridge KOReader**, vía `deploy.ps1`, **o** copiar manualmente:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Luego reinicie KOReader y active el plugin si hace falta.

## ✏️ Uso — widget

* Muestra las obras recientes (portada / título / autor / porcentaje según opciones)
* Un toque abre el libro en KOReader
* El **dock del widget** (independiente del launcher) lanza acciones Android o escribe una acción « pending »; el bridge la ejecuta al arrancar KOReader
* Casilla **Mostrar el dock** + **Editar el dock** / restaurar atajos por defecto

## 🏠 Uso — launcher e-ink

Pantalla completa (barra de sistema oculta). Pulsación larga en el escritorio para el menú.

| Zona | Contenido |
|------|-----------|
| Barra de estado (opcional) | Hora, fecha (formatos: Lunes, Lun. 27 nov., …), Wi‑Fi, Bluetooth, batería |
| Widgets Android | Añadir / redimensionar / ordenar |
| Panel de historial | Libros KOReader + botón Quitar |
| Cuadrícula del escritorio | Apps, carpetas, atajos KOReader — columnas / tamaños ajustables, colocación por celdas |
| Dock del launcher | Apps + notificaciones (badge) + slots personalizados — show/hide independiente del widget |
| Cajón de apps | Búsqueda, anclar al dock, iconos custom, ocultar apps |

**Modo desplazamiento**: menú del escritorio → *Modo desplazamiento* → flechas en cada icono → *Terminar*.

También: Alt+Tab / barra de tareas, listener de notificaciones, servicio de accesibilidad (hotkeys), icon packs (lista completa + búsqueda).

## 🎯 Acciones del dock (widget y launcher)

| Acción | Lado | Descripción |
|--------|------|-------------|
| Abrir KOReader | Android | Lanza la app KOReader |
| Abrir un libro | Android | Abre un archivo vía KOReader |
| Lanzar una app Android | Android | Package / actividad |
| Abrir URI / intent | Android | URI libre |
| Continuar la lectura | Bridge | Última entrada del historial |
| Historial KOReader | Bridge | Abre el historial |
| Carpeta | Bridge | Abre una carpeta en el file manager |
| Colección | Bridge | Abre una colección |
| Plugin | Bridge | Lanza la UI de un plugin |
| Acción de sistema | Bridge | Módulo / dispatcher |
| Dispatcher (ID libre) | Bridge | ID de acción Dispatcher |

## ⚙️ Configuración

### Widget (`fr.atelier.koreaderhistory`)

* Carpeta KOReader, package (`org.koreader.launcher` / F-Droid / auto)
* Número de obras, excluir terminados / no-libros
* Mostrar autor / porcentaje / páginas, nombre de archivo, portada letra, tema oscuro
* Dock: mostrar, arriba/abajo, tamaño, marcos, edición de slots

### Launcher (`fr.atelier.einklauncher`)

* Tamaños / marcos / columnas: dock, escritorio, cajón
* Fuentes: etiquetas escritorio / cajón, barra de estado, historial
* Barra de estado: elementos + formato de fecha
* Apps ocultas / iconos custom (cajón y escritorio)
* Iconos del dock **Aplicaciones** y **Notificaciones** personalizables
* Enlaces: almacenamiento, home, accesibilidad, notificaciones, usage stats

## 🔗 Recursos

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
