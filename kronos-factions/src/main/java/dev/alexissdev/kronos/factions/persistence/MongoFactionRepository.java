package dev.alexissdev.kronos.factions.persistence;

import dev.alexissdev.kronos.common.database.MongoConnectionFactory;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.alexissdev.kronos.factions.domain.Faction;
import dev.alexissdev.kronos.factions.domain.FactionHome;
import dev.alexissdev.kronos.factions.domain.FactionMember;
import dev.alexissdev.kronos.factions.domain.FactionRole;
import dev.alexissdev.kronos.factions.repository.FactionRepository;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@Singleton
public class MongoFactionRepository implements FactionRepository {

    private static final String COLLECTION = "factions";

    private final MongoCollection<Document> collection;
    private final Executor executor;

    @Inject
    public MongoFactionRepository(MongoConnectionFactory factory) {
        this.collection = factory.getDatabase().getCollection(COLLECTION);
        this.executor = Executors.newCachedThreadPool();
    }

    @Override
    public CompletableFuture<Optional<Faction>> findById(String id) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.eq("_id", id)).first();
            return Optional.ofNullable(doc).map(this::toFaction);
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Faction>> findByName(String name) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(Filters.regex("name", "^" + name + "$", "i")).first();
            return Optional.ofNullable(doc).map(this::toFaction);
        }, executor);
    }

    @Override
    public CompletableFuture<Optional<Faction>> findByMember(UUID playerUuid) {
        return CompletableFuture.supplyAsync(() -> {
            Document doc = collection.find(
                    Filters.elemMatch("members", Filters.eq("uuid", playerUuid.toString()))
            ).first();
            return Optional.ofNullable(doc).map(this::toFaction);
        }, executor);
    }

    @Override
    public CompletableFuture<List<Faction>> findTopByKills(int limit) {
        return CompletableFuture.supplyAsync(() -> {
            List<Faction> result = new ArrayList<>();
            collection.find()
                    .sort(Sorts.descending("kills"))
                    .limit(limit)
                    .forEach(doc -> result.add(toFaction(doc)));
            return result;
        }, executor);
    }

    @Override
    public CompletableFuture<Faction> save(Faction faction) {
        return CompletableFuture.supplyAsync(() -> {
            collection.replaceOne(
                    Filters.eq("_id", faction.getId()),
                    toDocument(faction),
                    new ReplaceOptions().upsert(true)
            );
            return faction;
        }, executor);
    }

    @Override
    public CompletableFuture<Void> delete(String id) {
        return CompletableFuture.runAsync(
                () -> collection.deleteOne(Filters.eq("_id", id)), executor);
    }

    private Faction toFaction(Document doc) {
        Map<UUID, FactionMember> members = new LinkedHashMap<>();
        for (Document m : doc.getList("members", Document.class, Collections.emptyList())) {
            String uuidStr = m.getString("uuid");
            String roleStr = m.getString("role");
            Long joinedAtMs = m.getLong("joinedAt");
            if (uuidStr == null || roleStr == null) continue;
            try {
                UUID uuid = UUID.fromString(uuidStr);
                FactionRole role = FactionRole.valueOf(roleStr);
                Instant joinedAt = joinedAtMs != null ? Instant.ofEpochMilli(joinedAtMs) : Instant.now();
                members.put(uuid, new FactionMember(uuid, role, joinedAt));
            } catch (IllegalArgumentException ignored) {
            }
        }

        Set<String> allies = new HashSet<>(doc.getList("allies", String.class, Collections.emptyList()));
        Set<String> enemies = new HashSet<>(doc.getList("enemies", String.class, Collections.emptyList()));

        String leaderIdStr = doc.getString("leaderId");
        UUID leaderId;
        try {
            leaderId = leaderIdStr != null ? UUID.fromString(leaderIdStr) : new UUID(0, 0);
        } catch (IllegalArgumentException e) {
            leaderId = new UUID(0, 0);
        }

        Long createdAtMs = doc.getLong("createdAt");

        Faction faction = new Faction(
                doc.getString("_id"),
                doc.getString("name"),
                leaderId,
                doc.getInteger("maxDtk", 20),
                doc.getInteger("dtkRemaining", 20),
                doc.getInteger("kills", 0),
                doc.getInteger("deaths", 0),
                doc.getDouble("balance") != null ? doc.getDouble("balance") : 0.0,
                createdAtMs != null ? Instant.ofEpochMilli(createdAtMs) : Instant.now(),
                members, allies, enemies
        );

        Document homeDoc = doc.get("home", Document.class);
        if (homeDoc != null) {
            Double hy = homeDoc.getDouble("y");
            faction.setHome(new FactionHome(
                    homeDoc.getString("world"),
                    homeDoc.getDouble("x") != null ? homeDoc.getDouble("x") : 0,
                    hy != null ? hy : 64,
                    homeDoc.getDouble("z") != null ? homeDoc.getDouble("z") : 0,
                    homeDoc.getDouble("yaw") != null ? homeDoc.getDouble("yaw").floatValue() : 0f,
                    homeDoc.getDouble("pitch") != null ? homeDoc.getDouble("pitch").floatValue() : 0f
            ));
        }

        return faction;
    }

    private Document toDocument(Faction f) {
        List<Document> membersDoc = new ArrayList<>();
        for (FactionMember m : f.getMembers().values()) {
            membersDoc.add(new Document()
                    .append("uuid", m.getUuid().toString())
                    .append("role", m.getRole().name())
                    .append("joinedAt", m.getJoinedAt().toEpochMilli()));
        }

        return new Document()
                .append("_id", f.getId())
                .append("name", f.getName())
                .append("leaderId", f.getLeaderId().toString())
                .append("maxDtk", f.getMaxDtk())
                .append("dtkRemaining", f.getDtkRemaining())
                .append("kills", f.getKills())
                .append("deaths", f.getDeaths())
                .append("balance", f.getBalance())
                .append("createdAt", f.getCreatedAt().toEpochMilli())
                .append("members", membersDoc)
                .append("allies", new ArrayList<>(f.getAllies()))
                .append("enemies", new ArrayList<>(f.getEnemies()))
                .append("home", f.getHome() == null ? null : new Document()
                        .append("world", f.getHome().getWorld())
                        .append("x", f.getHome().getX())
                        .append("y", f.getHome().getY())
                        .append("z", f.getHome().getZ())
                        .append("yaw", (double) f.getHome().getYaw())
                        .append("pitch", (double) f.getHome().getPitch()));
    }
}
