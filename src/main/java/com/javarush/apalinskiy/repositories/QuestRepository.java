package com.javarush.apalinskiy.repositories;

import com.javarush.apalinskiy.quest.io.QuestJsonReader;
import com.javarush.apalinskiy.quest.core.QuestNavigator;
import com.javarush.apalinskiy.quest.model.QuestNode;
import lombok.Getter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Getter
public class QuestRepository {

    private record Snapshot(QuestNavigator nav, String version) {
    }

    private final AtomicReference<Snapshot> ref = new AtomicReference<>();
    private final int startId;

    private QuestRepository(int startId, Snapshot snapshot) {
        this.startId = startId;
        this.ref.set(Objects.requireNonNull(snapshot, "snapshot"));
    }

    public static QuestRepository fromClasspath(String resourceName, int startId) throws java.io.IOException {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        try (InputStream in = cl.getResourceAsStream(resourceName)) {
            if (in == null) {
                throw new java.io.IOException("Resource not found on classpath: " + resourceName);
            }
            byte[] bytes = in.readAllBytes();
            return fromBytes(bytes, startId);
        }
    }

    private static QuestRepository fromBytes(byte[] bytes, int startId) throws IOException {
        QuestJsonReader reader = new QuestJsonReader();
        List<QuestNode> nodes = reader.read(new StringReader(new String(bytes, StandardCharsets.UTF_8)));
        QuestNavigator nav = QuestNavigator.from(nodes, startId);
        String version = "sha256:" + sha256(bytes);
        return new QuestRepository(startId, new Snapshot(nav, version));
    }

    public String version() {
        return ref.get().version;
    }

    public QuestNode start() {
        return ref.get().nav.start();
    }

    public QuestNode get(int id) {
        return ref.get().nav.get(id);
    }

    public Optional<QuestNode> choose(int fromId, String answer) {
        return ref.get().nav.choose(fromId, answer);
    }

    private static String sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(data));
        } catch (Exception e) {
            return "unknown";
        }
    }
}