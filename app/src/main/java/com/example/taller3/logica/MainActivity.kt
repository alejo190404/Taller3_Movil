// MainActivity.kt
package com.example.taller3.logica

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.example.taller3.databinding.ActivityMainBinding
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    // Variables de Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityMainBinding

    // Instancia de la base de datos de Firebase
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference

    companion object {
        private const val TAG = "MainActivity" // Etiqueta para logging
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Inicialización de binding para la vista
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inicialización de FirebaseAuth
        auth = Firebase.auth

        // Configuración del listener para el botón de login
        binding.loginButton.setOnClickListener {
            val email = binding.editTextTextEmailAddress.text.toString()
            val password = binding.editTextTextPassword.text.toString()
            signInUser(email, password) // Intentar iniciar sesión
        }

        // Configuración del listener para el botón de registro
        binding.registerButton.setOnClickListener {
            val intent = Intent(this, RegisterPage::class.java) // Ir a la página de registro
            startActivity(intent)
        }
    }

    // Método que se llama cuando la actividad está a punto de empezar
    override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser // Obtener el usuario actual
        updateUI(currentUser) // Actualizar la interfaz de usuario según si el usuario está autenticado
    }

    // Método para actualizar la interfaz de usuario según el estado de la autenticación
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser != null) {
            val intent = Intent(this, MapaActivity::class.java) // Redirigir al usuario a la actividad del mapa
            intent.putExtra("user", currentUser.email) // Pasar el correo del usuario a la siguiente actividad
            startActivity(intent)
            finish() // Finalizar la actividad actual
        } else {
            // Limpiar los campos de entrada si no hay usuario autenticado
            binding.editTextTextEmailAddress.setText("")
            binding.editTextTextPassword.setText("")
        }
    }

    // Método para validar que los campos del formulario no están vacíos
    private fun validateForm(): Boolean {
        var valid = true
        val email = binding.editTextTextEmailAddress.text.toString()
        if (TextUtils.isEmpty(email)) {
            binding.editTextTextEmailAddress.error = "Requerido." // Mostrar error si el campo de email está vacío
            valid = false
        } else {
            binding.editTextTextEmailAddress.error = null
        }

        val password = binding.editTextTextPassword.text.toString()
        if (TextUtils.isEmpty(password)) {
            binding.editTextTextPassword.error = "Requerido." // Mostrar error si el campo de password está vacío
            valid = false
        } else {
            binding.editTextTextPassword.error = null
        }
        return valid // Devolver si el formulario es válido
    }

    // Método para validar que el email tiene el formato correcto
    private fun isEmailValid(email: String): Boolean {
        return email.contains("@") && email.contains(".") && email.length >= 5
    }

    // Método para intentar iniciar sesión con Firebase Authentication
    private fun signInUser(email: String, password: String) {
        // Validar el formulario y el formato del email antes de intentar iniciar sesión
        if (validateForm() && isEmailValid(email)) {
            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task -> // Intentar iniciar sesión
                    if (task.isSuccessful) {
                        Log.d(TAG, "signInWithEmail:success") // Ingreso exitoso
                        val user = auth.currentUser
                        updateUI(user) // Actualizar la interfaz de usuario con los datos del usuario autenticado
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.exception) // Error en el inicio de sesión
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        updateUI(null) // Si falla, actualiza la interfaz como si no hubiera usuario autenticado
                    }
                }
        } else {
            Toast.makeText(this, "Please enter valid email and password.", Toast.LENGTH_SHORT).show()
        }
    }
}
