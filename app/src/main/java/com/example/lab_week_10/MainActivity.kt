package com.example.lab_week_10

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.lab_week_10.database.Total
import com.example.lab_week_10.database.TotalDatabase
import com.example.lab_week_10.database.TotalObject
import com.example.lab_week_10.viewmodels.TotalViewModel
import java.util.Date

class MainActivity : AppCompatActivity() {

    // lazy properties
    private val db by lazy { prepareDatabase() }
    private val viewModel by lazy {
        ViewModelProvider(this)[TotalViewModel::class.java]
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeValueFromDatabase()
        prepareViewModel()
    }

    private fun updateText(total: Int) {
        findViewById<TextView>(R.id.text_total).text =
            getString(R.string.text_total, total)
    }

    private fun prepareViewModel() {
        // Observe LiveData so UI stays in sync with the ViewModel
        viewModel.total.observe(this, Observer { total ->
            updateText(total ?: 0)
        })

        // optional: observe date if you want to show somewhere in UI later
        viewModel.date.observe(this, Observer { date ->
            // currently we show date on onStart; keep this if you need live update elsewhere
        })

        // set button listener
        findViewById<Button>(R.id.button_increment).setOnClickListener {
            // increment in ViewModel (main thread)
            viewModel.incrementTotal()

            // read new total from LiveData (may be null briefly, default to 0)
            val newTotal = viewModel.total.value ?: 0
            updateText(newTotal)

            // persist immediately in IO coroutine to avoid blocking UI
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val now = Date().toString()
                    db.totalDao().update(Total(ID, TotalObject(newTotal, now)))
                    // also update viewModel date on main thread
                    launch(Dispatchers.Main) {
                        viewModel.setDate(now)
                    }
                } catch (t: Throwable) {
                    Log.e("MainActivity", "Error updating DB on increment", t)
                    launch(Dispatchers.Main) {
                        Toast.makeText(this@MainActivity, "Save error: ${t::class.simpleName}: ${t.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun prepareDatabase(): TotalDatabase {
        return Room.databaseBuilder(
            applicationContext,
            TotalDatabase::class.java,
            "total-database"
        )
            // mig 1->2 included so existing users won't lose data
            .addMigrations(MIGRATION_1_2)
            // If schema identity still fails (mismatched hashes), allow destructive migration as a
            // temporary fallback so the app can open. This will wipe existing DB — see notes.
            .fallbackToDestructiveMigration()
            .build()
    }

    private fun initializeValueFromDatabase() {
        // run DB ops in coroutine (IO dispatcher)
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val list = db.totalDao().getTotal(ID)
                if (list.isEmpty()) {
                    // insert default with empty date (or set Date().toString() if you prefer)
                    db.totalDao().insert(Total(id = ID, totalObject = TotalObject(0, "")))
                    // set ViewModel on main thread
                    launch(Dispatchers.Main) {
                        viewModel.setTotal(0, "")
                    }
                } else {
                    val row = list.first()
                    // set ViewModel from DB (setTotal can use postValue internally)
                    launch(Dispatchers.Main) {
                        viewModel.setTotal(row.totalObject.value, row.totalObject.date)
                    }
                }
            } catch (t: Throwable) {
                Log.e("MainActivity", "Error initializing DB", t)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "DB init error: ${t::class.simpleName}: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // show last update if available
        val last = viewModel.date.value
        if (!last.isNullOrEmpty()) {
            Toast.makeText(this, "Last updated: $last", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPause() {
        super.onPause()
        // persist on IO coroutine to avoid blocking UI thread
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val currentValue = viewModel.total.value ?: 0
                val now = Date().toString()
                db.totalDao().update(Total(ID, TotalObject(currentValue, now)))
                // update date in ViewModel on main thread
                launch(Dispatchers.Main) {
                    viewModel.setDate(now)
                }
            } catch (t: Throwable) {
                Log.e("MainActivity", "Error persisting onPause", t)
                launch(Dispatchers.Main) {
                    Toast.makeText(this@MainActivity, "Save error: ${t::class.simpleName}: ${t.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    companion object {
        const val ID: Long = 1

        // Migration from v1 (id, total) -> v2 (id, value, date)
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with embedded TotalObject columns: value and date
                // make `date` NOT NULL with default empty string to avoid nullability issues
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `total_new` (
                        `id` INTEGER NOT NULL PRIMARY KEY,
                        `value` INTEGER NOT NULL,
                        `date` TEXT NOT NULL DEFAULT ''
                    )
                """.trimIndent()
                )

                // Be defensive: check whether the old `total` column exists before selecting it.
                var inserted = false
                var cursor: android.database.Cursor? = null
                try {
                    cursor = database.query("PRAGMA table_info('total')")
                    var hasOldTotalColumn = false
                    while (cursor.moveToNext()) {
                        val nameIndex = cursor.getColumnIndex("name")
                        if (nameIndex >= 0) {
                            val colName = cursor.getString(nameIndex)
                            if (colName == "total") {
                                hasOldTotalColumn = true
                                break
                            }
                        }
                    }
                    cursor.close()

                    if (hasOldTotalColumn) {
                        // read the old value safely
                        cursor = database.query("SELECT id, total FROM total LIMIT 1")
                        if (cursor.moveToFirst()) {
                            val valueIndex = cursor.getColumnIndex("total")
                            val idIndex = cursor.getColumnIndex("id")
                            val oldTotal = if (valueIndex >= 0) cursor.getInt(valueIndex) else 0
                            val rowId = if (idIndex >= 0) cursor.getLong(idIndex) else 1L
                            // insert into new table, ensure non-null date
                            database.execSQL("INSERT OR REPLACE INTO `total_new` (id, value, date) VALUES ($rowId, $oldTotal, '')")
                            inserted = true
                        }
                        cursor.close()
                    }
                } catch (e: Exception) {
                    // If anything goes wrong while trying to read old schema, log and fall back to default
                    android.util.Log.e("Migration", "Migration 1->2: failed to copy old value, falling back to default", e)
                    try {
                        database.execSQL("INSERT OR REPLACE INTO `total_new` (id, value, date) VALUES (1, 0, '')")
                        inserted = true
                    } catch (_: Exception) {
                        // ignore - we will still attempt to rename below
                    }
                } finally {
                    try { cursor?.close() } catch (_: Exception) {}
                }

                if (!inserted) {
                    // ensure there's at least a default row so app code that expects id=1 won't fail
                    try {
                        database.execSQL("INSERT OR REPLACE INTO `total_new` (id, value, date) VALUES (1, 0, '')")
                    } catch (e: Exception) {
                        android.util.Log.e("Migration", "Migration 1->2: failed to insert default row", e)
                    }
                }

                // Replace old table with new schema
                try {
                    database.execSQL("DROP TABLE IF EXISTS total")
                } catch (e: Exception) {
                    android.util.Log.w("Migration", "Could not drop old total table", e)
                }

                try {
                    database.execSQL("ALTER TABLE total_new RENAME TO total")
                } catch (e: Exception) {
                    android.util.Log.e("Migration", "Failed to rename total_new to total", e)
                    // if renaming fails, there's not much we can do here in migration
                }
            }
        }
    }
}
