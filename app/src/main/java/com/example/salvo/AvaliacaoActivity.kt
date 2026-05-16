package com.example.salvo

import android.os.Bundle
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textfield.TextInputEditText

class AvaliacaoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_avaliacao)

        // 1. Ajuste de tela Edge-to-Edge
        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        // 2. Vinculando os componentes da tela
        val btnFechar = findViewById<ImageView>(R.id.iv_fechar_avaliacao)
        val ratingBar = findViewById<RatingBar>(R.id.rating_bar_servico)
        val chipGroupElogios = findViewById<ChipGroup>(R.id.chip_group_elogios)
        val etComentario = findViewById<TextInputEditText>(R.id.et_comentario)
        val btnEnviar = findViewById<MaterialButton>(R.id.btn_enviar_avaliacao)

        // 3. Ação para fechar a tela se clicar na setinha
        btnFechar.setOnClickListener {
            finish()
        }

        // 4. Lógica do Botão de Enviar Avaliação
        btnEnviar.setOnClickListener {
            val nota = ratingBar.rating
            val comentario = etComentario.text.toString().trim()

            // Pega todos os elogios (Chips) que o usuário clicou
            val elogiosSelecionados = mutableListOf<String>()
            val idsSelecionados = chipGroupElogios.checkedChipIds

            for (id in idsSelecionados) {
                val chip = findViewById<com.google.android.material.chip.Chip>(id)
                if (chip != null) {
                    elogiosSelecionados.add(chip.text.toString())
                }
            }

            // Validação simples: exigir pelo menos 1 estrela
            if (nota == 0f) {
                Toast.makeText(this, "Por favor, dê uma nota de 1 a 5 estrelas.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Aqui no futuro nós vamos enviar esses dados para a nossa API (Ktor)!
            // Por enquanto, mostramos um resumo e fechamos a tela.
            val resumo = "Nota: $nota\nElogios: $elogiosSelecionados"
            Toast.makeText(this, "Avaliação enviada com sucesso!", Toast.LENGTH_SHORT).show()
            println(resumo) // Imprime no Logcat para você conferir

            finish() // Fecha a tela e volta para a Home
        }
    }
}