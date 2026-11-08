<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Widget de historial · Dock · Android</strong><br/>App · bridge athistory.koplugin · l’Atelier</p>
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

Aplicación Android **KOReader History** (por l’Atelier): widget de historial de lectura y dock de acciones, enlazados a KOReader mediante el plugin bridge **Atelier History Bridge** (`athistory.koplugin`).

El widget lee el historial de KOReader en el dispositivo; el dock puede abrir KOReader, un libro, una app, una URI, o pedir al bridge que ejecute una acción en KOReader (historial, carpeta, colección, plugin, dispatcher…).

## 🧩 Componentes

| Elemento | Función |
|----------|---------|
| App Android (`app/`, Gradle) | Widget + configuración + dock |
| `athistory.koplugin` | Bridge: lee `settings/athistory/pending.lua` y ejecuta la acción |
| Assets incluidos | Copia del bridge instalable desde la app |

Package: `fr.atelier.koreaderhistory`.

## ⬇️ Instalación

### Aplicación Android

1. Abrir el proyecto Gradle (`ATkoreaderhistory`)
2. Compilar: `gradlew.bat assembleDebug` (o Android Studio)
3. Instalar el APK en el dispositivo
4. Conceder acceso a **todos los archivos** (necesario para leer el historial KOReader)
5. Añadir el widget **KOReader History** a la pantalla de inicio
6. Configurar la ruta KOReader, número de obras, filtros, tema, dock…

### Bridge KOReader

Desde la app: **Instalar el bridge KOReader** (copia `athistory.koplugin` en los plugins de KOReader), **o** copiar manualmente:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Luego reinicie KOReader y active el plugin si hace falta.

## ✏️ Uso

* El widget muestra las obras recientes (portada / título / autor / porcentaje según opciones)
* Un toque abre el libro en KOReader
* El **dock** (iconos configurables) lanza acciones Android o escribe una acción « pending » y abre KOReader; el bridge la ejecuta al arrancar

### Acciones del dock

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

## ⚙️ Configuración del widget

* Carpeta KOReader, package (`org.koreader.launcher` / F-Droid / auto)
* Número de obras, excluir terminados / no-libros
* Mostrar autor / porcentaje, tema oscuro (e-ink)
* Dock arriba, tamaño de iconos, edición de slots

## 🔗 Recursos

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
