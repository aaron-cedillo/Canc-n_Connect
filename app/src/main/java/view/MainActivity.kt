package view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.cancunconnect.R
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private val firestore = FirebaseFirestore.getInstance()
    private val combiRoutes: MutableMap<Marker, List<LatLng>> = mutableMapOf()
    private val busRoutes: MutableMap<Marker, List<LatLng>> = mutableMapOf()
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa el mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configura el Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    Toast.makeText(this, "Ya estás en Inicio", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.navigation_rutas -> {
                    val intent = Intent(this, RoutesActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_cuenta -> {
                    val intent = Intent(this, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        // Configura el botón SOS para mostrar el menú emergente
        val sosButton: Button = findViewById(R.id.button_sos)
        sosButton.setOnClickListener {
            showSOSMenu()
        }
    }

    private fun showSOSMenu() {
        // Opciones para el menú de emergencia
        val options = arrayOf("Llamar a una ambulancia", "Solicitar ayuda de emergencia")

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Opciones de SOS")
        builder.setItems(options) { _, which ->
            when (which) {
                0 -> {
                    // Acción para llamar a una ambulancia
                    Toast.makeText(this, "Llamando a una ambulancia...", Toast.LENGTH_SHORT).show()
                }
                1 -> {
                    // Acción para solicitar ayuda de emergencia
                    Toast.makeText(this, "Solicitando ayuda de emergencia...", Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configura la posición inicial del mapa
        val cancun = LatLng(21.1619, -86.8515)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cancun, 12.0f))

        // Activa la ubicación del usuario en el mapa si el permiso está concedido
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // Carga las rutas desde Firestore sin mostrarlas aún
        cargarRutasDesdeFirestore()
    }

    private fun cargarRutasDesdeFirestore() {
        firestore.collection("rutas")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val nombreRuta = document.getString("nombre_ruta") ?: "Ruta Desconocida"
                    val tipo = document.getString("tipo") ?: "combi"
                    val coordenadas = document.get("coordenadas") as? List<Map<String, Double>>

                    // Verifica que las coordenadas no sean nulas
                    if (coordenadas != null) {
                        val latLngList = coordenadas.mapNotNull {
                            val lat = it["lat"]
                            val lng = it["lng"]
                            if (lat != null && lng != null) LatLng(lat, lng) else null
                        }

                        // Agrega la ruta a la lista correspondiente con marcadores de diferentes colores
                        if (latLngList.isNotEmpty()) {
                            val markerOptions = MarkerOptions().position(latLngList[0]).title(nombreRuta)

                            // Agrega los marcadores según el tipo con colores distintos
                            if (tipo.equals("combi", ignoreCase = true)) {
                                val marker = mMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
                                combiRoutes[marker!!] = latLngList
                            } else if (tipo.equals("camion", ignoreCase = true)) {
                                val marker = mMap.addMarker(markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)))
                                busRoutes[marker!!] = latLngList
                            }
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar rutas", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showUserLocation() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val userLatLng = LatLng(location.latitude, location.longitude)

                // Intentamos obtener el recurso como Drawable y luego convertirlo a Bitmap
                val drawable = ContextCompat.getDrawable(this, R.drawable.user_location_icon)
                if (drawable != null) {
                    // Convertimos el drawable a un Bitmap
                    val bitmap = Bitmap.createBitmap(
                        drawable.intrinsicWidth,
                        drawable.intrinsicHeight,
                        Bitmap.Config.ARGB_8888
                    )
                    val canvas = Canvas(bitmap)
                    drawable.setBounds(0, 0, canvas.width, canvas.height)
                    drawable.draw(canvas)

                    // Usamos el Bitmap para crear el icono del marcador
                    val markerIcon = BitmapDescriptorFactory.fromBitmap(bitmap)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(userLatLng)
                            .title("Tu ubicación")
                            .icon(markerIcon)
                    )
                } else {
                    Toast.makeText(this, "Error: No se pudo cargar el icono de ubicación.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun showMarkersAndRoutes(routes: Map<Marker, List<LatLng>>, color: String) {
        mMap.clear()

        for ((marker, route) in routes) {
            mMap.addMarker(MarkerOptions().position(marker.position).title(marker.title))
            val polylineOptions = PolylineOptions().addAll(route).color(android.graphics.Color.parseColor(color)).width(10f)
            mMap.addPolyline(polylineOptions)
        }
    }

    private fun checkLocationPermission() {
        if (checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Ya tienes el permiso, realiza la operación que necesita la ubicación
            startLocationRelatedTask()
        }
    }

    private fun startLocationRelatedTask() {
        TODO("Not yet implemented")
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // Permiso concedido
                startLocationRelatedTask()
            } else {
                // Permiso denegado
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}
