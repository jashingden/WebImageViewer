package com.eddy.webcrawler.data.repository

import android.content.Context
import com.eddy.webcrawler.data.db.AppDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase
) {

    suspend fun backupToUri(outputStream: OutputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Ensure DB is flushed to disk
            database.openHelper.writableDatabase.query("PRAGMA checkpoint(FULL)").close()

            ZipOutputStream(outputStream).use { zos ->
                // 1. Backup Database
                val dbName = "webcrawler_database"
                val dbFile = context.getDatabasePath(dbName)
                val dbShm = File(dbFile.path + "-shm")
                val dbWal = File(dbFile.path + "-wal")

                addToZip(zos, dbFile, "databases/${dbFile.name}")
                if (dbShm.exists()) addToZip(zos, dbShm, "databases/${dbShm.name}")
                if (dbWal.exists()) addToZip(zos, dbWal, "databases/${dbWal.name}")

                // 2. Backup Images
                val imagesDir = File(context.filesDir, "images")
                if (imagesDir.exists()) {
                    imagesDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(context.filesDir).path
                            addToZip(zos, file, "files/$relativePath")
                        }
                    }
                }

                // 3. Backup DataStore
                val datastoreDir = File(context.filesDir, "datastore")
                if (datastoreDir.exists()) {
                    datastoreDir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            val relativePath = file.relativeTo(context.filesDir).path
                            addToZip(zos, file, "files/$relativePath")
                        }
                    }
                }
            }
        }
    }

    private fun addToZip(zos: ZipOutputStream, file: File, entryName: String) {
        if (!file.exists()) return
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        FileInputStream(file).use { it.copyTo(zos) }
        zos.closeEntry()
    }

    suspend fun restoreFromUri(inputStream: InputStream): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            // Close database before restoration
            database.close()

            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val destFile = validateZipEntry(entry.name, context.dataDir)
                    
                    destFile.parentFile?.mkdirs()
                    if (!entry.isDirectory) {
                        FileOutputStream(destFile).use { output ->
                            zis.copyTo(output)
                        }
                    }
                    
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
        }
    }

    private fun validateZipEntry(entryName: String, destDir: File): File {
        val destFile = File(destDir, entryName)
        val canonicalPath = destFile.canonicalPath
        if (!canonicalPath.startsWith(destDir.canonicalPath)) {
            throw SecurityException("Potential Zip Slip attack: $entryName")
        }
        return destFile
    }
}
