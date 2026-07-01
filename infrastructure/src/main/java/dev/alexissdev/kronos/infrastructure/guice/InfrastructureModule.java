package dev.alexissdev.kronos.infrastructure.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.core.repository.*;
import dev.alexissdev.kronos.core.service.EconomyService;
import dev.alexissdev.kronos.infrastructure.mongo.*;
import dev.alexissdev.kronos.infrastructure.redis.RedisConnectionFactory;
import dev.alexissdev.kronos.infrastructure.redis.RedisFreezeRepository;
import dev.alexissdev.kronos.infrastructure.redis.RedisTimerRepository;
import dev.alexissdev.kronos.infrastructure.vault.VaultEconomyService;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

public class InfrastructureModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(MongoConnectionFactory.class).in(Singleton.class);
        bind(RedisConnectionFactory.class).in(Singleton.class);

        bind(FactionRepository.class).to(MongoFactionRepository.class).in(Singleton.class);
        bind(PlayerRepository.class).to(MongoPlayerRepository.class).in(Singleton.class);
        bind(ClaimRepository.class).to(MongoClaimRepository.class).in(Singleton.class);
        bind(TimerRepository.class).to(RedisTimerRepository.class).in(Singleton.class);
        bind(KothRepository.class).to(MongoKothRepository.class).in(Singleton.class);
        bind(FreezeRepository.class).to(RedisFreezeRepository.class).in(Singleton.class);

        bind(EconomyService.class).to(VaultEconomyService.class).in(Singleton.class);
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
