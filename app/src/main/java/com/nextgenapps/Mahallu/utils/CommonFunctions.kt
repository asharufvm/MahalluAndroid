package com.nextgenapps.Mahallu.utils


import android.content.Context
import android.widget.Toast
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import android.content.SharedPreferences

object CommonFunctions {

    private const val PREF_NAME = "MahalluPrefs"
    private const val ORGANIZATION_ID_KEY = "organizationId"

    fun showToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    fun logout(navController: NavController, context: Context) {
        FirebaseAuth.getInstance().signOut()
        showToast(context, "Logged out")
        navController.navigate("login") {
            popUpTo(0) { inclusive = true } // Clears backstack
        }
    }

    fun getOrganizationPath(context: Context, childPath: String): String {
        val sharedPrefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val orgId = sharedPrefs.getString(ORGANIZATION_ID_KEY, null)
        return if (!orgId.isNullOrEmpty()) {
            "/organizations/$orgId/$childPath"
        } else {
            throw IllegalStateException("Organization ID not found in SharedPreferences")
        }
    }
}
