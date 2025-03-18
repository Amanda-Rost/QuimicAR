package com.google.ar.core.examples.java.principal.rendering;

import android.util.Log;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
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
    private static final String CONNECTION_STRING = "mongodb://aluno:QuimicAR@cluster0.nc4hk.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
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

    /**
     * Retorna a instância do banco de dados.
     */
    public MongoDatabase getDatabase() {
        return this.database;
    }

    /**
     * Busca um composto no banco de dados pelo seu formato.
     * O resultado é retornado via callback.
     */
    public void buscarCompostoPorFormato(String formato, OnDatabaseResultListener listener) {
        if (database == null) {
            Log.e(TAG, "Banco de dados não está conectado.");
            listener.onError(new Exception("Banco de dados não conectado"));
            return;
        }

        MongoCollection<Document> collection = database.getCollection("compostos");

        collection.find(Filters.eq("formato", formato)).first().subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1); // Solicita apenas um documento
            }

            @Override
            public void onNext(Document document) {
                Log.d(TAG, "Documento encontrado no MongoDB: " + document.toJson());

                Binary objeto3D = document.get("3D", Binary.class);
                Binary config3D = document.get("config3D", Binary.class);
                Binary textura = document.get("textura", Binary.class);

                if (objeto3D != null && config3D != null && textura != null) {
                    listener.onSuccess(objeto3D.getData(), config3D.getData(), textura.getData());
                } else {
                    Log.e(TAG, "Modelo 3D encontrado, mas está incompleto.");
                    listener.onError(new Exception("Modelo 3D incompleto no banco de dados"));
                }
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Erro ao buscar no MongoDB: " + t.getMessage(), t);
                listener.onError(new Exception("Erro ao buscar no MongoDB: " + t.getMessage()));
            }

            @Override
            public void onComplete() {
                // Busca finalizada
            }
        });
    }

    /**
     * Interface para callback dos resultados do banco de dados.
     */
    public interface OnDatabaseResultListener {
        void onSuccess(byte[] object3D, byte[] config3D, byte[] textura);
        void onError(Exception e);
    }

    /**
     * Fecha a conexão com o MongoDB.
     */
    public void fecharConexao() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            Log.d(TAG, "Conexão com MongoDB fechada.");
        }
    }
}
