package com.orlando.bingoapp.camara

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Muestra la foto de una tabla (cargada en segundo plano y en tamaño reducido). */
@Composable
fun FotoTabla(ruta: String, modifier: Modifier = Modifier) {
    val imagen by produceState<ImageBitmap?>(initialValue = null, ruta) {
        value = withContext(Dispatchers.IO) { cargarReducida(ruta)?.asImageBitmap() }
    }
    imagen?.let {
        Image(
            bitmap = it,
            contentDescription = "Foto de la tabla",
            contentScale = ContentScale.Fit,
            modifier = modifier
        )
    }
}

/** Carga la foto a ~1000 px y la gira según cómo se tomó (dato EXIF). */
private fun cargarReducida(ruta: String, ladoMaximo: Int = 1000): Bitmap? {
    val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeFile(ruta, medidas)
    var escala = 1
    while (maxOf(medidas.outWidth, medidas.outHeight) / (escala * 2) >= ladoMaximo) escala *= 2

    val bitmap = BitmapFactory.decodeFile(ruta, BitmapFactory.Options().apply { inSampleSize = escala })
        ?: return null

    val grados = when (ExifInterface(ruta).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
        ExifInterface.ORIENTATION_ROTATE_90 -> 90f
        ExifInterface.ORIENTATION_ROTATE_180 -> 180f
        ExifInterface.ORIENTATION_ROTATE_270 -> 270f
        else -> 0f
    }
    if (grados == 0f) return bitmap
    val matriz = Matrix().apply { postRotate(grados) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matriz, true)
}
