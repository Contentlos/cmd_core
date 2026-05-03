# Befehlsreferenz – NightWatch Core

Alle Befehle existieren unter den beiden gleichwertigen Aliassen:

- `/nw …`
- `/nightwatch …`

Die Berechtigung wird **serverseitig** über das Permission-System geprüft.
Wenn eine Stufe genannt wird, ist das die Mindeststufe (CONSOLE darf alles).

## Basisbefehle

| Befehl | Stufe | Beschreibung |
| --- | --- | --- |
| `/nw help` | USER | Listet die wichtigsten Unterbefehle auf. |
| `/nw version` | USER | Mod-Version + interner Modulname. |
| `/nw status` | HELPER | Server-Übersicht inkl. Version, TPS, Spielern. |
| `/nw reload` | ADMIN | Lädt Permissions, Module und Config neu. |
| `/nw save` | ADMIN | Speichert alle internen Daten + Welt. |
| `/nw panel` | MODERATOR* | Öffnet das Admin-Panel beim Spieler. |
| `/nw debug` | ADMIN | Schaltet den Debug-Modus an/aus. |
| `/nw modules` | ADMIN | Listet alle Module mit Status. |

*Mindeststufe für das Panel ist konfigurierbar (`admin.minPanelPermissionLevel`).

## Permissions

| Befehl | Stufe | Beschreibung |
| --- | --- | --- |
| `/nw perms get <spieler>` | MODERATOR | Zeigt Stufe + Quelle. |
| `/nw perms set <spieler> <level>` | OWNER | Setzt Stufe explizit. |
| `/nw perms list` | ADMIN | Listet alle gespeicherten Einträge. |
| `/nw perms remove <spieler>` | OWNER | Entfernt einen Eintrag. |
| `/nw perms reload` | ADMIN | Liest die JSON neu ein. |
| `/nw perms save` | ADMIN | Schreibt die JSON. |

`level` kann sein: `USER`, `HELPER`, `MODERATOR`, `ADMIN`, `OWNER`.

## Admin-Aktionen

Alle Befehle nutzen denselben Service wie das Panel.

| Befehl | Stufe | Beschreibung |
| --- | --- | --- |
| `/nw admin heal <spieler>` | HELPER | Heilt Leben + Hunger. |
| `/nw admin feed <spieler>` | HELPER | Füttert. |
| `/nw admin fly <spieler> <true|false>` | MODERATOR | Schaltet Flug. |
| `/nw admin gamemode <spieler> <survival|creative|adventure|spectator>` | MODERATOR | Setzt Gamemode. |
| `/nw admin tp <spieler> <ziel>` | MODERATOR | Teleportiert Spieler zu Ziel. |
| `/nw admin tphere <spieler>` | MODERATOR | Teleportiert Spieler zu dir. |
| `/nw admin kick <spieler> <grund>` | MODERATOR | Kickt mit Grund. |
| `/nw admin broadcast <nachricht>` | HELPER | Servernachricht. |
| `/nw admin day` | MODERATOR | Setzt Tag. |
| `/nw admin night` | MODERATOR | Setzt Nacht. |
| `/nw admin weather <clear|rain|thunder>` | MODERATOR | Setzt Wetter. |
| `/nw admin info <spieler>` | HELPER | Spieler-Info im Chat. |

## Logs & Verlauf

| Befehl | Stufe | Beschreibung |
| --- | --- | --- |
| `/nw logs latest` | MODERATOR | Letzte 25 Audit-Einträge. |
| `/nw logs audit` | MODERATOR | Letzte 100 Audit-Einträge. |
| `/nw history` | MODERATOR | Verlauf der letzten Aktionen. |
| `/nw history <spieler>` | MODERATOR | Verlauf gefiltert nach Spielername. |

## Beispiele

```text
/nw panel
/nw status
/nw admin heal Contentlos
/nw admin gamemode Spieler123 creative
/nw admin tp Helfer Contentlos
/nw perms set Spieler123 ADMIN
/nw perms list
/nw history Contentlos
```

## Hotkey im Client

- Standard: **F6** (konfigurierbar im Vanilla-Tastenmenü).
- Funktion: schickt `OPEN_PANEL` an den Server. Der Server prüft die
  Stufe und antwortet mit `ClientboundOpenPanelPayload` oder einer
  Fehlermeldung im Chat.
