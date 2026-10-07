package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ProfileFragment : Fragment(R.layout.fragment_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate header
        val fullName = "${TriageSession.firstName} ${TriageSession.lastName}".trim()
        view.findViewById<TextView>(R.id.profileNameText).text = fullName
        view.findViewById<TextView>(R.id.profileEmailText).text = TriageSession.email

        // Populate personal info card
        view.findViewById<TextView>(R.id.profileInfoCard).text =
            "PERSONAL INFO\n\n" +
            "👤 Full Name        $fullName\n" +
            "📞 Phone            ${TriageSession.phone.ifEmpty { "—" }}\n" +
            "🩸 Blood Type       ${TriageSession.bloodType.ifEmpty { "—" }}\n" +
            "🎂 Date of Birth    ${TriageSession.dateOfBirth.ifEmpty { "—" }}"

        // Tab navigation
        view.findViewById<View>(R.id.homeTab).setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }
        view.findViewById<View>(R.id.historyTab).setOnClickListener {
            findNavController().navigate(R.id.action_profile_to_history)
        }

        // Logout
        view.findViewById<View>(R.id.logoutBtn).setOnClickListener {
            TriageSession.clear()
            findNavController().navigate(R.id.action_profile_to_splash)
        }
    }
}
