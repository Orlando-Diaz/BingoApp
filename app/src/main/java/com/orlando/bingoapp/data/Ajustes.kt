package com.orlando.bingoapp.data

import android.content.Context

/**
 * Ajustes simples guardados en SharedPreferences (pares clave-valor en el teléfono),
 * parecido a un application.properties que se puede cambiar desde la app.
 */
class Ajustes(context: Context) {

    private val prefs = context.getSharedPreferences("ajustes", Context.MODE_PRIVATE)

    /** Precio de un tablero de 4 tablas en los sencillos (el de 8 vale el doble). */
    var precioCuatroSencillo: Int
        get() = prefs.getInt(CLAVE_SENCILLO, 500)
        set(valor) {
            prefs.edit().putInt(CLAVE_SENCILLO, valor).commit() // commit: se guarda en el momento
        }

    /** Precio de un tablero de 4 tablas en las llenas. */
    var precioCuatroLlena: Int
        get() = prefs.getInt(CLAVE_LLENA, 1000)
        set(valor) {
            prefs.edit().putInt(CLAVE_LLENA, valor).commit()
        }

    private companion object {
        const val CLAVE_SENCILLO = "precio4_sencillo"
        const val CLAVE_LLENA = "precio4_llena"
    }
}
