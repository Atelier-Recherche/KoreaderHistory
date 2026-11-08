<div align="center">

<table>
<tr>
<td><img src="readme-media/logo_koreaderhistory.png" alt="KOReader History" width="140" /></td>
<td align="left">
<h1 style="margin:0">KOReader History</h1>
<p style="margin:0.25em 0 0"><strong>Widget historique · Dock · Android</strong><br/>App · bridge athistory.koplugin · l’Atelier</p>
</td>
</tr>
</table>

<a href="https://atelier.atechnologie.fr/" title="l'Atelier – Association de fabrication de livres et d'outils de recherche"><img src="readme-media/logoasso.jpg" alt="l'Atelier" width="200" /></a>  
<sub>Développé par <a href="https://atelier.atechnologie.fr/">l'Atelier</a> — fabrication de livres et outils de recherche (EHESS)</sub>

<p>
🇫🇷 <a href="README.md"><b>Français</b></a> ·
🇬🇧 <a href="README.en.md">English</a> ·
🇩🇪 <a href="README.de.md">Deutsch</a> ·
🇪🇸 <a href="README.es.md">Español</a>
</p>

<p>
<a href="https://atelier.atechnologie.fr/"><img src="https://img.shields.io/badge/🌐_l'Atelier-atelier.atechnologie.fr-2d5016?style=for-the-badge" alt="Site l'Atelier" /></a>

</p>

</div>

---

## 📸 Aperçu

| Widget | Dock |
| :---: | :---: |
| <img src="readme-media/screen1.jpg" alt="KOReader History — Widget" width="400" /> | <img src="readme-media/screen2.jpg" alt="KOReader History — Dock" width="400" /> |

## 📖 À propos

Application Android **KOReader History** (par l’Atelier) : widget d’historique de lecture et dock d’actions, reliés à KOReader via le plugin bridge **Atelier History Bridge** (`athistory.koplugin`).

Le widget lit l’historique KOReader sur l’appareil ; le dock peut ouvrir KOReader, un livre, une app, une URI, ou demander au bridge d’exécuter une action dans KOReader (historique, dossier, collection, plugin, dispatcher…).

## 🧩 Composants

| Élément | Rôle |
|---------|------|
| App Android (`app/`, Gradle) | Widget + configuration + dock |
| `athistory.koplugin` | Bridge : lit `settings/athistory/pending.lua` et exécute l’action |
| Assets embarqués | Copie du bridge installable depuis l’app |

Package : `fr.atelier.koreaderhistory`.

## ⬇️ Installation

### Application Android

1. Ouvrir le projet Gradle (`ATkoreaderhistory`)
2. Compiler : `gradlew.bat assembleDebug` (ou depuis Android Studio)
3. Installer l’APK sur l’appareil
4. Accorder l’accès à **tous les fichiers** (nécessaire pour lire l’historique KOReader)
5. Ajouter le widget **KOReader History** à l’écran d’accueil
6. Configurer le chemin KOReader, le nombre d’ouvrages, filtres, thème, dock…

### Bridge KOReader

Depuis l’app : **Installer le bridge KOReader** (copie `athistory.koplugin` dans le dossier plugins KOReader), **ou** copier manuellement :

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Puis redémarrer KOReader et activer le plugin si besoin.

## ✏️ Utilisation

* Le widget affiche les ouvrages récents (couverture / titre / auteur / pourcentage selon les options)
* Un tap ouvre le livre dans KOReader
* Le **dock** (icônes configurables) lance des actions Android ou écrit une action « pending » puis ouvre KOReader ; le bridge l’exécute au démarrage

### Actions du dock

| Action | Côté | Description |
|--------|------|-------------|
| Ouvrir KOReader | Android | Lance l’app KOReader |
| Ouvrir un livre | Android | Ouvre un fichier via KOReader |
| Lancer une app Android | Android | Package / activité |
| Ouvrir URI / intent | Android | URI libre |
| Continuer la lecture | Bridge | Dernier ouvrage de l’historique |
| Historique KOReader | Bridge | Ouvre l’historique |
| Dossier | Bridge | Ouvre un dossier dans le file manager |
| Collection | Bridge | Ouvre une collection |
| Plugin | Bridge | Lance l’UI d’un plugin |
| Action système | Bridge | Module / dispatcher |
| Dispatcher (ID libre) | Bridge | ID d’action Dispatcher |

## ⚙️ Configuration (widget)

* Dossier KOReader, package (`org.koreader.launcher` / F-Droid / auto)
* Nombre d’ouvrages, exclure terminés / non-livres
* Afficher auteur / pourcentage, thème sombre (e-ink)
* Dock en haut, taille des icônes, édition des slots

## 🔗 Ressources

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
