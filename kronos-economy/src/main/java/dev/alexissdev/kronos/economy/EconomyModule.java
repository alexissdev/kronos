package dev.alexissdev.kronos.economy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import dev.alexissdev.kronos.economy.command.MoneyCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EconomyService.class).to(VaultEconomyService.class).in(Singleton.class);
        bind(MoneyCommand.class).in(Singleton.class);
    }

    @Provides
    @Singleton
    Economy provideVaultEconomy() {
        RegisteredServiceProvider<Economy> rsp = Bukkit.getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            throw new IllegalStateException("Vault Economy no encontrada. ¿Está Vault instalado?");
        }
        return rsp.getProvider();
    }
}
