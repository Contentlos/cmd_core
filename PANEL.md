# Admin-Panel – Aufbau und Funktionen

Das Admin-Panel ist eine vollständig nutzbare GUI im NightWatch-Stil. Es
besteht aus einer linken Tab-Liste, einem Header mit Akzentlinie, einem
zentralen Inhaltsbereich (Karten und Tabellen) und einer Fußzeile mit
allgemeinen Aktionen.

Akzentfarbe konfigurierbar (`panel.panelAccentColor`):
`BLUE`, `CYAN`, `VIOLET`, `RED`, `GREEN`, `AMBER`.

Öffnen:

- Hotkey **F6** (Default, konfigurierbar im Vanilla-Tastenmenü)
- `/nw panel` oder `/nightwatch panel`

Berechtigung: Mindeststufe = `admin.minPanelPermissionLevel` (Default
`MODERATOR`). Konsole bekommt eine sinnvolle Fehlermeldung statt einer GUI.

## Architektur

- Client-GUI rendert nur Daten, die er vom Server bekommt (`PanelState`
  hält die zuletzt empfangenen Payloads).
- Jede Aktion sendet ein `ServerboundActionPayload`/`ServerboundRequestPayload`
  an den Server.
- Der Server prüft die Berechtigung in `ServerboundHandlers`, ruft den
  passenden Service (`AdminActionService`, `PermissionService`,
  `ServerStatusService`, …) auf und schreibt einen `AuditEntry`.
- Die Antwort kommt als `ClientboundActionResultPayload` zurück und wird
  als Toast und im Chat angezeigt.

## Tabs

### 1. Übersicht

| Anzeige | Quelle |
| --- | --- |
| Servername | `MinecraftServer.getMotd()` |
| Minecraft-Version | `MinecraftServer.getServerVersion()` |
| NeoForge-Version | `FMLLoader.versionInfo().neoForgeVersion()` |
| NightWatch-Core-Version | `CMDCore.VERSION` |
| Online / Max Spieler | `getPlayerCount`/`getMaxPlayers` |
| TPS | `MinecraftServer.getAverageTickTimeNanos()` |
| Uptime | `bootTimeMs` |
| Welt | `WorldData.getLevelName()` |
| Dimensionen | alle `getAllLevels()` |
| Geladene Module | `ModuleRegistry.sorted()` |
| Debug-Modus | `general.enableDebugMode` |
| Permissions / Audit | aus Config |
| Speicher | `Runtime.totalMemory/freeMemory/maxMemory` |

Buttons:

- *Aktualisieren*  → `REQUEST(OVERVIEW)`
- *Daten speichern*  → `ACTION(SAVE_DATA)` (Stufe `MODERATOR`)
- *Config neu laden*  → `ACTION(RELOAD_CONFIG)` (Stufe `ADMIN`)
- *Debug umschalten*  → `ACTION(TOGGLE_DEBUG)` (Stufe `ADMIN`)
- *Module neu laden*  → `ACTION(RELOAD_MODULES)` (Stufe `ADMIN`)

### 2. Spieler

Zeigt alle Online-Spieler mit Name, Stufe, Gamemode, Dimension, HP, Hunger,
Ping. Filterfeld + Zielfeld.

Aktionen pro ausgewähltem Spieler:

| Button | Stufe |
| --- | --- |
| Heilen | HELPER |
| Füttern | HELPER |
| Flug | MODERATOR |
| Tp zu | MODERATOR |
| Tp her | MODERATOR |
| Survival/Creative/Spectator | MODERATOR |
| Kick | MODERATOR |
| Info | HELPER |

> Inventar/Enderchest-Buttons sind **nicht** im UI, weil sich keine saubere,
> in 1.21.1 NeoForge ohne Mixins funktionierende Lösung anbietet. Das Backend
> ist über `PlayerService` vorbereitet.

### 3. Berechtigungen

Tabelle: Name | UUID | Stufe | Quelle (`JSON` / `OP_FALLBACK` / `CONSOLE` / `DEFAULT`).

Buttons:

- *Setzen* (Stufe `OWNER`)
- *Entfernen* (Stufe `OWNER`)
- *Reload* (Stufe `ADMIN`)
- *Speichern* (Stufe `ADMIN`)
- *OP-Cache* (löscht den Cache, Stufe `ADMIN`)

### 4. Admin-Aktionen

