package com.example.bloodbank

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import android.widget.Toast
import com.example.bloodbank.ui.home.HomeFragment
import android.view.View

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            setContentView(R.layout.activity_main)

            val toolbar: androidx.appcompat.widget.Toolbar? = findViewById(R.id.toolbar)
            toolbar?.let {
                setSupportActionBar(it)
                supportActionBar?.title = getString(R.string.app_name)
            }
            Toast.makeText(this, "MainActivity started", Toast.LENGTH_SHORT).show()

            val drawerLayout: DrawerLayout? = findViewById(R.id.drawer_layout)
            val navView: NavigationView? = findViewById(R.id.nav_view)
            val bottomNav: com.google.android.material.bottomnavigation.BottomNavigationView? = findViewById(R.id.bottom_nav)
            
            if (drawerLayout == null || navView == null || bottomNav == null) {
                Toast.makeText(this, "Layout initialization failed", Toast.LENGTH_LONG).show()
                return
            }
            val navController = findNavController(R.id.nav_host_fragment)
            if (savedInstanceState == null) {
                // Force-load the graph so we start at Splash every fresh launch
                val inflater = navController.navInflater
                val graph = inflater.inflate(R.navigation.nav_graph)
                navController.graph = graph
            }

            // Passing each menu ID as a set of Ids because each
            // menu should be considered as top level destinations.
            appBarConfiguration = AppBarConfiguration(
                setOf(
                    R.id.nav_home,
                    R.id.nav_donors,
                    R.id.nav_requests,
                    R.id.nav_donation_centers,
                    R.id.nav_profile
                ), drawerLayout
            )

            setupActionBarWithNavController(navController, appBarConfiguration)
            navView.setupWithNavController(navController)
            bottomNav.setupWithNavController(navController)

            // Hide bottom nav and lock drawer on Splash/Login screens
            navController.addOnDestinationChangedListener { _, destination, _ ->
                val authScreens = setOf(R.id.nav_splash, R.id.nav_login)
                if (destination.id in authScreens) {
                    bottomNav.visibility = View.GONE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                } else {
                    bottomNav.visibility = View.VISIBLE
                    drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                }
            }

            // Safely handle items that are not destinations (e.g., logout)
            navView.setNavigationItemSelectedListener { menuItem ->
                when (menuItem.itemId) {
                    R.id.nav_logout -> {
                        drawerLayout.closeDrawers()
                        Snackbar.make(
                            findViewById(R.id.drawer_layout),
                            getString(R.string.logout),
                            Snackbar.LENGTH_SHORT
                        ).show()
                        true
                    }
                    R.id.nav_chatbot -> {
                        val handled = try {
                            findNavController(R.id.nav_host_fragment).navigate(R.id.nav_chatbot)
                            true
                        } catch (_: Exception) { false }
                        drawerLayout.closeDrawers()
                        handled
                    }
                    else -> {
                        val handled = try {
                            NavigationUI.onNavDestinationSelected(menuItem, navController)
                        } catch (_: Exception) {
                            false
                        }
                        if (!handled) {
                            Snackbar.make(
                                findViewById(R.id.drawer_layout),
                                getString(R.string.error_occurred),
                                Snackbar.LENGTH_SHORT
                            ).show()
                        }
                        drawerLayout.closeDrawers()
                        handled
                    }
                }
            }
        } catch (e: Exception) {
            Snackbar.make(
                findViewById(R.id.drawer_layout),
                getString(R.string.error_occurred),
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
