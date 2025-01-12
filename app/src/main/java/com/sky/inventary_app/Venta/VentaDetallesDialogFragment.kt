package com.example.tuapp

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.google.firebase.firestore.FirebaseFirestore
import dbProductos
import com.sky.inventary_app.Data.dbVenta
import com.sky.inventary_app.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class VentaDetallesDialogFragment : DialogFragment() {
    private lateinit var db: FirebaseFirestore
    private var ventaHecha: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            ventaHecha = it.getBoolean("ventaHecha", false)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflar el layout para este fragmento
        return inflater.inflate(R.layout.dialog_venta_detalles, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        // Obtener referencias a las vistas
        val tvIdVenta: TextView = view.findViewById(R.id.tvIdVenta)
        val tvTotalVenta: TextView = view.findViewById(R.id.tvTotalVenta)
        val tvFechaVenta: TextView = view.findViewById(R.id.tvFechaVenta)
        val tvNombreComprador: TextView = view.findViewById(R.id.tvNombreComprador)
        val tvIdVendedor: TextView = view.findViewById(R.id.tvIdVendedor)
        val btnClose: Button = view.findViewById(R.id.btnClose)
        val btnSave: Button = view.findViewById(R.id.btnSave)

        // Configurar el botón de cerrar
        btnClose.setOnClickListener {
            dismiss()
        }

        btnSave.setOnClickListener {
            // Finalizar la compra
            finalizarCompra()
        }

        // Leer los datos de SharedPreferences
        val sharedPreferences = activity?.getSharedPreferences("VentaPrefs", Context.MODE_PRIVATE)
        val productosJson = sharedPreferences?.getString("productos", "[]")
        val idVenta = sharedPreferences?.getString("idVenta", "ID no disponible")
        val totalVenta = sharedPreferences?.getString("totalVenta", "0.0")?.toDoubleOrNull() ?: 0.0
        val nombreComprador = sharedPreferences?.getString("nombreComprador", "Nombre no disponible")

        // Leer el idUsuario de SharedPreferences
        val userSharedPreferences = activity?.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val idVendedor = userSharedPreferences?.getString("nombreUsuario", "Vendedor no disponible")

        // Configurar las vistas con los datos de la venta
        tvTotalVenta.text = "Total: S/.${String.format("%.2f", totalVenta)}"
        tvFechaVenta.text = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        tvNombreComprador.text = "Comprador: $nombreComprador"
        tvIdVendedor.text = "Vendedor: $idVendedor"

        asignarIdVenta { generatedIdVenta ->
            tvIdVenta.text = "ID de la Venta: $generatedIdVenta"
        }
    }

    private fun finalizarCompra() {
        val sharedPreferences = activity?.getSharedPreferences("VentaPrefs", Context.MODE_PRIVATE)
        val productosJson = sharedPreferences?.getString("productos", "[]")
        val totalVenta = sharedPreferences?.getString("totalVenta", "0.0")?.toDoubleOrNull() ?: 0.0
        val nombreComprador = sharedPreferences?.getString("nombreComprador", "Nombre no disponible")

        val userSharedPreferences = activity?.getSharedPreferences("MyAppPrefs", Context.MODE_PRIVATE)
        val idVendedor = userSharedPreferences?.getString("nombreUsuario", "Vendedor no disponible")

        asignarIdVenta { generatedIdVenta ->
            val venta = dbVenta(
                idVenta = generatedIdVenta,
                totalVenta = totalVenta,
                fechaVenta = Date(),
                nombreComprador = nombreComprador ?: "",
                idVendedor = idVendedor ?: "",
                productos = deserializeProductos(productosJson ?: "[]")
            )

            db.collection("Ventas").document(generatedIdVenta).set(venta)
                .addOnSuccessListener {
                    Toast.makeText(context, "Venta registrada:", Toast.LENGTH_LONG).show()
                    // Enviar el resultado a la actividad
                    sendResult(true)
                    dismiss()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Error al registrar la venta: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("Firebase", "Error al registrar la venta", e)
                    // Enviar el resultado a la actividad como false
                    sendResult(false)
                    dismiss()
                }
        }
    }

    private fun sendResult(ventaHecha: Boolean) {
        targetFragment?.onActivityResult(targetRequestCode, if (ventaHecha) Activity.RESULT_OK else Activity.RESULT_CANCELED, null)
    }

    private fun asignarIdVenta(callback: (String) -> Unit) {
        db.collection("Ventas")
            .get()
            .addOnSuccessListener { result ->
                val idVenta = result.size() + 1
                callback(idVenta.toString())
            }
            .addOnFailureListener { exception ->
                Log.w("VentaDetallesDialog", "Error obteniendo documentos.", exception)
            }
    }

    private fun deserializeProductos(productosJson: String): List<dbProductos> {
        val gson = Gson()
        val listType = object : TypeToken<List<dbProductos>>() {}.type
        return gson.fromJson(productosJson, listType)
    }

    companion object {
        // Este método puede ser usado para crear una nueva instancia del dialogo
        fun newInstance(ventaHecha: Boolean): VentaDetallesDialogFragment {
            val fragment = VentaDetallesDialogFragment()
            val args = Bundle().apply {
                putBoolean("ventaHecha", ventaHecha)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun actualizarStockProductos(productos: List<Pair<dbProductos, Int>>) {
        for ((producto, cantidadVendida) in productos) {
            val nuevaCantidad = producto.stock - cantidadVendida
            if (nuevaCantidad >= 0) {
                db.collection("Productos").document(producto.idProducto)
                    .update("stock", nuevaCantidad)
                    .addOnSuccessListener {
                        Log.d("VentaDetallesDialog", "Stock actualizado para el producto: ${producto.nombreProducto}")
                    }
                    .addOnFailureListener { e ->
                        Log.e("VentaDetallesDialog", "Error actualizando stock para el producto: ${producto.nombreProducto}", e)
                    }
            } else {
                Log.w("VentaDetallesDialog", "No hay suficiente stock para el producto: ${producto.nombreProducto}")
            }
        }
    }


}
