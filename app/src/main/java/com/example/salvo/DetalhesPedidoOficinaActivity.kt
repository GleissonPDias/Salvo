package com.example.salvo

import android.graphics.Color
import android.location.Geocoder
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class DetalhesPedidoOficinaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalhes_pedido_oficina)

        findViewById<ImageView>(R.id.btn_voltar_detalhes).setOnClickListener { finish() }

        // 1. Recebe os dados da tela anterior
        val clienteNome = intent.getStringExtra("NOME_CLIENTE")
        val veiculo = intent.getStringExtra("VEICULO_CLIENTE")
        val descricao = intent.getStringExtra("DESC")
        val destino = intent.getStringExtra("DESTINO")
        val data = intent.getStringExtra("DATA")
        val status = intent.getStringExtra("STATUS")

        // Recebe a latitude e longitude
        val lat = intent.getDoubleExtra("ORIGEM_LAT", 0.0)
        val lng = intent.getDoubleExtra("ORIGEM_LNG", 0.0)

        // 2. Preenche os dados básicos na tela
        findViewById<TextView>(R.id.tv_detalhe_cliente).text = clienteNome
        findViewById<TextView>(R.id.tv_detalhe_veiculo).text = veiculo
        findViewById<TextView>(R.id.tv_detalhe_desc).text = descricao ?: "Sem descrição"
        findViewById<TextView>(R.id.tv_detalhe_destino).text = destino
        findViewById<TextView>(R.id.tv_detalhe_data).text = data

        // 3. Lógica de preenchimento e cores do Status
        val tvStatus = findViewById<TextView>(R.id.tv_detalhe_status)
        tvStatus.text = status

        when (status?.lowercase()) {
            "confirmado", "concluído" -> tvStatus.setTextColor(Color.parseColor("#10B981"))
            "a caminho" -> tvStatus.setTextColor(Color.parseColor("#F59E0B"))
            "no local" -> tvStatus.setTextColor(Color.parseColor("#8B5CF6"))
            "em andamento" -> tvStatus.setTextColor(Color.parseColor("#3B82F6"))
            "cancelado" -> tvStatus.setTextColor(Color.parseColor("#EF4444"))
            else -> tvStatus.setTextColor(Color.parseColor("#FFFFFF")) // Branco por causa do Dark Theme
        }

        // 4. Mágica do Geocoder: Converter Lat/Lng em Endereço
        val tvOrigem = findViewById<TextView>(R.id.tv_detalhe_origem)

        if (lat != 0.0 && lng != 0.0) {
            tvOrigem.text = "Buscando endereço..." // Mensagem de carregamento

            // Inicia uma thread em segundo plano para não travar a tela
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val geocoder = Geocoder(this@DetalhesPedidoOficinaActivity, Locale.getDefault())
                    val addresses = geocoder.getFromLocation(lat, lng, 1) // Busca 1 resultado

                    if (!addresses.isNullOrEmpty()) {
                        // getAddressLine(0) devolve o endereço completo formatado
                        val enderecoFormatado = addresses[0].getAddressLine(0)

                        // Volta para a thread principal para atualizar a tela
                        withContext(Dispatchers.Main) {
                            tvOrigem.text = enderecoFormatado
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            tvOrigem.text = "Endereço não encontrado (Lat: $lat, Lng: $lng)"
                        }
                    }
                } catch (e: Exception) {
                    // Cai aqui se o celular estiver sem internet no momento
                    withContext(Dispatchers.Main) {
                        tvOrigem.text = "Lat: $lat, Lng: $lng (Sem conexão para buscar rua)"
                    }
                }
            }
        } else {
            tvOrigem.text = "Localização de origem não informada"
        }
    }
}