package com.example.taller3.logica

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.firebase.auth.FirebaseUser
import com.example.taller3.Data.Datos
import com.example.taller3.Data.Usuario
import com.example.taller3.R
import com.example.taller3.databinding.ActivityMapaUserBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlin.math.pow

class MapaUserActivity : AppCompatActivity(), OnMapReadyCallback {

    // Binding para acceder a los elementos de la vista
    private lateinit var binding: ActivityMapaUserBinding

    // Instancia de Google Map
    private lateinit var mMap: GoogleMap

    // Cliente para obtener la ubicación actual
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Instancia de Firebase Authentication
    private lateinit var auth: FirebaseAuth

    // Variables para almacenar las ubicaciones del usuario
    private var userLocation: LatLng? = null
    private var myLocation: LatLng? = null

    // UID del usuario
    private var userUid: String? = null

    // Marcadores en el mapa
    private var userMarker: Marker? = null
    private var myMarker: Marker? = null

    // Instancia de Firebase Realtime Database
    private val database = FirebaseDatabase.getInstance()
    private lateinit var userRef: DatabaseReference

    // Ruta de la base de datos para los usuarios
    val USERS_PATH = "users/"

    override fun onStart() {
        super.onStart()
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        navigateToMainIfUserNotLogged(currentUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapaUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        userRef = database.getReference(USERS_PATH)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtener el mapa cuando esté listo
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        var tempLocation: LatLng? = null

        // Obtener la ubicación actual del dispositivo
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    myLocation = LatLng(location.latitude, location.longitude)
                    tempLocation = myLocation
                    Log.e("LocationSuccess", "Latitud: ${myLocation?.latitude}, Longitud: ${myLocation?.longitude}")

                    // Obtener los datos del usuario desde el intent
                    val intent = intent
                    val userLatitude = intent.getDoubleExtra("latitud", 0.0)
                    val userLongitude = intent.getDoubleExtra("longitud", 0.0)
                    userUid = intent.getStringExtra("uid")
                    userLocation = LatLng(userLatitude, userLongitude)

                    // Verificar permisos de ubicación
                    verifyLocationPermissions()
                    loadUsersData(tempLocation)

                    // Calcular la distancia si la ubicación del usuario es válida
                    userLocation?.let {
                        val distance = calculateDistance(myLocation!!.latitude, myLocation!!.longitude, it.latitude, it.longitude)
                        binding.tvDistancia.text = "Distancia hasta su ubicación: ${String.format("%.2f", distance)} metros"
                    } ?: run {
                        Log.e("MapaUserActivity", "La ubicación del usuario no está disponible.")
                    }
                } else {
                    Log.e("FailedLocation", "No se pudo obtener la ubicación.")
                }
            }
            .addOnFailureListener {
                Log.e("MapaUserActivity", "Error al obtener la ubicación.")
            }
    }

    private fun loadUsersData(currentLocation: LatLng?) {
        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (userSnapshot in dataSnapshot.children) {
                    val user = userSnapshot.getValue(Usuario::class.java)
                    Log.e("UserInfo", "Nombre: ${user?.nombre}  Latitud: ${user?.latitud} Longitud: ${user?.longitud}")
                    if (user?.uid == userUid) {
                        userMarker?.remove() // Eliminar marcador previo
                        if (user != null) {
                            userLocation = LatLng(user.latitud, user.longitud)
                        }
                        myMarker = mMap.addMarker(MarkerOptions().position(userLocation!!).title("Usuario seleccionado"))
                        userMarker = mMap.addMarker(MarkerOptions().position(currentLocation!!).title("Ubicación del usuario seleccionado"))
                        myMarker?.let {
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLocation!!, 13f))
                        }
                        val distance = calculateDistance(currentLocation!!.latitude, currentLocation.longitude, userLocation!!.latitude, userLocation!!.longitude)
                        binding.tvDistancia.text = "Distancia hasta su ubicación: ${String.format("%.2f", distance)} metros"
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("DatabaseError", "Error en la consulta a la base de datos: ${databaseError.message}")
            }
        })
    }

    private fun verifyLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestLocationPermissions()
        }
    }

    private fun requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), Datos.MY_PERMISSION_REQUEST_LOCATION)
        }
    }

    private fun navigateToMainIfUserNotLogged(currentUser: FirebaseUser?) {
        if (currentUser == null) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // Radio de la Tierra en km
        val lat1Radians = Math.toRadians(lat1)
        val lon1Radians = Math.toRadians(lon1)
        val lat2Radians = Math.toRadians(lat2)
        val lon2Radians = Math.toRadians(lon2)

        val dlon = lon2Radians - lon1Radians
        val dlat = lat2Radians - lat1Radians

        val a = Math.sin(dlat / 2).pow(2) + Math.cos(lat1Radians) * Math.cos(lat2Radians) * Math.sin(dlon / 2).pow(2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

        return earthRadius * c * 1000 // Convertir a metros
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == Datos.MY_PERMISSION_REQUEST_LOCATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permisos concedidos
            } else {
                Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
