package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class ResultFragment : Fragment(R.layout.fragment_result) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val result = TriageSession.aiResult

        if (result != null) {
            view.findViewById<TextView>(R.id.severityText).text =
                "⚠️ Severity Level\n${result.severity}\nBased on your symptom answers"

            view.findViewById<TextView>(R.id.conditionText).text =
                result.possibleCondition

            view.findViewById<TextView>(R.id.confidenceText).text =
                "${result.confidence} confidence"

            view.findViewById<TextView>(R.id.adviceText).text =
                "💊 BASIC ADVICE\n\n${result.advice}"

            view.findViewById<TextView>(R.id.doctorText).text =
                "Recommended specialty:\n${result.recommendedDoctor}"
        }

        view.findViewById<View>(R.id.backBtn).setOnClickListener {
            findNavController().navigate(R.id.homeFragment)
        }

        view.findViewById<View>(R.id.viewDoctorBtn).setOnClickListener {
            findNavController().navigate(R.id.action_result_to_doctor)
        }

        view.findViewById<View>(R.id.confirmVisitBtn).setOnClickListener {
            findNavController().navigate(R.id.action_result_to_confirm)
        }
    }
}