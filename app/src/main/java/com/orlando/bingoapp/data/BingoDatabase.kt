package com.orlando.bingoapp.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.orlando.bingoapp.data.dao.ModoDao
import com.orlando.bingoapp.data.dao.PartidaDao
import com.orlando.bingoapp.data.dao.PremioDao
import com.orlando.bingoapp.data.dao.TableroEnPartidaDao
import com.orlando.bingoapp.data.dao.TableroDao
import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.data.entity.Partida
import com.orlando.bingoapp.data.entity.Premio
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.data.entity.Tablero
import com.orlando.bingoapp.data.entity.TableroEnPartida

@Database(
    entities = [Tablero::class, Tabla::class, Modo::class, Partida::class, Premio::class, TableroEnPartida::class],
    version = 6,
    exportSchema = true,
    // Room compara los schemas/*.json con el código nuevo y genera la migración solo.
    // 2 -> 3: tabla de premios y columna usarEnSencillo en modos.
    // 3 -> 4: modos.cualquierPosicion y premios.figuras.
    // 4 -> 5: tabla tablas_pagadas y partidas.precioCuatroTablas.
    // 5 -> 6: cobro por tablero: sale tablas_pagadas, entra tableros_en_partida.
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
        AutoMigration(from = 3, to = 4),
        AutoMigration(from = 4, to = 5),
        AutoMigration(from = 5, to = 6, spec = Migracion5a6::class)
    ]
)
@TypeConverters(Converters::class)
abstract class BingoDatabase : RoomDatabase() {

    abstract fun tableroDao(): TableroDao
    abstract fun modoDao(): ModoDao
    abstract fun partidaDao(): PartidaDao
    abstract fun premioDao(): PremioDao
    abstract fun tableroEnPartidaDao(): TableroEnPartidaDao

    companion object {
        @Volatile
        private var instancia: BingoDatabase? = null

        /**
         * Versión 1 -> 2: se agrega la tabla de partidas.
         * Es como un script de Flyway: modifica la base de datos existente
         * SIN borrar los tableros que ya estaban registrados.
         */
        private val MIGRACION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `partidas` (" +
                        "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`fecha` INTEGER NOT NULL, " +
                        "`balotas` TEXT NOT NULL, " +
                        "`finalizada` INTEGER NOT NULL)"
                )
            }
        }

        /** Una sola instancia para toda la app (como un @Bean singleton en Spring). */
        fun obtener(context: Context): BingoDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BingoDatabase::class.java,
                    "bingo.db"
                )
                    .addMigrations(MIGRACION_1_2)
                    .build()
                    .also { instancia = it }
            }
    }
}
