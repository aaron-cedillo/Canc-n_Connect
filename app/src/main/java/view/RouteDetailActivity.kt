package view

import Model.Stop
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.util.Log
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cancunconnect.R
import viewmodel.StopsAdapter
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore

class RouteDetailActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var map: GoogleMap
    private lateinit var stopsRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    // Coordenada inicial de Cancún
    private val cancun = LatLng(21.1619, -86.8515)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        // Inicializar Firestore
        firestore = FirebaseFirestore.getInstance()

        // Configurar RecyclerView
        stopsRecyclerView = findViewById(R.id.stops_recycler_view)
        stopsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Configuración del botón de regreso
        val backToProfileContainer: LinearLayout = findViewById(R.id.back_to_routes_container)
        backToProfileContainer.setOnClickListener {
            val intent = Intent(this, RoutesActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Obtener y mostrar el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Cargar los detalles de la ruta
        loadRouteDetails()
    }

    private fun loadRouteDetails() {
        val routeId = intent.getStringExtra("route_id")
        Log.d("RouteDetailActivity", "route_id: $routeId")

        if (routeId != null) {
            firestore.collection("rutas").document(routeId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        Log.d("RouteDetailActivity", "Documento encontrado: ${document.id}")

                        // Obtener las paradas
                        val stopsData = document.get("stops") as? List<Map<String, Any>>
                        val stops = stopsData?.map { stopData ->
                            Stop(
                                name = stopData["name"] as String,
                                lat = stopData["lat"] as Double,
                                lng = stopData["lng"] as Double
                            )
                        } ?: emptyList()

                        Log.d("RouteDetailActivity", "Número de paradas: ${stops.size}")

                        // Obtener coordenadas de la ruta y dibujarla en el mapa
                        val coordinates = document.get("coordenadas") as? List<Map<String, Double>>
                        if (coordinates != null) {
                            drawRouteOnMap(coordinates)
                            Log.d("RouteDetailActivity", "Número de coordenadas: ${coordinates.size}")
                        } else {
                            Log.e("RouteDetailActivity", "Las coordenadas son nulas")
                        }

                        // Mostrar las paradas en el RecyclerView
                        stopsRecyclerView.adapter = StopsAdapter(stops)
                    } else {
                        Log.e("RouteDetailActivity", "El documento no existe")
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RouteDetailActivity", "Error al cargar el documento: $e")
                }
        } else {
            Log.e("RouteDetailActivity", "El route_id es nulo")
        }
    }

    private fun drawRouteOnMap(coordinates: List<Map<String, Double>>) {
        val polylineOptions = PolylineOptions()
            .width(10f)
            .color(android.graphics.Color.BLUE)

        for (coordinate in coordinates) {
            val lat = coordinate["lat"]
            val lng = coordinate["lng"]
            if (lat != null && lng != null) {
                polylineOptions.add(LatLng(lat, lng))
            }
        }
        map.addPolyline(polylineOptions)

        // Centrar la cámara en la primera coordenada
        if (coordinates.isNotEmpty()) {
            val firstPoint = coordinates[0]
            val lat = firstPoint["lat"]
            val lng = firstPoint["lng"]
            if (lat != null && lng != null) {
                map.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(lat, lng), 12f))
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap

        // Mueve la cámara a Cancún cuando el mapa esté listo
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cancun, 12.0f))

        // Verifica y solicita permisos de ubicación
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            showUserLocation()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
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
                    map.addMarker(
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
                showUserLocation()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }
}
