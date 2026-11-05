package com.orlando.bingoapp.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.orlando.bingoapp.data.dao.ModoDao
import com.orlando.bingoapp.data.dao.TableroDao
import com.orlando.bingoapp.data.entity.Modo
import com.orlando.bingoapp.data.entity.Tabla
import com.orlando.bingoapp.data.entity.Tablero

@Database(
    entities = [Tablero::class, Tabla::class, Modo::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class BingoDatabase : RoomDatabase() {

    abstract fun tableroDao(): TableroDao
    abstract fun modoDao(): ModoDao

    companion object {
        @Volatile
        private var instancia: BingoDatabase? = null

        /** Una sola instancia para toda la app (como un @Bean singleton en Spring). */
        fun obtener(context: Context): BingoDatabase =
            instancia ?: synchronized(this) {
                instancia ?: Room.databaseBuilder(
                    context.applicationContext,
                    BingoDatabase::class.java,
                    "bingo.db"
                ).build().also { instancia = it }
            }
    }
}
