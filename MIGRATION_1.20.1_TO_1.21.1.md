# Migration 1.20.1 (Forge) → 1.21.1 (NeoForge)

Diese Datei dokumentiert die Umstellung des CMD-Core-/NightWatch-Core-Mods.

## Zielversionen

| Bereich | Vorher | Jetzt |
| --- | --- | --- |
| Minecraft | 1.20.1 | **1.21.1** |
| Modloader | Forge | **NeoForge 21.1.228+** |
| Java | 17 | **21** |
| Mappings | Searge/Forge | NeoForge + Parchment 2024.11.17 |
| Build | ForgeGradle | **NeoGradle 7.1.26** |

## Was ist passiert?

- Der ursprüngliche `com.example.examplemod`-Skeleton-Code wurde entfernt.
- Das gesamte CMD-Core-System wurde nach `de.contentlos.cmdcore` neu
  aufgebaut. Es wurde nichts aus dem 1.12.2-/1.20.1-Code 1:1 übernommen,
  weil viele APIs (`PacketTarget`, `KeyBinding`, `IForgeRegistry`,
  `Mojangson`, …) in 1.21.1 nicht mehr existieren oder anders heißen.
- Die Mod-ID heißt weiterhin `nightwatch_core`. Intern existiert das
  CMD-Core-Modul als zentraler Service-Layer.

## Forge → NeoForge

| Thema | Forge 1.20.1 | NeoForge 1.21.1 |
| --- | --- | --- |
| Event-Bus | `MinecraftForge.EVENT_BUS` | `NeoForge.EVENT_BUS` |
| Mod-Klasse | `@Mod(modid)` | `@Mod(modid)` (mehrere Klassen mit `dist=` möglich) |
| Mod-Konstruktor | `(IEventBus modBus)` | `(IEventBus modBus, ModContainer container)` |
| Configs | `ForgeConfigSpec` | `ModConfigSpec` (neoforge.common) |
| Config-Datei pro Server | `ModConfig.Type.SERVER` | `container.registerConfig(...)` |
| `@OnlyIn` | `@OnlyIn(Dist.CLIENT)` | `@OnlyIn(Dist.CLIENT)` (gleicher API-Pfad) |
| Networking | `SimpleChannel` + `IMessage` | `RegisterPayloadHandlersEvent` + `CustomPacketPayload` |
| Pakete | `@Identifier`/`registerMessage` | `playToServer`/`playToClient` mit `StreamCodec` |
| Senden | `channel.sendTo(...)` | `PacketDistributor.sendToServer(...)` / `sendToPlayer(...)` |

## Command-Migration

- `/nightwatch` und `/nw` sind jetzt mit Brigadier (`Commands.literal/argument`).
- Stufenprüfung erfolgt nicht über Vanilla-OP-Level, sondern über den
  `PermissionService`. Das Vanilla-`requires` filtert nur grob.
- Befehle und Panel teilen sich `AdminActionService` als gemeinsamen
  Service-Layer.

## Networking-Migration

- Alle Pakete sind jetzt Records in `de.contentlos.cmdcore.common.network.payload`.
- `RegisterPayloadHandlersEvent` registriert sie mit Versionierung
  (`PROTOCOL_VERSION = "1"`).
- Clientbound-Handler werden über `ClientHandlerBridge` gefiltert: Auf einem
  Dedicated-Server greift die Methode nicht in Client-Code, weil
  `FMLEnvironment.dist == Dist.CLIENT` falsch ist.

## Config-Migration

Eine einzige Server-Config (`nightwatch_core-server.toml`) mit fünf
Sektionen (`general`, `admin`, `permissions`, `logging`, `panel`).

Empfehlung: nicht direkt von der alten Config kopieren – Schemas haben
sich geändert. Dem Bedarf nach werden Werte über das Panel-Tab
*Einstellungen* gesetzt und live gespeichert.

## Datenpersistenz

- Pfad: `world/serverconfig/nightwatch/`.
- Atomare JSON-Schreibe (Tempdatei + rename).
- Defekte Dateien werden mit `.broken-<timestamp>` gesichert und durch
  Defaults ersetzt – kein Crash.

## Client/Server-Trennung

- Common-Code referenziert keine `net.minecraft.client.*`-Klassen.
- Der Hotkey wird in `de.contentlos.cmdcore.client.ClientSetup`
  registriert (`@Mod(... dist = Dist.CLIENT)`).
- Die GUI-Klassen sind alle mit `@OnlyIn(Dist.CLIENT)` markiert oder
  nutzen Klassen, die nur über die Bridge angesprochen werden.

## Bekannte Probleme / Unterschiede

- `KeyMapping.consumeClick()` wird auf `ClientTickEvent.Post` geprüft. In
  1.20.1-Forge war das ein eigenes `InputEvent`. In 1.21.1 reicht der
  Tick-Event.
- `MinecraftServer.getAverageTickTimeNanos()` ersetzt
  `getAverageTickTime()` (Wert ist jetzt in Nanosekunden).
- `ServerPlayer.teleportTo(level, x, y, z, yRot, xRot)` ist die
  empfohlene Variante in 1.21.1.

## Offene TODOs (siehe README)

- Vanish/Freeze-Aktionen.
- Inventar/Enderchest-GUI im Panel.
- Optional: Verlauf-Export als JSON-Download.
