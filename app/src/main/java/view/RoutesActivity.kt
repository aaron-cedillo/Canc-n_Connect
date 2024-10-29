package view

import Model.Route
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cancunconnect.R
import viewmodel.RoutesAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.FirebaseFirestore

class RoutesActivity : AppCompatActivity() {

    private lateinit var routesRecyclerView: RecyclerView
    private lateinit var routesAdapter: RoutesAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var searchRoute: EditText
    private var routesList: MutableList<Route> = mutableListOf()
    private var filteredRoutesList: MutableList<Route> = mutableListOf() // Lista filtrada

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_routes)

        // Inicializa Firestore
        firestore = FirebaseFirestore.getInstance()

        // Inicializa las vistas
        searchRoute = findViewById(R.id.search_route)
        routesRecyclerView = findViewById(R.id.routes_recycler_view)
        routesRecyclerView.layoutManager = LinearLayoutManager(this)

        // Configura el adaptador
        routesAdapter = RoutesAdapter(filteredRoutesList) { route ->
            val routeId = route.id

            // Verificación de depuración
            Log.d("RoutesActivity", "route_id: $routeId")

            if (routeId != null) {
                val intent = Intent(this, RouteDetailActivity::class.java)
                intent.putExtra("route_id", routeId)
                startActivity(intent)
            } else {
                Log.e("RoutesActivity", "El route_id es nulo")
            }
        }
        routesRecyclerView.adapter = routesAdapter

        // Cargar datos desde Firestore
        loadRoutesFromFirestore()

        // Configura el filtro de búsqueda
        searchRoute.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                filterRoutes(s.toString())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Configura el Bottom Navigation
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.navigation_rutas -> {
                    Toast.makeText(this, "Ya estás en Rutas", Toast.LENGTH_SHORT).show()
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
    }

    // Función para cargar las rutas desde Firestore
    private fun loadRoutesFromFirestore() {
        firestore.collection("rutas")
            .get()
            .addOnSuccessListener { documents ->
                routesList.clear() // Limpiar la lista antes de agregar nuevos datos
                for (document in documents) {
                    val route = Route(
                        id = document.id,
                        nombre = document.getString("nombre_ruta") ?: "Ruta Desconocida",
                        horarioInicio = document.getString("horario_inicio") ?: "6:00 AM",
                        horarioFin = document.getString("horario_fin") ?: "10:00 PM",
                        tipo = document.getString("tipo") ?: "camion"
                    )
                    routesList.add(route)
                }
                filteredRoutesList.addAll(routesList) // Iniciar la lista filtrada con todos los elementos
                routesAdapter.notifyDataSetChanged() // Notificamos al adaptador para que actualice la lista
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al cargar rutas", Toast.LENGTH_SHORT).show()
            }
    }

    // Función para filtrar las rutas en función del texto ingresado
    private fun filterRoutes(query: String) {
        val lowerCaseQuery = query.lowercase()
        filteredRoutesList.clear()
        if (lowerCaseQuery.isEmpty()) {
            filteredRoutesList.addAll(routesList) // Muestra todas las rutas si el campo de búsqueda está vacío
        } else {
            routesList.forEach { route ->
                if (route.nombre.lowercase().contains(lowerCaseQuery)) {
                    filteredRoutesList.add(route)
                }
            }
        }
        routesAdapter.notifyDataSetChanged() // Notificar al adaptador que los datos han cambiado
    }
}
