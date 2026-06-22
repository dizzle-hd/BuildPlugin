# BuildPlugin

Ein Minecraft Build-Battle-Plugin für **Paper 1.21.x**.  
Vier Spieler oder zwei Teams bauen in einer automatisch generierten Void-Arena und bewerten anschließend gegenseitig ihre Builds.

## Features

- Automatisch generierte Void-Arena (`build_arena`) mit 4 Plattformen (je 128×128 Blöcke)
- Barrier-Wände zwischen den Parzellen
- Queue-System mit Action Bar: Spieler treten einer Warteschlange bei und das Spiel startet automatisch bei 4 Spielern
- Kompatibel mit jedem NPC-Plugin – einfach `/build join solo 60` als Klick-Befehl verlinken
- Solo- und Teammodus (2 Spieler pro Team)
- Konfigurierbare Bauzeit per GUI oder Queue-Befehl
- Voting-Phase: Spieler fliegen frei und bewerten Builds mit `/vote <1-5>`
- Test-Befehl für Admins zur schnellen Überprüfung der Map-Generierung

## Anforderungen

- Paper 1.21.4
- Java 21

## Installation

1. JAR aus den [Releases](../../releases) herunterladen
2. JAR in den `plugins/`-Ordner legen
3. Server starten – Welt `build_arena` wird automatisch als Void-Welt erstellt

## Befehle

### Spieler-Befehle
| Befehl | Beschreibung |
|--------|-------------|
| `/build join <solo\|team> [minuten]` | Queue beitreten |
| `/build leave` | Queue verlassen |
| `/build queue` | Aktive Queues anzeigen |
| `/vote <1-5>` | In der Voting-Phase abstimmen |

### Admin-Befehle
| Befehl | Beschreibung |
|--------|-------------|
| `/build test` | Testspiel starten (2 min Bau, 1 min Voting) |
| `/build gui` | Einstellungs-GUI öffnen |
| `/build start` | Spiel manuell starten |
| `/build stop` | Spiel abbrechen |
| `/build addplayer <spieler> <team>` | Spieler manuell hinzufügen |
| `/build status` | Spielstatus & Queue-Übersicht |

## NPC-Plugin Integration

Das Plugin enthält kein eigenes NPC-System – verwende dein bevorzugtes NPC-Plugin (z.B. Citizens, FancyNPCs, ZNPCsPlus) und verlinke folgende Befehle als Klick-Aktion:

```
/build join solo 30    → Solo-Queue, 30 Minuten Bauzeit
/build join solo 60    → Solo-Queue, 60 Minuten Bauzeit
/build join team 60    → Team-Queue, 60 Minuten Bauzeit
```

Spieler sehen dann automatisch in der **Action Bar** (über der Hotbar):

```
⏳ You are in a queue (Solo | 60 min). Please wait. [1/4 players]
```

Sobald 4 Spieler in derselben Queue sind, startet das Spiel automatisch.

## Spielablauf

```
1. WARTEN    – Spieler klickt NPC → /build join solo 60 → Queue beitreten
               Action Bar zeigt Queue-Status [X/4 players]
2. BAUEN     – 4 Spieler erreicht → Spiel startet automatisch
               Spieler bauen in ihrer 128×128 Parzelle in der Void-Arena
3. VOTING    – Außenwände weg, Spieler fliegen frei, /vote <1-5>
4. ERGEBNIS  – Punkte summiert, Sieger verkündet
               Spieler zurück zum Spawn teleportiert
```

## Schnelltest

```
/build test
```

Startet ein Testspiel mit dir alleine (2 min Bau, 1 min Voting) um zu prüfen, ob die Void-Arena und Plattformen korrekt generiert werden.

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
