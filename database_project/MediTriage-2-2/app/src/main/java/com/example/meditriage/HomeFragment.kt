package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeFragment : Fragment(R.layout.fragment_home) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Populate real patient name
        val greeting = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 0..11  -> "GOOD MORNING"
            in 12..17 -> "GOOD AFTERNOON"
            else      -> "GOOD EVENING"
        }
        view.findViewById<TextView>(R.id.greetingLabel).text = greeting
        view.findViewById<TextView>(R.id.patientNameText).text =
            "${TriageSession.firstName} ${TriageSession.lastName}".trim()

        // Load real session + visit counts
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getHistory(TriageSession.patientId)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    val visits = list.count { it.visited == "Y" }

                    view.findViewById<TextView>(R.id.sessionsCountText).text =
                        "📋\n${list.size}\nSessions"
                    view.findViewById<TextView>(R.id.visitsCountText).text =
                        "🩺\n$visits\nVisits"

                    // Show most recent session in the recent card
                    if (list.isNotEmpty()) {
                        val latest = list.first()
                        val severityIcon = when (latest.severityLevel?.lowercase()) {
                            "high risk"   -> "🔴"
                            "medium risk" -> "🟠"
                            else          -> "🟢"
                        }
                        view.findViewById<TextView>(R.id.recentSessionText).text =
                            "$severityIcon  ${latest.primaryCondition ?: "—"}\n" +
                            "${latest.sessionDate ?: ""}          ${latest.severityLevel ?: ""}"

                        if (list.size > 1) {
                            val prev = list[1]
                            val prevIcon = when (prev.severityLevel?.lowercase()) {
                                "high risk"   -> "🔴"
                                "medium risk" -> "🟠"
                                else          -> "🟢"
                            }
                            view.findViewById<TextView>(R.id.prevSessionText).text =
                                "$prevIcon  ${prev.primaryCondition ?: "—"}\n" +
                                "${prev.sessionDate ?: ""}          ${prev.severityLevel ?: ""}"
                        }
                    }
                }
            } catch (_: Exception) { /* keep placeholder text */ }
        }

        view.findViewById<View>(R.id.startTriageCard).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_triage)
        }
        view.findViewById<View>(R.id.triageTab).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_triage)
        }
        view.findViewById<View>(R.id.historyTab).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_history)
        }
        view.findViewById<View>(R.id.profileTab).setOnClickListener {
            findNavController().navigate(R.id.action_home_to_profile)
        }
    }
}
