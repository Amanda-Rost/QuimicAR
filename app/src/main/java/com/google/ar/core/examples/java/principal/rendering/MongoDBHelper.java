package com.google.ar.core.examples.java.principal.rendering;

import android.util.Log;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.bson.types.Binary;
import com.mongodb.reactivestreams.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MongoDBHelper {

    private static final String TAG = "MongoDBHelper";
    private static final String CONNECTION_STRING = "mongodb+srv://aluno:QuimicAR@cluster0.nc4hk.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static final String DATABASE_NAME = "QuimicAR";

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBHelper() {
        connectToMongoDB();
    }

    private void connectToMongoDB() {
        try {
            ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();



            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(DATABASE_NAME);

            Log.d(TAG, "Conexão com MongoDB estabelecida.");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao conectar ao MongoDB: " + e.getMessage(), e);
        }
    }

    public MongoDatabase getDatabase(){
        return this.database;
    }

    public void inserirComposto(String formato, byte[] objeto3D, byte[] config3D, byte[] textura) {
        if (database == null) {
            Log.e(TAG, "Banco de dados não está conectado.");
            return;
        }

        MongoCollection<Document> collection = database.getCollection("compostos");

        Document documento = new Document("formato", formato)
                .append("3D", new Binary(objeto3D))
                .append("config3D", new Binary(config3D))
                .append("textura", new Binary(textura));

        collection.insertOne(documento).subscribe(new Subscriber<InsertOneResult>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1);
            }

            @Override
            public void onNext(InsertOneResult result) {
                Log.d(TAG, "Documento inserido com sucesso! ID: " + result.getInsertedId());
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Erro ao inserir documento: " + t.getMessage(), t);
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Inserção concluída.");
            }
        });
    }

//    public void insertDocument(String collectionName, Document document) {
//        database.getCollection(collectionName).insertOne(document).subscribe(new Subscriber<>() {
//            @Override
//            public void onSubscribe(Subscription s) {
//                s.request(1);
//            }
//
//            @Override
//            public void onNext(Void aVoid) {
//                Log.d(TAG, "Documento inserido com sucesso.");
//            }
//
//            @Override
//            public void onError(Throwable t) {
//                Log.e(TAG, "Erro ao inserir documento: " + t.getMessage(), t);
//            }
//
//            @Override
//            public void onComplete() {
//                // Operação concluída
//            }
//        });
//    }

    // Outros métodos para operações no MongoDB podem ser adicionados aqui
}
