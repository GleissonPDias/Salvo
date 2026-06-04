package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

// 🚀 AQUI ESTÃO OS IMPORTS CORRETOS DA PASTA MODEL
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.PollingStatusResponse

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

// 🚀 IMPORTS CORRETOS DO RETROFIT
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AcompanhamentoPedidoActivity : AppCompatActivity(), OnMapReadyCallback {

    private var pedidoId: Int = -1
    private var mGoogleMap: GoogleMap? = null
    private var marcadorGuincho: Marker? = null

    private lateinit var tvStatusTitulo: TextView
    private lateinit var tvTempoChegada: TextView
    private lateinit var layoutPrestador: View
    private lateinit var tvNomeMotorista: TextView
    private lateinit var tvVeiculoPrestador: TextView
    private lateinit var btnConcluir: MaterialButton

    private val handler = Handler(Looper.getMainLooper())
    private val checarStatusRunnable = object : Runnable {
        override fun run() {
            buscarStatusNaApi()
            handler.postDelayed(this, 5000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acompanhamento_pedido)

        pedidoId = intent.getIntExtra("PEDIDO_ID", -1)
        if (pedidoId == -1) {
            Toast.makeText(this, "Pedido inválido.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapa_acompanhamento) as SupportMapFragment
        mapFragment.getMapAsync(this)

        tvStatusTitulo = findViewById(R.id.tv_status_titulo)
        tvTempoChegada = findViewById(R.id.tv_tempo_chegada)
        layoutPrestador = findViewById(R.id.layout_detalhes_prestador)
        tvNomeMotorista = findViewById(R.id.tv_nome_motorista)
        tvVeiculoPrestador = findViewById(R.id.tv_veiculo_prestador)
        btnConcluir = findViewById(R.id.btn_concluir_servico)

        findViewById<MaterialToolbar>(R.id.toolbar_acompanhamento).setNavigationOnClickListener {
            finish()
        }

        btnConcluir.setOnClickListener {
            btnConcluir.isEnabled = false
            btnConcluir.text = "FINALIZANDO..."

            val dadosStatus = mapOf("status" to "concluido")

            RetrofitClient.apiService.atualizarStatusPedido(pedidoId, dadosStatus)
                .enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@AcompanhamentoPedidoActivity, "Serviço encerrado com sucesso!", Toast.LENGTH_LONG).show()
                            val intent = Intent(this@AcompanhamentoPedidoActivity, AvaliacaoActivity::class.java)
                            intent.putExtra("PEDIDO_ID", pedidoId)
                            startActivity(intent)
                            finish()
                        } else {
                            btnConcluir.isEnabled = true
                            btnConcluir.text = "CONCLUIR SERVIÇO"
                            Toast.makeText(this@AcompanhamentoPedidoActivity, "Erro ao encerrar chamado.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        btnConcluir.isEnabled = true
                        btnConcluir.text = "CONCLUIR SERVIÇO"
                        Toast.makeText(this@AcompanhamentoPedidoActivity, "Falha de rede.", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mGoogleMap = googleMap
        mGoogleMap?.uiSettings?.isZoomControlsEnabled = true
        handler.post(checarStatusRunnable)
    }

    private fun buscarStatusNaApi() {
        RetrofitClient.apiService.checarStatusPedido(pedidoId).enqueue(object : Callback<PollingStatusResponse> {
            override fun onResponse(call: Call<PollingStatusResponse>, response: Response<PollingStatusResponse>) {
                if (response.isSuccessful && response.body() != null) {
                    val dados = response.body()!!
                    atualizarFluxoEPerfil(dados)
                }
            }
            override fun onFailure(call: Call<PollingStatusResponse>, t: Throwable) {}
        })
    }

    private fun atualizarFluxoEPerfil(dados: PollingStatusResponse) {
        val status = dados.status ?: "Pendente"

        when (status.lowercase()) {
            "pendente" -> {
                tvStatusTitulo.text = "Buscando guincho próximo..."
                tvTempoChegada.visibility = View.GONE
                layoutPrestador.visibility = View.GONE
                btnConcluir.visibility = View.GONE
            }
            "aceito", "em_andamento", "em andamento" -> {
                tvStatusTitulo.text = "Guincho a caminho"
                tvTempoChegada.text = dados.eta ?: "15 min"
                tvTempoChegada.visibility = View.VISIBLE
                layoutPrestador.visibility = View.VISIBLE
                btnConcluir.visibility = View.VISIBLE

                tvNomeMotorista.text = dados.providerName ?: "Prestador Parceiro"
                tvVeiculoPrestador.text = "${dados.vehicleName ?: "Guincho"} • Placa ${dados.vehiclePlate ?: "---"}"

                val lat = dados.latitude ?: 0.0
                val lng = dados.longitude ?: 0.0

                if (lat != 0.0 && lng != 0.0) {
                    val posicaoGuincho = LatLng(lat, lng)
                    if (marcadorGuincho == null) {
                        marcadorGuincho = mGoogleMap?.addMarker(
                            MarkerOptions()
                                .position(posicaoGuincho)
                                .title("Seu Guincho")
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
                        )
                        mGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(posicaoGuincho, 15f))
                    } else {
                        marcadorGuincho?.position = posicaoGuincho
                    }
                }
            }
            "concluido", "concluído" -> {
                handler.removeCallbacks(checarStatusRunnable)
                Toast.makeText(this, "Serviço concluído!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, AvaliacaoActivity::class.java).putExtra("PEDIDO_ID", pedidoId))
                finish()
            }
            "cancelado" -> {
                handler.removeCallbacks(checarStatusRunnable)
                Toast.makeText(this, "O pedido foi cancelado.", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(checarStatusRunnable)
    }
}