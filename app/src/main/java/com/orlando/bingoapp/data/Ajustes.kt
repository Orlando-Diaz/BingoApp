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

    /** Modo automático: cada cuántos segundos sale una balota. */
    var segundosEntreBalotas: Int
        get() = prefs.getInt(CLAVE_SEGUNDOS, 8)
        set(valor) {
            prefs.edit().putInt(CLAVE_SEGUNDOS, valor).apply()
        }

    /** Modo automático: si la app canta en voz alta. */
    var vozActiva: Boolean
        get() = prefs.getBoolean(CLAVE_VOZ, true)
        set(valor) {
            prefs.edit().putBoolean(CLAVE_VOZ, valor).apply()
        }

    /** Nombre de la voz elegida en el menú de voces (null = la de por defecto). */
    var vozNombre: String?
        get() = prefs.getString(CLAVE_VOZ_NOMBRE, null)
        set(valor) {
            prefs.edit().putString(CLAVE_VOZ_NOMBRE, valor).apply()
        }

    /** Tono de la voz: 1 = normal, menos = más grave. */
    var tonoVoz: Float
        get() = prefs.getFloat(CLAVE_TONO, 1f)
        set(valor) {
            prefs.edit().putFloat(CLAVE_TONO, valor).apply()
        }

    /** Huella (hash) del patrón que protege la pestaña Tableros; null = todavía no se ha creado. */
    var patronTableros: String?
        get() = prefs.getString(CLAVE_PATRON, null)
        set(valor) {
            prefs.edit().putString(CLAVE_PATRON, valor).commit()
        }

    private companion object {
        const val CLAVE_PATRON = "patron_tableros"
        const val CLAVE_VOZ_NOMBRE = "voz_nombre"
        const val CLAVE_TONO = "tono_voz"
        const val CLAVE_SENCILLO = "precio4_sencillo"
        const val CLAVE_LLENA = "precio4_llena"
        const val CLAVE_SEGUNDOS = "segundos_entre_balotas"
        const val CLAVE_VOZ = "voz_activa"
    }
}
