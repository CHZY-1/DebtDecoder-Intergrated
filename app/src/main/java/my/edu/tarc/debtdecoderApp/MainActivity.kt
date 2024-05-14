package my.edu.tarc.debtdecoderApp

import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.expenses_and_budget_mobileassignment.util.FirebaseStorageManager
import my.edu.tarc.debtdecoderApp.util.FirebaseSynchronizationManager
import com.example.expenses_and_budget_mobileassignment.util.getFirebaseInstance
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import my.edu.tarc.debtdecoder.R
import my.edu.tarc.debtdecoder.databinding.ActivityMainBinding
import my.edu.tarc.debtdecoder.databinding.FragmentAdviceQuizBinding

class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding
    lateinit var firebaseStorage: FirebaseStorage
    lateinit var firebaseDatabase: FirebaseDatabase
    lateinit var synchronizationManager: FirebaseSynchronizationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = getFirebaseInstance()
        firebaseStorage = FirebaseStorageManager.getDefaultStorage()
        synchronizationManager = FirebaseSynchronizationManager(firebaseStorage, firebaseDatabase, applicationContext)

        // Sync categories images from Firebase Storage to firebase real time database
        binding.btnHeaderSync.setOnClickListener {
            // To launch a coroutine
            lifecycleScope.launch {
                synchronizationManager.performFullSynchronization()
            }
        }

        // Navigation
        // Finding the NavController
        // https://dev.to/vtsen/replace-fragment-tag-with-fragmentcontainerview-causing-runtime-error-1b3p?comments_sort=oldest
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment

        val navController = navHostFragment.navController

        // Setting up the BottomNavigationView with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // main activity include the toolbar in header_toolbar file
        setSupportActionBar(binding.toolbar)
//        supportActionBar?.hide()

        navController.addOnDestinationChangedListener { _, destination, _ ->

            //don't show back button if it is main module fragment
            val showBackButton = destination.id !in
                    arrayOf(R.id.navigation_dashboard,
                        R.id.navigation_debt,
                        R.id.navigation_income,
                        R.id.navigation_expense,
                        R.id.navigation_more
                    )

            supportActionBar?.setDisplayHomeAsUpEnabled(showBackButton)
            supportActionBar?.setDisplayShowHomeEnabled(showBackButton)

            // change title in header based on module
            val titleIndex = when (destination.id) {
                R.id.navigation_dashboard -> 0
                R.id.navigation_debt -> 1
                R.id.navigation_income -> 2
                R.id.navigation_expense -> 3
                R.id.navigation_more -> 4
                R.id.navigation_advice -> 5
                R.id.myAccount -> 6
                else -> -1
            }

            // module titles string array in String.xml
            val moduleTitles = resources.getStringArray(R.array.module_titles)

            if (titleIndex != -1) {
                supportActionBar?.title =moduleTitles[titleIndex]
            }
        }

        onBackPressed(true, navController)
    }

    override fun onSupportNavigateUp(): Boolean {
        return findNavController(R.id.nav_host_fragment).navigateUp()
    }
    fun toggleHeaderSyncBtnVisibility(show: Boolean) {
        val btnHeaderSync = binding.btnHeaderSync
        btnHeaderSync .visibility = if (show) View.VISIBLE else View.GONE
    }

    fun AppCompatActivity.onBackPressed(isEnabled: Boolean, navController: NavController) {
        onBackPressedDispatcher.addCallback(this,
            object : OnBackPressedCallback(isEnabled) {
                override fun handleOnBackPressed() {
                    if (!navController.navigateUp()) {
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            })
    }
}