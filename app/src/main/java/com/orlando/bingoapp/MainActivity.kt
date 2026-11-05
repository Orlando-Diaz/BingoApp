package com.orlando.bingoapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
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

private enum class Pantalla { LISTA_TABLEROS, REGISTRO_TABLERO }

/** Navegación simple entre pantallas. Cuando haya más, pasamos a Navigation Compose. */
@Composable
fun BingoApp() {
    var pantalla by rememberSaveable { mutableStateOf(Pantalla.LISTA_TABLEROS) }
    val registroViewModel: RegistroTableroViewModel = viewModel()

    when (pantalla) {
        Pantalla.LISTA_TABLEROS -> ListaTablerosScreen(
            onNuevoTablero = {
                registroViewModel.reiniciar()
                pantalla = Pantalla.REGISTRO_TABLERO
            }
        )
        Pantalla.REGISTRO_TABLERO -> RegistroTableroScreen(
            viewModel = registroViewModel,
            onSalir = { pantalla = Pantalla.LISTA_TABLEROS }
        )
    }
}
