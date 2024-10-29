package view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.stripe.android.PaymentConfiguration
import com.stripe.android.view.CardInputWidget
import com.google.android.gms.wallet.PaymentsClient
import com.google.android.gms.wallet.Wallet
import com.google.android.gms.wallet.PaymentDataRequest
import com.google.android.gms.wallet.AutoResolveHelper
import com.google.android.gms.wallet.WalletConstants
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.LinearLayout
import com.example.cancunconnect.R
import org.json.JSONArray
import org.json.JSONObject
import java.util.*

class SubscriptionActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedPlan: String = "mensual"

    // Google Pay Client
    private lateinit var paymentsClient: PaymentsClient
    private lateinit var subscriptionInfoTextView: TextView
    private lateinit var startPlanButton: Button
    private lateinit var verifySubscriptionButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        subscriptionInfoTextView = findViewById(R.id.subscription_info_text_view)
        startPlanButton = findViewById(R.id.start_plan_button)
        verifySubscriptionButton = findViewById(R.id.verify_subscription_button)

        // Inicializa Stripe con tu clave pública
        PaymentConfiguration.init(
            applicationContext,
            "pk_test_51QBusaB2sUU9VXMG1D884mBuURCvoABQbh3wK76My2AMtZ7irI1dBgI9cWVrLssN8vPk26Jgdmm5U50zvWgq5RYV00PjXiMVVu"
        )

        // Inicializa Google Pay
        paymentsClient = Wallet.getPaymentsClient(
            this,
            Wallet.WalletOptions.Builder().setEnvironment(WalletConstants.ENVIRONMENT_TEST).build()
        )

        verifySubscriptionStatus()

        startPlanButton.setOnClickListener {
            simulateCardPayment()
        }

        verifySubscriptionButton.setOnClickListener {
            showSubscriptionInfo()
        }

        val monthlyButton: Button = findViewById(R.id.monthly_plan_button)
        val yearlyButton: Button = findViewById(R.id.yearly_plan_button)
        val googlePayButton: Button = findViewById(R.id.google_pay_button)

        monthlyButton.setOnClickListener {
            selectedPlan = "mensual"
            Toast.makeText(this, "Plan mensual seleccionado", Toast.LENGTH_SHORT).show()
        }

        yearlyButton.setOnClickListener {
            selectedPlan = "anual"
            Toast.makeText(this, "Plan anual seleccionado", Toast.LENGTH_SHORT).show()
        }

        googlePayButton.setOnClickListener {
            createGooglePayRequest()
        }

        // Configuración del botón de regreso
        val backToProfileContainer: LinearLayout = findViewById(R.id.back_to_profile_container)
        backToProfileContainer.setOnClickListener {
            val intent = Intent(this, ProfileActivity::class.java)
            startActivity(intent)
            finish()
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    true
                }
                R.id.navigation_rutas -> {
                    startActivity(Intent(this, RoutesActivity::class.java))
                    true
                }
                R.id.navigation_cuenta -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun verifySubscriptionStatus() {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            firestore.collection("suscripciones").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val tipoSuscripcion = document.getString("tipo_suscripcion") ?: "Desconocido"
                        val fechaFin = document.getTimestamp("fecha_fin")?.toDate()
                        subscriptionInfoTextView.text = "Suscripción: $tipoSuscripcion\nVence el: $fechaFin"
                        subscriptionInfoTextView.visibility = View.VISIBLE

                        // Ocultar opciones de pago y botón de verificar
                        findViewById<LinearLayout>(R.id.payment_options_container).visibility = View.GONE
                        verifySubscriptionButton.visibility = View.GONE // Oculta el botón de verificar
                    } else {
                        findViewById<LinearLayout>(R.id.payment_options_container).visibility = View.VISIBLE
                        subscriptionInfoTextView.visibility = View.GONE
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al verificar suscripción", Toast.LENGTH_SHORT).show()
                }
        }
    }




    private fun showSubscriptionInfo() {
        // Muestra la información de suscripción en lugar de las opciones de pago
        subscriptionInfoTextView.visibility = View.VISIBLE
        startPlanButton.visibility = View.GONE
    }

    private fun simulateCardPayment() {
        val cardInputWidget = findViewById<CardInputWidget>(R.id.cardInputWidget)
        val params = cardInputWidget.paymentMethodCreateParams

        if (params != null) {
            Toast.makeText(this, "Simulando pago exitoso con tarjeta...", Toast.LENGTH_SHORT).show()
            val simulatedPaymentIntentId = "simulated_payment_intent_id_${UUID.randomUUID()}"
            saveSubscriptionToFirestore(simulatedPaymentIntentId)
        } else {
            Toast.makeText(this, "Por favor completa todos los detalles de pago.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createGooglePayRequest() {
        val paymentDataRequestJson = getGooglePayRequestJson()
        val paymentDataRequest = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())

        paymentDataRequest?.let {
            AutoResolveHelper.resolveTask(
                paymentsClient.loadPaymentData(it),
                this,
                LOAD_PAYMENT_DATA_REQUEST_CODE
            )
        }
    }

    private fun getGooglePayRequestJson(): JSONObject {
        val baseRequest = JSONObject().apply {
            put("apiVersion", 2)
            put("apiVersionMinor", 0)
        }

        val allowedAuthMethods = JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS")
        val allowedCardNetworks = JSONArray().put("MASTERCARD").put("VISA")

        val tokenizationSpecification = JSONObject().apply {
            put("type", "PAYMENT_GATEWAY")
            put("parameters", JSONObject().apply {
                put("gateway", "stripe")
                put("stripe:version", "2020-08-27")
                put("stripe:publishableKey", "tu_clave_publica")
            })
        }

        val cardPaymentMethod = JSONObject().apply {
            put("type", "CARD")
            put("parameters", JSONObject().apply {
                put("allowedAuthMethods", allowedAuthMethods)
                put("allowedCardNetworks", allowedCardNetworks)
                put("billingAddressRequired", true)
            })
            put("tokenizationSpecification", tokenizationSpecification)
        }

        val transactionInfo = JSONObject().apply {
            put("totalPrice", if (selectedPlan == "mensual") "100.00" else "1500.00")
            put("totalPriceStatus", "FINAL")
            put("currencyCode", "MXN")
        }

        val merchantInfo = JSONObject().apply {
            put("merchantName", "CancunConnect")
        }

        return baseRequest.apply {
            put("allowedPaymentMethods", JSONArray().put(cardPaymentMethod))
            put("transactionInfo", transactionInfo)
            put("merchantInfo", merchantInfo)
        }
    }

    companion object {
        const val LOAD_PAYMENT_DATA_REQUEST_CODE = 991
    }

    private fun saveSubscriptionToFirestore(paymentIntentId: String) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            val subscriptionData = hashMapOf(
                "tipo_suscripcion" to selectedPlan,
                "fecha_inicio" to com.google.firebase.Timestamp.now(),
                "fecha_fin" to calculateEndDate(selectedPlan),
                "monto_pagado" to if (selectedPlan == "mensual") 100.00 else 1500.00,
                "estado_suscripcion" to "activa",
                "auto_renovacion" to true,
                "pago_id" to paymentIntentId
            )

            firestore.collection("suscripciones").document(userId)
                .set(subscriptionData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Suscripción guardada exitosamente", Toast.LENGTH_SHORT).show()
                    verifySubscriptionStatus() // Actualiza el estado de la suscripción
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Error al guardar la suscripción", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun calculateEndDate(plan: String): com.google.firebase.Timestamp {
        val calendar = Calendar.getInstance()
        if (plan == "mensual") {
            calendar.add(Calendar.MONTH, 1)
        } else {
            calendar.add(Calendar.YEAR, 1)
        }
        return com.google.firebase.Timestamp(calendar.time)
    }
}
