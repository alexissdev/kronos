package dev.alexissdev.kronos.economy;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.economy.service.EconomyService;
import dev.alexissdev.kronos.economy.command.MoneyCommand;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Módulo Guice que configura el sistema de economía del plugin Kronos HCF.
 *
 * <p>Registra todos los bindings necesarios para que la inyección de dependencias
 * funcione correctamente en la capa de economía:</p>
 * <ul>
 *   <li>Vincula {@link EconomyService} a su implementación {@link VaultEconomyService}.</li>
 *   <li>Registra {@link MoneyCommand} como singleton para evitar instancias duplicadas.</li>
 *   <li>Provee la instancia de la economía de Vault mediante {@link #provideVaultEconomy()}.</li>
 * </ul>
 *
 * <p>Este módulo debe instalarse en el inyector raíz del plugin durante el método
 * {@code onEnable()}. Requiere que el plugin Vault esté instalado y que haya al menos
 * un plugin de economía compatible activo (p. ej. EssentialsX) antes de que el
 * inyector sea creado; de lo contrario lanzará {@link IllegalStateException}.</p>
 */
public class EconomyModule extends AbstractModule {

    /**
     * Configura los bindings de Guice para el módulo de economía.
     *
     * <p>Asocia la interfaz {@link EconomyService} con la implementación basada en Vault
     * ({@link VaultEconomyService}) y registra el comando de dinero como singleton.</p>
     */
    @Override
    protected void configure() {
        bind(EconomyService.class).to(VaultEconomyService.class).in(Singleton.class);
        bind(MoneyCommand.class).in(Singleton.class);
    }

    /**
     * Provee la implementación de {@link Economy} de Vault obtenida del gestor de servicios de Bukkit.
     *
     * <p>Vault actúa como capa de abstracción entre el plugin y el sistema de economía concreto
     * instalado en el servidor. Al solicitar la {@link Economy} a través del gestor de servicios,
     * el plugin no depende de una implementación específica (EssentialsX, CMI, etc.).</p>
     *
     * @return instancia de {@link Economy} registrada por el plugin de economía activo
     * @throws IllegalStateException si Vault no está instalado o no hay ningún plugin de economía
     *                               registrado en el gestor de servicios de Bukkit
     */
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
