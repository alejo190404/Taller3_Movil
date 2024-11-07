package com.example.taller3.logica

import android.content.Intent
import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.example.taller3.Data.Datos.Companion.GALLERY_REQUEST_CODE
import com.example.taller3.Data.Datos.Companion.MY_PERMISSION_REQUEST_LOCATION
import com.example.taller3.Data.Usuario
import com.example.taller3.databinding.ActivityRegisterPageBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.storage

class RegisterPage : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterPageBinding
    // Firebase Authentication
    private lateinit var auth: FirebaseAuth
    // Realtime Database reference
    private val database = FirebaseDatabase.getInstance()
    private lateinit var myRef: DatabaseReference
    val PATH_USERS = "users/"
    // Location permission handling
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var myLocation: Location? = null
    // Image storage
    private var selectedImageUri: Uri? = null
    private lateinit var storage: FirebaseStorage

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterPageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize the fused location client to handle location requests
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Check if location permissions are granted
        checkLocationPermission()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Button to select image from gallery
        binding.buttonSelectImage.setOnClickListener { openGalleryForImageSelection() }

        // Register button logic
        binding.registerbutton.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            val password = binding.editTextPassword.text.toString()
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("FirebaseRegister", "createUserWithEmail:onComplete:" + task.isSuccessful)
                        val user = auth.currentUser
                        if (user != null) {
                            var usuario = Usuario()
                            usuario.uid = user.uid
                            usuario.profileImgUrl = selectedImageUri?.lastPathSegment ?: ""
                            usuario.nombre = binding.editTextName.text.toString()
                            usuario.apellido = binding.editTextLastName.text.toString()
                            usuario.noIdentificacion = binding.editTextId.text.toString().toInt()
                            usuario.estado = true
                            if (myLocation != null) {
                                usuario.latitud = myLocation!!.latitude
                                usuario.longitud = myLocation!!.longitude
                            }
                            // Saving user information to the Realtime Database
                            myRef = database.getReference(PATH_USERS + user.uid)
                            myRef.setValue(usuario)
                            updateUserUI(user)
                            uploadUserProfileImage()
                        }
                    } else {
                        Toast.makeText(this, "createUserWithEmail:Failure: " + task.exception.toString(),
                            Toast.LENGTH_SHORT).show()
                        task.exception?.message?.let { Log.e("FirebaseRegister", it) }
                    }
                }
        }
    }

    // Handles the result of permission request
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_PERMISSION_REQUEST_LOCATION) { // Location permission request
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, set the location
                fetchUserLocation()
            } else {
                // Permission denied
                Toast.makeText(this, "Location permissions denied :(", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Check if location permissions are granted
    private fun checkLocationPermission() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions if not granted
            requestLocationPermissions()
        } else {
            // Permissions granted, fetch location
            fetchUserLocation()
        }
    }

    // Fetch and store the last known location of the user
    @SuppressLint("MissingPermission")
    private fun fetchUserLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    myLocation = location
                }
            }
    }

    // Request location permissions from the user
    private fun requestLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // The user has previously denied permissions
            Toast.makeText(this, "Permissions denied :(", Toast.LENGTH_SHORT).show()
        } else {
            // Request permissions
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION), MY_PERMISSION_REQUEST_LOCATION)
        }
    }

    // Update the UI with the user's information after successful registration
    private fun updateUserUI(user: FirebaseUser) {
        val intent = Intent(this, MapaActivity::class.java)
        intent.putExtra("user", user.email)
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }

    // Open the gallery to select an image for the user's profile
    fun openGalleryForImageSelection() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    // Handle the result of image selection from the gallery
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            // Get the selected image URI
            selectedImageUri = data.data
            // Display the selected image in the ImageView
            binding.imageViewContact.setImageURI(selectedImageUri)

            Toast.makeText(this, "Selected image: " + selectedImageUri?.lastPathSegment, Toast.LENGTH_SHORT).show()
        }
    }

    // Upload the user's selected profile image to Firebase Storage
    private fun uploadUserProfileImage() {
        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("images/${auth.currentUser?.uid}/profile_image.jpg")
            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    Log.i("FBApp", "Image successfully uploaded")
                }
                .addOnFailureListener { exception ->
                    Log.e("FBApp", "Failed to upload image", exception)
                }
        } else {
            Log.e("FBApp", "No image selected")
        }
    }
}
