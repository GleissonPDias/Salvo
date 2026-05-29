package com.example.salvo.util

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

/**
 * Utility class para aplicar máscaras em campos de entrada
 * Fornece máscaras para telefone, CNPJ, CPF, preço, placa, etc.
 */
object InputMaskUtil {

    /**
     * Aplica máscara de telefone: (XX) XXXXX-XXXX
     */
    fun aplicarMascaraTelefone(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                var telefone = s?.toString()?.replace(Regex("[^0-9]"), "") ?: ""

                if (telefone.length > 11) {
                    telefone = telefone.substring(0, 11)
                }

                val formatado = when (telefone.length) {
                    in 0..1 -> telefone
                    in 2..6 -> "(${telefone.substring(0, 2)}) ${telefone.substring(2)}"
                    else -> "(${telefone.substring(0, 2)}) ${telefone.substring(2, 7)}-${telefone.substring(7)}"
                }

                isUpdating = true
                editText.setText(formatado)
                editText.setSelection(formatado.length)
                isUpdating = false
            }
        })
    }

    /**
     * Aplica máscara de CNPJ: XX.XXX.XXX/XXXX-XX
     */
    fun aplicarMascaraCNPJ(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                var cnpj = s?.toString()?.replace(Regex("[^0-9]"), "") ?: ""

                if (cnpj.length > 14) {
                    cnpj = cnpj.substring(0, 14)
                }

                val formatado = when (cnpj.length) {
                    in 0..1 -> cnpj
                    in 2..4 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2)}"
                    in 5..7 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5)}"
                    in 8..11 -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5, 8)}/" +
                            "${cnpj.substring(8)}"
                    else -> "${cnpj.substring(0, 2)}.${cnpj.substring(2, 5)}.${cnpj.substring(5, 8)}/" +
                            "${cnpj.substring(8, 12)}-${cnpj.substring(12)}"
                }

                isUpdating = true
                editText.setText(formatado)
                editText.setSelection(formatado.length)
                isUpdating = false
            }
        })
    }

    /**
     * Aplica máscara de CPF: XXX.XXX.XXX-XX
     */
    fun aplicarMascaraCPF(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                var cpf = s?.toString()?.replace(Regex("[^0-9]"), "") ?: ""

                if (cpf.length > 11) {
                    cpf = cpf.substring(0, 11)
                }

                val formatado = when (cpf.length) {
                    in 0..2 -> cpf
                    in 3..5 -> "${cpf.substring(0, 3)}.${cpf.substring(3)}"
                    in 6..8 -> "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6)}"
                    else -> "${cpf.substring(0, 3)}.${cpf.substring(3, 6)}.${cpf.substring(6, 9)}" +
                            "-${cpf.substring(9)}"
                }

                isUpdating = true
                editText.setText(formatado)
                editText.setSelection(formatado.length)
                isUpdating = false
            }
        })
    }

    /**
     * Aplica máscara de Preço em formato brasileiro: R$ X.XXX,XX
     */
    fun aplicarMascaraPreco(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                var preco = s?.toString()?.replace(Regex("[^0-9]"), "") ?: ""

                if (preco.isEmpty()) {
                    preco = "0"
                }

                if (preco.length > 8) {
                    preco = preco.substring(0, 8)
                }

                // Garantir que tem 2 casas decimais
                while (preco.length < 3) {
                    preco = "0$preco"
                }

                val inteiro = preco.substring(0, preco.length - 2)
                val decimal = preco.substring(preco.length - 2)

                // Formatar com separador de milhar
                val integerFormatted = inteiro.reversed().chunked(3).joinToString(".").reversed()

                val formatado = "R$ $integerFormatted,$decimal"

                isUpdating = true
                editText.setText(formatado)
                editText.setSelection(formatado.length)
                isUpdating = false
            }
        })
    }

    /**
     * Aplica máscara de Placa de Veículo
     * Suporta: XXX-XXXX (padrão antigo) ou XXXXXX (Mercosul)
     */
    fun aplicarMascaraPlaca(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            var isUpdating = false

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                if (isUpdating) return

                // 🔧 CORRIGIDO: Era "precoFormatado" mas deveria ser "s"
                var placa = s?.toString()?.replace(Regex("[^0-9A-Za-z]"), "")?.uppercase() ?: ""

                if (placa.length > 7) {
                    placa = placa.substring(0, 7)
                }

                val formatado = if (placa.length <= 3) {
                    placa
                } else if (placa.length <= 7) {
                    "${placa.substring(0, 3)}-${placa.substring(3)}"
                } else {
                    placa
                }

                isUpdating = true
                editText.setText(formatado)
                editText.setSelection(formatado.length)
                isUpdating = false
            }
        })
    }

    /**
     * Remove todos os caracteres especiais de um texto
     * Útil para enviar dados limpos para a API
     */
    fun removerMascara(texto: String): String {
        return texto.replace(Regex("[^0-9A-Za-z]"), "")
    }

    /**
     * Obtém apenas os números de um texto com máscara
     */
    fun obterApenasNumeros(texto: String): String {
        return texto.replace(Regex("[^0-9]"), "")
    }

    /**
     * Converte preço formatado (R$ 1.000,00) para Double
     */
    fun converterPrecoParaDouble(precoFormatado: String): Double {
        val limpo = precoFormatado.replace(Regex("[^0-9]"), "")
        return if (limpo.isEmpty()) 0.0 else (limpo.toInt() / 100.0)
    }
}