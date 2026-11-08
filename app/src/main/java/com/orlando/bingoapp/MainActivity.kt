package com.orlando.bingoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.orlando.bingoapp.ui.cobro.CobroScreen
import com.orlando.bingoapp.ui.juego.JuegoScreen
import com.orlando.bingoapp.ui.jugadas.EditorJugadaScreen
import com.orlando.bingoapp.ui.jugadas.EditorJugadaViewModel
import com.orlando.bingoapp.ui.jugadas.JugadasScreen
import com.orlando.bingoapp.ui.tableros.DetalleTableroScreen
import com.orlando.bingoapp.ui.tableros.DetalleTableroViewModel
import com.orlando.bingoapp.ui.tableros.EditorTablaScreen
import com.orlando.bingoapp.ui.tableros.EditorTablaViewModel
import com.orlando.bingoapp.ui.tableros.ListaTablerosScreen
import com.orlando.bingoapp.ui.tableros.RegistroTableroScreen
import com.orlando.bingoapp.ui.tableros.RegistroTableroViewModel
import com.orlando.bingoapp.ui.theme.BingoAppTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BingoAppTheme {
                BingoApp()
            }
        }
    }
}

private enum class Pantalla { JUEGO, COBRO, LISTA_TABLEROS, REGISTRO_TABLERO, DETALLE_TABLERO, EDITOR_TABLA, JUGADAS, EDITOR_JUGADA }

/** Navegación simple entre pantallas. Cuando haya más, pasamos a Navigation Compose. */
@Composable
fun BingoApp() {
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.JUEGO) }
    val registroViewModel: RegistroTableroViewModel = viewModel()
    val editorJugadaViewModel: EditorJugadaViewModel = viewModel()
    val detalleTableroViewModel: DetalleTableroViewModel = viewModel()
    val editorTablaViewModel: EditorTablaViewModel = viewModel()

    // Pantallas de formulario: ocupan toda la pantalla (sin barra inferior)
    when (pantalla) {
        Pantalla.REGISTRO_TABLERO -> {
            RegistroTableroScreen(
                viewModel = registroViewModel,
                onSalir = { pantalla = Pantalla.LISTA_TABLEROS }
            )
            return
        }
        Pantalla.DETALLE_TABLERO -> {
            DetalleTableroScreen(
                viewModel = detalleTableroViewModel,
                onEditarTabla = { tabla, numeroTablero ->
                    editorTablaViewModel.cargar(tabla, numeroTablero)
                    pantalla = Pantalla.EDITOR_TABLA
                },
                onSalir = { pantalla = Pantalla.LISTA_TABLEROS }
            )
            return
        }
        Pantalla.EDITOR_TABLA -> {
            EditorTablaScreen(
                viewModel = editorTablaViewModel,
                onSalir = { pantalla = Pantalla.DETALLE_TABLERO }
            )
            return
        }
        Pantalla.EDITOR_JUGADA -> {
            EditorJugadaScreen(
                viewModel = editorJugadaViewModel,
                onSalir = { pantalla = Pantalla.JUGADAS }
            )
            return
        }
        else -> {}
    }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = pantalla == Pantalla.JUEGO,
                    onClick = { pantalla = Pantalla.JUEGO },
                    icon = { Text("🎱", fontSize = 22.sp) },
                    label = { Text("Juego") }
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.COBRO,
                    onClick = { pantalla = Pantalla.COBRO },
                    icon = { Text("💵", fontSize = 22.sp) },
                    label = { Text("Cobro") }
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.LISTA_TABLEROS,
                    onClick = { pantalla = Pantalla.LISTA_TABLEROS },
                    icon = { Text("📋", fontSize = 22.sp) },
                    label = { Text("Tableros") }
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.JUGADAS,
                    onClick = { pantalla = Pantalla.JUGADAS },
                    icon = { Text("⭐", fontSize = 22.sp) },
                    label = { Text("Jugadas") }
                )
            }
        }
    ) { padding ->
        // consumeWindowInsets evita que las pantallas internas dupliquen los márgenes del sistema
        Box(Modifier.padding(padding).consumeWindowInsets(padding)) {
            when (pantalla) {
                Pantalla.COBRO -> CobroScreen(onEmpezar = { pantalla = Pantalla.JUEGO })
                Pantalla.LISTA_TABLEROS -> ListaTablerosScreen(
                    onNuevoTablero = {
                        registroViewModel.reiniciar()
                        pantalla = Pantalla.REGISTRO_TABLERO
                    },
                    onVerTablero = { id ->
                        detalleTableroViewModel.cargar(id)
                        pantalla = Pantalla.DETALLE_TABLERO
                    }
                )
                Pantalla.JUGADAS -> JugadasScreen(
                    onNueva = {
                        editorJugadaViewModel.cargar(null)
                        pantalla = Pantalla.EDITOR_JUGADA
                    },
                    onEditar = { jugada ->
                        editorJugadaViewModel.cargar(jugada)
                        pantalla = Pantalla.EDITOR_JUGADA
                    }
                )
                else -> JuegoScreen(onIrACobro = { pantalla = Pantalla.COBRO })
            }
        }
    }
}
