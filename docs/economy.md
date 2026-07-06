# kronos-economy

The economy module wraps [Vault](https://github.com/milkbowl/Vault) to provide a unified async economy interface that any Kronos module can depend on without coupling to a specific economy plugin.

---

## Vault Abstraction

Vault is a permissions/economy/chat abstraction layer for Bukkit plugins. It delegates all actual balance operations to the concrete economy plugin installed on the server (EssentialsX, CMI, etc.). The `kronos-economy` module:

1. Obtains the `net.milkbowl.vault.economy.Economy` instance via Guice at startup.
2. Wraps every Vault call in a `CompletableFuture` so callers never block.
3. Guarantees all Vault calls happen on the **Bukkit main thread** using a custom `Executor`.

---

## EconomyService Interface

`dev.alexissdev.kronos.economy.service.EconomyService`

| Method | Parameters | Return | Description |
|---|---|---|---|
| `getBalance` | `UUID playerUuid` | `CompletableFuture<Double>` | Query current balance |
| `deposit` | `UUID playerUuid, double amount` | `CompletableFuture<Void>` | Credit the player's account |
| `withdraw` | `UUID playerUuid, double amount` | `CompletableFuture<Void>` | Debit the player's account |
| `transfer` | `UUID fromUuid, UUID toUuid, double amount` | `CompletableFuture<Void>` | Atomic withdraw + deposit chain |
| `hasEnoughBalance` | `UUID playerUuid, double amount` | `CompletableFuture<Boolean>` | Pre-validation without attempting debit |

All methods throw checked exceptions on failure:

- `HCFException` — invalid amount (≤ 0) or Vault reported a transaction error
- `InsufficientFundsException` — thrown by `withdraw` when `balance < amount`

---

## Main-Thread Executor Strategy

Vault's `Economy` API is **not thread-safe**. All calls must originate from the Bukkit main thread. `VaultEconomyService` creates a custom `Executor` at construction time:

```java
this.mainThreadExecutor = runnable -> {
    if (Bukkit.isPrimaryThread()) {
        runnable.run();
    } else {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
};
```

Every `CompletableFuture.supplyAsync(...)` call uses this executor as its second argument. This means:

- If a service method calls `economyService.getBalance(uuid)` from an async MongoDB callback, the Vault call is safely dispatched to the main thread.
- The `CompletableFuture` returned by the async operation still runs its `.thenAccept` callbacks on the Lettuce/MongoDB thread pool — only the actual Vault call runs on the main thread.

---

## VaultEconomyService Implementation

`dev.alexissdev.kronos.economy.VaultEconomyService`

### `getBalance`

```java
CompletableFuture.supplyAsync(() -> {
    OfflinePlayer player = Bukkit.getOfflinePlayer(playerUuid);
    return economy.getBalance(player);
}, mainThreadExecutor);
```

### `deposit`

Validates `amount > 0`, then calls `economy.depositPlayer(player, amount)`. If `EconomyResponse.transactionSuccess()` is `false`, completes exceptionally with `HCFException`.

### `withdraw`

Calls `economy.has(player, amount)` first. If insufficient, throws `InsufficientFundsException(requested, actual)`. Otherwise calls `economy.withdrawPlayer(player, amount)`.

### `transfer`

```java
withdraw(fromUuid, amount).thenCompose(v -> deposit(toUuid, amount));
```

If `withdraw` fails, `deposit` never executes — atomic from the caller's perspective.

### `hasEnoughBalance`

```java
CompletableFuture.supplyAsync(() ->
    economy.has(Bukkit.getOfflinePlayer(playerUuid), amount),
mainThreadExecutor);
```

---

## MoneyCommand Subcommands (`/money`, `/balance`)

The `/money` command is a `DispatchCommand` with the following subcommands:

| Subcommand | Permission | Description |
|---|---|---|
| `/money` or `/balance` | — | Show your own balance |
| `/money pay <player> <amount>` | — | Transfer money to another online player |
| `/money set <player> <amount>` | `kronos.admin` | Set a player's balance to an exact amount (admin) |
| `/money give <player> <amount>` | `kronos.admin` | Add money to a player without deducting from source |
| `/baltop` | — | Display the top players by balance |

All balance operations go through `EconomyService`, not directly through Vault, to ensure consistent async handling.

---

## Guice Module — `EconomyModule`

Binds `VaultEconomyService` as singleton implementation of `EconomyService`. The `Economy` instance from Vault is obtained in `EconomyModule.configure()` via the Bukkit `ServicesManager`:

```java
Economy economy = Bukkit.getServicesManager()
    .getRegistration(Economy.class).getProvider();
bind(Economy.class).toInstance(economy);
bind(EconomyService.class).to(VaultEconomyService.class).in(Singleton.class);
```

If Vault or an economy plugin is not present at startup, the module throws an exception and the plugin disables itself.

---

## InsufficientFundsException

`dev.alexissdev.kronos.economy.exception.InsufficientFundsException`

Carries both the `requested` and `available` amounts so command handlers can display a useful error message to the player:

```
Insufficient funds: you need $500.00 but only have $120.00.
```
