package sevynidd.bloomseason.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Entity(tableName = "day_entry")
data class DayEntryEntity(
    @androidx.room.PrimaryKey val epochDay: Long,
    val bleeding: Int,
)

@Entity(tableName = "pain_entry", primaryKeys = ["epochDay", "location"])
data class PainEntryEntity(
    val epochDay: Long,
    val location: String,
    val intensity: Int,
)

@Dao
abstract class BloomDao {
    @Query("SELECT * FROM day_entry")
    abstract fun observeDays(): Flow<List<DayEntryEntity>>

    @Query("SELECT * FROM pain_entry")
    abstract fun observePain(): Flow<List<PainEntryEntity>>

    @Query("SELECT * FROM day_entry")
    abstract suspend fun allDays(): List<DayEntryEntity>

    @Query("SELECT * FROM pain_entry")
    abstract suspend fun allPain(): List<PainEntryEntity>

    @Upsert
    abstract suspend fun upsertDay(entry: DayEntryEntity)

    @Query("DELETE FROM day_entry WHERE epochDay = :epochDay")
    abstract suspend fun deleteDay(epochDay: Long)

    @Query("DELETE FROM day_entry WHERE epochDay BETWEEN :from AND :to AND bleeding >= :minBleeding")
    abstract suspend fun deleteBleeding(from: Long, to: Long, minBleeding: Int)

    @Query("DELETE FROM pain_entry WHERE epochDay = :epochDay")
    abstract suspend fun deletePain(epochDay: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertPain(entries: List<PainEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertDays(entries: List<DayEntryEntity>)

    @Query("DELETE FROM day_entry")
    abstract suspend fun clearDays()

    @Query("DELETE FROM pain_entry")
    abstract suspend fun clearPain()

    @Transaction
    open suspend fun saveDay(day: DayEntryEntity?, epochDay: Long, pain: List<PainEntryEntity>) {
        if (day == null) deleteDay(epochDay) else upsertDay(day)
        deletePain(epochDay)
        if (pain.isNotEmpty()) insertPain(pain)
    }

    /** Importierte Einträge ergänzen die vorhandenen; für importierte Tage ersetzen sie die Schmerzen. */
    @Transaction
    open suspend fun mergeIn(days: List<DayEntryEntity>, pain: List<PainEntryEntity>) {
        (days.map { it.epochDay } + pain.map { it.epochDay }).distinct().forEach { deletePain(it) }
        insertDays(days)
        insertPain(pain)
    }

    @Transaction
    open suspend fun replaceAll(days: List<DayEntryEntity>, pain: List<PainEntryEntity>) {
        clearDays()
        clearPain()
        insertDays(days)
        insertPain(pain)
    }
}

@Database(entities = [DayEntryEntity::class, PainEntryEntity::class], version = 1, exportSchema = false)
abstract class BloomDatabase : RoomDatabase() {
    abstract fun dao(): BloomDao

    companion object {
        @Volatile private var instance: BloomDatabase? = null

        /** Eine gemeinsame Instanz für die App und den Erinnerungs-Worker. */
        fun get(context: Context): BloomDatabase = instance ?: synchronized(this) {
            instance ?: open(context.applicationContext).also { instance = it }
        }

        private const val NAME = "bloomseason.db"
        private const val LEGACY_NAME = "bloomseason-plain.db"
        private val SUFFIXES = listOf("", "-wal", "-shm", "-journal")

        private fun isPlain(file: File): Boolean =
            file.inputStream().use { input -> ByteArray(16).also { input.read(it) } }.contentEquals("SQLite format 3\u0000".toByteArray())

        /** Öffnet die verschlüsselte Datenbank; eine unverschlüsselte aus Version 0.5 oder älter wird dabei übernommen. */
        private fun open(app: Context): BloomDatabase {
            System.loadLibrary("sqlcipher")
            val current = app.getDatabasePath(NAME)
            val legacy = app.getDatabasePath(LEGACY_NAME)
            if (current.exists() && !legacy.exists() && isPlain(current)) {
                SUFFIXES.forEach { suffix ->
                    val from = File(current.path + suffix)
                    if (from.exists()) from.renameTo(File(legacy.path + suffix))
                }
            }
            val encrypted = Room.databaseBuilder(app, BloomDatabase::class.java, NAME)
                .openHelperFactory(SupportOpenHelperFactory(DatabaseKey.passphrase(app)))
                .build()
            if (legacy.exists()) {
                // Die alte Datei wird erst gelöscht, wenn alle Einträge in der verschlüsselten Datenbank stehen.
                val plain = Room.databaseBuilder(app, BloomDatabase::class.java, LEGACY_NAME).build()
                try {
                    runBlocking {
                        encrypted.dao().replaceAll(plain.dao().allDays(), plain.dao().allPain())
                    }
                } finally {
                    plain.close()
                }
                SUFFIXES.forEach { File(legacy.path + it).delete() }
            }
            return encrypted
        }
    }
}
