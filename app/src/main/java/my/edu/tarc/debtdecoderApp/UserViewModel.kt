package com.example.expenses_and_budget_mobileassignment

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class UserViewModel : ViewModel() {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> get() = _userData

    // Save or update user data in the database
    fun saveUserInfo(user: FirebaseUser, fullName: String, age: String, income: String) {
        val uid = user.uid
        val userInfo = User(
            uid = uid,
            email = user.email,
            fullName = fullName,
            age = age,
            income = income
        )

        database.child("users").child(uid).setValue(userInfo)
            .addOnSuccessListener {
                Log.d("UserViewModel", "User information saved successfully")
                _userData.value = userInfo
            }
            .addOnFailureListener {
                Log.e("UserViewModel", "Failed to save user data: ${it.message}")
            }
    }

    // Load user data from the database based on the user's UID
    fun loadUserInfo() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            database.child("users").child(uid).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val userInfo = snapshot.getValue(User::class.java)
                    // Include the UID in the User object if not already present
                    _userData.value = userInfo?.copy(uid = uid)
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("UserViewModel", "Failed to load user data: ${error.message}")
                }
            })
        }
    }
}

