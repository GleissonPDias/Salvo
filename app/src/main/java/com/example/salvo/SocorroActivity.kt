package com.example.salvo

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.salvo.model.PedidoSocorroRequest
import com.example.salvo.model.PedidoSocorroResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.material.button.MaterialButton
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SocorroActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var tvStatus: TextView

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
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    private fun pegarLocalizacaoEChamarAPI() {
        tvStatus.text = "Buscando satélite..."

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                tvStatus.text = "GPS OK! Varrendo o mapa num raio de 15km..."

                // 1. Pegar dados (No futuro, o serviço e veículo virão da tela)
                val meuUserId = intent.getIntExtra("USER_ID", 1)
                val tipoServico = "Guincho"

                val pedido = PedidoSocorroRequest(
                    customerId = meuUserId,
                    latitude = location.latitude,
                    longitude = location.longitude,
                    serviceType = tipoServico,
                    vehicleId = 1,
                    description = "Preciso de ajuda urgente!"
                )

                // 2. Chamar a API
                // ATENÇÃO: Substitua 'RetrofitClient.apiService' pelo nome correto do seu Retrofit!
                RetrofitClient.apiService.solicitarSocorro(pedido).enqueue(object : Callback<PedidoSocorroResponse> {

                    override fun onResponse(call: Call<PedidoSocorroResponse>, response: Response<PedidoSocorroResponse>) {
                        if (response.isSuccessful && response.body()?.sucesso == true) {
                            val notificados = response.body()?.mecanicosNotificados ?: 0

                            if (notificados > 0) {
                                tvStatus.text = "SUCESSO! Notificamos $notificados oficinas. Aguardando aceite..."
                                tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")) // Verde
                            } else {
                                tvStatus.text = "Ops! Nenhuma oficina de $tipoServico encontrada na sua região."
                                tvStatus.setTextColor(android.graphics.Color.parseColor("#FF9800")) // Laranja
                            }
                        } else {
                            val erroCru = response.errorBody()?.string() ?: "Erro desconhecido"
                            tvStatus.text = "Erro da API: $erroCru"
                            tvStatus.setTextColor(android.graphics.Color.RED)
                        }
                    }

                    override fun onFailure(call: Call<PedidoSocorroResponse>, t: Throwable) {
                        tvStatus.text = "Erro de conexão: Verifique sua internet."
                    }
                })

            } else {
                tvStatus.text = "Sinal fraco. Abra o Google Maps por 5 segundos e tente de novo."
            }
        }
    }
}