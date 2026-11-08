package com.orlando.bingoapp.ui.tableros

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.orlando.bingoapp.camara.LectorFotoTabla
import com.orlando.bingoapp.camara.MENSAJE_FOTO_FALLIDA
import com.orlando.bingoapp.camara.llenarCasillas
import com.orlando.bingoapp.data.BingoDatabase
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.domain.ReglasBingo
import com.orlando.bingoapp.domain.ValidadorTabla
import kotlinx.coroutines.launch
import java.io.File

/** Editar los números de una tabla ya registrada (a mano o con una foto nueva). */
class EditorTablaViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = BingoDatabase.obtener(application).tableroDao()
    private val lector = LectorFotoTabla(application)
    private var tabla: Tabla? = null
    private var fotoPendiente: File? = null

    val casillas = mutableStateListOf(*Array(25) { "" })
    var numeroTablero by mutableStateOf(0)
        private set
    var posicion by mutableStateOf(0)
        private set
    var foto by mutableStateOf<String?>(null)
        private set
    var mostrarVacias by mutableStateOf(false)
        private set
    var mensajeError by mutableStateOf<String?>(null)
        private set
    var leyendoFoto by mutableStateOf(false)
        private set
    var mensajeFoto by mutableStateOf<String?>(null)
        private set
    var guardado by mutableStateOf(false)
        private set

    fun cargar(tablaAEditar: Tabla, numeroDelTablero: Int) {
        tabla = tablaAEditar
        numeroTablero = numeroDelTablero
        posicion = tablaAEditar.posicion
        foto = tablaAEditar.fotoPath
        tablaAEditar.numeros.forEachIndexed { i, n ->
            casillas[i] = if (i == ReglasBingo.INDICE_CENTRO || n == Tabla.CENTRO_LIBRE) "" else n.toString()
        }
        mostrarVacias = false
        mensajeError = null
        mensajeFoto = null
        guardado = false
    }

    fun cambiarCasilla(indice: Int, texto: String) {
        casillas[indice] = texto.filter { it.isDigit() }.take(2)
        mensajeError = null
    }

    fun erroresActuales(): Map<Int, String> {
        val numeros = casillas.map { it.toIntOrNull() }
        return if (mostrarVacias) ValidadorTabla.errores(numeros) else ValidadorTabla.erroresVisibles(numeros)
    }

    fun prepararFoto(): Uri {
        val archivo = lector.nuevoArchivo()
        fotoPendiente = archivo
        return lector.uriPara(archivo)
    }

    fun fotoTomada(exito: Boolean) {
        val archivo = fotoPendiente ?: return
        fotoPendiente = null
        if (!exito) return
        viewModelScope.launch {
            leyendoFoto = true
            mensajeFoto = null
            try {
                mensajeFoto = lector.llenarCasillas(archivo, casillas)
                foto = archivo.path
                mostrarVacias = true
            } catch (e: Exception) {
                mensajeFoto = MENSAJE_FOTO_FALLIDA
            } finally {
                leyendoFoto = false
            }
        }
    }

    fun guardar() {
        val original = tabla ?: return
        if (ValidadorTabla.errores(casillas.map { it.toIntOrNull() }).isNotEmpty()) {
            mostrarVacias = true
            mensajeError = "Revisa las casillas marcadas en rojo"
            return
        }
        val numeros = casillas.mapIndexed { i, texto ->
            if (i == ReglasBingo.INDICE_CENTRO) Tabla.CENTRO_LIBRE else texto.toInt()
        }
        viewModelScope.launch {
            dao.actualizarTabla(original.copy(numeros = numeros, fotoPath = foto))
            guardado = true
        }
    }

    override fun onCleared() {
        lector.cerrar()
    }
}
