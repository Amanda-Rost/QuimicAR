package config.principal.rendering;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface CompostoService {

    @GET("compostos/formato/{formato}")
    Call<Composto> getCompostoByFormato(@Path("formato") String formato);

    @GET("compostos/nomenclatura/{nomenclatura}")
    Call<Composto> getCompostoByNomenclatura(@Path("nomenclatura") String nomenclatura);
}