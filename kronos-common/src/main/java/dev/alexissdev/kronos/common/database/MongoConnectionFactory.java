package dev.alexissdev.kronos.common.database;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

/**
 * Fábrica singleton gestionada por Guice que establece y provee la conexión a MongoDB
 * para el plugin Kronos HCF.
 *
 * <p>Al construirse, crea un {@link MongoClient} configurado con la URI de conexión y registra
 * automáticamente un {@link PojoCodecProvider} que permite serializar y deserializar objetos
 * Java (POJO) directamente hacia y desde documentos BSON de MongoDB, eliminando la necesidad
 * de mapeo manual en los repositorios del plugin.</p>
 *
 * <p>Los parámetros de conexión ({@code mongo.uri} y {@code mongo.database}) son inyectados
 * por Guice mediante la anotación {@link com.google.inject.name.Named}, y deben estar
 * vinculados en el módulo Guice correspondiente antes de usar esta clase.</p>
 *
 * <p>Solo existe una instancia de esta clase durante el ciclo de vida del plugin
 * ({@code @Singleton}). Al apagar el plugin debe llamarse {@link #close()} para liberar
 * los recursos de la conexión.</p>
 */
@Singleton
public class MongoConnectionFactory {

    private final MongoClient client;
    private final MongoDatabase database;

    /**
     * Construye la fábrica de conexión a MongoDB e inicializa el cliente con el codec POJO.
     *
     * <p>El {@link PojoCodecProvider} en modo automático permite serializar cualquier
     * clase Java anotada o no que MongoDB pueda inferir, facilitando el mapeo entre
     * entidades de dominio y documentos de la base de datos.</p>
     *
     * @param mongoUri     URI de conexión completa de MongoDB (p. ej. {@code "mongodb://localhost:27017"});
     *                     inyectada con la clave {@code mongo.uri}
     * @param databaseName nombre de la base de datos a utilizar;
     *                     inyectado con la clave {@code mongo.database}
     */
    @Inject
    public MongoConnectionFactory(
            @Named("mongo.uri") String mongoUri,
            @Named("mongo.database") String databaseName
    ) {
        CodecRegistry pojoRegistry = CodecRegistries.fromRegistries(
                MongoClientSettings.getDefaultCodecRegistry(),
                CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(mongoUri))
                .codecRegistry(pojoRegistry)
                .build();

        this.client = MongoClients.create(settings);
        this.database = client.getDatabase(databaseName).withCodecRegistry(pojoRegistry);
    }

    /**
     * Devuelve la instancia de {@link MongoDatabase} configurada con soporte POJO.
     * Los repositorios y servicios del plugin deben usar este método para obtener
     * acceso a las colecciones de MongoDB.
     *
     * @return base de datos MongoDB lista para operar con colecciones y documentos
     */
    public MongoDatabase getDatabase() {
        return database;
    }

    /**
     * Cierra el cliente de MongoDB y libera todos los recursos de conexión asociados.
     * Debe invocarse durante el apagado del plugin (p. ej. en {@code onDisable()}) para
     * evitar fugas de conexiones.
     */
    public void close() {
        client.close();
    }
}
