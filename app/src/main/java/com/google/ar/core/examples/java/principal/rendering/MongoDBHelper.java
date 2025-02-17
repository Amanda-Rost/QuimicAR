package com.google.ar.core.examples.java.principal.rendering;

import android.util.Log;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoDBHelper {
    private static final String TAG = "MongoDBHelper";
    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBHelper() {
        // Usar uma string de conexão direta, sem o +srv
        String connectionString = "mongodb://aluno:quimicAR@cluster0-shard-00-00.nc4hk.mongodb.net:27017,cluster0-shard-00-01.nc4hk.mongodb.net:27017,cluster0-shard-00-02.nc4hk.mongodb.net:27017/";
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(new ConnectionString(connectionString))
                .build();
        mongoClient = MongoClients.create(settings);
        database = mongoClient.getDatabase("QuimicAR");
    }

    public MongoDatabase getDatabase() {
        return database;
    }

    public boolean testarConexao() {
        try {
            Document ping = new Document("ping", 1);
            database.runCommand(ping);
            Log.d(TAG, "Pinged your deployment. You successfully connected to MongoDB!");
            return true;
        } catch (MongoException e) {
            Log.e(TAG, "Falha na conexão com MongoDB", e);
            return false;
        }
    }

    public Document findObject3D(String textImage) {
        MongoCollection<Document> collection = database.getCollection("compostos");
        return collection.find(new Document("textImage", textImage)).first();
    }

    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }
}
