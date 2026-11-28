<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Widget · Dock · E-ink launcher · Android</strong><br/>Apps · athistory.koplugin bridge · l’Atelier</p>
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

Android monorepo (by l’Atelier) with **two independent apps** and a shared KOReader bridge:

1. **KOReader History** — reading-history widget + action dock  
2. **Atelier e-ink Launcher** — fullscreen home launcher aimed at e-ink / Boox (desktop, dock, drawer, notifications…)

Both apps share `core/` and the **Atelier History Bridge** plugin (`athistory.koplugin`). The **widget dock** and the **launcher dock** are **separate** (distinct prefs and slots): you can install only the widget, only the launcher, or both.

## 🧩 Components

| Piece | Package / path | Role |
|-------|----------------|------|
| Widget app | `fr.atelier.koreaderhistory` (`app/`) | History widget + config + widget dock |
| Launcher | `fr.atelier.einklauncher` (`launcher/`) | E-ink home: desktop, dock, apps, notifs |
| Core | `core/` | Prefs, history, dock, icons, bridge helpers |
| `athistory.koplugin` | KOReader plugins | Bridge: reads `settings/athistory/pending.lua` and runs the action |
| `deploy.ps1` | repo root | Build + install both APKs + ADB grants + push plugin |

## ⬇️ Installation

### Via `deploy.ps1` (recommended)

```powershell
.\deploy.ps1            # build if needed + install widget + launcher + plugin
.\deploy.ps1 -Force     # force rebuild
.\deploy.ps1 -SkipInstall
```

### Manual

1. Open the Gradle project (`ATkoreaderhistory`)
2. Build: `gradlew.bat :app:assembleDebug :launcher:assembleDebug`
3. Install the APKs on the device
4. Grant **All files access** (needed to read KOReader history)
5. **Widget**: add **KOReader History** to the home screen and configure  
6. **Launcher** (optional): set **Atelier e-ink Launcher** as the default home app; enable notification / accessibility / usage access as needed

### KOReader bridge

From the widget app: **Install KOReader bridge**, via `deploy.ps1`, **or** copy manually:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Then restart KOReader and enable the plugin if needed.

## ✏️ Usage — widget

* Shows recent books (cover / title / author / percent depending on options)
* A tap opens the book in KOReader
* The **widget dock** (independent from the launcher) launches Android actions or writes a “pending” action; the bridge runs it when KOReader starts
* **Show dock** toggle + **Edit dock** / restore default shortcuts

## 🏠 Usage — e-ink launcher

Fullscreen (system status bar hidden). Long-press the desktop for the menu.

| Area | Content |
|------|---------|
| Status bar (optional) | Time, date (formats: Monday, Mon 27 Nov, …), Wi‑Fi, Bluetooth, battery |
| Android widgets | Add / resize / reorder |
| History panel | KOReader books + Remove button |
| Desktop grid | Apps, folders, KOReader shortcuts — adjustable columns / sizes, cell placement |
| Launcher dock | Apps + notifications (badge) + custom slots — show/hide independent from the widget |
| App drawer | Search, pin to dock, custom icons, hide apps |

**Move mode**: desktop menu → *Move mode* → arrows on each icon → *Done*.

Also: Alt+Tab / task bar, notification listener, accessibility service (hotkeys), icon packs (full list + search).

## 🎯 Dock actions (widget & launcher)

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

## ⚙️ Configuration

### Widget (`fr.atelier.koreaderhistory`)

* KOReader folder, package (`org.koreader.launcher` / F-Droid / auto)
* Item count, exclude finished / non-books
* Show author / percent / pages, filename, letter cover, dark theme
* Dock: show, top/bottom, size, borders, slot editing

### Launcher (`fr.atelier.einklauncher`)

* Sizes / borders / columns: dock, desktop, drawer
* Fonts: desktop / drawer labels, status bar, history
* Status bar: elements + date format
* Hidden apps / custom icons (drawer & desktop)
* Customizable dock **Apps** and **Notifications** icons
* Links: storage, home, accessibility, notifications, usage stats

## 🔗 Resources

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
