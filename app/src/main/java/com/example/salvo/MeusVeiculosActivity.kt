package com.example.salvo

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.salvo.adapter.VehicleAdapter
import com.example.salvo.model.AuthResponse
import com.example.salvo.model.Vehicle
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import java.io.ByteArrayOutputStream
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MeusVeiculosActivity : AppCompatActivity() {

    private lateinit var rvVeiculos: RecyclerView
    private var base64Veiculo: String? = null
    private var customerId: Int = -1
    private var currentDialogImageView: ImageView? = null

    // Lógica para pegar a imagem da galeria
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
        setContentView(R.layout.activity_meus_veiculos)

        // Puxar do SessionManager
        val sessionManager = com.example.salvo.utils.SessionManager(this)
        customerId = sessionManager.buscarUserId()

        if (customerId == -1) {
            customerId = intent.getIntExtra("USER_ID", -1)
        }

        if (customerId == -1) {
            Toast.makeText(this, "Erro de sessão: Faça login novamente.", Toast.LENGTH_LONG).show()
        }

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar_veiculos)
        toolbar.setNavigationOnClickListener { finish() }

        rvVeiculos = findViewById(R.id.rv_meus_veiculos)
        rvVeiculos.layoutManager = LinearLayoutManager(this)

        findViewById<FloatingActionButton>(R.id.fab_adicionar_carro).setOnClickListener {
            base64Veiculo = null
            currentDialogImageView = null
            abrirDialogoCadastroEdicao(null) // Passa null para indicar "Novo Cadastro"
        }

        configurarSwipeToDelete()
    }

    override fun onResume() {
        super.onResume()
        if (customerId != -1) {
            carregarVeiculos()
        }
    }

    // --- 1. CARREGAR E LISTAR ---
    private fun carregarVeiculos() {
        RetrofitClient.apiService.obterVeiculosCliente(customerId).enqueue(object : Callback<List<Vehicle>> {
            override fun onResponse(call: Call<List<Vehicle>>, response: Response<List<Vehicle>>) {
                if (response.isSuccessful) {
                    val lista = response.body()?.toMutableList() ?: mutableListOf()

                    rvVeiculos.adapter = VehicleAdapter(
                        vehicles = lista,
                        onVehicleClick = { veiculoClicado -> abrirDialogoCadastroEdicao(veiculoClicado) },
                        onVehicleLongClick = { veiculoClicado -> abrirDialogoCadastroEdicao(veiculoClicado) }
                    )
                }
            }
            override fun onFailure(call: Call<List<Vehicle>>, t: Throwable) {
                Toast.makeText(this@MeusVeiculosActivity, "Falha ao carregar veículos.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 2. CADASTRO / EDIÇÃO UNIFICADO ---
    private fun abrirDialogoCadastroEdicao(veiculoExistente: Vehicle?) {
        base64Veiculo = null // Reseta a foto para a nova operação

        val dialog = BottomSheetDialog(this, R.style.Theme_Salvo)
        val view = layoutInflater.inflate(R.layout.layout_dialog_add_veiculo, null)
        dialog.setContentView(view)

        val ivPreview = view.findViewById<ImageView>(R.id.iv_preview_foto)
        val etMarca = view.findViewById<TextInputEditText>(R.id.et_marca_veiculo)
        val etModelo = view.findViewById<TextInputEditText>(R.id.et_nome_veiculo)
        val etPlaca = view.findViewById<TextInputEditText>(R.id.et_placa_veiculo)
        val etTipo = view.findViewById<TextInputEditText>(R.id.et_tipo_veiculo)
        val etCor = view.findViewById<TextInputEditText>(R.id.et_cor_veiculo)

        val btnSalvar = view.findViewById<MaterialButton>(R.id.btn_salvar_veiculo)

        // Esconde o campo de manutenção para o Cliente
        view.findViewById<View>(R.id.til_data_manutencao).visibility = View.GONE

        // Se for edição, preenche com os dados antigos
        if (veiculoExistente != null) {
            btnSalvar.text = "SALVAR ALTERAÇÕES"
            etModelo.setText(veiculoExistente.name)
            etPlaca.setText(veiculoExistente.plate)

            etMarca.setText(veiculoExistente.brand)
            etCor.setText(veiculoExistente.color)
            etTipo.setText(veiculoExistente.vehicleType)

            if (!veiculoExistente.vehiclePhoto.isNullOrEmpty()) {
                try {
                    val imageBytes = Base64.decode(veiculoExistente.vehiclePhoto, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ivPreview.setImageBitmap(bitmap)
                    ivPreview.imageTintList = null
                    ivPreview.scaleType = ImageView.ScaleType.CENTER_CROP
                } catch (e: Exception) { }
            }
        }

        // Clique para abrir a galeria
        view.findViewById<View>(R.id.card_foto_novo_veiculo).setOnClickListener {
            currentDialogImageView = ivPreview
            selecionarFotoVeiculo.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // Clique para salvar
        btnSalvar.setOnClickListener {
            // 🚀 AQUI: Pegando todos os dados corretamente!
            val marca = etMarca.text.toString().trim()
            val modelo = etModelo.text.toString().trim()
            val placa = etPlaca.text.toString().trim()
            val tipo = etTipo.text.toString().trim()
            val cor = etCor.text.toString().trim()

            // Validação completa de todos os campos
            if (marca.isNotEmpty() && modelo.isNotEmpty() && placa.isNotEmpty() && tipo.isNotEmpty() && cor.isNotEmpty()) {
                if (veiculoExistente == null) {
                    salvarNovoVeiculoNaApi(modelo, placa, marca, cor, tipo, dialog)
                } else {
                    atualizarVeiculoNaApi(veiculoExistente.id, modelo, placa, marca, cor, tipo, veiculoExistente.vehiclePhoto, dialog)
                }
            } else {
                Toast.makeText(this, "Preencha todos os campos obrigatórios.", Toast.LENGTH_SHORT).show()
            }
        }
        dialog.show()
    }

    // --- 3. SALVAR NOVO (VIA API CLIENTE) ---
    private fun salvarNovoVeiculoNaApi(modelo: String, placa: String, marca: String, cor: String, tipo: String, dialog: BottomSheetDialog) {
        val dados = mapOf(
            "customer_id" to customerId.toString(),
            "name" to modelo,
            "plate" to placa,
            "brand" to marca,
            "color" to cor,
            "vehicle_type" to tipo,
            "vehicle_photo" to base64Veiculo
        )

        RetrofitClient.apiService.adicionarVeiculoCliente(dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MeusVeiculosActivity, "Carro salvo!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    carregarVeiculos()
                } else {
                    val erroBody = response.errorBody()?.string() ?: "Sem detalhes"
                    Toast.makeText(this@MeusVeiculosActivity, "Erro: $erroBody", Toast.LENGTH_LONG).show()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@MeusVeiculosActivity, "Falha de conexão.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 4. ATUALIZAR EXISTENTE (VIA API CLIENTE) ---
    private fun atualizarVeiculoNaApi(veiculoId: Int, modelo: String, placa: String, marca: String, cor: String, tipo: String, fotoAntiga: String?, dialog: BottomSheetDialog) {
        val dados = mutableMapOf(
            "customer_id" to customerId.toString(),
            "name" to modelo,
            "plate" to placa,
            "brand" to marca,
            "color" to cor,
            "vehicle_type" to tipo
        )

        val fotoFinal = base64Veiculo ?: fotoAntiga
        if (fotoFinal != null) {
            dados["vehicle_photo"] = fotoFinal
        }

        RetrofitClient.apiService.atualizarVeiculoCliente(veiculoId, dados).enqueue(object : Callback<AuthResponse> {
            override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MeusVeiculosActivity, "Dados atualizados!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    carregarVeiculos()
                }
            }
            override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                Toast.makeText(this@MeusVeiculosActivity, "Falha de conexão.", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // --- 5. EXCLUIR (SWIPE TO DELETE) ---
    private fun configurarSwipeToDelete() {
        val callback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return

                val adapter = rvVeiculos.adapter as? VehicleAdapter
                val veiculoAlvo = adapter?.getVehicleAt(position)

                if (veiculoAlvo != null) {
                    adapter.removerItem(position)
                    RetrofitClient.apiService.excluirVeiculoCliente(veiculoAlvo.id, customerId).enqueue(object: Callback<AuthResponse>{
                        override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {}
                        override fun onFailure(call: Call<AuthResponse>, t: Throwable) { carregarVeiculos() }
                    })
                }
            }
        }
        ItemTouchHelper(callback).attachToRecyclerView(rvVeiculos)
    }

    // --- 6. CONVERSOR DE IMAGEM ---
    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
            val bytes = outputStream.toByteArray()
            Base64.encodeToString(bytes, Base64.DEFAULT)
        } catch (e: Exception) { null }
    }
}