<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>History widget · Dock · Android</strong><br/>App · athistory.koplugin bridge · l’Atelier</p>
</td>
</tr>
</table>

<a href="https://atelier.atechnologie.fr/" title="l'Atelier – book fabrication and research tools association"><img src="https://raw.githubusercontent.com/Atelier-Recherche/.github/main/profile/logo.jpg" alt="l'Atelier" width="200" /></a>  
<sub>Developed by <a href="https://atelier.atechnologie.fr/">l'Atelier</a> — book fabrication and research tools (EHESS)</sub>

<p>
🇫🇷 <a href="README.md">Français</a> ·
🇬🇧 <a href="README.en.md"><b>English</b></a> ·
🇩🇪 <a href="README.de.md">Deutsch</a> ·
🇪🇸 <a href="README.es.md">Español</a>
</p>

<p>
<a href="https://atelier.atechnologie.fr/"><img src="https://img.shields.io/badge/🌐_l'Atelier-atelier.atechnologie.fr-2d5016?style=for-the-badge" alt="l'Atelier website" /></a>

</p>

</div>

---

## 📸 Preview

| Widget | Dock |
| :---: | :---: |
| <img src="readme-media/screen1.jpg" alt="KOReader History — Widget" width="400" /> | <img src="readme-media/screen2.jpg" alt="KOReader History — Dock" width="400" /> |

## 📖 About

Android app **KOReader History** (by l’Atelier): reading-history widget and action dock, linked to KOReader through the **Atelier History Bridge** plugin (`athistory.koplugin`).

The widget reads KOReader history on the device; the dock can open KOReader, a book, an app, a URI, or ask the bridge to run an action inside KOReader (history, folder, collection, plugin, dispatcher…).

## 🧩 Components

| Piece | Role |
|-------|------|
| Android app (`app/`, Gradle) | Widget + configuration + dock |
| `athistory.koplugin` | Bridge: reads `settings/athistory/pending.lua` and runs the action |
| Bundled assets | Bridge copy installable from the app |

Package: `fr.atelier.koreaderhistory`.

## ⬇️ Installation

### Android application

1. Open the Gradle project (`ATkoreaderhistory`)
2. Build: `gradlew.bat assembleDebug` (or Android Studio)
3. Install the APK on the device
4. Grant **All files access** (needed to read KOReader history)
5. Add the **KOReader History** widget to the home screen
6. Configure KOReader path, item count, filters, theme, dock…

### KOReader bridge

From the app: **Install KOReader bridge** (copies `athistory.koplugin` into KOReader plugins), **or** copy manually:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Then restart KOReader and enable the plugin if needed.

## ✏️ Usage

* The widget shows recent books (cover / title / author / percent depending on options)
* A tap opens the book in KOReader
* The **dock** (configurable icons) launches Android actions or writes a “pending” action then opens KOReader; the bridge executes it on startup

### Dock actions

| Action | Side | Description |
|--------|------|-------------|
| Open KOReader | Android | Launch KOReader |
| Open a book | Android | Open a file via KOReader |
| Launch an Android app | Android | Package / activity |
| Open URI / intent | Android | Free URI |
| Continue reading | Bridge | Latest history entry |
| KOReader history | Bridge | Open history |
| Folder | Bridge | Open a folder in the file manager |
| Collection | Bridge | Open a collection |
| Plugin | Bridge | Launch a plugin UI |
| System action | Bridge | Module / dispatcher |
| Dispatcher (free ID) | Bridge | Dispatcher action ID |

## ⚙️ Widget configuration

* KOReader folder, package (`org.koreader.launcher` / F-Droid / auto)
* Item count, exclude finished / non-books
* Show author / percent, dark theme (e-ink)
* Dock on top, icon size, slot editing

## 🔗 Resources

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
