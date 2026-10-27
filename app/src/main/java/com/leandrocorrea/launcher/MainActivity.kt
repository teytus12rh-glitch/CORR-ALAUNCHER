package com.leandrocorrea.launcher

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.SoundEffectConstants
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class AppModel(
    val name: String,
    val packageName: String,
    val icon: Drawable?,
    val isAddButton: Boolean = false
)

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences
    private val PREF_FAVS = "favorite_apps_list"
    private lateinit var recyclerFavorites: RecyclerView
    private var allInstalledApps = mutableListOf<AppModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("LeandroApexPrefs", Context.MODE_PRIVATE)

        setupDateTime()
        fetchAllInstalledApps()
        setupFavoritesRecycler()
        setupSidebarAndInputs()
    }

    private fun setupDateTime() {
        val txtClock = findViewById<TextView>(R.id.txtClock)
        val txtDayWeek = findViewById<TextView>(R.id.txtDayWeek)
        val txtDateFull = findViewById<TextView>(R.id.txtDateFull)
        val txtDateExtra = findViewById<TextView>(R.id.txtDateExtra)

        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val dayWeekFormat = SimpleDateFormat("EEEE", Locale("pt", "BR"))
        val fullDateFormat = SimpleDateFormat("d 'de' MMMM 'de' yyyy", Locale("pt", "BR"))
        val extraFormat = SimpleDateFormat("'Ano:' yyyy ' | Mês:' MM ' | Dia:' dd", Locale.getDefault())

        txtClock?.text = timeFormat.format(now)
        txtDayWeek?.text = dayWeekFormat.format(now).replaceFirstChar { it.uppercase() }
        txtDateFull?.text = fullDateFormat.format(now)
        txtDateExtra?.text = extraFormat.format(now)
    }

    private fun fetchAllInstalledApps() {
        allInstalledApps.clear()
        val pm = packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg != packageName) {
                val label = info.loadLabel(pm).toString()
                val icon = info.loadIcon(pm)
                allInstalledApps.add(AppModel(label, pkg, icon))
            }
        }
        allInstalledApps.sortBy { it.name.lowercase(Locale.ROOT) }
    }

    private fun getSavedFavorites(): MutableSet<String> {
        val saved = prefs.getStringSet(PREF_FAVS, null)
        if (saved == null) {
            val defaultFavorites = mutableSetOf<String>()
            for (app in allInstalledApps.take(4)) {
                defaultFavorites.add(app.packageName)
            }
            prefs.edit().putStringSet(PREF_FAVS, defaultFavorites).apply()
            return defaultFavorites
        }
        return saved.toMutableSet()
    }

    private fun setupFavoritesRecycler() {
        recyclerFavorites = findViewById(R.id.recyclerFavorites)
        recyclerFavorites.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        refreshFavoritesList()
    }

    private fun refreshFavoritesList() {
        val favPackages = getSavedFavorites()
        val displayList = mutableListOf<AppModel>()

        for (pkg in favPackages) {
            val found = allInstalledApps.find { it.packageName == pkg }
            if (found != null) {
                displayList.add(found)
            }
        }

        // Botão especial "+" para adicionar favoritos
        displayList.add(AppModel("Adicionar", "", null, isAddButton = true))

        recyclerFavorites.adapter = FavoritesAdapter(
            items = displayList,
            onClick = { item ->
                if (item.isAddButton) {
                    showAddFavoriteDialog()
                } else {
                    val launchIntent = packageManager.getLaunchIntentForPackage(item.packageName)
                    if (launchIntent != null) {
                        startActivity(launchIntent)
                    } else {
                        Toast.makeText(this, "Aplicativo não pôde ser aberto", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onLongClick = { item ->
                if (!item.isAddButton) {
                    showRemoveFavoriteDialog(item)
                }
            }
        )
    }

    private fun showAddFavoriteDialog() {
        val currentFavs = getSavedFavorites()
        val available = allInstalledApps.filter { !currentFavs.contains(it.packageName) }

        if (available.isEmpty()) {
            Toast.makeText(this, "Todos os apps já estão nos favoritos!", Toast.LENGTH_SHORT).show()
            return
        }

        val names = available.map { it.name }.toTypedArray()
        AlertDialog.Builder(this)
            .setTitle("Adicionar aos Favoritos")
            .setItems(names) { _, which ->
                val selected = available[which]
                currentFavs.add(selected.packageName)
                prefs.edit().putStringSet(PREF_FAVS, currentFavs).apply()
                refreshFavoritesList()
                Toast.makeText(this, "${selected.name} adicionado!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showRemoveFavoriteDialog(item: AppModel) {
        AlertDialog.Builder(this)
            .setTitle("Favoritos")
            .setMessage("Deseja remover '${item.name}' dos favoritos?")
            .setPositiveButton("Remover") { _, _ ->
                val currentFavs = getSavedFavorites()
                currentFavs.remove(item.packageName)
                prefs.edit().putStringSet(PREF_FAVS, currentFavs).apply()
                refreshFavoritesList()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun setupSidebarAndInputs() {
        findViewById<View>(R.id.menuSettings)?.setOnClickListener {
            startActivity(Intent(Settings.ACTION_SETTINGS))
        }

        findViewById<View>(R.id.menuApps)?.setOnClickListener {
            showAddFavoriteDialog()
        }

        findViewById<View>(R.id.btnHdmi1)?.setOnClickListener {
            Toast.makeText(this, "Alternando para HDMI 1", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnHdmi2)?.setOnClickListener {
            Toast.makeText(this, "Alternando para HDMI 2 (eARC)", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.btnHdmi3)?.setOnClickListener {
            Toast.makeText(this, "Alternando para HDMI 3", Toast.LENGTH_SHORT).show()
        }
    }
}

class FavoritesAdapter(
    private val items: List<AppModel>,
    private val onClick: (AppModel) -> Unit,
    private val onLongClick: (AppModel) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.ViewHolder>() {

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgAppIcon)
        val txt: TextView = view.findViewById(R.id.txtAppName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.card_app_item, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        if (item.isAddButton) {
            holder.txt.text = "+ Favorito"
            holder.img.setImageDrawable(ColorDrawable(Color.TRANSPARENT))
        } else {
            holder.txt.text = item.name
            holder.img.setImageDrawable(item.icon)
        }

        holder.view.setOnFocusChangeListener { view, hasFocus ->
            if (hasFocus) {
                view.playSoundEffect(SoundEffectConstants.CLICK)
            }
        }

        holder.view.setOnClickListener {
            it.playSoundEffect(SoundEffectConstants.CLICK)
            onClick(item)
        }

        holder.view.setOnLongClickListener {
            it.playSoundEffect(SoundEffectConstants.CLICK)
            onLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = items.size
}
