package com.orlando.bingoapp.data

import androidx.room.DeleteTable
import androidx.room.migration.AutoMigrationSpec

/**
 * 5 -> 6: el cobro pasa de ser por tabla a ser por tablero completo.
 * Room no puede adivinar si una tabla que desaparece fue borrada o renombrada,
 * así que se le dice explícitamente con @DeleteTable.
 */
@DeleteTable(tableName = "tablas_pagadas")
class Migracion5a6 : AutoMigrationSpec
