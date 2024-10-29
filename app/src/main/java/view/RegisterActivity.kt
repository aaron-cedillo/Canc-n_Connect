package view

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.MotionEvent
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.cancunconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var nameEditText: EditText
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var registerButton: Button
    private lateinit var loginText: TextView

    private var isPasswordVisible = false
    private var isConfirmPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializa Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Obtiene las vistas
        nameEditText = findViewById(R.id.name_edit_text)
        emailEditText = findViewById(R.id.email_edit_text)
        passwordEditText = findViewById(R.id.password_edit_text)
        confirmPasswordEditText = findViewById(R.id.confirm_password_edit_text)
        registerButton = findViewById(R.id.register_button)
        loginText = findViewById(R.id.login_text)

        // Configura la visibilidad de la contraseña
        passwordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (passwordEditText.right - passwordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility(passwordEditText, isPasswordVisible)
                    isPasswordVisible = !isPasswordVisible
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Configura la visibilidad de la confirmación de contraseña
        confirmPasswordEditText.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                if (event.rawX >= (confirmPasswordEditText.right - confirmPasswordEditText.compoundDrawables[2].bounds.width())) {
                    togglePasswordVisibility(confirmPasswordEditText, isConfirmPasswordVisible)
                    isConfirmPasswordVisible = !isConfirmPasswordVisible
                    return@setOnTouchListener true
                }
            }
            false
        }

        // Configura el botón de registro
        registerButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()
            val confirmPassword = confirmPasswordEditText.text.toString()

            if (password == confirmPassword) {
                registerUser(name, email, password)
            } else {
                Toast.makeText(this, "Las contraseñas no coinciden", Toast.LENGTH_SHORT).show()
            }
        }

        // Configura la opción de inicio de sesión
        loginText.setOnClickListener {
            val intent = Intent(this, SplashActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun togglePasswordVisibility(editText: EditText, isVisible: Boolean) {
        if (isVisible) {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility_off, 0)
        } else {
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            editText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_visibility, 0)
        }
        editText.setSelection(editText.text.length) // Coloca el cursor al final del texto
    }

    private fun registerUser(name: String, email: String, password: String) {
        // Verifica que los campos no estén vacíos
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(baseContext, "Por favor completa todos los campos.", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("RegisterActivity", "Attempting to register user: $email") // Log de intento de registro

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registro exitoso, guardar los datos en Firestore
                    val userId = auth.currentUser?.uid
                    val user = hashMapOf(
                        "name" to name,
                        "email" to email
                    )

                    userId?.let {
                        firestore.collection("users").document(it).set(user)
                            .addOnSuccessListener {
                                Log.d("RegisterActivity", "User data saved to Firestore")
                                Toast.makeText(baseContext, "Registro exitoso.", Toast.LENGTH_SHORT).show()
                                // Redirige a la pantalla de inicio de sesión
                                val intent = Intent(this, SplashActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Log.w("RegisterActivity", "Error saving user data", e)
                                Toast.makeText(baseContext, "Registro exitoso, pero error al guardar datos: ${e.message}", Toast.LENGTH_SHORT).show()
                                // Redirige a la pantalla de inicio de sesión también en caso de error
                                val intent = Intent(this, SplashActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                    }
                } else {
                    // Si el registro falla, muestra un mensaje.
                    Log.w("RegisterActivity", "createUser:failure", task.exception)
                    Toast.makeText(baseContext, "Registro fallido: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}
