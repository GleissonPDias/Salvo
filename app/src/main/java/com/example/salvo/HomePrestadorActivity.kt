package com.example.salvo

import android.content.Intent
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.salvo.databinding.ActivityHomePrestadorBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.CircleOptions
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch
import com.example.salvo.adapter.RecentActivity
import com.example.salvo.adapter.RecentActivityAdapter
import com.example.salvo.model.AceitarPedidoRequestApp
import com.example.salvo.model.AceitarPedidoResponse
import com.example.salvo.network.WebSocketManager

class HomePrestadorActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityHomePrestadorBinding
    private lateinit var viewModel: HomePrestadorViewModel

    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUserId: Int = -1
    private var socketManager: WebSocketManager? = null

    private var pedidoAtualId: Int = -1
    private var pedidoAtualPreco: Double = 0.0
    private var pedidoAtualDistancia: Double = 0.0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            pegarLocalizacaoAtual()
        } else {
            Toast.makeText(this, "Permissão negada. O Radar precisa do GPS.", Toast.LENGTH_LONG).show()
            atualizarMapa(LatLng(-23.55052, -46.633308))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomePrestadorBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentUserId = intent.getIntExtra("USER_ID", -1)
        Log.d("SALVO_WEBSOCKET", "ID do prestador logado recebido na Home: $currentUserId")

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_radar) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupViewModel()
        setupListeners()
        setupAlertaSocorroListeners()
        observeViewModel()
        setupRecyclerView()
        setupBottomNavigation()
    }

    private fun setupRecyclerView() {
        val dadosFigma = listOf(
            RecentActivity(1, "Carlos Silva", "Towing Service", "2.5km", "R$ 120", "14:30"),
            RecentActivity(2, "Mariana Costa", "Battery Jump", "4.1km", "R$ 80", "11:15")
        )
        binding.rvAtividadesRecentes.layoutManager = LinearLayoutManager(this)
        binding.rvAtividadesRecentes.adapter = RecentActivityAdapter(dadosFigma)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap?.uiSettings?.isZoomControlsEnabled = false
        mMap?.uiSettings?.isCompassEnabled = true
        verificarPermissaoGPS()
    }

    private fun verificarPermissaoGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            pegarLocalizacaoAtual()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    @SuppressLint("MissingPermission")
    private fun pegarLocalizacaoAtual() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val minhaPosicao = LatLng(location.latitude, location.longitude)
                atualizarMapa(minhaPosicao)
            } else {
                Toast.makeText(this, "Sinal de GPS fraco.", Toast.LENGTH_SHORT).show()
                atualizarMapa(LatLng(-23.55052, -46.633308))
            }
        }
    }

    private fun atualizarMapa(posicao: LatLng) {
        mMap?.clear()
        mMap?.addMarker(MarkerOptions().position(posicao).title("Você está aqui"))
        mMap?.addCircle(
            CircleOptions()
                .center(posicao)
                .radius(1500.0)
                .strokeColor(Color.parseColor("#FF9800"))
                .strokeWidth(2f)
                .fillColor(Color.parseColor("#26FF9800"))
        )
        mMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(posicao, 14f))
    }

    private fun setupViewModel() {
        val repository = ProviderHomeRepository()
        val providerIdString = currentUserId.toString()
        val factory = HomePrestadorViewModelFactory(repository, providerIdString)
        viewModel = ViewModelProvider(this, factory).get(HomePrestadorViewModel::class.java)
    }

    private fun setupListeners() {
        binding.switchStatus.setOnCheckedChangeListener { buttonView, isChecked ->
            if (buttonView.isPressed) {
                viewModel.toggleStatus(isChecked)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupAlertaSocorroListeners() {
        val alertaBinding = binding.containerAlerta

        alertaBinding.btnRecusar.setOnClickListener {
            alertaBinding.root.visibility = View.GONE
            Toast.makeText(this, "Chamado recusado.", Toast.LENGTH_SHORT).show()
        }

        var dX = 0f
        alertaBinding.btnSliderArrastar.setOnTouchListener { view, event ->
            val trackLargura = alertaBinding.layoutDeslizar.width
            val thumbLargura = view.width
            val limiteMaximo = (trackLargura - thumbLargura - 15).toFloat()

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dX = view.x - event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    var novaPosicaoX = event.rawX + dX
                    if (novaPosicaoX < 5f) novaPosicaoX = 5f
                    if (novaPosicaoX > limiteMaximo) novaPosicaoX = limiteMaximo
                    view.x = novaPosicaoX
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (view.x >= (limiteMaximo * 0.75f)) {

                        // 1. O botão chegou até o final! Prepara o pacote para a API
                        val requestSegura = AceitarPedidoRequestApp(
                            requestId = pedidoAtualId,
                            providerId = currentUserId,
                            price = pedidoAtualPreco,
                            distance = pedidoAtualDistancia
                        )

                        // 2. Chama o seu backend usando o Retrofit (Ajuste o RetrofitClient para a sua classe real)
                        RetrofitClient.apiService.aceitarSocorro(requestSegura).enqueue(object : retrofit2.Callback<AceitarPedidoResponse> {
                            override fun onResponse(call: retrofit2.Call<AceitarPedidoResponse>, response: retrofit2.Response<AceitarPedidoResponse>) {
                                if (response.isSuccessful && response.body()?.sucesso == true) {
                                    Toast.makeText(this@HomePrestadorActivity, "Serviço Confirmado!", Toast.LENGTH_LONG).show()
                                    binding.containerAlerta.root.visibility = View.GONE
                                    view.x = 5f
                                } else {
                                    Toast.makeText(this@HomePrestadorActivity, "Outra oficina aceitou primeiro!", Toast.LENGTH_LONG).show()
                                    binding.containerAlerta.root.visibility = View.GONE
                                    view.x = 5f
                                }
                            }

                            override fun onFailure(call: retrofit2.Call<AceitarPedidoResponse>, t: Throwable) {
                                Toast.makeText(this@HomePrestadorActivity, "Erro de conexão.", Toast.LENGTH_SHORT).show()
                                view.animate().x(5f).setDuration(200).start()
                            }
                        })

                    } else {
                        // O mecânico soltou o dedo no meio do caminho, devolve o botão pro início
                        view.animate().x(5f).setDuration(200).start()
                    }
                    true
                } else -> false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.switchStatus.isChecked = state.isOnline

                if (state.isOnline) {
                    binding.tvStatusText.text = "ONLINE"
                    binding.tvStatusText.setTextColor(getColor(R.color.salvo_azul_neon))
                    binding.tvRadarStatus.text = "● Procurando"
                    binding.tvRadarStatus.setTextColor(Color.parseColor("#4CAF50"))

                    if (socketManager == null && currentUserId != -1) {
                        Log.d("SALVO_WEBSOCKET", "Iniciando WebSocketManager para o ID: $currentUserId")
                        socketManager = WebSocketManager(currentUserId) { jsonChamado ->
                            Log.w("SALVO_WEBSOCKET", "Abrindo CARD LARANJA na tela!")

                            pedidoAtualId = jsonChamado.getInt("requestId")
                            pedidoAtualPreco = jsonChamado.getDouble("rawPreco")
                            pedidoAtualDistancia = jsonChamado.getDouble("rawDistancia")

                            // DISPARO CRÍTICO: Torna o include visível e popula dados
                            binding.containerAlerta.root.visibility = View.VISIBLE
                            binding.containerAlerta.tvAlertaVeiculo.text = jsonChamado.getString("veiculo")
                            binding.containerAlerta.tvAlertaDefeito.text = jsonChamado.getString("defeito")
                            binding.containerAlerta.tvAlertaPreco.text = jsonChamado.getString("preco")
                            binding.containerAlerta.tvAlertaClienteNome.text = jsonChamado.getString("clienteNome")
                            binding.containerAlerta.tvAlertaClienteNota.text = jsonChamado.getString("clienteNota")
                            binding.containerAlerta.tvAlertaDistancia.text = jsonChamado.getString("distanciaText")

                        }
                        socketManager?.conectar()
                    }

                } else {
                    binding.tvStatusText.text = "OFFLINE"
                    binding.tvStatusText.setTextColor(getColor(R.color.salvo_texto_secundario))
                    binding.tvRadarStatus.text = "○ Desconectado"
                    binding.tvRadarStatus.setTextColor(Color.parseColor("#9E9E9E"))

                    socketManager?.desconectar()
                    socketManager = null
                }

                state.errorMessage?.let { msg ->
                    Toast.makeText(this@HomePrestadorActivity, msg, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.selectedItemId = R.id.nav_radar
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_radar -> true
                R.id.nav_servicos -> {
                    startActivity(Intent(this, CardapioServicosActivity::class.java).putExtra("USER_ID", currentUserId))
                    false
                }
                R.id.nav_frota -> {
                    startActivity(Intent(this, GestaoFrotaActivity::class.java).putExtra("USER_ID", currentUserId))
                    false
                }
                R.id.nav_chat -> {
                    Toast.makeText(this, "Chat em desenvolvimento!", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_perfil -> {
                    startActivity(Intent(this, PerfilOficinaActivity::class.java).putExtra("USER_ID", currentUserId))
                    false
                }
                else -> false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        socketManager?.desconectar()
    }
}