Zentral sortierte Aktionen für den Schnellzugriff:

- Spieler-Aktionen: Heilen, Füttern, Flug, Tp zu, Tp her, Kick, Info,
  Koordinaten kopieren, Titel-Nachricht
- Gamemode-Auswahl (Survival/Creative/Adventure/Spectator)
- Wetter (Klar/Regen/Gewitter)
- Welt speichern, Daten speichern
- Tag/Nacht
- Broadcast-Eingabefeld + Senden
- Debug-Testnachricht (für Admins)

Jede Aktion liefert ein Toast (grün/rot) und einen Audit-Log-Eintrag.

### 5. Server

Anzeigen: Servername, Welt, Spielerzahl, Uptime, TPS, Speicher, Dimensionen.

Buttons (jeweils mit der Stufe wie oben):

- Tag / Nacht
- Wetter klar / Regen / Gewitter
- Welt speichern
- Module neu laden, Config neu laden
- Broadcast-Eingabefeld + Senden

Stop/Restart absichtlich **nicht** als Button. Ein versehentlicher Klick
würde den Server umlegen – das ist mit der aktuellen Sicherheitsabfrage in
1.21.1 NeoForge nicht sauber umsetzbar.

### 6. Module

Tabelle: ID | Name | Status (`AKTIV`/`AUS`) | Mindeststufe | Abschaltbar.

Buttons:

- *Aktivieren*, *Deaktivieren*, *Reload* (Stufe `ADMIN`).
- *Alle Reload* (Stufe `ADMIN`).
- Kernmodul (`core`) ist nicht deaktivierbar; das Panel zeigt das mit
  „Abschaltbar: nein“ (in Rot) und der Server lehnt einen Disable
  ausdrücklich ab.

### 7. Logs

Audit-Liste (paginiert + filterbar). Spalten: Zeit | Admin | Aktion | Ziel
| Quelle | OK?

- Filterfeld + Limit (50/100/250).
- Daten kommen über `ClientboundLogListPayload` – serverseitig limitiert.
- Erfolgsspalte zeigt `§a✓` bzw. `§c✗`.

### 8. Verlauf

Wie *Logs*, aber mit `Kind = HISTORY`. Visuell identisch, semantisch
zukunftssicher (für getrennte Datenquellen).

- *Verlauf löschen* nur per Aktion `CLEAR_HISTORY` und nur für `OWNER`.

### 9. Einstellungen

Listet die Live-Werte der Server-Config:

- `general.enableCore` (Neustart)
- `general.enableDebugMode`
- `general.dataSaveIntervalSeconds`
- `admin.enableAdminSystem`, `admin.minPanelPermissionLevel`
- `admin.allowKick`, `admin.allowTeleport`, `admin.allowGamemodeChange`,
  `admin.allowWeatherControl`, `admin.allowTimeControl`
- `permissions.enablePermissionSystem`, `permissions.defaultPermissionLevel`,
  `permissions.allowOpFallback`
- `logging.enableAuditLog`, `logging.logAdminCommands`,
  `logging.logPanelActions`, `logging.logPlayerJoin`,
  `logging.maxHistoryEntries`
- `panel.enableAdminPanel`, `panel.panelAccentColor`,
  `panel.panelAnimations`, `panel.panelDefaultTab`,
  `panel.panelKeybindEnabled`

Aktionen:

- *Setzen* (Stufe `ADMIN`).
- *Aktualisieren* (Liste neu laden).
- *Config reload* / *Speichern*.

Hinweis: Werte mit `requiresRestart = true` (z. B. `general.enableCore`)
werden über die ConfigSnapshotService-Logik blockiert; der Server gibt
„Schlüssel ist nicht zur Laufzeit änderbar“ zurück.

### 10. Debug

Sichtbar für `ADMIN`/`OWNER`. Zeigt:

- Modstatus
- Debug-Flag
- Anzahl gecachter Permissions
- Anzahl Audit-Einträge
- Online-Spieler
- Welt
- TPS
- Datenpfad

Buttons:

- *Debug umschalten* (Stufe `ADMIN`)
- *Test-Nachricht* (Stufe `ADMIN`)
- *OP-Cache leeren* (Stufe `ADMIN`)
- *Daten speichern* (Stufe `MODERATOR`)
- *Aktualisieren* (Stufe `MODERATOR`)
