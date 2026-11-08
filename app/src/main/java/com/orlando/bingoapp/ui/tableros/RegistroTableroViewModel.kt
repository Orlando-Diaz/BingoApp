package com.orlando.bingoapp.ui.tableros

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.camara.LectorFotoTabla
import com.orlando.bingoapp.camara.MENSAJE_FOTO_FALLIDA
import com.orlando.bingoapp.camara.llenarCasillas
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.domain.ReglasBingo
import com.orlando.bingoapp.domain.ValidadorTabla
import kotlinx.coroutines.launch
import java.io.File

enum class PasoRegistro { DATOS, TABLAS }

/**
 * Guarda el estado del registro de un tablero mientras se llena.
 * Al vivir en el ViewModel, no se pierde si se gira el teléfono.
 */
class RegistroTableroViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).tableroDao()

    var paso by mutableStateOf(PasoRegistro.DATOS)
        private set
    var numeroTexto by mutableStateOf("")
        private set
    var cantidadTablas by mutableStateOf(4)
        private set
    var tablaActual by mutableStateOf(0)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var mostrarVacias by mutableStateOf(false)
        private set
    var guardado by mutableStateOf(false)
        private set

    // Una lista de 25 textos por cada tabla del tablero
    private var tablas: List<SnapshotStateList<String>> = emptyList()

    // ---------- Cámara ----------
    private val lector = LectorFotoTabla(application)

    /** Ruta de la foto de cada tabla (null si se escribió a mano). */
    private var fotos: SnapshotStateList<String?> = mutableStateListOf()

    /** Archivo donde la cámara está guardando la foto en este momento. */
    private var fotoPendiente: File? = null

    var leyendoFoto by mutableStateOf(false)
        private set
    var mensajeFoto by mutableStateOf<String?>(null)
        private set

    val fotoActual: String?
        get() = fotos.getOrNull(tablaActual)

    val casillasActuales: SnapshotStateList<String>
        get() = tablas[tablaActual]

    fun reiniciar() {
        paso = PasoRegistro.DATOS
        numeroTexto = ""
        cantidadTablas = 4
        tablaActual = 0
        mensajeError = null
        mostrarVacias = false
        guardado = false
        tablas = emptyList()
        fotos = mutableStateListOf()
        mensajeFoto = null
    }

    fun cambiarNumero(texto: String) {
        numeroTexto = texto.filter { it.isDigit() }.take(4)
        mensajeError = null
    }

    fun cambiarCantidad(cantidad: Int) {
        cantidadTablas = cantidad
    }

    fun confirmarDatos() {
        val numero = numeroTexto.toIntOrNull()
        if (numero == null || numero <= 0) {
            mensajeError = "Escribe el número del tablero"
            return
        }
        viewModelScope.launch {
            if (dao.buscarPorNumero(numero) != null) {
                mensajeError = "Ya existe un tablero con el número $numero"
                return@launch
            }
            // Solo se crean casillas nuevas si cambió la cantidad (para no borrar lo ya escrito)
            if (tablas.size != cantidadTablas) {
                tablas = List(cantidadTablas) { mutableStateListOf(*Array(25) { "" }) }
                fotos = mutableStateListOf(*arrayOfNulls<String>(cantidadTablas))
            }
            tablaActual = 0
            mensajeError = null
            paso = PasoRegistro.TABLAS
        }
    }

    fun cambiarCasilla(indice: Int, texto: String) {
        casillasActuales[indice] = texto.filter { it.isDigit() }.take(2)
    }

    fun erroresActuales(): Map<Int, String> {
        val numeros = casillasActuales.map { it.toIntOrNull() }
        return if (mostrarVacias) ValidadorTabla.errores(numeros)
        else ValidadorTabla.erroresVisibles(numeros)
    }

    /** Prepara el archivo y devuelve la dirección que se le pasa a la cámara. */
    fun prepararFoto(): Uri {
        val archivo = lector.nuevoArchivo()
        fotoPendiente = archivo
        return lector.uriPara(archivo)
    }

    /** La cámara terminó: si tomó la foto, se leen los números y se llena la cuadrícula. */
    fun fotoTomada(exito: Boolean) {
        val archivo = fotoPendiente ?: return
        fotoPendiente = null
        if (!exito) return
        val indiceTabla = tablaActual

        viewModelScope.launch {
            leyendoFoto = true
            mensajeFoto = null
            try {
                mensajeFoto = lector.llenarCasillas(archivo, tablas[indiceTabla])
                fotos[indiceTabla] = archivo.path
                mostrarVacias = true
            } catch (e: Exception) {
                mensajeFoto = MENSAJE_FOTO_FALLIDA
            } finally {
                leyendoFoto = false
            }
        }
    }

    fun siguiente() {
        val errores = ValidadorTabla.errores(casillasActuales.map { it.toIntOrNull() })
        if (errores.isNotEmpty()) {
            mostrarVacias = true
            mensajeError = "Revisa las casillas marcadas en rojo"
            return
        }
        mostrarVacias = false
        mensajeError = null
        mensajeFoto = null
        if (tablaActual < cantidadTablas - 1) tablaActual++ else guardar()
    }

    fun anterior() {
        mostrarVacias = false
        mensajeError = null
        mensajeFoto = null
        if (tablaActual > 0) tablaActual-- else paso = PasoRegistro.DATOS
    }

    private fun guardar() {
        val numero = numeroTexto.toInt()
        viewModelScope.launch {
            val tablero = Tablero(numero = numero, cantidadTablas = cantidadTablas)
            val tablasAGuardar = tablas.mapIndexed { i, casillas ->
                Tabla(
                    posicion = i + 1,
                    fotoPath = fotos.getOrNull(i),
                    numeros = casillas.mapIndexed { indice, texto ->
                        if (indice == ReglasBingo.INDICE_CENTRO) Tabla.CENTRO_LIBRE else texto.toInt()
                    }
                )
            }
            dao.guardarTableroConTablas(tablero, tablasAGuardar)
            guardado = true
        }
    }

    override fun onCleared() {
        lector.cerrar()
    }
}
