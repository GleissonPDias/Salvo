package com.example.salvo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.salvo.utils.SessionManager

// 🚀 IMPORTS CORRETOS DOS MODELOS
import com.example.salvo.model.PedidoSocorroRequest
import com.example.salvo.model.PedidoSocorroResponse
import com.example.salvo.model.PollingStatusResponse
import com.example.salvo.model.Vehicle

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

// 🚀 IMPORTS CORRETOS DO RETROFIT
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class SocorroActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvStatus: TextView

    private var meuUserId = -1
    private var meuNome = "Cliente"

    private var veiculosDoCliente: List<Vehicle> = emptyList()
    private var idVeiculoSelecionado: Int? = null

    private var jobPolling: Job? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            buscarLocalizacaoInicial()
        } else {
            val tvEndereco = findViewById<TextView>(R.id.tv_endereco_detectado)
            tvEndereco?.text = "GPS desativado/sem permissão."
            Toast.makeText(this, "Precisamos do GPS para achar a oficina mais próxima!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socorro)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvStatus = findViewById(R.id.tv_status_radar)

        findViewById<MaterialToolbar>(R.id.toolbar_socorro).setNavigationOnClickListener { finish() }

        val sessionManager = SessionManager(this)
        meuUserId = sessionManager.buscarUserId()
        meuNome = sessionManager.buscarUserNome()

        if (meuUserId == -1) {
            meuUserId = intent.getIntExtra("USER_ID", 1)
            meuNome = intent.getStringExtra("NOME_USUARIO") ?: "Cliente"
        }

        carregarVeiculosDoCliente()
        buscarLocalizacaoInicial()

        val btnConfirmar = findViewById<MaterialButton>(R.id.btn_confirmar_socorro)
        btnConfirmar.setOnClickListener {
            if (idVeiculoSelecionado == null) {
                Toast.makeText(this, "Por favor, selecione qual veículo está com problema.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            tvStatus.text = "Verificando permissões do GPS..."
            verificarPermissaoGPS()
        }
    }

    private fun buscarLocalizacaoInicial() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    exibirEnderecoNaTela(location.latitude, location.longitude)
                }
            }
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun exibirEnderecoNaTela(lat: Double, lon: Double) {
        val tvEndereco = findViewById<TextView>(R.id.tv_endereco_detectado)

        try {
            val geocoder = Geocoder(this, Locale("pt", "BR"))
            val enderecos = geocoder.getFromLocation(lat, lon, 1)

            if (!enderecos.isNullOrEmpty()) {
                val endereco = enderecos[0]
                val rua = endereco.thoroughfare ?: endereco.subLocality ?: "Rua desconhecida"
                val numero = endereco.subThoroughfare ?: ""

                tvEndereco?.text = if (numero.isNotEmpty()) "$rua, $numero" else rua
            } else {
                tvEndereco?.text = "Região desconhecida"
            }
        } catch (e: Exception) {
            tvEndereco?.text = "GPS (Lat: ${"%.4f".format(lat)}, Lon: ${"%.4f".format(lon)})"
        }
    }

    private fun carregarVeiculosDoCliente() {
        if (meuUserId == -1) return

        RetrofitClient.apiService.obterVeiculosCliente(meuUserId).enqueue(object : Callback<List<Vehicle>> {
            override fun onResponse(call: Call<List<Vehicle>>, response: Response<List<Vehicle>>) {
                if (response.isSuccessful) {
                    veiculosDoCliente = response.body() ?: emptyList()

                    if (veiculosDoCliente.isEmpty()) {
                        Toast.makeText(this@SocorroActivity, "Você precisa cadastrar um veículo no Perfil primeiro.", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }

                    val nomesVeiculos = veiculosDoCliente.map { "${it.name} - ${it.plate}" }

                    val dropdown = findViewById<AutoCompleteTextView>(R.id.actv_veiculo_socorro)
                    val adapter = ArrayAdapter(
                        this@SocorroActivity,
                        android.R.layout.simple_dropdown_item_1line,
                        nomesVeiculos
                    )

                    dropdown.setAdapter(adapter)

                    dropdown.setOnItemClickListener { _, _, position, _ ->
                        idVeiculoSelecionado = veiculosDoCliente[position].id
                    }
                }
            }
            override fun onFailure(call: Call<List<Vehicle>>, t: Throwable) {
                Toast.makeText(this@SocorroActivity, "Falha ao carregar lista de veículos.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun verificarPermissaoGPS() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            pegarLocalizacaoEChamarAPI()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun pegarLocalizacaoEChamarAPI() {
        tvStatus.text = "Buscando satélite em tempo real..."

        val cancellationTokenSource = CancellationTokenSource()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { localizacaoRecebida ->
                if (localizacaoRecebida != null) {
                    tvStatus.text = "GPS OK! Varrendo o mapa num raio de 15km..."
                    exibirEnderecoNaTela(localizacaoRecebida.latitude, localizacaoRecebida.longitude)

                    val tipoServico = "Guincho"

                    val pedido = PedidoSocorroRequest(
                        customerId = meuUserId,
                        clienteNome = meuNome,
                        latitude = localizacaoRecebida.latitude,
                        longitude = localizacaoRecebida.longitude,
                        serviceType = tipoServico,
                        vehicleId = idVeiculoSelecionado!!,
                        description = "Preciso de ajuda urgente!"
                    )

                    RetrofitClient.apiService.solicitarSocorro(pedido)
                        .enqueue(object : Callback<PedidoSocorroResponse> {
                            override fun onResponse(call: Call<PedidoSocorroResponse>, response: Response<PedidoSocorroResponse>) {
                                if (response.isSuccessful && response.body()?.sucesso == true) {
                                    val notificados = response.body()?.mecanicosNotificados ?: 0
                                    val requestIdGerado = response.body()?.requestId

                                    if (notificados > 0 && requestIdGerado != null) {
                                        tvStatus.text = "SUCESSO! Notificamos $notificados oficinas. Aguardando aceite..."
                                        tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                                        iniciarPollingDeBusca(requestIdGerado)
                                    } else {
                                        tvStatus.text = "Ops! Nenhuma oficina encontrada na sua região."
                                        tvStatus.setTextColor(android.graphics.Color.parseColor("#F39C12"))
                                    }
                                } else {
                                    tvStatus.text = "Erro da API: ${response.errorBody()?.string()}"
                                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                                }
                            }

                            override fun onFailure(call: Call<PedidoSocorroResponse>, t: Throwable) {
                                tvStatus.text = "Erro de conexão: Verifique sua internet."
                                tvStatus.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                            }
                        })
                } else {
                    tvStatus.text = "Não foi possível encontrar o satélite. Tente ir para um local aberto."
                }
            }
    }

    private fun iniciarPollingDeBusca(requestId: Int) {
        jobPolling?.cancel()

        jobPolling = lifecycleScope.launch {
            while (isActive) {
                // Aqui a chamada está 100% amarrada aos imports corretos do topo
                RetrofitClient.apiService.checarStatusPedido(requestId).enqueue(object : Callback<PollingStatusResponse> {
                    override fun onResponse(call: Call<PollingStatusResponse>, response: Response<PollingStatusResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            val statusServidor = body?.status?.lowercase()

                            tvStatus.text = "Pedido ID: $requestId\nStatus no Servidor: ${body?.status}"

                            when (statusServidor) {
                                "accepted", "aceito" -> {
                                    jobPolling?.cancel()
                                    Toast.makeText(this@SocorroActivity, "Oficina a caminho!", Toast.LENGTH_SHORT).show()

                                    // 🚀 Mágica para abrir a tela do mapa!
                                    val intent = Intent(this@SocorroActivity, AcompanhamentoPedidoActivity::class.java)
                                    intent.putExtra("PEDIDO_ID", requestId)
                                    startActivity(intent)
                                    finish()
                                }
                                "canceled", "cancelado" -> {
                                    jobPolling?.cancel()
                                    tvStatus.text = "Busca cancelada pelo servidor."
                                    tvStatus.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                                }
                                "searching", "pendente" -> {
                                    tvStatus.append("\nAGUARDANDO ACEITE...")
                                }
                            }
                        } else {
                            tvStatus.text = "Erro no Servidor: Código ${response.code()}"
                            tvStatus.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                        }
                    }

                    override fun onFailure(call: Call<PollingStatusResponse>, t: Throwable) {
                        tvStatus.text = "Falha de Conversão/Rede:\n${t.localizedMessage}"
                        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF3B30"))
                    }
                })
                delay(5000)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        jobPolling?.cancel()
    }
}