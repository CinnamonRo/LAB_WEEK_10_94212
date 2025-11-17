package com.example.lab_week_10

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.lab_week_10.database.TotalDatabase
import com.example.lab_week_10.database.Total
import com.example.lab_week_10.viewmodels.TotalViewModel

class MainActivity : AppCompatActivity() {

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
        viewModel.total.observe(this, Observer<Int?> { total ->
            updateText(total ?: 0)
        })

        // set button listener
        findViewById<Button>(R.id.button_increment).setOnClickListener {
            val newTotal = viewModel.incrementTotal()
            updateText(newTotal)
            // persist immediately in IO coroutine to avoid blocking UI
            lifecycleScope.launch(Dispatchers.IO) {
                db.totalDao().update(Total(ID, newTotal))
            }
        }
    }

    private val db by lazy { prepareDatabase() }
    private val viewModel by lazy {
        ViewModelProvider(this)[TotalViewModel::class.java]
    }

    private fun prepareDatabase(): TotalDatabase {
        return Room.databaseBuilder(
            applicationContext,
            TotalDatabase::class.java, "total-database"
        ).addMigrations(MIGRATION_1_2).build()
    }

    private fun initializeValueFromDatabase() {
        // run DB ops in coroutine (IO dispatcher)
        lifecycleScope.launch(Dispatchers.IO) {
            val total = db.totalDao().getTotal(ID)
            if (total.isEmpty()) {
                db.totalDao().insert(Total(id = 1, total = 0))
                // no need to set viewModel (default is 0)
            } else {
                val value = total.first().total
                // ViewModel.setTotal uses postValue, can be called from background thread
                viewModel.setTotal(value)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        // persist on IO coroutine to avoid blocking UI thread
        lifecycleScope.launch(Dispatchers.IO) {
            db.totalDao().update(Total(ID, viewModel.total.value ?: 0))
        }
    }
    companion object {
        const val ID: Long = 1

        // Migration to preserve existing single-row data when schema changes from v1 -> v2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create new table with desired schema
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS `total_new` (
                        `id` INTEGER NOT NULL,
                        `total` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                """.trimIndent())

                // Copy first row from old table into new table with id = 1
                val cursor = database.query("SELECT id, total FROM total LIMIT 1")
                if (cursor.moveToFirst()) {
                    val total = cursor.getInt(1)
                    database.execSQL("INSERT OR REPLACE INTO `total_new` (id, total) VALUES (1, $total)")
                }
                cursor.close()

                // Replace old table
                database.execSQL("DROP TABLE IF EXISTS total")
                database.execSQL("ALTER TABLE total_new RENAME TO total")
            }
        }
    }
}
