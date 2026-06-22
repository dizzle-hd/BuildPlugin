# BuildPlugin

Ein Minecraft Build-Battle-Plugin für **Paper 1.21.x**.  
Vier Spieler oder zwei Teams bauen in einer automatisch generierten Void-Arena und bewerten anschließend gegenseitig ihre Builds.

## Features

- Automatisch generierte Void-Arena (`build_arena`) mit 4 Plattformen (je 128×128 Blöcke)
- Barrier-Wände zwischen den Parzellen – keine Wege raus
- Konfigurierbare Bauzeit (30 / 45 / 60 / 90 / 120 min) per NPC oder GUI
- Solo- und Teammodus (2 Spieler pro Team)
- Voting-Phase: Spieler fliegen frei und bewerten Builds mit `/vote <1-5>`
- Klickbare NPC-Lobbys in der Hauptwelt für einfachen Spieleinstieg
- Einstellungs-GUI für Admins
- Automatisches Zurückteleportieren nach Spielende

## Anforderungen

- Paper 1.21.4
- Java 21

## Installation

1. JAR aus den [Releases](../../releases) herunterladen
2. JAR in den `plugins/`-Ordner des Servers legen
3. Server (neu) starten – die Welt `build_arena` wird automatisch erstellt

## Befehle

| Befehl | Beschreibung | Permission |
|--------|-------------|------------|
| `/build gui` | Einstellungs-GUI öffnen | `buildplugin.admin` |
| `/build start` | Spiel starten | `buildplugin.admin` |
| `/build stop` | Spiel abbrechen | `buildplugin.admin` |
| `/build addplayer <spieler> <team>` | Spieler manuell hinzufügen | `buildplugin.admin` |
| `/build status` | Spielstatus & verbleibende Zeit | `buildplugin.admin` |
| `/build npc place <30\|45\|60\|90\|120>` | NPC an aktueller Position setzen | `buildplugin.admin` |
| `/build npc remove` | Nächsten NPC entfernen (≤10 Blöcke) | `buildplugin.admin` |
| `/build npc list` | Alle NPCs anzeigen | `buildplugin.admin` |
| `/vote <1-5>` | In der Voting-Phase abstimmen | `buildplugin.vote` |

## NPC-System

Platziere Villager-NPCs in der Hauptwelt, die Spieler direkt in eine Runde bringen:

```
/build npc place 30   → NPC für 30-Minuten-Runden
/build npc place 60   → NPC für 60-Minuten-Runden
```

Ein Klick auf den NPC öffnet ein Menü zur Auswahl von **Solo** oder **Team (2 Spieler)**.  
NPC-Positionen werden in der `config.yml` gespeichert und beim Neustart wiederhergestellt.

## Spielablauf

```
1. WARTEN    – Spieler klicken NPC → Solo/Team wählen
2. BAUEN     – Bauzeit läuft, Spieler bauen in ihrer Parzelle
3. VOTING    – Außenwände weg, Spieler fliegen und tippen /vote <1-5>
4. ERGEBNIS  – Punkte werden summiert, Sieger verkündet
               Spieler werden zurück zum Spawn teleportiert
```

## Konfiguration (`config.yml`)

```yaml
game:
  build-time: 3600      # Standard-Bauzeit in Sekunden
  voting-time: 600      # Abstimmzeit in Sekunden
  area-size: 128        # Größe jeder Parzelle in Blöcken
  wall-height: 64       # Höhe der Barrier-Wände
  mode: solo            # solo | teams
```

## Bauen aus dem Quellcode

```bash
git clone https://github.com/dizzle-hd/BuildPlugin.git
cd BuildPlugin
mvn clean package
# JAR liegt in target/BuildPlugin.jar
```

## Lizenz

MIT
