package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import com.google.ar.core.examples.java.principal.rendering.Composto;
import com.google.ar.core.examples.java.principal.rendering.CompostoService;

import org.junit.Before;
import org.junit.Test;

import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class TesteCompostoService {

    private CompostoService compostoService;

    @Before
    public void setUp() {
        // Configure Retrofit
        System.out.println("Iniciando teste ... \n");
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://api-conexao-mongo-db-quimic-ar-xvwf.vercel.app/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        // Create a real instance of CompostoService
        compostoService = retrofit.create(CompostoService.class);
    }

    @Test
    public void testGetCompostoByNomenclatura() throws Exception {
        // Act
        Call<Composto> call = compostoService.getCompostoByNomenclatura("METAN");
        Response<Composto> response = call.execute();
    
        // Assert
        if (response.isSuccessful() && response.body() != null) {
            Composto composto = response.body();
            
            // Log para ver os detalhes da resposta
            System.out.println("Objeto Composto: " + composto);
            System.out.println("Nomenclatura: " + composto.getNomenclatura());
            System.out.println("Objeto 3D Path: " + composto.getObjeto3D());
            System.out.println("Config 3D Path: " + composto.getConfig3D().getDataAsString());
            System.out.println("Textura Path: " + composto.getTextura().getDataAsString());
    
            assertEquals("METAN", composto.getNomenclatura());
            assertEquals("models/metano.obj", composto.getObjeto3D());
            assertEquals("models/metano.mtl", composto.getConfig3D().getDataAsString());
            assertEquals("models/pretobranco.png", composto.getTextura().getDataAsString());
        } else {
            throw new AssertionError("Falha ao buscar dados da API: " + response.message());
        }
    }
}