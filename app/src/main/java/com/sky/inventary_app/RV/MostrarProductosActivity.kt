package com.sky.inventary_app.RV

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.sky.inventary_app.MenuActivity
import com.sky.inventary_app.R
import dbProductos

class MostrarProductosActivity : AppCompatActivity() {

    private lateinit var rvProductos: RecyclerView
    private lateinit var productoAdapter: ProductoAdapter
    private lateinit var productoList: ArrayList<dbProductos>
    private lateinit var db: FirebaseFirestore
    private lateinit var tvListaVacia: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mostrar_productos)

        rvProductos = findViewById(R.id.rvProductos)
        tvListaVacia = findViewById(R.id.tvListaVacia) // Asegúrate de tener este TextView en tu layout
        rvProductos.layoutManager = LinearLayoutManager(this)
        productoList = ArrayList()
        productoAdapter = ProductoAdapter(productoList, this, tvListaVacia) // Pasa la vista para lista vacía
        rvProductos.adapter = productoAdapter

        db = FirebaseFirestore.getInstance()

        cargarProductos()
    }

    override fun onResume() {
        super.onResume()
        // Vuelve a cargar los productos cuando la actividad vuelve al primer plano
        cargarProductos()
    }

    private fun cargarProductos() {
        db.collection("Productos").get()
            .addOnSuccessListener { documents ->
                productoList.clear()
                for (document in documents) {
                    val producto = document.toObject(dbProductos::class.java)
                    productoList.add(producto)
                }
                // Ordena la lista por precioCompra en orden ascendente
                productoList.sortBy { it.precioVenta }
                productoAdapter.notifyDataSetChanged()
                productoAdapter.verificarListaVacia() // Actualiza la vista de lista vacía
            }
            .addOnFailureListener { exception ->
                // Manejar errores aquí
            }
    }


    override fun onBackPressed() {
        val intent = Intent(this, MenuActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(intent)
        finish()
    }

}

