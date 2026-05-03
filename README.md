# NightWatch Core (CMD Core)

NightWatch Core ist ein deutschsprachiges Server-Verwaltungs- und Admin-System
für **Minecraft 1.21.1** auf **NeoForge**. Intern trägt der Modulkern den
Namen *CMD Core*.

> **Mod-ID:** `nightwatch_core`  ·  **Mod-Version:** `0.2.0`  ·  **MC:** `1.21.1`  ·  **NeoForge:** `21.1.228+`

## Was bietet der Mod?

- Vollständiges Admin-Panel mit zehn Tabs (Übersicht, Spieler, Berechtigungen,
  Admin-Aktionen, Server, Module, Logs, Verlauf, Einstellungen, Debug).
- Permission-System mit den Stufen `USER`, `HELPER`, `MODERATOR`, `ADMIN`,
  `OWNER`, `CONSOLE` inkl. JSON-Persistenz und optionalem OP-Fallback.
- Einheitliche Backend-Logik: Admin-Panel und Befehle nutzen denselben
  Service-Layer.
- Audit-Log und Verlauf mit Quelle (`COMMAND` / `PANEL` / `SYSTEM`).
- Modul-System mit aktivieren/deaktivieren/neu laden – Kernmodule sind
  geschützt.
- Saubere Client/Server-Trennung: Auf dem Dedicated-Server wird kein
  GUI-Code geladen.
- Sechs TOML-Bereiche zur Konfiguration (general/admin/permissions/
  logging/panel) in einer einzigen Server-Config.

## Installation

1. Sicherstellen, dass NeoForge **21.1.228** oder neuer für 1.21.1 installiert
   ist.
2. Die Datei `nightwatch_core-0.2.0.jar` in den `mods/`-Ordner des
   Servers (und der gewünschten Clients) kopieren.
3. Server starten. Die Config wird unter
   `world/serverconfig/nightwatch_core-server.toml` automatisch erzeugt.
4. Alle Datenbanken werden unter `world/serverconfig/nightwatch/` (siehe
   `CMDDataPaths`) abgelegt.

## Erster Start

- Beim Start protokolliert der Mod: *„CMD-Core Services initialisiert.“*
- Falls noch keine Permission-Datei existiert, ist OP-Fallback (sofern in
  der Config aktiviert) wirksam: Vanilla-OP-Stufen werden in CMD-Stufen
  übersetzt (`OP 4` → `OWNER`, `OP 3` → `ADMIN`, …).
- OWNER-UUIDs lassen sich per Config (`permissions.ownerUUIDs`) hart
  setzen.

## Befehle

Siehe [COMMANDS.md](COMMANDS.md) für die vollständige Liste mit Beispielen.

Kurzreferenz:

| Bereich     | Beispiel                                         |
| ----------- | ------------------------------------------------ |
| Allgemein   | `/nw status`, `/nw reload`, `/nw save`           |
| Panel       | `/nw panel`, Hotkey **F6** (konfigurierbar)      |
| Admin       | `/nw admin heal <spieler>`                       |
| Permission  | `/nw perms set <spieler> <level>`                |
| Logs        | `/nw logs latest`, `/nw history <spieler>`       |

## Admin-Panel

Siehe [PANEL.md](PANEL.md) für die genaue Beschreibung jedes Tabs, der
Datenquellen und der Berechtigungen pro Aktion.

Wichtig:

- Jeder Klick im Panel wird serverseitig geprüft.
- Der Client sendet eine Anfrage, der Server validiert die Berechtigung,
  führt die Aktion aus und schreibt einen Audit-Eintrag.

## Configs

Datei: `world/serverconfig/nightwatch_core-server.toml`

Bereiche:

- `[general]`   — allgemeine Schalter (Debug, Auto-Save).
- `[admin]`    — Admin-System, Mindeststufe für das Panel, erlaubte Aktionen.
- `[permissions]` — Permission-Kern, Default-Stufe, OP-Fallback, OWNER-UUIDs.
- `[logging]`  — Audit-Log und Verlauf.
- `[panel]`    — Akzentfarbe, Animationen, Standard-Tab, Hotkey.

Über das Panel-Tab *Einstellungen* lassen sich viele Werte zur Laufzeit
verändern. Werte, die einen Neustart erfordern, sind dort entsprechend
markiert.

## Logs und Verlauf

- Audit-Log: `world/serverconfig/nightwatch/admin_history.json` (rolliert
  über `logging.maxHistoryEntries`).
- Permissions: `world/serverconfig/nightwatch/permissions.json`.
- Modul-Status: `world/serverconfig/nightwatch/module_states.json`.

Das Panel sendet nie die komplette Datei an den Client – serverseitig wird
nach Filter und Limit reduziert (Standardlimit: 100 Einträge).

## Bekannte Einschränkungen

- Inventar/Enderchest-Anzeige im Panel ist auf **Backend** vorbereitet,
  aber bewusst noch nicht clientseitig sichtbar, weil sich keine saubere,
  ohne Mixins/Reflection auskommende GUI-Lösung in 1.21.1 NeoForge anbietet.
- Vanish und Freeze sind im Panel als Aktionen vorgesehen, aber im aktuellen
  Stand nicht implementiert. Die zugehörigen Buttons existieren daher nicht
  im UI – es gibt keine Fake-Buttons.
- Manche Configs erfordern einen Neustart und werden im Tab *Einstellungen*
  als „Neustart nötig“ markiert.

## Lizenz

`All Rights Reserved` (siehe `gradle.properties`). Jede Weitergabe nur mit
ausdrücklicher Erlaubnis von Contentlos.
