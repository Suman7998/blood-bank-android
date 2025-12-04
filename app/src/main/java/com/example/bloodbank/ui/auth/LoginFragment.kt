package com.example.bloodbank.ui.auth

import android.app.AlertDialog
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.bloodbank.R
import com.example.bloodbank.databinding.DialogRegisterBinding
import com.example.bloodbank.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val db by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun isOnline(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnLogin.setOnClickListener {
            val identifier = binding.etUsername.text?.toString()?.trim() ?: ""
            val password = binding.etPassword.text?.toString()?.trim() ?: ""

            if (identifier.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), getString(R.string.error_field_required), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isOnline()) {
                Toast.makeText(requireContext(), "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            signInWithUsernameOrEmail(identifier, password)
        }

        binding.btnRegister.setOnClickListener { showRegisterDialog() }
        binding.btnForgot.setOnClickListener {
            Toast.makeText(requireContext(), "Default password is 123456789 for demo", Toast.LENGTH_LONG).show()
        }
    }

    private fun showRegisterDialog() {
        val dialogBinding = DialogRegisterBinding.inflate(layoutInflater)

        // Blood groups list
        val groups = resources.getStringArray(R.array.blood_types)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, groups)
        dialogBinding.etBloodGroup.setAdapter(adapter)

        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("New Register")
            .setView(dialogBinding.root)
            .setPositiveButton("Submit") { d, _ ->
                val username = dialogBinding.etUsername.text?.toString()?.trim().orEmpty()
                val password = dialogBinding.etPassword.text?.toString()?.trim().orEmpty()
                val confirm = dialogBinding.etConfirmPassword.text?.toString()?.trim().orEmpty()
                val name = dialogBinding.etName.text?.toString()?.trim().orEmpty()
                val age = dialogBinding.etAge.text?.toString()?.trim().orEmpty()
                val location = dialogBinding.etLocation.text?.toString()?.trim().orEmpty()
                val weight = dialogBinding.etWeight.text?.toString()?.trim().orEmpty()
                val height = dialogBinding.etHeight.text?.toString()?.trim().orEmpty()
                val bg = dialogBinding.etBloodGroup.text?.toString()?.trim().orEmpty()

                if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                    Toast.makeText(requireContext(), getString(R.string.error_field_required), Toast.LENGTH_SHORT).show()
                    d.dismiss()
                    return@setPositiveButton
                }
                if (password != confirm) {
                    Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                    return@setPositiveButton
                }
                if (password.length < 6) {
                    Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    d.dismiss()
                    return@setPositiveButton
                }

                registerUser(username, password, name, age, location, weight, height, bg)
                d.dismiss()
            }
            .setNegativeButton("Cancel") { d, _ -> d.dismiss() }
            .create()

        dialog.show()
    }

    private fun syntheticEmailFor(usernameLower: String): String {
        // Dedicated domain to avoid collisions with real emails
        return "$usernameLower@users.bloodbank.local"
    }

    private fun registerUser(
        username: String,
        password: String,
        fullName: String,
        age: String,
        location: String,
        weight: String,
        height: String,
        bloodGroup: String
    ) {
        val usernameLower = username.lowercase()
        if (!usernameLower.matches(Regex("^[a-z0-9_]{3,20}$"))) {
            Toast.makeText(requireContext(), "Invalid username. Use 3-20 chars: a-z, 0-9, _", Toast.LENGTH_LONG).show()
            return
        }

        if (!isOnline()) {
            Toast.makeText(requireContext(), "No internet connection. Please connect and try again.", Toast.LENGTH_LONG).show()
            return
        }

        val email = syntheticEmailFor(usernameLower)

        // Create auth user with synthetic email
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { authResult ->
                val uid = authResult.user?.uid ?: run {
                    Toast.makeText(requireContext(), "Registration failed: No UID", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                val profile = hashMapOf(
                    "email" to email,
                    "username" to usernameLower,
                    "fullName" to fullName,
                    "age" to age,
                    "location" to location,
                    "weight" to weight,
                    "height" to height,
                    "bloodGroup" to bloodGroup,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                // Reserve username and create profile atomically
                db.runTransaction { txn ->
                    val unameRef = db.collection("usernames").document(usernameLower)
                    val existing = txn.get(unameRef)
                    if (existing.exists()) {
                        throw IllegalStateException("USERNAME_TAKEN")
                    }
                    val userRef = db.collection("users").document(uid)
                    txn.set(unameRef, mapOf("uid" to uid))
                    txn.set(userRef, profile)
                }
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Registration successful", Toast.LENGTH_SHORT).show()
                        try { findNavController().navigate(R.id.nav_home) } catch (_: Exception) {}
                    }
                    .addOnFailureListener { e ->
                        val isTaken = e.message?.contains("USERNAME_TAKEN") == true
                        if (isTaken) {
                            // Only delete auth user if username is already taken
                            auth.currentUser?.delete()
                            Toast.makeText(requireContext(), "Username already taken", Toast.LENGTH_LONG).show()
                        } else {
                            // Keep the auth user so it remains visible in Firebase Authentication
                            val message = when (e) {
                                is FirebaseFirestoreException -> when (e.code) {
                                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Permission denied in Firestore. Please adjust Firestore rules."
                                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Firestore unavailable. Check your internet connection and try again."
                                    else -> e.localizedMessage ?: "Registration partially completed"
                                }
                                else -> e.localizedMessage ?: "Registration partially completed"
                            }
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                        }
                    }
            }
            .addOnFailureListener { e ->
                val message = when (e) {
                    is FirebaseAuthException -> when (e.errorCode) {
                        "ERROR_EMAIL_ALREADY_IN_USE" -> "Username already taken"
                        "ERROR_OPERATION_NOT_ALLOWED" -> "Enable Email/Password in Firebase Authentication settings"
                        "ERROR_NETWORK_REQUEST_FAILED" -> "Network error. Please check your internet connection and try again."
                        else -> e.localizedMessage ?: "Registration failed"
                    }
                    else -> e.localizedMessage ?: "Registration failed"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
            }
    }

    private fun signInWithUsernameOrEmail(identifier: String, password: String) {
        val trimmed = identifier.trim()
        if (trimmed.contains("@")) {
            // Treat as email
            auth.signInWithEmailAndPassword(trimmed, password)
                .addOnSuccessListener {
                    try { findNavController().navigate(R.id.nav_home) } catch (_: Exception) {}
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(), "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            return
        }

        val unameLower = trimmed.lowercase()
        val email = syntheticEmailFor(unameLower)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                try { findNavController().navigate(R.id.nav_home) } catch (_: Exception) {}
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Login failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
