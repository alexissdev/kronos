package dev.alexissdev.kronos.plugin.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.common.database.MongoConnectionFactory;
import dev.alexissdev.kronos.common.database.RedisConnectionFactory;

/**
 * Módulo Guice responsable de configurar las conexiones a las bases de datos del plugin.
 *
 * <p>Registra como singletons las fábricas de conexión a MongoDB ({@link MongoConnectionFactory})
 * y a Redis ({@link RedisConnectionFactory}), garantizando que solo exista una instancia de
 * cada conexión durante todo el ciclo de vida del plugin. Los parámetros de conexión (URI, host,
 * puerto, contraseña) son inyectados en las propias fábricas mediante anotaciones {@code @Named}
 * definidas en {@link dev.alexissdev.kronos.plugin.guice.RootModule}.
 */
public class DatabaseModule extends AbstractModule {

    /**
     * Registra los bindings de las fábricas de conexión como singletons en el contenedor Guice.
     *
     * <p>Al ser singletons, la conexión a MongoDB y a Redis se establece una sola vez y es
     * compartida por todos los repositorios y servicios que la requieran.
     */
    @Override
    protected void configure() {
        bind(MongoConnectionFactory.class).in(Singleton.class);
        bind(RedisConnectionFactory.class).in(Singleton.class);
    }
}
