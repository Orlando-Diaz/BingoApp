package com.orlando.bingoapp.data

import androidx.room.TypeConverter

/**
 * SQLite no sabe guardar listas, así que las convertimos a texto.
 * Equivalente al AttributeConverter de JPA.
 */
class Converters {

    // List<Int>  <->  "5,18,33,47,62,..."
    @TypeConverter
    fun numerosATexto(numeros: List<Int>): String = numeros.joinToString(",")

    @TypeConverter
    fun textoANumeros(texto: String): List<Int> =
        if (texto.isBlank()) emptyList() else texto.split(",").map { it.toInt() }

    // List<String>  <->  "1111100000...;0000011111..."
    @TypeConverter
    fun figurasATexto(figuras: List<String>): String = figuras.joinToString(";")

    @TypeConverter
    fun textoAFiguras(texto: String): List<String> =
        if (texto.isBlank()) emptyList() else texto.split(";")
}
