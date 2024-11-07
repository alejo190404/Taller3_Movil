package com.example.taller3.logica

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.taller3.Data.Usuario
import com.example.taller3.R
import com.bumptech.glide.Glide

// Adaptador personalizado para mostrar los usuarios disponibles en un ListView
class DisponiblesAdapterActivity (
    context: Context,
    private val userList: List<Usuario>, // Lista de usuarios que se mostrarán
    private val fotoUrlList: List<String> // Lista de URLs de las fotos de perfil de los usuarios
) : ArrayAdapter<Usuario>(context, 0, userList) {

    // Método que devuelve la vista de cada elemento de la lista
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        var view = convertView
        // Si la vista no existe, infla una nueva
        if (view == null) {
            view = LayoutInflater.from(context).inflate(
                R.layout.activity_disponibles_adapter, // Layout del item de la lista
                parent,
                false
            )
        }
        // Obtener el usuario y su URL de foto correspondiente
        val usuario = userList[position]
        val fotoUrl = fotoUrlList[position]
        // Si la URL de la foto está vacía, se usa una imagen predeterminada
        val foto = if (fotoUrl.isEmpty()) R.drawable.baseline_tag_faces_24 else fotoUrl

        // Mostrar la foto del usuario
        val ivFotoPerfil = view!!.findViewById<ImageView>(R.id.fotoUsuario)

        // Usar Glide para cargar la imagen de forma eficiente
        Glide.with(context)
            .load(foto) // Cargar la URL de la foto
            .placeholder(R.drawable.baseline_tag_faces_24) // Imagen de placeholder mientras carga
            .error(R.drawable.baseline_tag_faces_24) // Imagen si hay error en la carga
            .into(ivFotoPerfil)

        // Mostrar el nombre del usuario
        view.findViewById<TextView>(R.id.nombreUsuario).text = usuario.nombre

        // Manejar el clic en el botón que abre el mapa con la ubicación del usuario
        view.findViewById<Button>(R.id.ubicacionUsuario).setOnClickListener {
            val intent = Intent(context, MapaUserActivity::class.java)
            // Pasar los datos de ubicación del usuario a la actividad del mapa
            intent.putExtra("latitud", usuario.latitud)
            intent.putExtra("longitud", usuario.longitud)
            intent.putExtra("uid", usuario.uid) // Pasar el ID del usuario

            // Iniciar la actividad de mapa
            context.startActivity(intent)
        }

        return view // Retornar la vista del item
    }
}
