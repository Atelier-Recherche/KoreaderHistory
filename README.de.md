<div align="center" id="head">


<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Verlaufs-Widget · Dock · Android</strong><br/>App · athistory.koplugin-Bridge · l’Atelier</p>
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

Android-App **KOReader History** (von l’Atelier): Lesehistorie-Widget und Aktions-Dock, verbunden mit KOReader über das Bridge-Plugin **Atelier History Bridge** (`athistory.koplugin`).

Das Widget liest die KOReader-Historie auf dem Gerät; das Dock kann KOReader, ein Buch, eine App, eine URI öffnen oder die Bridge bitten, eine Aktion in KOReader auszuführen (Historie, Ordner, Sammlung, Plugin, Dispatcher…).

## 🧩 Komponenten

| Teil | Rolle |
|------|------|
| Android-App (`app/`, Gradle) | Widget + Konfiguration + Dock |
| `athistory.koplugin` | Bridge: liest `settings/athistory/pending.lua` und führt die Aktion aus |
| Eingebettete Assets | Bridge-Kopie, installierbar aus der App |

Package: `fr.atelier.koreaderhistory`.

## ⬇️ Installation

### Android-Anwendung

1. Gradle-Projekt öffnen (`ATkoreaderhistory`)
2. Bauen: `gradlew.bat assembleDebug` (oder Android Studio)
3. APK auf dem Gerät installieren
4. Zugriff auf **alle Dateien** gewähren (nötig zum Lesen der KOReader-Historie)
5. Widget **KOReader History** zum Startbildschirm hinzufügen
6. KOReader-Pfad, Anzahl, Filter, Theme, Dock konfigurieren…

### KOReader-Bridge

Aus der App: **KOReader-Bridge installieren** (kopiert `athistory.koplugin` in die KOReader-Plugins), **oder** manuell kopieren:

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Danach KOReader neu starten und das Plugin bei Bedarf aktivieren.

## ✏️ Nutzung

* Das Widget zeigt kürzlich gelesene Werke (Cover / Titel / Autor / Prozent je nach Optionen)
* Ein Tippen öffnet das Buch in KOReader
* Das **Dock** (konfigurierbare Icons) startet Android-Aktionen oder schreibt eine „pending“-Aktion und öffnet KOReader; die Bridge führt sie beim Start aus

### Dock-Aktionen

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

## ⚙️ Widget-Konfiguration

* KOReader-Ordner, Package (`org.koreader.launcher` / F-Droid / auto)
* Anzahl, fertige / Nicht-Bücher ausschließen
* Autor / Prozent anzeigen, dunkles Theme (E-Ink)
* Dock oben, Icon-Größe, Slot-Bearbeitung

## 🔗 Ressourcen

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
