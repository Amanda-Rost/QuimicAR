package com.google.ar.core.examples.java.principal.rendering;

import android.util.Log;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MongoDBHelper {

    private static final String TAG = "MongoDBHelper";
    private static final String BASE_URL = "https://api-conexao-mongo-db-quimic-ar-xvwf.vercel.app/";
    private boolean isConnected;
    private CompostoService compostoService;

    public MongoDBHelper() {
        // Configura o Retrofit para se conectar ao serviço
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Cria uma instância do serviço CompostoService
        compostoService = retrofit.create(CompostoService.class);
    }

    public void checkConnection(OnConnectionCheckListener listener) {
        Call<Composto> call = compostoService.getCompostoByNomenclatura("METAN");
        call.enqueue(new Callback<Composto>() {
            @Override
            public void onResponse(Call<Composto> call, Response<Composto> response) {
                if (response.isSuccessful()) {
                    setConnected(true);
                    Log.d(TAG, "Conexão bem-sucedida!");
                } else {
                    setConnected(false);
                    Log.e(TAG, "Falha na conexão: " + response.message());
                }
                listener.onConnectionChecked(isConnected);
            }

            @Override
            public void onFailure(Call<Composto> call, Throwable t) {
                setConnected(false);
                Log.e(TAG, "Erro de conexão: " + t.getMessage(), t);
                listener.onConnectionChecked(isConnected);
            }
        });
    }

    public void buscarCompostoPorFormato(String formato, OnDatabaseResultListener listener) {
        Call<Composto> call = compostoService.getCompostoByFormato(formato);
        call.enqueue(new Callback<Composto>() {
            @Override
            public void onResponse(Call<Composto> call, Response<Composto> response) {
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    Composto composto = response.body();
                    listener.onSuccess(
                        composto.getObjeto3D(),
                        composto.getConfig3D().getDataAsString(),
                        composto.getTextura().getDataAsString()
                    );
                } else {
                    listener.onError(new Exception("Composto não encontrado"));
                }
            }

            @Override
            public void onFailure(Call<Composto> call, Throwable t) {
                Log.e(TAG, "Erro ao buscar composto: " + t.getMessage(), t);
                listener.onError(new Exception("Erro ao buscar composto: " + t.getMessage()));
            }
        });
    }

    public void buscarCompostoPorNomenclatura(String nomenclatura, OnDatabaseResultListener listener) {
        Call<Composto> call = compostoService.getCompostoByNomenclatura(nomenclatura);
        call.enqueue(new Callback<Composto>() {
            @Override
            public void onResponse(Call<Composto> call, Response<Composto> response) {
                Log.d(TAG, "Response code: " + response.code());
                Log.d(TAG, "Response body: " + response.body());

                if (response.isSuccessful() && response.body() != null) {
                    Composto composto = response.body();
                    listener.onSuccess(
                        composto.getObjeto3D(),
                        composto.getConfig3D().getDataAsString(),
                        composto.getTextura().getDataAsString()
                    );
                } else {
                    listener.onError(new Exception("Composto não encontrado"));
                }
            }

            @Override
            public void onFailure(Call<Composto> call, Throwable t) {
                Log.e(TAG, "Erro ao buscar composto: " + t.getMessage(), t);
                listener.onError(new Exception("Erro ao buscar composto: " + t.getMessage()));
            }
        });
    }

    public void setConnected(boolean verificação) {
        this.isConnected = verificação;
    }

    public boolean getDatabase() {
        return this.isConnected;
    }

    public interface OnDatabaseResultListener {
        void onSuccess(String object3DPath, String config3DPath, String texturaPath);
        void onError(Exception e);
    }

    public interface OnConnectionCheckListener {
        void onConnectionChecked(boolean isConnected);
    }
}