package com.orlando.bingoapp.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Copia de seguridad de toda la app en un archivo .zip:
 *   bingo.db      -> la base de datos (tableros, tablas, jugadas, rondas...)
 *   fotos/...     -> las fotos de las tablas
 *   ajustes.txt   -> los precios
 *
 * Sirve para pasar todo a otro celular o para tener un respaldo.
 *
 * Restaurar se hace en dos pasos para no dañar la base de datos mientras la app la usa:
 *   1. prepararRestauracion: revisa la copia y la deja lista.
 *   2. Se reinicia la app y aplicarPendiente reemplaza los datos antes de abrir la base.
 */
class CopiaSeguridad(private val context: Context) {

    private val carpetaFotos = File(context.filesDir, "fotos")

    /** Crea el archivo de la copia y lo devuelve para compartirlo. */
    suspend fun exportar(): File = withContext(Dispatchers.IO) {
        // Room escribe primero en un archivo aparte (-wal). Este comando pasa todo
        // al archivo principal, para que la copia tenga los últimos cambios.
        BingoDatabase.obtener(context).openHelper.writableDatabase
            .query("PRAGMA wal_checkpoint(FULL)").use { it.moveToFirst() }

        val carpeta = File(context.cacheDir, "copias").apply {
            deleteRecursively()
            mkdirs()
        }
        val archivo = File(carpeta, "BingoApp_copia_${LocalDate.now()}.zip")
        val ajustes = Ajustes(context)

        ZipOutputStream(archivo.outputStream().buffered()).use { zip ->
            agregar(zip, context.getDatabasePath(BingoDatabase.NOMBRE), BASE_DE_DATOS)
            carpetaFotos.listFiles()?.forEach { agregar(zip, it, "fotos/${it.name}") }
            zip.putNextEntry(ZipEntry(AJUSTES))
            zip.write(
                "precio4_sencillo=${ajustes.precioCuatroSencillo}\nprecio4_llena=${ajustes.precioCuatroLlena}\n"
                    .toByteArray()
            )
            zip.closeEntry()
        }
        archivo
    }

    /**
     * Prepara la restauración: descomprime y revisa la copia, y la deja lista en una
     * carpeta "pendiente". NO toca todavía los datos actuales: eso se hace al reiniciar
     * la app (aplicarPendiente), antes de abrir la base de datos, para que nada la esté usando.
     *
     * Devuelve null si quedó lista, o un mensaje de error.
     */
    suspend fun prepararRestauracion(uri: Uri): String? = withContext(Dispatchers.IO) {
        val pendiente = carpetaPendiente(context).apply { deleteRecursively() }
        val extraido = File(context.cacheDir, "extraido").apply {
            deleteRecursively()
            mkdirs()
        }
        val nombre = nombreDelArchivo(uri)
        try {
            // 1. Descomprimir en una carpeta temporal
            val entrada = context.contentResolver.openInputStream(uri)
                ?: return@withContext "No se pudo abrir el archivo \"$nombre\""
            val contenido = mutableListOf<String>()
            ZipInputStream(entrada.buffered()).use { zip ->
                var item = zip.nextEntry
                while (item != null) {
                    val destino = File(extraido, item.name)
                    // Seguridad: que ningún archivo del zip se salga de la carpeta
                    if (!destino.canonicalPath.startsWith(extraido.canonicalPath + File.separator)) {
                        return@withContext "El archivo \"$nombre\" no es una copia válida"
                    }
                    if (!item.isDirectory) {
                        destino.parentFile?.mkdirs()
                        destino.outputStream().use { zip.copyTo(it) }
                        contenido += item.name
                    }
                    item = zip.nextEntry
                }
            }

            // 2. Buscar la base de datos, aunque venga dentro de una carpeta
            //    (pasa si alguna app descomprimió y volvió a comprimir el archivo)
            if (contenido.isEmpty()) {
                return@withContext "\"$nombre\" no es un archivo .zip. Elige el archivo BingoApp_copia_….zip"
            }
            val base = extraido.walkTopDown().firstOrNull { it.isFile && it.name == BASE_DE_DATOS }
                ?: return@withContext "\"$nombre\" no tiene los datos de BingoApp. Trae: " +
                    contenido.take(5).joinToString(", ") + if (contenido.size > 5) "…" else ""

            // 3. Revisar que la versión sea compatible
            val version = SQLiteDatabase.openDatabase(base.path, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
            if (version > BingoDatabase.VERSION) {
                return@withContext "La copia viene de una versión más nueva de BingoApp. Actualiza la app en este celular primero."
            }

            // 4. Dejarla lista para aplicarla al reiniciar
            base.parentFile!!.copyRecursively(pendiente, overwrite = true)
            null
        } catch (e: Exception) {
            pendiente.deleteRecursively()
            "No se pudo leer \"$nombre\": el archivo parece dañado"
        } finally {
            extraido.deleteRecursively()
        }
    }

    /** El nombre con el que la persona ve el archivo (ej: "BingoApp_copia_2026-11-08.zip"). */
    private fun nombreDelArchivo(uri: Uri): String =
        try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        } catch (e: Exception) {
            null
        } ?: "el archivo"

    private fun agregar(zip: ZipOutputStream, archivo: File, nombre: String) {
        if (!archivo.exists()) return
        zip.putNextEntry(ZipEntry(nombre))
        archivo.inputStream().use { it.copyTo(zip) }
        zip.closeEntry()
    }

    companion object {
        private const val BASE_DE_DATOS = "bingo.db"
        private const val AJUSTES = "ajustes.txt"

        private fun carpetaPendiente(context: Context) = File(context.filesDir, "restauracion_pendiente")

        /**
         * Se llama al arrancar la app, ANTES de abrir la base de datos.
         * Si hay una restauración pendiente, reemplaza los datos con los de la copia.
         */
        fun aplicarPendiente(context: Context) {
            val pendiente = carpetaPendiente(context)
            val base = File(pendiente, BASE_DE_DATOS)
            if (!base.exists()) return

            val actual = context.getDatabasePath(BingoDatabase.NOMBRE)
            listOf(actual, File(actual.path + "-wal"), File(actual.path + "-shm")).forEach { it.delete() }
            actual.parentFile?.mkdirs()
            base.copyTo(actual, overwrite = true)

            val fotos = File(context.filesDir, "fotos")
            fotos.deleteRecursively()
            File(pendiente, "fotos").takeIf { it.exists() }?.copyRecursively(fotos, overwrite = true)

            File(pendiente, AJUSTES).takeIf { it.exists() }?.let { restaurarAjustes(context, it) }
            pendiente.deleteRecursively()
        }

        private fun restaurarAjustes(context: Context, archivo: File) {
            val valores = archivo.readLines()
                .mapNotNull { linea -> linea.split("=").takeIf { it.size == 2 }?.let { it[0] to it[1].toIntOrNull() } }
                .toMap()
            val ajustes = Ajustes(context)
            valores["precio4_sencillo"]?.let { ajustes.precioCuatroSencillo = it }
            valores["precio4_llena"]?.let { ajustes.precioCuatroLlena = it }
        }
    }
}
