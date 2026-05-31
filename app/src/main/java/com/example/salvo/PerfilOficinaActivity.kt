package com.example.salvo

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.ServiceItem
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import com.google.android.material.button.MaterialButton
import java.io.ByteArrayOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PerfilOficinaActivity : AppCompatActivity() {

    private var oficinaId = -1
    // Reinserida a sua chave original
    private val GOOGLE_MAPS_API_KEY = "AIzaSyDXqV98pRQpyjpc0jXT47VAhks7iLncCZg"

    // --- LANÇADORES (ACTIVITY RESULTS) ---

    private val selecionarFoto = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            val base64String = uriToBase64(uri)
            if (base64String != null) {
                findViewById<ImageView>(R.id.iv_banner_imagem).setImageURI(uri)
                atualizarNoServidor("user_banner", base64String)
                Toast.makeText(this, "Salvando imagem...", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val startAutocomplete = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            val etLocalizacao = findViewById<EditText>(R.id.et_edit_localizacao)
            etLocalizacao.setText(place.address)
            etLocalizacao.tag = place.latLng // Coordenadas salvas no "tag" do campo
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_perfil_oficina)

        // 1. Recupera o ID da oficina
        oficinaId = intent.getIntExtra("USER_ID", -1)
        if (oficinaId == -1) {
            Toast.makeText(this, "Erro: Usuário não identificado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // 2. Inicializa o Places SDK com a sua chave
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, GOOGLE_MAPS_API_KEY)
        }

        // 3. Configurações de Botões e Cliques (Tudo dentro do onCreate agora para evitar crash)
        configurarBotoesNavegacao()

        findViewById<View>(R.id.btn_editar_banner).setOnClickListener {
            selecionarFoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // 4. Configura os campos para edição
        configurarCampoEditavel(R.id.btn_edit_nome, R.id.tv_val_nome_fantasia, R.id.layout_edit_nome, R.id.et_edit_nome, R.id.btn_save_nome, "user_name")
        configurarCampoEditavel(R.id.btn_edit_cnpj, R.id.tv_val_cnpj, R.id.layout_edit_cnpj, R.id.et_edit_cnpj, R.id.btn_save_cnpj, "user_cpf_cnpj")
        configurarLocalizacaoInteligente()

        configurarAjustesTela()
    }

    override fun onResume() {
        super.onResume()
        // Recarrega os dados toda vez que o usuário volta para esta tela
        if (oficinaId != -1) {
            carregarDadosDoPerfil()
            carregarServicosDaOficina()
        }
    }

    private fun configurarBotoesNavegacao() {
        // Botão para ver Cardápio de Serviços
        val btnVerServicos = findViewById<View>(R.id.btn_visualizar_servicos)
        btnVerServicos.setOnClickListener {
            val intent = Intent(this, CardapioServicosActivity::class.java)
            intent.putExtra("USER_ID", oficinaId)
            startActivity(intent)
        }

        // Botão para ver Frota (Veículos)
        val btnVerVeiculos = findViewById<View>(R.id.btn_visualizar_veiculos)
        btnVerVeiculos.setOnClickListener {
            val intent = Intent(this, GestaoFrotaActivity::class.java)
            intent.putExtra("USER_ID", oficinaId)
            startActivity(intent)
        }

        // 🔥 NOVO: Botão Sair da Conta
        val btnSair = findViewById<MaterialButton>(R.id.btn_sair_conta)
        btnSair.setOnClickListener {
            val sessionManager = com.example.salvo.utils.SessionManager(this)
            sessionManager.limparSessao()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    // --- BUSCA DE DADOS (API) ---

    private fun carregarDadosDoPerfil() {
        RetrofitClient.apiService.obterPerfil(oficinaId).enqueue(object : Callback<Map<String, String?>> {
            override fun onResponse(call: Call<Map<String, String?>>, response: Response<Map<String, String?>>) {
                if (response.isSuccessful) {
                    val dados = response.body() ?: return

                    findViewById<TextView>(R.id.tv_banner_nome).text = dados["nome"] ?: "Oficina"
                    findViewById<TextView>(R.id.tv_val_nome_fantasia).text = dados["nome"] ?: ""

                    val rating = dados["rating"] ?: "5.0"
                    val reviews = dados["reviews"] ?: "0"
                    findViewById<TextView>(R.id.tv_banner_rating).text = "★ $rating ($reviews reviews)"

                    dados["banner"]?.let { base64 ->
                        if (base64.isNotEmpty()) exibirBase64NoImageView(base64, findViewById(R.id.iv_banner_imagem))
                    }

                    findViewById<TextView>(R.id.tv_val_cnpj).text = dados["cnpj"] ?: "Não informado"
                    findViewById<TextView>(R.id.tv_val_localizacao).text = dados["endereco"] ?: "Não definido"
                }
            }
            override fun onFailure(call: Call<Map<String, String?>>, t: Throwable) {
                Log.e("API", "Erro ao carregar perfil")
            }
        })
    }

    private fun carregarServicosDaOficina() {
        // Agora usamos ServiceItem para manter o padrão do novo cardápio
        RetrofitClient.apiService.obterServicos(oficinaId).enqueue(object : Callback<List<ServiceItem>> {
            override fun onResponse(call: Call<List<ServiceItem>>, response: Response<List<ServiceItem>>) {
                if (response.isSuccessful) {
                    val servicos = response.body() ?: emptyList()
                    val ativos = servicos.filter { it.isActive }

                    val resumo = if (ativos.isEmpty()) "Nenhum serviço ativo no momento."
                    else ativos.joinToString("  |  ") { it.serviceType }

                    findViewById<TextView>(R.id.tv_especialidades_lista).text = resumo
                }
            }
            override fun onFailure(call: Call<List<ServiceItem>>, t: Throwable) {
                Log.e("API", "Erro ao carregar especialidades")
            }
        })
    }

    // --- LÓGICA DE ATUALIZAÇÃO ---

    private fun atualizarPacoteNoServidor(dados: Map<String, String>) {
        RetrofitClient.apiService.atualizarCampoPerfil(oficinaId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@PerfilOficinaActivity, "Alteração salva com sucesso!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@PerfilOficinaActivity, "Erro ao salvar no servidor", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@PerfilOficinaActivity, "Erro de conexão", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 2. Mantém a sua função original funcionando para nome, cnpj, foto, etc.
    private fun atualizarNoServidor(campo: String, valor: String) {
        atualizarPacoteNoServidor(mapOf(campo to valor))
    }

    private fun configurarCampoEditavel(btnEditId: Int, tvValorId: Int, layoutEditId: Int, etEditId: Int, btnSaveId: Int, campoBanco: String) {
        val btnEdit = findViewById<ImageView>(btnEditId)
        val tvValor = findViewById<TextView>(tvValorId)
        val layoutEdit = findViewById<View>(layoutEditId)
        val etEdit = findViewById<EditText>(etEditId)
        val btnSave = findViewById<MaterialButton>(btnSaveId)

        btnEdit.setOnClickListener {
            tvValor.visibility = View.GONE
            btnEdit.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE
            etEdit.setText(tvValor.text)
        }

        btnSave.setOnClickListener {
            val novoValor = etEdit.text.toString()
            tvValor.text = novoValor
            tvValor.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE
            layoutEdit.visibility = View.GONE
            atualizarNoServidor(campoBanco, novoValor)
        }
    }

    private fun configurarLocalizacaoInteligente() {
        val btnEdit = findViewById<ImageView>(R.id.btn_edit_localizacao)
        val tvValor = findViewById<TextView>(R.id.tv_val_localizacao)
        val layoutEdit = findViewById<View>(R.id.layout_edit_localizacao)
        val etEdit = findViewById<EditText>(R.id.et_edit_localizacao)
        val btnSave = findViewById<MaterialButton>(R.id.btn_save_localizacao)

        btnEdit.setOnClickListener {
            tvValor.visibility = View.GONE
            btnEdit.visibility = View.GONE
            layoutEdit.visibility = View.VISIBLE

            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,
                listOf(Place.Field.ADDRESS, Place.Field.LAT_LNG))
                .setCountry("BR")
                .build(this)
            startAutocomplete.launch(intent)
        }

        btnSave.setOnClickListener {
            val novoEnd = etEdit.text.toString()
            val latLng = etEdit.tag as? LatLng

            tvValor.text = novoEnd
            tvValor.visibility = View.VISIBLE
            btnEdit.visibility = View.VISIBLE
            layoutEdit.visibility = View.GONE

            if (latLng != null) {
                // 🔥 O SEGREDO: Monta o pacote triplo e envia de UMA vez só!
                val pacoteLocalizacao = mapOf(
                    "user_address" to novoEnd,
                    "latitude" to latLng.latitude.toString(),
                    "longitude" to latLng.longitude.toString()
                )
                atualizarPacoteNoServidor(pacoteLocalizacao)
            } else {
                // Prevenção de falha caso a LatLng esteja nula
                atualizarNoServidor("user_address", novoEnd)
            }
        }
    }

    // --- UTILITÁRIOS ---

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 40, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) { null }
    }

    private fun exibirBase64NoImageView(base64Str: String, imageView: ImageView) {
        try {
            val decodedString = Base64.decode(base64Str, Base64.DEFAULT)
            val decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
            imageView.setImageBitmap(decodedByte)
        } catch (e: Exception) {
            Log.e("IMG", "Erro ao exibir base64")
        }
    }

    private fun configurarAjustesTela() {
        // Usei o root view genérico aqui para evitar erro caso seu ID de layout mude
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}