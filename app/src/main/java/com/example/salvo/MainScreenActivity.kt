package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class MainScreenActivity : AppCompatActivity() {

    private lateinit var tvBoasVindas: TextView
    private lateinit var cardPedidos: MaterialCardView
    private lateinit var cardSobre: MaterialCardView
    private lateinit var cardAvaliacao: MaterialCardView
    private lateinit var btnLogoff: MaterialButton

    // 1. Nova variável para o Botão/Card de pedir socorro
    private lateinit var btnPedirSocorro: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main_screen)

        // Ajuste das barras de sistema (Edge-to-Edge)
        val mainLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        iniciarComponentes()

        // 2. Recebendo os dados que vieram do LoginActivity
        val nomeUsuario = intent.getStringExtra("NOME_USUARIO") ?: "Usuário"
        val idUsuarioLogado = intent.getIntExtra("USER_ID", -1) // Recupera o ID do Banco

        tvBoasVindas.text = "Boas-vindas, $nomeUsuario!"

        // ==========================================
        // NAVEGAÇÃO PRINCIPAL (IR PARA O RADAR)
        // ==========================================
        btnPedirSocorro.setOnClickListener {
            val intent = Intent(this, SocorroActivity::class.java)
            // 3. Coloca o ID do usuário na mochila para a tela de Socorro usar na API
            intent.putExtra("USER_ID", idUsuarioLogado)
            startActivity(intent)
        }

        // ==========================================
        // OUTRAS NAVEGAÇÕES
        // ==========================================
        cardPedidos.setOnClickListener {
            startActivity(Intent(this, MeusPedidosActivity::class.java))
        }

        cardSobre.setOnClickListener {
            startActivity(Intent(this, SobreActivity::class.java))
        }

        cardAvaliacao.setOnClickListener {
            startActivity(Intent(this, AvaliacaoActivity::class.java))
        }

        // LOGOFF
        btnLogoff.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Limpa o histórico de telas para o usuário não voltar com o botão do celular
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun iniciarComponentes() {
        tvBoasVindas = findViewById(R.id.tv_boas_vindas)
        cardPedidos = findViewById(R.id.card_pedidos)
        cardSobre = findViewById(R.id.card_sobre)
        cardAvaliacao = findViewById(R.id.card_avaliacao)
        btnLogoff = findViewById(R.id.btn_logoff)

        // 4. Lembre-se de criar um botão no seu activity_main_screen.xml com este ID exato!
        btnPedirSocorro = findViewById(R.id.btn_pedir_socorro)
    }
}