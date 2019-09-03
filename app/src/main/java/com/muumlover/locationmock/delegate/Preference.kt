package com.muumlover.locationmock.delegate

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.muumlover.locationmock.MainActivity
import kotlin.reflect.KProperty


class RealLocation {
    var address: String = ""
    var city: String = ""
    var country: String = ""
    var latitude: Double = 0.0
    var longitude: Double = 0.0
}

class PreferenceRealLocation(val name: String) {
    private val prefs: SharedPreferences =
        MainActivity.instance.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    fun getValue(): ArrayList<RealLocation> {
        Log.i("info", "调用$this 的getValue()")
        return getRealLocation(name)
    }

    fun setValue(value: ArrayList<RealLocation>) {
        Log.i("info", "调用$this 的setValue() value参数值为：$value")
        putRealLocation(name, value)
    }

    private fun putRealLocation(
        key: String,
        value: ArrayList<RealLocation>
    ) {
        val editor = prefs.edit()
        editor.clear()
        editor.putString(key, Gson().toJson(value))
        editor.apply()
    }

    private fun getRealLocation(
        key: String
    ): ArrayList<RealLocation> {
        return if (!prefs.contains(key)) {
            ArrayList()
        } else {
            Gson().fromJson(
                prefs.getString(key, ""),
                object : TypeToken<ArrayList<RealLocation>>() {}.type
            )
        }
    }
}

class Preference<T>(val name: String, private val default: T) {
    private val prefs: SharedPreferences by lazy {
        MainActivity.instance.applicationContext.getSharedPreferences(
            name,
            Context.MODE_PRIVATE
        )
    }

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        Log.i("info", "调用$this 的getValue()")
        return getSharePreferences(name, default)
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        Log.i("info", "调用$this 的setValue() value参数值为：$value")
        putSharePreferences(name, value)
    }

    @SuppressLint("CommitPrefEdits")
    private fun putSharePreferences(name: String, value: T) = with(prefs.edit()) {
        when (value) {
            is Long -> putLong(name, value)
            is String -> putString(name, value)
            is Int -> putInt(name, value)
            is Boolean -> putBoolean(name, value)
            is Float -> putFloat(name, value)
            is Double -> putDouble(name, value)
            else -> throw IllegalArgumentException("This type of data cannot be saved!")
        }.apply()
    }

    @Suppress("UNCHECKED_CAST")
    private fun getSharePreferences(name: String, default: T): T = with(prefs) {
        val res: Any = when (default) {
            is Long -> getLong(name, default)
            is String -> getString(name, default)
            is Int -> getInt(name, default)
            is Boolean -> getBoolean(name, default)
            is Float -> getFloat(name, default)
            is Double -> getDouble(name, default)
            else -> throw IllegalArgumentException("This type of data cannot be saved!")
        }
        return res as T
    }

    private fun putDouble(
        key: String,
        value: Double
    ): SharedPreferences.Editor {
        return prefs.edit().putLong(key, java.lang.Double.doubleToRawLongBits(value))
    }

    private fun getDouble(
        key: String,
        defaultValue: Double
    ): Double {
        return if (!prefs.contains(key)) defaultValue
        else java.lang.Double.longBitsToDouble(prefs.getLong(key, 0))
    }
}