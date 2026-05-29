package com.example.salvo.dialog

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.RadioGroup
import android.widget.Toast
import com.example.salvo.R
import com.example.salvo.model.ServiceRequest
import com.google.android.material.bottomsheet.BottomSheetDialog

/**
 * Dialog para alterar o status do pedido de forma clara e visual
 */
class OrderStatusDialog(
    context: Context,
    private val pedido: ServiceRequest,
    private val onStatusChanged: (String) -> Unit
) : BottomSheetDialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val view = LayoutInflater.from(context).inflate(R.layout.dialog_change_order_status, null)
        setupViews(view)
        setContentView(view)
    }

    private fun setupViews(view: android.view.View) {
        val radioGroup = view.findViewById<RadioGroup>(R.id.rg_status_options)
        val btnConfirmar = view.findViewById<Button>(R.id.btn_confirmar_status)
        val btnCancelar = view.findViewById<Button>(R.id.btn_cancelar_status)

        // Selecionar status atual
        when (pedido.status) {
            "searching" -> radioGroup.check(R.id.rb_searching)
            "accepted" -> radioGroup.check(R.id.rb_accepted)
            "in_progress" -> radioGroup.check(R.id.rb_in_progress)
            "completed" -> radioGroup.check(R.id.rb_completed)
            "canceled" -> radioGroup.check(R.id.rb_canceled)
        }

        btnConfirmar.setOnClickListener {
            val checkedId = radioGroup.checkedRadioButtonId
            if (checkedId != -1) {
                val novoStatus = when (checkedId) {
                    R.id.rb_searching -> "searching"
                    R.id.rb_accepted -> "accepted"
                    R.id.rb_in_progress -> "in_progress"
                    R.id.rb_completed -> "completed"
                    R.id.rb_canceled -> "canceled"
                    else -> pedido.status
                }

                onStatusChanged(novoStatus)
                dismiss()
            } else {
                Toast.makeText(context, "Selecione um status", Toast.LENGTH_SHORT).show()
            }
        }

        btnCancelar.setOnClickListener {
            dismiss()
        }
    }
}