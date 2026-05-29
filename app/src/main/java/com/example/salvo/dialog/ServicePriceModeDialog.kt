package com.example.salvo.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioButton
import com.example.salvo.R
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Dialog que permite escolher entre dois tipos de preço:
 * 1. Preço Fixo - valor único para cada serviço
 * 2. Preço por Km/Hora - valor varia conforme distância/tempo
 */
class ServicePriceModeDialog(
    context: Context,
    private val onModeSelected: (mode: String) -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_service_price_mode, null)
        setupViews(view)
        setContentView(view)
    }

    private fun setupViews(view: android.view.View) {
        val rbFixed = view.findViewById<RadioButton>(R.id.radio_fixed_price)
        val rbKm = view.findViewById<RadioButton>(R.id.radio_per_km)
        val btnConfirmar = view.findViewById<Button>(R.id.btn_confirmar_modo)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancelar_modo)

        // 1. Define o estado inicial padrão (Preço Fixo selecionado)
        rbFixed.isChecked = true
        rbKm.isChecked = false

        // 2. Lógica manual para alternar a seleção (Contorna o bug do RadioGroup aninhado)
        rbFixed.setOnClickListener {
            rbFixed.isChecked = true
            rbKm.isChecked = false
        }

        rbKm.setOnClickListener {
            rbKm.isChecked = true
            rbFixed.isChecked = false
        }

        // 3. Captura o modo selecionado ao confirmar
        btnConfirmar.setOnClickListener {
            val modo = if (rbKm.isChecked) "per_km" else "fixed"
            onModeSelected(modo)
            dismiss()
        }

        btnCancelar.setOnClickListener {
            dismiss()
        }
    }
}