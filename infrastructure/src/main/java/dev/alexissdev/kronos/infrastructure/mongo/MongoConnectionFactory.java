package dev.alexissdev.kronos.infrastructure.mongo;

import com.google.inject.Inject;
import com.google.inject.Named;
import com.google.inject.Singleton;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

@Singleton
public class MongoConnectionFactory {

    private final MongoClient client;
    private final MongoDatabase database;

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

    public MongoDatabase getDatabase() {
        return database;
    }

    public void close() {
        client.close();
    }
}
