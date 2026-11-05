package com.orlando.bingoapp.data

import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.domain.Figuras

/** Modos que la app trae listos la primera vez que se abre. */
object DatosIniciales {

    suspend fun cargarSiHaceFalta(db: BingoDatabase) {
        val dao = db.modoDao()
        if (dao.contar() == 0) dao.insertarTodos(modosPredefinidos())
    }

    fun modosPredefinidos(): List<Modo> = listOf(
        // Valen en los sencillos
        Modo(nombre = "4 esquinas", figuras = Figuras.CUATRO_ESQUINAS, usarEnSencillo = true),
        Modo(nombre = "Cuadrito de 4", figuras = Figuras.CUADRITOS, usarEnSencillo = true),
        Modo(nombre = "Línea horizontal", figuras = Figuras.HORIZONTALES, usarEnSencillo = true),
        Modo(nombre = "Línea vertical", figuras = Figuras.VERTICALES, usarEnSencillo = true),
        Modo(nombre = "Línea diagonal", figuras = Figuras.DIAGONALES, usarEnSencillo = true),
        // Para los premios de la llena
        Modo(nombre = Figuras.NOMBRE_LLENO, figuras = Figuras.LLENO),
        Modo(nombre = "Letra X", figuras = Figuras.X),
        Modo(nombre = "Letra L", figuras = Figuras.L),
        Modo(nombre = "Letra T", figuras = Figuras.T),
        Modo(nombre = "Letra U", figuras = Figuras.U),
        Modo(nombre = "Marco", figuras = Figuras.MARCO)
    )
}
