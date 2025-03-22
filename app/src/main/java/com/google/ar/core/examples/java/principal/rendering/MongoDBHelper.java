package com.google.ar.core.examples.java.principal.rendering;

// Importações necessárias para o funcionamento do MongoDB e logging
import android.util.Log;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoClients;
import com.mongodb.reactivestreams.client.MongoDatabase;
import org.bson.Document;
import org.bson.types.Binary;
import com.mongodb.reactivestreams.client.MongoCollection;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

public class MongoDBHelper {

    // Constantes para logging e conexão com o MongoDB
    private static final String TAG = "MongoDBHelper";
    private static final String CONNECTION_STRING = "mongodb://aluno:QuimicAR@cluster0.nc4hk.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0";
    private static final String DATABASE_NAME = "QuimicAR";

    // Variáveis para o cliente e banco de dados MongoDB
    private MongoClient mongoClient;
    private MongoDatabase database;

    // Construtor que inicia a conexão com o MongoDB
    public MongoDBHelper() {
        connectToMongoDB();
    }

    // Método para conectar ao MongoDB
    private void connectToMongoDB() {
        try {
            // Configura a conexão com o MongoDB usando a string de conexão
            ConnectionString connectionString = new ConnectionString(CONNECTION_STRING);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connectionString)
                    .build();

            // Cria o cliente MongoDB e obtém o banco de dados
            mongoClient = MongoClients.create(settings);
            database = mongoClient.getDatabase(DATABASE_NAME);

            Log.d(TAG, "Conexão com MongoDB estabelecida.");

        } catch (Exception e) {
            Log.e(TAG, "Erro ao conectar ao MongoDB: " + e.getMessage(), e);
        }
    }

    // Retorna o banco de dados conectado
    public MongoDatabase getDatabase() {
        return this.database;
    }

    // Método para buscar um composto pelo formato
    public void buscarCompostoPorFormato(String formato, OnDatabaseResultListener listener) {
        if (database == null) {
            Log.e(TAG, "Banco de dados não está conectado.");
            listener.onError(new Exception("Banco de dados não conectado"));
            return;
        }

        Log.d(TAG, "Iniciando busca por formato: " + formato);

        // Obtém a coleção de compostos do banco de dados
        MongoCollection<Document> collection = database.getCollection("compostos");

        // Realiza a busca pelo formato e processa o resultado
        collection.find(Filters.eq("formato", formato)).first().subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1); // Solicita um documento
            }

            @Override
            public void onNext(Document document) {
                Log.d(TAG, "Documento encontrado no MongoDB pelo formato: " + document.toJson());
                processDocument(document, listener);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Erro ao buscar no MongoDB: " + t.getMessage(), t);
                listener.onError(new Exception("Erro ao buscar no MongoDB: " + t.getMessage()));
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Busca por formato concluída.");
                // Se não encontrou nada, tenta buscar por nomenclatura
                buscarCompostoPorNomenclatura(formato, listener);
            }
        });
    }

    // Método para buscar um composto pela nomenclatura
    private void buscarCompostoPorNomenclatura(String nomenclatura, OnDatabaseResultListener listener) {
        Log.d(TAG, "Iniciando busca por nomenclatura: " + nomenclatura);

        MongoCollection<Document> collection = database.getCollection("compostos");

        // Realiza a busca pela nomenclatura e processa o resultado
        collection.find(Filters.eq("nomenclatura", nomenclatura)).first().subscribe(new Subscriber<Document>() {
            @Override
            public void onSubscribe(Subscription s) {
                s.request(1); // Solicita um documento
            }

            @Override
            public void onNext(Document document) {
                Log.d(TAG, "Documento encontrado no MongoDB pela nomenclatura: " + document.toJson());
                processDocument(document, listener);
            }

            @Override
            public void onError(Throwable t) {
                Log.e(TAG, "Erro ao buscar por nomenclatura no MongoDB: " + t.getMessage(), t);
                listener.onError(new Exception("Erro ao buscar por nomenclatura no MongoDB: " + t.getMessage()));
            }

            @Override
            public void onComplete() {
                Log.d(TAG, "Busca por nomenclatura concluída.");
            }
        });
    }

    // Processa o documento encontrado e chama o listener apropriado
    private void processDocument(Document document, OnDatabaseResultListener listener) {
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

    // Interface para lidar com os resultados da busca no banco de dados
    public interface OnDatabaseResultListener {
        void onSuccess(byte[] object3D, byte[] config3D, byte[] textura);
        void onError(Exception e);
    }

    // Fecha a conexão com o MongoDB
    public void fecharConexao() {
        if (mongoClient != null) {
            mongoClient.close();
            mongoClient = null;
            database = null;
            Log.d(TAG, "Conexão com MongoDB fechada.");
        }
    }
}