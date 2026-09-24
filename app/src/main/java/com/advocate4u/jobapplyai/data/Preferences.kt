package com.advocate4u.jobapplyai.data

import android.content.Context

class Preferences(context: Context) {
    private val p = context.getSharedPreferences("profile", Context.MODE_PRIVATE)
    var keywords: String
        get() = p.getString("keywords", "Senior .NET Developer") ?: "Senior .NET Developer"
        set(v) = p.edit().putString("keywords", v).apply()
    var minimumMatch: Int
        get() = p.getInt("minimumMatch", 70)
        set(v) = p.edit().putInt("minimumMatch", v).apply()
}
