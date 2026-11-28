<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Widget · Dock · E-Ink-Launcher · Android</strong><br/>Apps · athistory.koplugin-Bridge · l’Atelier</p>
</td>
</tr>
</table>

<a href="https://atelier.atechnologie.fr/" title="l'Atelier – Verein für Buchherstellung und Forschungswerkzeuge"><img src="https://raw.githubusercontent.com/Atelier-Recherche/.github/main/profile/logo.jpg" alt="l'Atelier" width="200" /></a>  
<sub>Entwickelt von <a href="https://atelier.atechnologie.fr/">l'Atelier</a> — Buchherstellung und Forschungswerkzeuge (EHESS)</sub>

<p>
🇫🇷 <a href="README.md">Français</a> ·
🇬🇧 <a href="README.en.md">English</a> ·
🇩🇪 <a href="README.de.md"><b>Deutsch</b></a> ·
🇪🇸 <a href="README.es.md">Español</a>
</p>

<p>
<a href="https://atelier.atechnologie.fr/"><img src="https://img.shields.io/badge/🌐_l'Atelier-atelier.atechnologie.fr-2d5016?style=for-the-badge" alt="Website l'Atelier" /></a>

</p>

</div>

---

## 📸 Vorschau

| Widget | Dock |
| :---: | :---: |
| <img src="readme-media/screen1.jpg" alt="KOReader History — Widget" width="400" /> | <img src="readme-media/screen2.jpg" alt="KOReader History — Dock" width="400" /> |

## 📖 Über

Android-Monorepo (von l’Atelier) mit **zwei unabhängigen Apps** und einer gemeinsamen KOReader-Bridge:

1. **KOReader History** — Lesehistorie-Widget + Aktions-Dock  
2. **Atelier e-ink Launcher** — Vollbild-Launcher für E-Ink / Boox (Desktop, Dock, App-Schublade, Benachrichtigungen…)

Beide Apps teilen sich `core/` und das Plugin **Atelier History Bridge** (`athistory.koplugin`). Das **Widget-Dock** und das **Launcher-Dock** sind **getrennt** (eigene Prefs und Slots): Sie können nur das Widget, nur den Launcher oder beides installieren.

## 🧩 Komponenten

| Teil | Package / Pfad | Rolle |
|------|----------------|------|
| Widget-App | `fr.atelier.koreaderhistory` (`app/`) | Historie-Widget + Config + Widget-Dock |
| Launcher | `fr.atelier.einklauncher` (`launcher/`) | E-Ink-Home: Desktop, Dock, Apps, Notifs |
| Core | `core/` | Prefs, Historie, Dock, Icons, Bridge-Helfer |
| `athistory.koplugin` | KOReader-Plugins | Bridge: liest `settings/athistory/pending.lua` und führt die Aktion aus |
| `deploy.ps1` | Repo-Wurzel | Build + Install beider APKs + ADB-Grants + Plugin-Push |

## ⬇️ Installation

### Mit `deploy.ps1` (empfohlen)

```powershell
.\deploy.ps1            # Build bei Bedarf + Install Widget + Launcher + Plugin
.\deploy.ps1 -Force     # Rebuild erzwingen
.\deploy.ps1 -SkipInstall
```

### Manuell

1. Gradle-Projekt öffnen (`ATkoreaderhistory`)
2. Bauen: `gradlew.bat :app:assembleDebug :launcher:assembleDebug`
3. APKs auf dem Gerät installieren
4. Zugriff auf **alle Dateien** gewähren (Lesen der KOReader-Historie)
5. **Widget**: **KOReader History** zum Startbildschirm hinzufügen und konfigurieren  
6. **Launcher** (optional): **Atelier e-ink Launcher** als Home-App setzen; Benachrichtigungs- / Barrierefreiheits- / Nutzungszugriff bei Bedarf aktivieren

### KOReader-Bridge

Aus der Widget-App: **KOReader-Bridge installieren**, über `deploy.ps1`, **oder** manuell kopieren:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Danach KOReader neu starten und das Plugin bei Bedarf aktivieren.

## ✏️ Nutzung — Widget

* Zeigt kürzlich gelesene Werke (Cover / Titel / Autor / Prozent je nach Optionen)
* Ein Tippen öffnet das Buch in KOReader
* Das **Widget-Dock** (unabhängig vom Launcher) startet Android-Aktionen oder schreibt eine „pending“-Aktion; die Bridge führt sie beim KOReader-Start aus
* Schalter **Dock anzeigen** + **Dock bearbeiten** / Standard-Shortcuts wiederherstellen

## 🏠 Nutzung — E-Ink-Launcher

Vollbild (System-Statusleiste ausgeblendet). Langer Druck auf dem Desktop für das Menü.

| Bereich | Inhalt |
|---------|--------|
| Statusleiste (optional) | Uhrzeit, Datum (Formate: Montag, Mo. 27. Nov., …), WLAN, Bluetooth, Akku |
| Android-Widgets | Hinzufügen / Größe ändern / Reihenfolge |
| Historie-Panel | KOReader-Bücher + Entfernen-Button |
| Desktop-Raster | Apps, Ordner, KOReader-Shortcuts — einstellbare Spalten / Größen, Zellenplatzierung |
| Launcher-Dock | Apps + Benachrichtigungen (Badge) + eigene Slots — Show/Hide unabhängig vom Widget |
| App-Schublade | Suche, an Dock anheften, eigene Icons, Apps ausblenden |

**Verschiebe-Modus**: Desktop-Menü → *Verschiebe-Modus* → Pfeile an jedem Icon → *Fertig*.

Außerdem: Alt+Tab / Taskleiste, Notification-Listener, Accessibility-Service (Hotkeys), Icon-Packs (volle Liste + Suche).

## 🎯 Dock-Aktionen (Widget & Launcher)

| Aktion | Seite | Beschreibung |
|--------|------|-------------|
| KOReader öffnen | Android | KOReader starten |
| Buch öffnen | Android | Datei über KOReader öffnen |
| Android-App starten | Android | Package / Activity |
| URI / Intent öffnen | Android | Freie URI |
| Weiterlesen | Bridge | Letzter Historieneintrag |
| KOReader-Historie | Bridge | Historie öffnen |
| Ordner | Bridge | Ordner im Dateimanager öffnen |
| Sammlung | Bridge | Sammlung öffnen |
| Plugin | Bridge | Plugin-UI starten |
| Systemaktion | Bridge | Modul / Dispatcher |
| Dispatcher (freie ID) | Bridge | Dispatcher-Aktions-ID |

## ⚙️ Konfiguration

### Widget (`fr.atelier.koreaderhistory`)

* KOReader-Ordner, Package (`org.koreader.launcher` / F-Droid / auto)
* Anzahl, fertige / Nicht-Bücher ausschließen
* Autor / Prozent / Seiten, Dateiname, Buchstaben-Cover, dunkles Theme
* Dock: anzeigen, oben/unten, Größe, Rahmen, Slot-Bearbeitung

### Launcher (`fr.atelier.einklauncher`)

* Größen / Rahmen / Spalten: Dock, Desktop, Schublade
* Schriften: Desktop- / Schubladen-Labels, Statusleiste, Historie
* Statusleiste: Elemente + Datumsformat
* Ausgeblendete Apps / eigene Icons (Schublade & Desktop)
* Anpassbare Dock-Icons **Apps** und **Benachrichtigungen**
* Links: Speicher, Home, Barrierefreiheit, Benachrichtigungen, Nutzungsstatistik

## 🔗 Ressourcen

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
