package com.example.cancunconnect.view

import com.example.cancunconnect.model.Stop
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
import com.example.cancunconnect.viewmodel.StopsAdapter
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

    private val cancun = LatLng(21.1619, -86.8515)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_route_detail)

        firestore = FirebaseFirestore.getInstance()
        stopsRecyclerView = findViewById(R.id.stops_recycler_view)
        stopsRecyclerView.layoutManager = LinearLayoutManager(this)

        val backToProfileContainer: LinearLayout = findViewById(R.id.back_to_routes_container)
        backToProfileContainer.setOnClickListener {
            val intent = Intent(this, RoutesActivity::class.java)
            startActivity(intent)
            finish()
        }

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadRouteDetails()
    }

    private fun loadRouteDetails() {
        val routeId = intent.getStringExtra("route_id")
        if (routeId != null) {
            firestore.collection("rutas").document(routeId).get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        val stopsData = document.get("stops") as? List<Map<String, Any>>
                        val stops = stopsData?.map { stopData ->
                            Stop(
                                name = stopData["name"] as String,
                                lat = stopData["lat"] as Double,
                                lng = stopData["lng"] as Double
                            )
                        } ?: emptyList()

                        val coordinates = document.get("coordenadas") as? List<Map<String, Double>>
                        if (coordinates != null) {
                            drawRouteOnMap(coordinates)
                        }

                        stopsRecyclerView.adapter = StopsAdapter(stops)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("RouteDetailActivity", "Error al cargar el documento: $e")
                }
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
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(cancun, 12.0f))

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            map.isMyLocationEnabled = true
            startLocationRelatedTask()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }
    }

    private fun startLocationRelatedTask() {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                } else {
                    Toast.makeText(this, "No se pudo obtener la ubicación actual", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                startLocationRelatedTask()
            } else {
                Toast.makeText(this, "Permiso de ubicación denegado", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
