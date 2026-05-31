package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton

class PerfilClienteActivity : AppCompatActivity() {

    private var userId: Int = -1
    private var nomeUsuario: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil_cliente)

        // 1. Recebendo os dados da navegação
        nomeUsuario = intent.getStringExtra("NOME_USUARIO") ?: "Cliente"
        userId = intent.getIntExtra("USER_ID", -1)

        // 2. Atualizando a interface
        val tvNomePerfil = findViewById<TextView>(R.id.tv_nome_perfil)
        tvNomePerfil.text = nomeUsuario

        // 3. Configurar Botões do Menu
        configurarBotoes()

        // 4. Configurar a Barra de Navegação
        configurarBottomNavigation()
    }

    private fun configurarBotoes() {
        val btnVeiculos = findViewById<LinearLayout>(R.id.btn_meus_veiculos)
        val btnHistorico = findViewById<LinearLayout>(R.id.btn_historico)
        val btnSair = findViewById<LinearLayout>(R.id.btn_sair)

        btnVeiculos.setOnClickListener {
            val intent = Intent(this, MeusVeiculosActivity::class.java)
            intent.putExtra("USER_ID", userId) // Passa o ID do cliente para a tela de frota
            startActivity(intent)
        }

        btnHistorico.setOnClickListener {
            val intent = Intent(this, MeusPedidosActivity::class.java)
            intent.putExtra("USER_ID", userId)
            intent.putExtra("NOME_USUARIO", nomeUsuario)
            startActivity(intent)
        }

        btnSair.setOnClickListener {
            val sessionManager = com.example.salvo.utils.SessionManager(this)
            sessionManager.limparSessao()

            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun configurarBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation_cliente)

        // IMPORTANTE: Marca o ícone de PERFIL como selecionado
        bottomNav.selectedItemId = R.id.nav_perfil

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Volta para a Home
                    val intent = Intent(this, MainScreenActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("NOME_USUARIO", nomeUsuario)
                    startActivity(intent)
                    finish() // Fecha a tela de perfil para não acumular na memória
                    true
                }
                R.id.nav_pedidos -> {
                    // Vai para a tela de Pedidos
                    val intent = Intent(this, MeusPedidosActivity::class.java)
                    intent.putExtra("USER_ID", userId)
                    intent.putExtra("NOME_USUARIO", nomeUsuario)
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_chat -> {
                    Toast.makeText(this, "Em breve: Chat!", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.nav_perfil -> {
                    // Já estamos aqui
                    true
                }
                else -> false
            }
        }
    }
}