package com.example.salvo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.VehicleAdapter
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.Vehicle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.ByteArrayOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class GestaoFrotaActivity : AppCompatActivity() {

    private lateinit var rvVeiculos: RecyclerView

    private var base64Veiculo: String? = null
    private var providerId: Int = -1

    private var currentDialogImageView: ImageView? = null

    private val selecionarFotoVeiculo = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            base64Veiculo = uriToBase64(uri)

            currentDialogImageView?.setImageURI(uri)
            currentDialogImageView?.imageTintList = null
            currentDialogImageView?.scaleType = ImageView.ScaleType.CENTER_CROP
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_gestao_frota)

        providerId = intent.getIntExtra("USER_ID", -1)

        rvVeiculos = findViewById(R.id.rv_veiculos)
        rvVeiculos.layoutManager = LinearLayoutManager(this)

        findViewById<ImageView>(R.id.btn_voltar).setOnClickListener { finish() }

        findViewById<FloatingActionButton>(R.id.fab_adicionar_veiculo).setOnClickListener {
            base64Veiculo = null
            currentDialogImageView = null
            abrirDialogoCadastro()
        }

        configurarSwipeToDelete()
    }

    override fun onResume() {
        super.onResume()
        if (providerId != -1) {
            carregarVeiculos()
        }
    }

    // --- 1. SWIPE TO DELETE ---
    private fun configurarSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val adapter = rvVeiculos.adapter as? VehicleAdapter
                val veiculoAlvo = adapter?.getVehicleAt(position)

                if (veiculoAlvo != null && adapter != null) {
                    adapter.removerItem(position)
                    deletarVeiculoNoServidor(veiculoAlvo.id)
                }
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvVeiculos)
    }

    private fun deletarVeiculoNoServidor(veiculoId: Int) {
        RetrofitClient.apiService.excluirVeiculo(veiculoId, providerId).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@GestaoFrotaActivity, "Veículo removido", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@GestaoFrotaActivity, "Erro ao remover", Toast.LENGTH_SHORT).show()
                    carregarVeiculos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                carregarVeiculos()
            }
        })
    }

    // --- 2. MENU PRINCIPAL DE AÇÕES DO VEÍCULO ---
    private fun abrirMenuAcoesVeiculo(veiculo: Vehicle) {
        val opcoes = arrayOf("Alterar Status Operacional", "Editar Dados / Foto")

        AlertDialog.Builder(this, R.style.Theme_Salvo)
            .setTitle(veiculo.name)
            .setItems(opcoes) { _, which ->
                when (which) {
                    0 -> abrirDialogoMudarStatus(veiculo)
                    1 -> abrirDialogoEdicao(veiculo)
                }
            }
            .show()
    }

    // --- 2.1 ALTERAR STATUS ---
    private fun abrirDialogoMudarStatus(veiculo: Vehicle) {
        val opcoes = arrayOf("Disponível", "Em atendimento", "Em manutenção")

        AlertDialog.Builder(this, R.style.Theme_Salvo)
            .setTitle("Status: ${veiculo.name}")
            .setItems(opcoes) { _, which ->
                val novoStatus = opcoes[which]
                atualizarStatusNoServidor(veiculo.id, novoStatus)
            }
            .show()
    }

    private fun atualizarStatusNoServidor(veiculoId: Int, novoStatus: String) {
        val dados = mapOf("provider_id" to providerId.toString(), "status" to novoStatus)
        RetrofitClient.apiService.atualizarStatusVeiculo(veiculoId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) carregarVeiculos()
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@GestaoFrotaActivity, "Falha na rede", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 2.2 EDITAR DADOS COMPLETOS ---
    private fun abrirDialogoEdicao(veiculo: Vehicle) {
        base64Veiculo = null // Limpa para garantir que só enviaremos foto se ele escolher uma nova
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_add_veiculo, null)
        dialog.setContentView(view)

        val ivPreview = view.findViewById<ImageView>(R.id.iv_preview_foto)
        val etNome = view.findViewById<EditText>(R.id.et_nome_veiculo)
        val etPlaca = view.findViewById<EditText>(R.id.et_placa_veiculo)
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_veiculo)

        // Preenche com os dados atuais
        btnSalvar.text = "SALVAR ALTERAÇÕES"
        etNome.setText(veiculo.name)
        etPlaca.setText(veiculo.plate)

        // Decodifica e mostra a foto atual no preview
        if (!veiculo.vehiclePhoto.isNullOrEmpty()) {
            try {
                val imageBytes = Base64.decode(veiculo.vehiclePhoto, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                ivPreview.setImageBitmap(bitmap)
                ivPreview.imageTintList = null
                ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                ivPreview.setImageResource(android.R.drawable.ic_menu_camera)
            }
        }

        view.findViewById<View>(R.id.card_foto_novo_veiculo).setOnClickListener {
            currentDialogImageView = ivPreview
            selecionarFotoVeiculo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val placa = etPlaca.text.toString().trim()

            if (nome.isNotEmpty() && placa.isNotEmpty()) {
                val dados = mutableMapOf(
                    "provider_id" to providerId.toString(),
                    "name" to nome,
                    "plate" to placa
                )

                // Só anexa a chave de foto se uma nova foi gerada
                val fotoAtual = base64Veiculo
                if (fotoAtual != null) {
                    dados["vehicle_photo"] = fotoAtual
                }

                RetrofitClient.apiService.atualizarVeiculoCompleto(veiculo.id, dados).enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@GestaoFrotaActivity, "Veículo atualizado!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            carregarVeiculos()
                        } else {
                            Toast.makeText(this@GestaoFrotaActivity, "Erro ao atualizar", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        Toast.makeText(this@GestaoFrotaActivity, "Falha na comunicação", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@GestaoFrotaActivity, "Preencha nome e placa", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // --- 3. CADASTRAR NOVO (MANTIDO INTACTO) ---
    private fun abrirDialogoCadastro() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.layout_dialog_add_veiculo, null)
        dialog.setContentView(view)

        val ivPreview = view.findViewById<ImageView>(R.id.iv_preview_foto)
        val etNome = view.findViewById<EditText>(R.id.et_nome_veiculo)
        val etPlaca = view.findViewById<EditText>(R.id.et_placa_veiculo)
        val btnSalvar = view.findViewById<Button>(R.id.btn_salvar_veiculo)

        view.findViewById<View>(R.id.card_foto_novo_veiculo).setOnClickListener {
            currentDialogImageView = ivPreview
            selecionarFotoVeiculo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        btnSalvar.setOnClickListener {
            val nome = etNome.text.toString().trim()
            val placa = etPlaca.text.toString().trim()

            if (nome.isNotEmpty() && placa.isNotEmpty()) {
                val dados = mapOf(
                    "provider_id" to providerId.toString(),
                    "name" to nome,
                    "plate" to placa,
                    "status" to "Disponível",
                    "vehicle_photo" to base64Veiculo
                )

                RetrofitClient.apiService.adicionarVeiculo(dados).enqueue(object : Callback<AuthResponse> {
                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                        if (response.isSuccessful) {
                            Toast.makeText(this@GestaoFrotaActivity, "Veículo cadastrado com sucesso!", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                            carregarVeiculos()
                        } else {
                            Toast.makeText(this@GestaoFrotaActivity, "Erro ao processar cadastro", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                        Toast.makeText(this@GestaoFrotaActivity, "Falha na comunicação", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(this@GestaoFrotaActivity, "Por favor, preencha o nome e a placa.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // --- 4. CARREGAR LISTA ---
    private fun carregarVeiculos() {
        RetrofitClient.apiService.obterVeiculos(providerId).enqueue(object : Callback<List<Vehicle>> {
            override fun onResponse(call: Call<List<Vehicle>>, response: Response<List<Vehicle>>) {
                if (response.isSuccessful) {
                    val lista = response.body()?.toMutableList() ?: mutableListOf()

                    // Aponta o clique para o nosso novo Menu Duplo
                    rvVeiculos.adapter = VehicleAdapter(lista) { veiculoClicado ->
                        abrirMenuAcoesVeiculo(veiculoClicado)
                    }
                }
            }
            override fun onFailure(call: Call<List<Vehicle>>, t: Throwable) {
                Toast.makeText(this@GestaoFrotaActivity, "Não foi possível carregar a frota", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 5. BASE64 ---
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}