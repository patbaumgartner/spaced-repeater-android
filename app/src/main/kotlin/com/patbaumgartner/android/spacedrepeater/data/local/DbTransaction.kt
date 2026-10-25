package com.patbaumgartner.android.spacedrepeater.data.local

import androidx.room.withTransaction

/**
 * Executes the given block inside a database transaction.
 * Abstraction over Room's [withTransaction] so repositories stay testable on the JVM:
 * tests inject [SyncDbTransaction] (runs the block inline), production uses [roomDbTransaction].
 */
interface DbTransaction {
    suspend fun <T> run(block: suspend () -> T): T
}

/** Production binding: delegates to Room's [withTransaction] extension. */
fun roomDbTransaction(db: androidx.room.RoomDatabase): DbTransaction = object : DbTransaction {
    override suspend fun <T> run(block: suspend () -> T): T = db.withTransaction(block)
}

/** Inline identity transaction for JVM tests — runs the block synchronously. */
object SyncDbTransaction : DbTransaction {
    override suspend fun <T> run(block: suspend () -> T): T = block()
}
