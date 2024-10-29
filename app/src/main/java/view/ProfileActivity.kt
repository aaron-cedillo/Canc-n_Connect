package view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import java.util.Date
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cancunconnect.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.squareup.picasso.Picasso

class ProfileActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var storage: FirebaseStorage
    private lateinit var profileImageView: ImageView
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var editProfileImageButton: ImageButton
    private lateinit var changeToPremiumButton: Button

    private var imageUri: Uri? = null
    private val PICK_IMAGE_REQUEST = 71

    private var originalName: String = ""
    private var originalEmail: String = ""
    private var originalProfileImageUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Inicializa Firebase Auth, Firestore y Storage
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        storage = FirebaseStorage.getInstance()

        // Obtiene las vistas
        profileImageView = findViewById(R.id.profile_image)
        changeToPremiumButton = findViewById(R.id.button_change_to_premium)
        nameEditText = findViewById(R.id.edit_text_name)
        emailEditText = findViewById(R.id.edit_text_email)
        passwordEditText = findViewById(R.id.edit_text_password)
        saveButton = findViewById(R.id.button_save)
        editProfileImageButton = findViewById(R.id.button_edit_profile_image)

        // Deshabilitar botón guardar inicialmente
        saveButton.isEnabled = false

        // Cargar los datos del usuario
        loadUserData()

        // Configura el botón de editar imagen de perfil
        editProfileImageButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, PICK_IMAGE_REQUEST)
        }

        // Configura la lógica de los botones de edición
        findViewById<ImageButton>(R.id.button_edit_name).setOnClickListener {
            nameEditText.isEnabled = true
        }

        findViewById<ImageButton>(R.id.button_edit_email).setOnClickListener {
            emailEditText.isEnabled = true
        }

        findViewById<ImageButton>(R.id.button_edit_password).setOnClickListener {
            passwordEditText.isEnabled = true
        }

        // Detectar cambios en los campos de texto para habilitar el botón de guardar
        val textWatcher = object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                saveButton.isEnabled = (
                        nameEditText.text.toString() != originalName ||
                                emailEditText.text.toString() != originalEmail ||
                                imageUri != null
                        )
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        }

        nameEditText.addTextChangedListener(textWatcher)
        emailEditText.addTextChangedListener(textWatcher)

        // Configura el botón de guardar
        saveButton.setOnClickListener {
            saveUserData()
        }

        // Verificar si el usuario tiene una suscripción y actualizar el botón
        checkUserSubscription()

        // Configura el Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    // Navegar a la actividad MainActivity
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_rutas -> {
                    // Navegar a la actividad RoutesActivity (Rutas)
                    val intent = Intent(this, RoutesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_cuenta -> {
                    // No hacer nada porque ya estás en la pantalla de Perfil
                    Toast.makeText(this, "Ya estás en el Perfil", Toast.LENGTH_SHORT).show()
                    true
                }
                else -> false
            }
        }

        auth = FirebaseAuth.getInstance()

        val logoutButton: Button = findViewById(R.id.button_logout)

        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, SplashActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null && data.data != null) {
            imageUri = data.data
            profileImageView.setImageURI(imageUri)
            saveButton.isEnabled = true
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        originalName = document.getString("name") ?: ""
                        originalEmail = document.getString("email") ?: ""
                        originalProfileImageUrl = document.getString("profile_image") ?: ""

                        nameEditText.setText(originalName)
                        emailEditText.setText(originalEmail)

                        if (originalProfileImageUrl.isNotEmpty()) {
                            Picasso.get().load(originalProfileImageUrl).into(profileImageView)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("ProfileActivity", "Error al cargar los datos del usuario", e)
                    Toast.makeText(this, "Error al cargar los datos", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun saveUserData() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Por favor completa todos los campos.", Toast.LENGTH_SHORT).show()
                return
            }

            if (imageUri != null) {
                // Obtener la extensión del archivo seleccionado
                val fileExtension = contentResolver.getType(imageUri!!)?.split("/")?.last() ?: "jpg"

                // Construir la referencia de almacenamiento usando la extensión
                val ref = storage.reference.child("profile_image/$userId.$fileExtension")

                ref.putFile(imageUri!!)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { uri ->
                            saveDataToFirestore(userId, name, email, uri.toString())
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("ProfileActivity", "Error al subir la imagen", e)
                        Toast.makeText(this, "Error al subir la imagen", Toast.LENGTH_SHORT).show()
                    }
            } else {
                saveDataToFirestore(userId, name, email, originalProfileImageUrl)
            }
        }
    }


    private fun saveDataToFirestore(userId: String, name: String, email: String, profileImageUrl: String) {
        val userUpdates: Map<String, Any> = mapOf(
            "name" to name,
            "email" to email,
            "profile_image" to profileImageUrl
        )

        firestore.collection("users").document(userId)
            .update(userUpdates)
            .addOnSuccessListener {
                Toast.makeText(this, "Datos guardados exitosamente", Toast.LENGTH_SHORT).show()
                saveButton.isEnabled = false
            }
            .addOnFailureListener { e ->
                Log.w("ProfileActivity", "Error al guardar los datos del usuario", e)
                Toast.makeText(this, "Error al guardar los datos", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserSubscription() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("suscripciones").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists() && document.getString("estado_suscripcion") == "activa") {
                        // Configura el botón para verificar la suscripción
                        changeToPremiumButton.text = "Verificar Suscripción"
                        changeToPremiumButton.setOnClickListener {
                            // Enviar a SubscriptionActivity en modo de verificación
                            val intent = Intent(this, SubscriptionActivity::class.java)
                            intent.putExtra("isVerification", true) // Añade el extra para verificar
                            startActivity(intent)
                        }
                    } else {
                        // Configura el botón para cambiar a Premium
                        changeToPremiumButton.text = "Cambiar a Premium"
                        changeToPremiumButton.setOnClickListener {
                            // Enviar a SubscriptionActivity en modo de selección de plan
                            val intent = Intent(this, SubscriptionActivity::class.java)
                            startActivity(intent)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileActivity", "Error al verificar la suscripción", e)
                }
        }
    }


    private fun showSubscriptionDetails(fechaFin: Date?) {
        Toast.makeText(this, "Suscripción activa hasta: $fechaFin", Toast.LENGTH_SHORT).show()
    }


    private fun showSubscriptionDetails() {
        Toast.makeText(this, "Mostrando detalles de la suscripción...", Toast.LENGTH_SHORT).show()
    }
}
