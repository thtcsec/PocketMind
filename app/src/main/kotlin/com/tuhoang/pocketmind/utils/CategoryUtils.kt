package com.tuhoang.pocketmind.utils

import android.content.Context
import com.tuhoang.pocketmind.R

object CategoryUtils {

    private val subcategoryArrayIds = listOf(
        R.array.subcat_food,
        R.array.subcat_transport,
        R.array.subcat_shopping,
        R.array.subcat_bills,
        R.array.subcat_health,
        R.array.subcat_entertainment,
        R.array.subcat_education,
        R.array.subcat_salary,
        R.array.subcat_other
    )

    fun presetCategories(context: Context): List<String> =
        context.resources.getStringArray(R.array.preset_categories).toList()

    fun subcategoriesFor(context: Context, parentCategory: String): List<String> {
        val index = presetCategories(context).indexOf(parentCategory)
        if (index < 0 || index >= subcategoryArrayIds.size) return emptyList()
        return context.resources.getStringArray(subcategoryArrayIds[index]).toList()
    }

    fun formatCategory(parent: String, subcategory: String?): String {
        val sub = subcategory?.trim().orEmpty()
        return if (sub.isEmpty()) parent.trim() else "${parent.trim()} › $sub"
    }

    fun parseCategory(full: String): Pair<String, String?> {
        val parts = full.split("›", limit = 2).map { it.trim() }
        return when (parts.size) {
            2 -> parts[0] to parts[1]
            else -> full.trim() to null
        }
    }

    fun allCategories(context: Context, prefs: PrefsManager = PrefsManager.getInstance()): List<String> {
        val merged = LinkedHashSet<String>()
        merged.addAll(presetCategories(context))
        merged.addAll(prefs.getCustomCategories())
        return merged.toList()
    }

    fun addCustomCategory(prefs: PrefsManager, name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val updated = prefs.getCustomCategories().toMutableSet()
        updated.add(trimmed)
        prefs.setCustomCategories(updated.toList())
    }
}
