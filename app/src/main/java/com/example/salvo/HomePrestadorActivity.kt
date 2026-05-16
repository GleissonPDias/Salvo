package com.example.salvo

import android.content.Intent
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
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

class HomePrestadorActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityHomePrestadorBinding
    private lateinit var viewModel: HomePrestadorViewModel

    private var mMap: GoogleMap? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentUserId: Int = -1
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

        // NOVO: Pegando o ID real enviado pela LoginActivity
        currentUserId = intent.getIntExtra("USER_ID", -1)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map_radar) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupViewModel()
        setupListeners()
        observeViewModel()
        setupRecyclerView()

        // NOVO: Inicializa a navegação
        setupBottomNavigation()
    }
    // Configura a lista de atividades recentes com os dados idênticos ao Figma
    private fun setupRecyclerView() {
        val dadosFigma = listOf(
            RecentActivity(1, "Carlos Silva", "Towing Service", "2.5km", "R$ 120", "14:30"),
            RecentActivity(2, "Mariana Costa", "Battery Jump", "4.1km", "R$ 80", "11:15")
        )

        // Configura o gerenciador de layout e acopla o adapter no RecyclerView do XML
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

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                binding.switchStatus.isChecked = state.isOnline

                if (state.isOnline) {
                    binding.tvStatusText.text = "ONLINE"
                    binding.tvStatusText.setTextColor(getColor(R.color.salvo_azul_neon))
                    binding.tvRadarStatus.text = "● Procurando"
                    binding.tvRadarStatus.setTextColor(Color.parseColor("#4CAF50"))
                } else {
                    binding.tvStatusText.text = "OFFLINE"
                    binding.tvStatusText.setTextColor(getColor(R.color.salvo_texto_secundario))
                    binding.tvRadarStatus.text = "○ Desconectado"
                    binding.tvRadarStatus.setTextColor(Color.parseColor("#9E9E9E"))
                }

                state.errorMessage?.let { msg ->
                    Toast.makeText(this@HomePrestadorActivity, msg, Toast.LENGTH_LONG).show()
                    viewModel.clearError()
                }
            }
        }
    }
    private fun setupBottomNavigation() {
        // Garante que o ícone do Radar esteja marcado visualmente ao abrir a tela
        binding.bottomNavigation.selectedItemId = R.id.nav_radar

        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_radar -> {
                    // Já estamos no Radar, não fazemos nada e retornamos true para manter selecionado
                    true
                }
                R.id.nav_servicos -> {
                    val intent = Intent(this, CardapioServicosActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    // Retorna false para que o botão 'Radar' continue aceso na Home por baixo
                    false
                }
                R.id.nav_frota -> {
                    val intent = Intent(this, GestaoFrotaActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    false
                }
                R.id.nav_chat -> {
                    // Como a tela de chat ainda não foi criada, mostramos um aviso
                    Toast.makeText(this, "Chat em desenvolvimento!", Toast.LENGTH_SHORT).show()
                    false
                }
                R.id.nav_perfil -> {
                    val intent = Intent(this, PerfilOficinaActivity::class.java)
                    intent.putExtra("USER_ID", currentUserId)
                    startActivity(intent)
                    false
                }
                else -> false
            }
        }
    }
}