package com.example.salvo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.Locale

class MainScreenActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private var userId: Int = -1
    private var nomeUsuario: String = ""

    // 🚀 Ferramenta de GPS
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // 🚀 O questionário de permissão para o cliente
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            buscarLocalizacaoCliente()
        } else {
            Toast.makeText(this, "Precisamos do GPS para encontrar socorro!", Toast.LENGTH_LONG).show()
            val tvEnderecoAtual = findViewById<TextView>(R.id.tv_endereco_atual)
            tvEnderecoAtual.text = "Permissão de GPS negada"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_screen)

        // Inicializa a ferramenta de GPS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 1. Recebendo os dados enviados pelo LoginActivity
        nomeUsuario = intent.getStringExtra("NOME_USUARIO") ?: "Cliente"
        userId = intent.getIntExtra("USER_ID", -1)

        // 2. Personalizando a saudação na tela
        val tvSaudacao = findViewById<TextView>(R.id.tv_saudacao)
        val primeiroNome = nomeUsuario.split(" ")[0]
        tvSaudacao.text = "Olá, $primeiroNome!"

        // 3. Inicializando o Mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_view) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // 4. Configurar os botões
        configurarBotoesServico()

        // 5. Configurar a Barra de Navegação Inferior
        configurarBottomNavigation()
    }

    private fun configurarBotoesServico() {
        val btnGuincho = findViewById<LinearLayout>(R.id.btn_guincho)
        val btnBateria = findViewById<LinearLayout>(R.id.btn_bateria)
        val btnPneu = findViewById<LinearLayout>(R.id.btn_pneu)
        val btnMecanica = findViewById<LinearLayout>(R.id.btn_mecanica)

        val abrirSocorro = { tipoServico: String ->
            val intent = Intent(this, SocorroActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("TIPO_SERVICO", tipoServico)
            startActivity(intent)
        }

        btnGuincho.setOnClickListener { abrirSocorro("Guincho") }
        btnBateria.setOnClickListener { abrirSocorro("Bateria") }
        btnPneu.setOnClickListener { abrirSocorro("Pneu") }
        btnMecanica.setOnClickListener { abrirSocorro("Mecanica") }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Esconde os controles padrão do Google para deixar o design mais limpo
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isMyLocationButtonEnabled = false

        // 🚀 Assim que o mapa carrega, pede o GPS
        verificarPermissaoGPS()
    }

    // ==========================================
    // 📍 LÓGICA DE GPS E ENDEREÇO
    // ==========================================
    private fun verificarPermissaoGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            buscarLocalizacaoCliente()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun buscarLocalizacaoCliente() {
        val tvEnderecoAtual = findViewById<TextView>(R.id.tv_endereco_atual)
        tvEnderecoAtual.text = "Buscando sinal do satélite..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val posicaoCliente = LatLng(location.latitude, location.longitude)

                // Atualiza o Mapa
                mMap.clear()
                mMap.addMarker(MarkerOptions().position(posicaoCliente).title("Você está aqui"))
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(posicaoCliente, 17f))

                // 🚀 O GEOCODER: Transforma a coordenada em nome de rua
                try {
                    val geocoder = Geocoder(this, Locale.getDefault())
                    val enderecos = geocoder.getFromLocation(location.latitude, location.longitude, 1)

                    if (!enderecos.isNullOrEmpty()) {
                        val endereco = enderecos[0]
                        val rua = endereco.thoroughfare ?: "Endereço desconhecido"
                        val numero = endereco.subThoroughfare ?: ""

                        // Exemplo de resultado: "Avenida Paulista, 1000"
                        val enderecoFormatado = if (numero.isNotEmpty()) "$rua, $numero" else rua
                        tvEnderecoAtual.text = enderecoFormatado
                    } else {
                        tvEnderecoAtual.text = "Localização encontrada (Sem nome de rua)"
                    }
                } catch (e: Exception) {
                    // Caso o serviço do Google esteja fora do ar ou sem internet
                    tvEnderecoAtual.text = "Localização encontrada"
                }

            } else {
                tvEnderecoAtual.text = "Erro: Ligue o GPS do celular"
            }
        }
    }

    // ==========================================
    // LÓGICA DA BARRA DE NAVEGAÇÃO
    // ==========================================
    private fun configurarBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_cliente)
        bottomNav.selectedItemId = R.id.nav_home

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { true }
                R.id.nav_pedidos -> {
                    val intent = Intent(this, MeusPedidosActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    startActivity(intent)
                    true
                }
                R.id.nav_chat -> {
                    Toast.makeText(this, "Em breve: Chat!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_perfil -> {
                    val intent = Intent(this, PerfilClienteActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("NOME_USUARIO", nomeUsuario)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}