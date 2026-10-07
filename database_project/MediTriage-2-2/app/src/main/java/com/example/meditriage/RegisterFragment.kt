package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class RegisterFragment : Fragment(R.layout.fragment_register) {

    private var selectedGender = "M"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.backBtn).setOnClickListener {
            findNavController().popBackStack()
        }

        val maleBtn   = view.findViewById<View>(R.id.maleBtn)
        val femaleBtn = view.findViewById<View>(R.id.femaleBtn)

        maleBtn.setOnClickListener {
            selectedGender = "M"
            maleBtn.setBackgroundResource(R.drawable.bg_option_selected)
            femaleBtn.setBackgroundResource(R.drawable.bg_input)
        }
        femaleBtn.setOnClickListener {
            selectedGender = "F"
            femaleBtn.setBackgroundResource(R.drawable.bg_option_selected)
            maleBtn.setBackgroundResource(R.drawable.bg_input)
        }

        view.findViewById<View>(R.id.createAccountBtn).setOnClickListener {
            val firstName = view.findViewById<EditText>(R.id.firstNameInput).text.toString().trim()
            val lastName  = view.findViewById<EditText>(R.id.lastNameInput).text.toString().trim()
            val email     = view.findViewById<EditText>(R.id.emailInput).text.toString().trim()
            val phone     = view.findViewById<EditText>(R.id.phoneInput).text.toString().trim()
            val blood     = view.findViewById<EditText>(R.id.bloodInput).text.toString().trim()
            val dob       = view.findViewById<EditText>(R.id.dobInput).text.toString().trim()

            if (firstName.isEmpty() || lastName.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (email.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.registerPatient(
                        RegisterRequest(
                            firstName   = firstName,
                            lastName    = lastName,
                            email       = email,
                            phone       = phone,
                            gender      = selectedGender,
                            bloodType   = blood,
                            dateOfBirth = dob
                        )
                    )
                    val body = response.body()
                    if (response.isSuccessful && body?.success == true && body.patientId != null) {
                        TriageSession.patientId   = body.patientId
                        TriageSession.firstName   = firstName
                        TriageSession.lastName    = lastName
                        TriageSession.email       = email
                        TriageSession.phone       = phone
                        TriageSession.bloodType   = blood
                        TriageSession.dateOfBirth = dob
                        TriageSession.gender      = selectedGender

                        Toast.makeText(requireContext(), "Account created ✅", Toast.LENGTH_SHORT).show()
                        findNavController().navigate(R.id.action_register_to_home)
                    } else {
                        val errMsg = response.errorBody()?.string() ?: "Unknown error"
                        Toast.makeText(requireContext(), "Server error: $errMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Cannot reach server. Check your connection.", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
