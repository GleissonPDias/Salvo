package com.example.salvo

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton

class SobreActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sobre)

        // 1. Ajuste de tela para não ficar por baixo da barra do celular
        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. Vinculando os componentes do XML com o Kotlin
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_sobre)
        val btnTermos = findViewById<MaterialButton>(R.id.btn_termos)
        val btnPrivacidade = findViewById<MaterialButton>(R.id.btn_privacidade)
        val btnDesenvolvedores = findViewById<MaterialButton>(R.id.btn_desenvolvedores)

        // 3. Ação da setinha de voltar na barra superior
        toolbar.setNavigationOnClickListener {
            finish() // Simplesmente fecha essa tela e volta para a anterior
        }

        // 4. Ações dos botões da tela
        btnTermos.setOnClickListener {
            // Aqui depois você pode colocar uma Intent para abrir um link no navegador
            Toast.makeText(this, "Abrindo Termos de Uso...", Toast.LENGTH_SHORT).show()
        }

        btnPrivacidade.setOnClickListener {
            Toast.makeText(this, "Abrindo Política de Privacidade...", Toast.LENGTH_SHORT).show()
        }

        btnDesenvolvedores.setOnClickListener {
            Toast.makeText(this, "Mostrando Equipe...", Toast.LENGTH_SHORT).show()
        }
    }
}