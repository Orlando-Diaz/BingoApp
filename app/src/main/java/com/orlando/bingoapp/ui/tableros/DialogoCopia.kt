package com.orlando.bingoapp.ui.tableros

import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import java.io.File

/** Guardar o restaurar la copia de seguridad de todos los datos. */
@Composable
fun DialogoCopia(viewModel: TablerosViewModel, onCerrar: () -> Unit) {
    val contexto = LocalContext.current
    var confirmandoRestaurar by remember { mutableStateOf(false) }

    // Selector de archivos de Android: la persona elige el .zip (de WhatsApp, Descargas, Drive...)
    val elegirArchivo = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) viewModel.restaurar(uri)
    }

    val estado = viewModel.estadoCopia

    // Cuando la copia está lista, se abre el menú de compartir
    LaunchedEffect(estado) {
        if (estado is EstadoCopia.ListaParaCompartir) {
            compartir(contexto, estado.archivo)
            viewModel.limpiarCopia()
            onCerrar()
        }
    }

    // Después de restaurar hay que reiniciar la app para que cargue los datos nuevos
    if (estado is EstadoCopia.Restaurada) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Copia restaurada") },
            text = { Text("La copia está lista. Al reiniciar, la app quedará con los datos de la copia.") },
            confirmButton = { TextButton(onClick = { reiniciarApp(contexto) }) { Text("Reiniciar") } }
        )
        return
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.limpiarCopia()
            onCerrar()
        },
        title = { Text("Copia de seguridad") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                when (estado) {
                    EstadoCopia.Trabajando -> Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator()
                        Text("Un momento…")
                    }
                    else -> {
                        Text(
                            "Guarda todos los tableros, jugadas, precios y fotos en un archivo. " +
                                "Mándalo por WhatsApp o guárdalo en Drive para pasarlo a otro celular o tener un respaldo."
                        )
                        Button(onClick = viewModel::exportar, modifier = Modifier.fillMaxWidth()) {
                            Text("Guardar copia")
                        }
                        OutlinedButton(onClick = { confirmandoRestaurar = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("Restaurar una copia")
                        }
                        if (estado is EstadoCopia.Error) {
                            Text(estado.mensaje, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.limpiarCopia()
                onCerrar()
            }) { Text("Cerrar") }
        }
    )

    if (confirmandoRestaurar) {
        AlertDialog(
            onDismissRequest = { confirmandoRestaurar = false },
            title = { Text("¿Restaurar una copia?") },
            text = {
                Text(
                    "Se BORRAN todos los datos de este celular y se reemplazan por los de la copia. " +
                        "Si aquí tienes algo que no está en la copia, guárdalo antes."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmandoRestaurar = false
                    elegirArchivo.launch(arrayOf("*/*"))
                }) { Text("Elegir archivo") }
            },
            dismissButton = { TextButton(onClick = { confirmandoRestaurar = false }) { Text("Cancelar") } }
        )
    }
}

/** Abre el menú de compartir de Android con el archivo de la copia. */
private fun compartir(contexto: Context, archivo: File) {
    val uri = FileProvider.getUriForFile(contexto, "${contexto.packageName}.fileprovider", archivo)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/zip"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    contexto.startActivity(Intent.createChooser(intent, "Guardar o enviar la copia"))
}

/** Vuelve a abrir la app desde cero (necesario después de reemplazar la base de datos). */
private fun reiniciarApp(contexto: Context) {
    val intent = contexto.packageManager.getLaunchIntentForPackage(contexto.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    contexto.startActivity(intent)
    Runtime.getRuntime().exit(0)
}
