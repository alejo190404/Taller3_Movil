package com.example.taller3.logica

import android.content.Context
import android.content.pm.PackageManager
import android.health.connect.datatypes.ExerciseRoute
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.location.Location
import com.example.taller3.R
import com.example.taller3.databinding.ActivityMapaBinding
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
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.json.JSONObject
import java.io.IOException
import com.example.taller3.Data.Datos.Companion.MY_PERMISSION_REQUEST_LOCATION
import com.example.taller3.Data.Usuario
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Build
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.annotation.RequiresApi
import com.google.firebase.auth.FirebaseUser
import java.lang.Math.atan2
import java.lang.Math.cos
import java.lang.Math.sin
import java.lang.Math.sqrt
import kotlin.math.pow

class MapaActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapaBinding
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var auth: FirebaseAuth
    private var myMarker: Marker? = null
    //Realtime Database
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    val PATH_USERS="users/"
    private var myLocation: Location? = null

    override fun onStart() {
        super.onStart()
        auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        updateUI(currentUser)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configura la barra de herramientas como la barra de acción de la actividad
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        // Cambia el título de la barra de herramientas
        supportActionBar?.title = "Taller 3"

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)



        checkLocationPermission()

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap


        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación actual"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                }
            }

        // Read JSON file and add markers
        val jsonFileString = getJsonDataFromAsset(applicationContext, "locations.json")
        val jsonObject = JSONObject(jsonFileString)

        val locations = jsonObject.getJSONArray("locationsArray")

        // Add markers from locationsArray
        for (i in 0 until locations.length()) {
            val locationObject = locations.getJSONObject(i)
            val latitude = locationObject.getDouble("latitude")
            val longitude = locationObject.getDouble("longitude")
            val title = locationObject.getString("name")
            val latLng = LatLng(latitude, longitude)
            mMap.addMarker(MarkerOptions().position(latLng).title(title))
        }
        // Revisar permisos
        checkLocationPermission()
    }

    // Function to read JSON file from assets folder
    private fun getJsonDataFromAsset(context: Context, fileName: String): String? {
        return try {
            val inputStream = context.assets.open(fileName)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            null
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == MY_PERMISSION_REQUEST_LOCATION){// Nuestros permisos
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){ // Validar que si se aceptaron ambos permisos
                // Permisos aceptados
                startLocationUpdates()
            }else{
                //El permiso no ha sido aceptado
                Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Si no tienes permisos, solicitarlos al usuario
            requestLocationPermission()
        } else {
            // Si tienes permisos
            startLocationUpdates()

        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Crear una solicitud de ubicación
        val locationRequest: LocationRequest = LocationRequest.create()
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationRequest.setInterval(2000) // Intervalo de actualización de ubicación en milisegundos

        // Configurar un callback para recibir actualizaciones de ubicación
        val locationCallback: LocationCallback = object : LocationCallback() {
            @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                // Obtener la ubicación actual del resultado
                val miUbi: Location? = locationResult.lastLocation
                //Asignar la ubicación por primera vez
                if(myLocation == null && miUbi != null){
                    myLocation = miUbi
                    //Crear nuevo marcador
                    myMarker = mMap.addMarker(MarkerOptions().position(LatLng(miUbi.latitude, miUbi.longitude)).title("Mi ubicación actual"))
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(miUbi.latitude, miUbi.longitude), 15f))
                }

                if (miUbi != null) {
                    // Actualizar la interfaz de usuario con la ubicación actual
                    if(miUbi != myLocation && myLocation != null){
                        val currentLatLng = LatLng(miUbi.latitude, miUbi.longitude)
                        val distanceBetweenTwoPoints = distance(miUbi.latitude, miUbi.longitude, myLocation!!.latitude, myLocation!!.longitude)
                        //Si se movió más de 7 metros (Esto es para el bug de la ubicación en un celular real)
                        //Toast.makeText(this@MapaActivity, "Distancia: $distanceBetweenTwoPoints", Toast.LENGTH_SHORT).show()
                        if(distanceBetweenTwoPoints > 7.0){
                            //Borrar marcador anterior
                            if(myMarker != null){
                                myMarker!!.remove()
                            }
                            //Crear nuevo marcador
                            myMarker = mMap.addMarker(MarkerOptions().position(currentLatLng).title("Mi ubicación actual"))
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                            //Actualizar en la base de datos la posicion
                            val currentUser = auth.currentUser
                            currentUser?.let {
                                // Obtén una referencia a la ubicación del usuario en la base de datos
                                myRef = database.getReference(PATH_USERS+currentUser.uid)
                                myRef.get().addOnSuccessListener { dataSnapshot ->
                                    // Obtiene el usuario actual de la base de datos
                                    val usuario = dataSnapshot.getValue(Usuario::class.java)
                                    usuario?.let {
                                        // Cambia el estado del usuario
                                        it.latitud = currentLatLng.latitude
                                        it.longitud = currentLatLng.longitude
                                        // Actualiza el estado del usuario en la base de datos
                                        myRef.setValue(it)
                                    }
                                }
                            }
                        }

                    }
                    //Actualizar ubicacion actual
                    myLocation = miUbi
                }
            }
        }

        // Solicitar actualizaciones de ubicación
        fusedLocationClient!!.requestLocationUpdates(locationRequest, locationCallback, null)

    }

    private fun requestLocationPermission(){
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)){
            //El usuario ya ha rechazado los permisos
            Toast.makeText(this, "Permisos denegados :(", Toast.LENGTH_SHORT).show()
        }else{
            //Pedir permisos
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_LOCATION)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.menu, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        return when (item.itemId) {
            R.id.menuLogOut -> {
                auth.signOut()
                val intent = Intent(this, MainActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            R.id.menuEstado -> {
                val currentUser = auth.currentUser
                currentUser?.let {
                    // Obtén una referencia a la ubicación del usuario en la base de datos
                    myRef = database.getReference(PATH_USERS+currentUser.uid)
                    myRef.get().addOnSuccessListener { dataSnapshot ->
                        // Obtiene el usuario actual de la base de datos
                        val usuario = dataSnapshot.getValue(Usuario::class.java)
                        usuario?.let {
                            // Cambia el estado del usuario
                            it.estado = !it.estado
                            // Actualiza el estado del usuario en la base de datos
                            myRef.setValue(it)
                            // Actualiza el texto del elemento de menú
                            item.title = if (it.estado) "Disponible" else "Desconectado"
                            Toast.makeText(this, "Estado actualizado: " + item.title, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                true
            }
            R.id.menuDisponibles -> {
                val intent = Intent(this, DisponiblesActivity::class.java)
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateUI(currentUser: FirebaseUser?) {
        if (currentUser == null) {
            // Navigate to the login activity
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // Radio medio de la Tierra en kilómetros

        val lat1Radians = Math.toRadians(lat1)
        val lon1Radians = Math.toRadians(lon1)
        val lat2Radians = Math.toRadians(lat2)
        val lon2Radians = Math.toRadians(lon2)

        val dlon = lon2Radians - lon1Radians
        val dlat = lat2Radians - lat1Radians

        val a = sin(dlat / 2).pow(2) + cos(lat1Radians) * cos(lat2Radians) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distanceInKm = earthRadius * c
        return distanceInKm * 1000 // Convertir a metros
    }
}