package com.example.salvo

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class RegisterChooseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_register_choose)


        val mainLayout = findViewById<android.view.View>(R.id.main)
        if (mainLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }

        val userOption = findViewById<Button>(R.id.btn_user)
        val ciaOption = findViewById<Button>(R.id.btn_company)

        // Ação para Cliente comum
        userOption.setOnClickListener {
            val intentUser = Intent(this, RegisterActivity::class.java)
            intentUser.putExtra("role", "customer")
            startActivity(intentUser)

        }

        ciaOption.setOnClickListener {
            val intentCia = Intent(this, RegisterMecActivity::class.java)
            intentCia.putExtra("role", "provider")
            startActivity(intentCia)
        }
    }
}