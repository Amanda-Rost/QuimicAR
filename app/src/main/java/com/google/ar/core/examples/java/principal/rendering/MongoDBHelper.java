package com.google.ar.core.examples.java.principal.rendering;

import android.util.Log;
import com.google.ar.core.examples.java.principal.rendering.validation.OnConnectionCheckListener;
import com.google.ar.core.examples.java.principal.rendering.validation.OnDatabaseResultListener;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


public class MongoDBHelper {

    private static final String TAG = "MongoDBHelper";
    private static final String BASE_URL = "https://api-conexao-mongo-db-quimic-ar-xvwf.vercel.app/";

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

    public void checkConnection(final OnConnectionCheckListener listener) {
        if (listener == null) {
            Log.w(TAG, "OnConnectionCheckListener is null. Cannot report connection status.");
            return;
        }

        Call<Composto> call = compostoService.getCompostoByNomenclatura("METAN");
        call.enqueue(new Callback<Composto>() {
            @Override
            public void onResponse(Call<Composto> call, Response<Composto> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Conexão bem-sucedida!");
                    listener.onConnectionChecked(true);
                } else {
                    Log.e(TAG, "Falha na conexão: " + response.message());
                    listener.onConnectionChecked(false);
                }
            }

            @Override
            public void onFailure(Call<Composto> call, Throwable t) {
                Log.e(TAG, "Erro de conexão: " + t.getMessage(), t);
                listener.onConnectionChecked(false);
            }
        });
    }

    public void buscarCompostoPorFormato(String formato, final OnDatabaseResultListener listener) {
        if (listener == null) {
            Log.w(TAG, "buscarCompostoPorFormato: OnDatabaseResultListener eh null.");
            return;
        }

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
                        composto.getConfig3D(),
                        composto.getTextura()
                    );
                } else {
                    Log.e(TAG, "buscarCompostoPorFormato - Error: " + response.code() + " - " + response.message());
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

    public void buscarCompostoPorNomenclatura(String nomenclatura, final OnDatabaseResultListener listener) {
        if (listener == null) {
            Log.w(TAG, "buscarCompostoPorNomenclatura: OnDatabaseResultListener eh null.");
            return;
        }

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
                        composto.getConfig3D(),
                        composto.getTextura()
                    );
                } else {
                    Log.e(TAG, "buscarCompostoPorNomenclatura - Error: " + response.code() + " - " + response.message());
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
}