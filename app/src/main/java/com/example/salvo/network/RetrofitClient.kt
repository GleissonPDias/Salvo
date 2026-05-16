package com.example.salvo

import com.example.salvo.model.AuthResponse
import com.example.salvo.model.LoginRequest
import com.example.salvo.model.PedidoSocorroRequest
import com.example.salvo.model.PedidoSocorroResponse
import com.example.salvo.model.ProviderServiceResponse
import com.example.salvo.model.RegisterRequest
import com.example.salvo.model.ServiceItem
import com.example.salvo.model.ServiceRequest
import com.example.salvo.model.Vehicle
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// 1. A Interface Central com TODAS as rotas
interface ApiService {
    @POST("login")
    fun login(@Body request: LoginRequest): Call<AuthResponse>

    @POST("cadastro")
    fun cadastrar(@Body request: RegisterRequest): Call<AuthResponse>

    @POST("solicitar-socorro")
    fun solicitarSocorro(@Body request: PedidoSocorroRequest): Call<PedidoSocorroResponse>

    @GET("listar-pedidos")
    fun listarPedidos(@Query("userId") userId: Int): Call<List<ServiceRequest>>

    // Rota para salvar edições (PATCH)
    @PATCH("atualizar-perfil/{id}")
    fun atualizarCampoPerfil(
        @Path("id") id: Int,
        @Body campos: Map<String, String>
    ): Call<AuthResponse>

    // --- ROTAS PARA ACABAR COM O HARDCODED ---

    // 1. Busca os dados básicos (Nome, CNPJ, Endereço)
    @GET("obter-perfil/{id}")
    fun obterPerfil(@Path("id") id: Int): Call<Map<String, String?>>

    // 2. Busca a lista de serviços e preços da tabela provider_services
    @GET("servicos-publicos/{id}")
    fun obterServicosDaOficina(@Path("id") id: Int): Call<List<ProviderServiceResponse>>


    @GET("veiculos-oficina/{providerId}")
    fun obterVeiculos(@Path("providerId") providerId: Int): Call<List<Vehicle>>

    @POST("adicionar-veiculo")
    fun adicionarVeiculo(@Body dados: Map<String, String?>): Call<AuthResponse>

    @DELETE("excluir-veiculo/{id}/{providerId}")
    fun excluirVeiculo(@Path("id") id: Int, @Path("providerId") providerId: Int): Call<AuthResponse>

    @PATCH("atualizar-status-veiculo/{id}")
    fun atualizarStatusVeiculo(
        @Path("id") id: Int,
        @Body dados: Map<String, String>
    ): Call<AuthResponse>

    @PUT("atualizar-veiculo/{id}")
    fun atualizarVeiculoCompleto(
        @Path("id") id: Int,
        @Body dados: Map<String, String?>
    ): Call<AuthResponse>

    // --- ROTAS DO CARDÁPIO DE SERVIÇOS ---

    @GET("servicos-oficina/{providerId}")
    fun obterServicos(
        @Path("providerId") providerId: Int
    ): Call<List<ServiceItem>>

    @POST("adicionar-servico")
    fun adicionarServico(
        @Body dados: Map<String, String>
    ): Call<AuthResponse>

    @PUT("atualizar-servico/{id}")
    fun atualizarServico(
        @Path("id") id: Int,
        @Body dados: Map<String, String>
    ): Call<AuthResponse>

    @PATCH("alternar-status-servico/{id}")
    fun alternarStatusServico(
        @Path("id") id: Int,
        @Body dados: Map<String, String>
    ): Call<AuthResponse>

    @DELETE("excluir-servico/{id}/{providerId}")
    fun excluirServico(
        @Path("id") id: Int,
        @Path("providerId") providerId: Int
    ): Call<AuthResponse>

}

// 2. O Singleton (O único "telefone" do aplicativo)
object RetrofitClient {
    private const val BASE_URL = "https://apisalvologin.onrender.com/" // A sua URL do Render

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}