# kronos-common

The `kronos-common` module is the shared foundation for all other Kronos modules. It provides the command framework, database connection factories, configuration utilities, shared enums, the SOTW/EOTW service contract, and the exception hierarchy.

---

## Command Framework

### BaseCommand

`dev.alexissdev.kronos.common.command.BaseCommand`

Abstract base for all top-level Bukkit commands. Implements both `CommandExecutor` and `TabCompleter`.

| Method | Description |
|---|---|
| `execute(CommandSender, String[])` | Abstract; implement the command logic here |
| `tabComplete(CommandSender, String[])` | Override to provide tab completion; default returns empty list |
| `requirePlayer(CommandSender)` | Returns `Player` or sends error and returns `null` |
| `requireArgs(CommandSender, String[], int required, String usage)` | Returns `false` and shows usage if `args.length < required` |
| `color(String)` | Translates `&` color codes to `§` |
| `msg(CommandSender, String)` | Colorizes and sends a message |
| `filterPrefix(List<String>, String)` | Filters options to those starting with prefix (case-insensitive) |
| `onlinePlayers(String)` | Returns online player names starting with prefix |
| `subcommands(String[] args, String... subs)` | Tab-complete the first argument from a list of subcommand names |

`onCommand` is `final` — permission check runs unconditionally before `execute` is called. Pass `null` as permission in the constructor for open-access commands.

### SubCommand

`dev.alexissdev.kronos.common.command.SubCommand`

Abstract base for individual actions within a `DispatchCommand` (a `BaseCommand` that routes to `SubCommand` instances by first argument).

| Method | Description |
|---|---|
| `name()` | Abstract; primary name (lowercase, e.g. `"create"`) |
| `aliases()` | Optional alternative names; default is empty |
| `execute(CommandSender, String[])` | Abstract; `args[0]` is the subcommand name, remaining are its own args |
| `tabComplete(CommandSender, String[])` | Override for argument-level tab completion |
| Same helpers as `BaseCommand` | `requirePlayer`, `requireArgs`, `color`, `filterPrefix`, `onlinePlayers` |

### DispatchCommand (pattern)

`DispatchCommand` extends `BaseCommand` and uses a `Set<SubCommand>` injected via Guice `Multibinder`. It routes `args[0]` to the matching `SubCommand` by name or alias:

```java
// In RootModule, faction subcommands are bound to @Named("faction"):
Multibinder<SubCommand> binder =
    Multibinder.newSetBinder(binder(), SubCommand.class, Names.named("faction"));
binder.addBinding().to(CreateFactionSub.class).in(Singleton.class);
// ...

// In FactionCommand (the DispatchCommand):
@Inject
public FactionCommand(@Named("faction") Set<SubCommand> subCommands) {
    super("kronos.faction");
    this.subCommands = subCommands;
}
```

### How to Register a New Command

1. **Create the command class** extending `BaseCommand` (for standalone) or `SubCommand` (for dispatch):

```java
public class MyCommand extends BaseCommand {
    @Inject
    public MyCommand(SomeService service) {
        super("kronos.mycommand"); // or null for no permission
        this.service = service;
    }

    @Override
    protected void execute(CommandSender sender, String[] args) {
        Player player = requirePlayer(sender);
        if (player == null) return;
        // implement logic
    }
}
```

2. **Declare the command in `plugin.yml`** (in `kronos-plugin`):

```yaml
commands:
  mycommand:
    description: Does something
    aliases: [mc]
```

3. **Register in `PluginEnableHandler.registerCommands()`**:

```java
registerCommand("mycommand", injector.getInstance(MyCommand.class));
```

4. **Bind as singleton in `RootModule`**:

```java
bind(MyCommand.class).in(Singleton.class);
```

---

## MessagesConfig

`dev.alexissdev.kronos.common.config.MessagesConfig`

A thin wrapper around a `Map<String, String>` loaded from `messages.yml`.

| Method | Description |
|---|---|
| `get(String key)` | Returns the raw string for the key; falls back to `"[MISSING: key]"` |
| `format(String key, String... pairs)` | Replaces `{placeholder}` patterns using varargs key-value pairs |

**Example:**

