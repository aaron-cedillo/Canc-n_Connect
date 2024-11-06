package com.example.cancunconnect.viewmodel

import com.example.cancunconnect.model.Stop
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cancunconnect.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.example.cancunconnect.view.RoutesActivity

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var stopsRecyclerView: RecyclerView
    private lateinit var firestore: FirebaseFirestore
    private var stopsList: MutableList<Stop> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Inicializar Firestore
        firestore = FirebaseFirestore.getInstance()

        // Inicializar el RecyclerView de las paradas
        stopsRecyclerView = findViewById(R.id.stops_recycler_view)
        stopsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Inicializar el adaptador
        val stopsAdapter = StopsAdapter(stopsList)
        stopsRecyclerView.adapter = stopsAdapter

        // Obtener el mapa
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Configurar el botón de regreso
        val backToProfileButton: TextView = findViewById(R.id.back_to_profile)
        backToProfileButton.setOnClickListener {
            // Navega de regreso a la pantalla de perfil
            val intent = Intent(this, RoutesActivity::class.java)
            startActivity(intent)
            finish() // Finaliza la actividad actual para evitar regresar
        }

        // Cargar las paradas y el recorrido desde Firestore
        loadRouteData()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Configurar la posición inicial del mapa (Cancún)
        val cancun = LatLng(21.1619, -86.8515)
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(cancun, 12.0f))

        // Cargar los datos del recorrido
        loadRouteData()
    }

    private fun loadRouteData() {
        val routeId = intent.getStringExtra("route_id")

        // Obtener el recorrido y las paradas desde Firestore
        firestore.collection("rutas").document(routeId!!)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Obtener las coordenadas de la ruta (GeoPoints)
                    val coordinates = document.get("coordenadas") as List<GeoPoint>

                    // Dibujar la línea en el mapa
                    val polylineOptions = PolylineOptions()
                    for (coordinate in coordinates) {
                        val latLng = LatLng(coordinate.latitude, coordinate.longitude)
                        polylineOptions.add(latLng)
                    }
                    mMap.addPolyline(polylineOptions)

                    // Cargar las paradas importantes
                    val stops = document.get("stops") as List<Map<String, Any>>
                    for (stop in stops) {
                        val stopName = stop["name"] as? String ?: "Parada desconocida"
                        val lat = stop["lat"] as? Double ?: 0.0
                        val lng = stop["lng"] as? Double ?: 0.0
                        stopsList.add(Stop(stopName, lat, lng))
                    }

                    // Actualizar el adaptador
                    stopsRecyclerView.adapter?.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                // Manejo de errores
            }
    }
}
