package com.example.taller3.logica

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.taller3.Data.Usuario
import com.example.taller3.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.io.File

class DisponiblesActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userList: MutableList<Usuario>
    private lateinit var fotoUrlList: MutableList<String>
    private var mUsuariosAdapter: DisponiblesAdapterActivity? = null
    private var mlista: ListView? = null
    // Realtime Database
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    val PATH_USERS = "users/"
    // Storage
    private lateinit var storage: FirebaseStorage

    override fun onStart() {
        super.onStart()
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_disponibles)

        myRef = database.getReference(PATH_USERS)
        storage = FirebaseStorage.getInstance()

        userList = mutableListOf()

        // Crear una lista vacía para las URLs de fotos
        fotoUrlList = mutableListOf<String>()

        // Inicializar la lista y el adaptador
        mlista = findViewById(R.id.listaUsuarios)
        mUsuariosAdapter = DisponiblesAdapterActivity(this, userList, fotoUrlList)
        mlista?.adapter = mUsuariosAdapter

        // Cargar la lista de usuarios disponibles
        loadAvailableUsers()
        nuevoUsuarioDisponible()
    }

    // Este método escucha en tiempo real los cambios en la base de datos, como cuando se agrega un nuevo usuario disponible
    private fun nuevoUsuarioDisponible() {
        myRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                // Este método se llama cuando se agrega un nodo hijo (nuevo usuario)
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                // Este método se llama cuando los datos de un nodo existente cambian
                val user = dataSnapshot.child("/").getValue(Usuario::class.java)
                user?.let {
                    // Si el usuario está disponible (estado = true), muestra un Toast con su nombre
                    if (it.estado) {
                        Toast.makeText(this@DisponiblesActivity, "Nuevo usuario disponible: " + it.nombre.toUpperCase(), Toast.LENGTH_SHORT).show()
                    }
                    // Aquí puedes trabajar con los datos del usuario que cambió
                }
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
                // Este método se llama cuando se elimina un nodo hijo (usuario)
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, prevChildKey: String?) {
                // Este método se llama cuando se mueve un nodo hijo
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Este método se llama cuando se cancela la operación
            }
        })
    }

    // Este método carga todos los usuarios disponibles desde la base de datos
    private fun loadAvailableUsers() {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                userList.clear()
                // Iterar sobre los usuarios en la base de datos y agregarlos a la lista si están disponibles
                for (userFolderSnapshot in dataSnapshot.children) {
                    // Cada "userFolderSnapshot" representa una carpeta de un usuario individual en la ruta "/users"
                    val user = userFolderSnapshot.child("/").getValue(Usuario::class.java)
                    user?.let {
                        // Solo agregar usuarios que están disponibles (estado = true) a la lista
                        if (it.estado) {
                            userList.add(it)
                        } else {
                            userList.remove(it)
                        }
                        mUsuariosAdapter?.notifyDataSetChanged()
                    }
                }
                // Después de cargar los usuarios, descargar las imágenes de perfil de cada uno
                downloadFiles()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Manejar errores de cancelación de la operación
            }
        })
    }

    // Este método se encarga de actualizar la interfaz de usuario según el estado de autenticación del usuario
    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser == null) {
            // Si no hay un usuario autenticado, redirigir a la pantalla de inicio de sesión
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    // Este método descarga las imágenes de perfil de los usuarios disponibles
    private fun downloadFiles() {
        for (user in userList) {
            if (user.profileImgUrl.isEmpty()) {
                // Si el usuario no tiene una imagen de perfil, añadir una entrada vacía
                fotoUrlList.add("")
                mUsuariosAdapter?.notifyDataSetChanged()
                continue
            }
            // Crear un archivo temporal para almacenar la imagen descargada
            val localFile = File.createTempFile("profile_image", "jpeg")
            val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/imagen.jpg")
            fotoUrlList.add(localFile.absolutePath)

            // Descargar la imagen de perfil del usuario desde Firebase Storage
            imageRef.getFile(localFile)
                .addOnSuccessListener { taskSnapshot ->
                    // Si la descarga fue exitosa, actualizar la lista de URLs de imágenes y notificar al adaptador
                    mUsuariosAdapter?.notifyDataSetChanged()
                    Log.i("FBApp", "Imagen descargada exitosamente")
                }.addOnFailureListener { exception ->
                    // Manejar error en caso de que la descarga falle
                    Log.e("FBApp", "Error al descargar la imagen", exception)
                }
        }
    }
}