```java
// messages.yml:
// scoreboard.kills: "&aKills: &f{kills}"

String line = messages.format("scoreboard.kills", "kills", String.valueOf(42));
// → "§aKills: §f42"
```

### Hot-reload

`MessagesConfig` is immutable once constructed. To reload messages without restarting the server, `ReloadSub` (the `/hcf reload` subcommand) re-reads `messages.yml`, creates a new `MessagesConfig` instance, and updates the binding. Because `MessagesConfig` is bound as a regular instance (not a singleton through the normal lifecycle), the reload can replace it by rebuilding the Guice injector or via a mutable holder pattern.

---

## Database Connection Factories

### MongoConnectionFactory

`dev.alexissdev.kronos.common.database.MongoConnectionFactory`

Bound in `DatabaseModule`. Provides a `MongoDatabase` instance:

```java
@Named("mongo.uri")     String uri      // e.g. "mongodb://localhost:27017"
@Named("mongo.database") String database // e.g. "kronoshcf"
```

Usage:

```java
@Inject
public MyMongoRepository(MongoConnectionFactory factory) {
    this.collection = factory.getDatabase().getCollection("myCollection");
}
```

### RedisConnectionFactory

`dev.alexissdev.kronos.common.database.RedisConnectionFactory`

Provides Lettuce async commands:

```java
@Named("redis.host")     String host
@Named("redis.port")     int    port
@Named("redis.password") String password
```

Usage:

```java
@Inject
public MyRedisRepo(RedisConnectionFactory factory) {
    this.redis = factory.async();
}
```

`factory.async()` returns `RedisAsyncCommands<String, String>` backed by a single Lettuce connection. All commands return `RedisFuture<T>` which can be converted to `CompletableFuture<T>` via `.toCompletableFuture()`.

---

## SOTW / EOTW Service

`dev.alexissdev.kronos.common.domain.SotwService`

| Method | Description |
|---|---|
| `startSotw(durationMs)` | Begin the Start-of-World period; disables PvP between players |
| `stopSotw()` | End SOTW early |
| `isSotwActive()` | Whether SOTW is currently active |
| `getSotwRemainingMs()` | Milliseconds remaining; 0 if not active |
| `startEotw(durationMs)` | Begin the End-of-World period; removes all PvP restrictions |
| `stopEotw()` | End EOTW early |
| `isEotwActive()` | Whether EOTW is currently active |
| `getEotwRemainingMs()` | Milliseconds remaining; 0 if not active |

The concrete implementation is `SotwManager` (in `kronos-plugin`), bound in `RootModule`:

```java
bind(SotwService.class).to(SotwManager.class).in(Singleton.class);
```

`SotwManager` uses a `BukkitRunnable` countdown and updates the scoreboard via `ScoreboardManager.tickAll()` every second.

---

## CrateType Enum

`dev.alexissdev.kronos.common.domain.CrateType`

| Value | Source |
|---|---|
| `KOTH` | Awarded on KOTH capture |
| `VOTE` | Awarded for server votes |
| `RANK` | Associated with donation/rank milestones |
| `EVENT` | Staff-distributed event prizes |

Used by both `kronos-koth` (as the `KothZone.rewardCrateType`) and `kronos-players` (in `CrateLocation`).

---

## Exception Hierarchy

| Class | Module | Extends | When thrown |
|---|---|---|---|
| `HCFException` | `kronos-common` | `RuntimeException` | Generic business rule violation |
| `FactionNotFoundException` | `kronos-factions` | `HCFException` | No faction found for the given ID |
| `FactionPermissionException` | `kronos-factions` | `HCFException` | Actor lacks required role |
| `ClaimConflictException` | `kronos-claims` | `HCFException` | Chunk already claimed, or overclaim conditions not met |
| `KothNotFoundException` | `kronos-koth` | `HCFException` | No KOTH zone found with the given name |
| `PlayerNotFoundException` | `kronos-players` | `HCFException` | No profile found for the given UUID |
| `InsufficientFundsException` | `kronos-economy` | `HCFException` | Balance too low for withdraw/transfer |

All exceptions are unchecked and safe to throw inside `CompletableFuture` chains (they propagate as the exceptional result and can be handled in `.exceptionally(ex -> ...)` callbacks).
