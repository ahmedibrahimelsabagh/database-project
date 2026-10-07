package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConfirmVisitFragment : Fragment(R.layout.fragment_confirm_visit) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.yesVisitedBtn).setOnClickListener {
            saveHistory(visited = "Y")
        }
        view.findViewById<View>(R.id.noVisitedBtn).setOnClickListener {
            saveHistory(visited = "N")
        }
    }

    private fun saveHistory(visited: String) {
        val sessionId = TriageSession.currentSessionId
        if (sessionId == -1) {
            findNavController().navigate(R.id.action_confirm_to_history)
            return
        }

        // ✅ Get real doctor ID from matched doctor
        val doctorId = TriageSession.matchedDoctor?.doctorId

        // Build symptom summaries
        val labels = listOf("Fever", "Breathing", "Pain", "Dizziness", "Nausea", "Bleeding", "Duration")
        val symptomSummaries = TriageSession.answers.mapIndexed { i, ans ->
            "${labels.getOrElse(i) { "Symptom ${i + 1}" }}: $ans"
        }

        lifecycleScope.launch {
            try {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

                ApiClient.api.saveHistory(
                    HistoryRequest(
                        sessionId        = sessionId,
                        doctorId         = doctorId,   // ✅ real doctor ID
                        visited          = visited,
                        visitDate        = if (visited == "Y") today else null,
                        actualDiagnosis  = if (visited == "Y")
                            TriageSession.aiResult?.possibleCondition else null,
                        symptomSummaries = symptomSummaries
                    )
                )

                // Audit log
                try {
                    ApiClient.api.saveAuditLog(
                        AuditLogRequest(
                            patientId  = TriageSession.patientId,
                            actionType = if (visited == "Y") "VISIT_CONFIRMED" else "VISIT_DECLINED",
                            tableName  = "PATIENT_HISTORY",
                            recordId   = sessionId
                        )
                    )
                } catch (e: Exception) { /* silent */ }

                val msg = if (visited == "Y") "Visit recorded ✅" else "Noted — no visit yet"
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(),
                    "Could not save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            findNavController().navigate(R.id.action_confirm_to_history)
        }
    }
}
