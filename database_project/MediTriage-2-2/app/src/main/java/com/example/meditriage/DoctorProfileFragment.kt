package com.example.meditriage

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DoctorProfileFragment : Fragment(R.layout.fragment_doctor_profile) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.backBtn).setOnClickListener {
            findNavController().popBackStack()
        }

        val doctor = TriageSession.matchedDoctor
        val result = TriageSession.aiResult

        if (doctor != null) {
            // Show real doctor info from Oracle
            view.findViewById<TextView?>(R.id.doctorNameText)?.text =
                "Dr. ${doctor.firstName} ${doctor.lastName}"

            view.findViewById<TextView?>(R.id.doctorSpecialtyText)?.text =
                doctor.specialtyName ?: result?.recommendedDoctor ?: "Specialist"

            view.findViewById<TextView?>(R.id.doctorClinicText)?.text =
                "📍 ${doctor.clinicName ?: "Medical Center"}\n${doctor.clinicAddress ?: ""}"

            view.findViewById<TextView?>(R.id.doctorFeeText)?.text =
                "${doctor.consultationFee?.toInt() ?: "—"} EGP / Visit"

            view.findViewById<TextView?>(R.id.doctorDaysText)?.text =
                "📅 ${doctor.availableDays ?: "Call for availability"}"

            view.findViewById<TextView?>(R.id.doctorRatingText)?.text =
                "⭐ ${doctor.rating ?: "—"}"

            view.findViewById<TextView?>(R.id.doctorPhoneText)?.text =
                "📞 ${doctor.phone ?: "+20 100 000 0000"}"

            // Book Appointment — use real doctor phone
            view.findViewById<View?>(R.id.bookAppointmentBtn)?.setOnClickListener {
                val phone = doctor.phone ?: "+20100000000"
                try {
                    val intent = Intent(Intent.ACTION_DIAL,
                        Uri.parse("tel:${phone.replace(" ", "")}"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Cannot open dialer", Toast.LENGTH_SHORT).show()
                }
            }

        } else {
            // Fallback — no doctor matched
            view.findViewById<TextView?>(R.id.doctorSpecialtyText)?.text =
                result?.recommendedDoctor ?: "General Practitioner"

            view.findViewById<View?>(R.id.bookAppointmentBtn)?.setOnClickListener {
                try {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:+20100000000"))
                    startActivity(intent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Cannot open dialer", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Save audit log
        val patientId = TriageSession.patientId
        if (patientId != -1) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    ApiClient.api.saveAuditLog(
                        AuditLogRequest(
                            patientId  = patientId,
                            actionType = "VIEW_DOCTOR_PROFILE",
                            tableName  = "DOCTOR",
                            recordId   = doctor?.doctorId ?: 0
                        )
                    )
                } catch (e: Exception) { /* silent */ }
            }
        }
    }
}
