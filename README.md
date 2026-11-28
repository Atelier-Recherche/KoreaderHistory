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

<a href="https://atelier.atechnologie.fr/" title="l'Atelier – Association de fabrication de livres et d'outils de recherche"><img src="https://raw.githubusercontent.com/Atelier-Recherche/.github/main/profile/logo.jpg" alt="l'Atelier" width="200" /></a>  
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

Monorepo Android (par l’Atelier) avec **deux applications indépendantes** et un bridge KOReader commun :

1. **KOReader History** — widget d’historique de lecture + dock d’actions  
2. **Atelier e-ink Launcher** — lanceur plein écran pensé e-ink / Boox (bureau, dock, tiroir, notifications…)

Les deux apps partagent le module `core/` et le plugin **Atelier History Bridge** (`athistory.koplugin`). Le dock du **widget** et celui du **launcher** sont **séparés** (prefs et slots distincts) : on peut n’installer que le widget, que le launcher, ou les deux.

## 🧩 Composants

| Élément | Package / chemin | Rôle |
|---------|------------------|------|
| Widget app | `fr.atelier.koreaderhistory` (`app/`) | Widget historique + config + dock widget |
| Launcher | `fr.atelier.einklauncher` (`launcher/`) | Home e-ink : bureau, dock, apps, notifs |
| Core | `core/` | Prefs, historique, dock, icônes, bridge helpers |
| `athistory.koplugin` | plugins KOReader | Bridge : lit `settings/athistory/pending.lua` et exécute l’action |
| `deploy.ps1` | racine | Build + install des 2 APKs + grants ADB + push plugin |

## ⬇️ Installation

### Via `deploy.ps1` (recommandé)

```powershell
.\deploy.ps1            # build si besoin + install widget + launcher + plugin
.\deploy.ps1 -Force     # force rebuild
.\deploy.ps1 -SkipInstall
```

### Manuel

1. Ouvrir le projet Gradle (`ATkoreaderhistory`)
2. Compiler : `gradlew.bat :app:assembleDebug :launcher:assembleDebug`
3. Installer les APKs sur l’appareil
4. Accorder l’accès à **tous les fichiers** (lecture historique KOReader)
5. **Widget** : ajouter **KOReader History** à l’écran d’accueil et configurer  
6. **Launcher** (optionnel) : définir **Atelier e-ink Launcher** comme application d’accueil ; activer accès notifications / accessibilité / usage si besoin

### Bridge KOReader

Depuis l’app widget : **Installer le bridge KOReader**, via `deploy.ps1`, **ou** copier manuellement :

```
<koreader>/plugins/athistory.koplugin/
├── _meta.lua
└── main.lua
```

Puis redémarrer KOReader et activer le plugin si besoin.

## ✏️ Utilisation — widget

* Affiche les ouvrages récents (couverture / titre / auteur / pourcentage selon les options)
* Un tap ouvre le livre dans KOReader
* Le **dock widget** (indépendant du launcher) lance des actions Android ou écrit une action « pending » ; le bridge l’exécute au démarrage de KOReader
* Case **Afficher le dock** + **Éditer le dock** / restaurer les raccourcis par défaut

## 🏠 Utilisation — launcher e-ink

Plein écran (barre système masquée). Long-press sur le bureau pour le menu.

| Zone | Contenu |
|------|---------|
| Barre d’état (optionnelle) | Heure, date (formats : Lundi, Lun. 27 nov., …), Wi‑Fi, Bluetooth, batterie |
| Widgets Android | Ajout / redimensionnement / ordre |
| Panneau historique | Livres KOReader + bouton Retirer |
| Grille bureau | Apps, dossiers, raccourcis KOReader — colonnes / tailles réglables, placement par cellules |
| Dock launcher | Apps + notifications (badge) + slots personnalisés — show/hide indépendant du widget |
| Tiroir d’apps | Recherche, épinglage dock, icônes custom, masquer des apps |

**Mode déplacement** : menu bureau → *Mode déplacement* → flèches sur chaque icône → *Terminer*.

Autres : Alt+Tab / barre de tâches, écouteur de notifications, service d’accessibilité (hotkeys), icon packs (liste complète + recherche).

## 🎯 Actions du dock (widget & launcher)

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

## ⚙️ Configuration

### Widget (`fr.atelier.koreaderhistory`)

* Dossier KOReader, package (`org.koreader.launcher` / F-Droid / auto)
* Nombre d’ouvrages, exclure terminés / non-livres
* Afficher auteur / pourcentage / pages, nom de fichier, couverture lettre, thème sombre
* Dock : afficher, position haut/bas, taille, cadres, édition des slots

### Launcher (`fr.atelier.einklauncher`)

* Tailles / cadres / colonnes : dock, bureau, tiroir
* Polices : libellés bureau / tiroir, barre d’état, historique
* Barre d’état : éléments + format de date
* Apps masquées / icônes custom (tiroir & bureau)
* Icônes dock **Applications** et **Notifications** personnalisables
* Liens : stockage, home, accessibilité, notifications, usage stats

## 🔗 Ressources

| | |
| --- | --- |
| 🌐 **l'Atelier** | [atelier.atechnologie.fr](https://atelier.atechnologie.fr/) |
| 📖 **KOReader** | [github.com/koreader/koreader](https://github.com/koreader/koreader) |

---

<div align="center">

<sub>🇫🇷 <a href="README.md">Français</a> · <a href="README.en.md">🇬🇧 English</a> · <a href="README.de.md">🇩🇪 Deutsch</a> · <a href="README.es.md">🇪🇸 Español</a> · KOReader History — <a href="https://atelier.atechnologie.fr/">l'Atelier</a></sub>

</div>
