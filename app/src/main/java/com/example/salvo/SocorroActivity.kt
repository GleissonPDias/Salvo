package com.example.salvo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.example.salvo.model.PedidoSocorroRequest
import com.example.salvo.model.PedidoSocorroResponse
import com.example.salvo.model.PollingStatusResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SocorroActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvStatus: TextView
    private var meuUserId = -1 // Variável global para usar no intent

    // VARIÁVEL DO RELÓGIO (POLLING)
    private var jobPolling: Job? = null

    // O "Avaliador de Permissão" moderno do Android
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

        if (granted) {
            pegarLocalizacaoEChamarAPI()
        } else {
            Toast.makeText(this, "Precisamos do GPS para achar a oficina mais próxima!", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socorro)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        tvStatus = findViewById(R.id.tv_status_radar)

        // Pega o ID do usuário que veio da tela anterior
        meuUserId = intent.getIntExtra("USER_ID", 1)

        val btnConfirmar = findViewById<MaterialButton>(R.id.btn_confirmar_socorro)

        btnConfirmar.setOnClickListener {
            tvStatus.text = "Verificando permissões do GPS..."
            verificarPermissaoGPS()
        }
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

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de GPS negada.", Toast.LENGTH_SHORT).show()
            return
        }

        val cancellationTokenSource = CancellationTokenSource()

        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cancellationTokenSource.token)
            .addOnSuccessListener { localizacaoRecebida ->
                if (localizacaoRecebida != null) {
                    tvStatus.text = "GPS OK! Varrendo o mapa num raio de 15km..."

                    val meuVehicleId = 1
                    val tipoServico = "Guincho"

                    if (meuVehicleId == -1) {
                        Toast.makeText(this, "Erro: Veículo não identificado.", Toast.LENGTH_LONG).show()
                        return@addOnSuccessListener
                    }

                    // Monta o pacote para enviar ao Backend
                    val pedido = PedidoSocorroRequest(
                        customerId = meuUserId,
                        latitude = localizacaoRecebida.latitude,
                        longitude = localizacaoRecebida.longitude,
                        serviceType = tipoServico,
                        vehicleId = meuVehicleId,
                        description = "Preciso de ajuda urgente!"
                    )

                    // Chama a API para criar o pedido e disparar o WebSocket para as oficinas
                    RetrofitClient.apiService.solicitarSocorro(pedido)
                        .enqueue(object : Callback<PedidoSocorroResponse> {
                            override fun onResponse(call: Call<PedidoSocorroResponse>, response: Response<PedidoSocorroResponse>) {
                                if (response.isSuccessful && response.body()?.sucesso == true) {
                                    val notificados = response.body()?.mecanicosNotificados ?: 0
                                    val requestIdGerado = response.body()?.requestId

                                    if (notificados > 0 && requestIdGerado != null) {
                                        tvStatus.text = "SUCESSO! Notificamos $notificados oficinas. Aguardando aceite..."
                                        tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))

                                        // 🚀 INICIA O RELÓGIO PARA ESCUTAR A RESPOSTA DA OFICINA
                                        iniciarPollingDeBusca(requestIdGerado)

                                    } else {
                                        tvStatus.text = "Ops! Nenhuma oficina encontrada na sua região."
                                        tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800"))
                                    }
                                } else {
                                    val erroCru = response.errorBody()?.string() ?: "Erro desconhecido"
                                    tvStatus.text = "Erro da API: $erroCru"
                                    tvStatus.setTextColor(android.graphics.Color.RED)
                                }
                            }

                            override fun onFailure(call: Call<PedidoSocorroResponse>, t: Throwable) {
                                tvStatus.text = "Erro de conexão: Verifique sua internet."
                                tvStatus.setTextColor(android.graphics.Color.RED)
                            }
                        })
                } else {
                    tvStatus.text = "Não foi possível encontrar o satélite. Tente ir para um local aberto."
                }
            }
    }

    // =========================================================================
    // 🔄 A MÁGICA: O RELOGINHO QUE ESCUTA O ACEITE DA OFICINA
    // =========================================================================
    private fun iniciarPollingDeBusca(requestId: Int) {
        jobPolling?.cancel()

        jobPolling = lifecycleScope.launch {
            while (isActive) {

                RetrofitClient.apiService.checarStatusPedido(requestId).enqueue(object : Callback<PollingStatusResponse> {
                    override fun onResponse(call: Call<PollingStatusResponse>, response: Response<PollingStatusResponse>) {
                        if (response.isSuccessful) {
                            val body = response.body()
                            val statusServidor = body?.status?.lowercase()

                            // 📺 EXIBE DIRETO NA TELA DO SEU CELULAR O QUE O SERVIDOR RESPONDEU:
                            tvStatus.text = "Pedido ID: $requestId\nStatus no Servidor: ${body?.status}"

                            when (statusServidor) {
                                "accepted" -> {
                                    jobPolling?.cancel()
                                    Toast.makeText(this@SocorroActivity, "Oficina a caminho!", Toast.LENGTH_SHORT).show()

                                    // Abre a tela de listagem
                                    val intent = Intent(this@SocorroActivity, MeusPedidosActivity::class.java)
                                    intent.putExtra("USER_ID", meuUserId)
                                    startActivity(intent)
                                    finish()
                                }
                                "canceled" -> {
                                    jobPolling?.cancel()
                                    tvStatus.text = "Busca cancelada pelo servidor."
                                    tvStatus.setTextColor(android.graphics.Color.RED)
                                }
                                "searching" -> {
                                    // Adiciona pontinhos dinâmicos abaixo do status para mostrar que continua buscando
                                    tvStatus.append("\nAGUARDANDO ACEITE...")
                                }
                            }
                        } else {
                            // Se der erro de rota (ex: 404 ou 500) vai aparecer na tela
                            tvStatus.text = "Erro no Servidor: Código ${response.code()}"
                            tvStatus.setTextColor(android.graphics.Color.RED)
                        }
                    }

                    override fun onFailure(call: Call<PollingStatusResponse>, t: Throwable) {
                        // Se o GSON quebrar por falta de algum campo, você verá o erro na tela!
                        tvStatus.text = "Falha de Conversão/Rede:\n${t.localizedMessage}"
                        tvStatus.setTextColor(android.graphics.Color.RED)
                    }
                })

                delay(5000) // Pergunta de 5 em 5 segundos
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Se o usuário apertar a seta de voltar do celular, nós matamos a Coroutine para não gastar bateria!
        jobPolling?.cancel()
    }
}