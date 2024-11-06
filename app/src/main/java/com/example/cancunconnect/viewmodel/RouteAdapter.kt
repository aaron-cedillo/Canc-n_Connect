package com.example.cancunconnect.viewmodel

import com.example.cancunconnect.model.Route
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.cancunconnect.R

class RoutesAdapter(
    private val routes: List<Route>,
    private val onRouteClick: (Route) -> Unit
) : RecyclerView.Adapter<RoutesAdapter.RouteViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route, parent, false)
        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        val route = routes[position]
        holder.bind(route)
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val routeName: TextView = view.findViewById(R.id.route_name)
        private val routeSchedule: TextView = view.findViewById(R.id.route_schedule)
        private val routeIcon: ImageView = view.findViewById(R.id.route_icon)
        private val viewRouteButton: TextView = view.findViewById(R.id.view_route_button)

        fun bind(route: Route) {
            routeName.text = route.nombre
            routeSchedule.text = "Horarios: ${route.horarioInicio} - ${route.horarioFin}"

            // Cambia el ícono según el tipo de ruta (camión o combi)
            if (route.tipo == "camion") {
                routeIcon.setImageResource(R.drawable.ic_bus_blanco)
            } else {
                routeIcon.setImageResource(R.drawable.ic_van_blanco)
            }

            // Manejar el clic en "Ver ruta"
            viewRouteButton.setOnClickListener {
                onRouteClick(route)
            }
        }
    }
}
