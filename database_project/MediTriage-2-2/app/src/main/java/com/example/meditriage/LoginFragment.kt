package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class LoginFragment : Fragment(R.layout.fragment_login) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.backBtn).setOnClickListener {
            findNavController().popBackStack()
        }

        view.findViewById<View>(R.id.loginSubmitBtn).setOnClickListener {
            val email = view.findViewById<EditText>(R.id.loginEmailInput).text.toString().trim()
            val phone = view.findViewById<EditText>(R.id.loginPhoneInput).text.toString().trim()

            if (email.isEmpty() && phone.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email or phone", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.loginPatient(LoginRequest(email = email, phone = phone))
                    val body = response.body()

                    if (response.isSuccessful && body?.success == true && body.patientId != null) {
                        // Populate global session with all stored patient data
                        TriageSession.patientId   = body.patientId
                        TriageSession.firstName   = body.firstName  ?: ""
                        TriageSession.lastName    = body.lastName   ?: ""
                        TriageSession.email       = body.email      ?: ""
                        TriageSession.phone       = body.phone      ?: ""
                        TriageSession.bloodType   = body.bloodType  ?: ""
                        TriageSession.dateOfBirth = body.dateOfBirth ?: ""
                        TriageSession.gender      = body.gender     ?: ""

                        Toast.makeText(requireContext(), "Welcome back, ${TriageSession.firstName}!", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_login_to_home)
                    } else {
                        val errMsg = response.errorBody()?.string() ?: "Account not found"
                        Toast.makeText(requireContext(), "Login failed: $errMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Cannot reach server: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
