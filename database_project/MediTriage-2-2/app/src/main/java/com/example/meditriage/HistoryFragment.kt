package com.example.meditriage

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

class HistoryFragment : Fragment(R.layout.fragment_history) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.homeTab).setOnClickListener {
            findNavController().navigate(R.id.action_history_to_home)
        }
        view.findViewById<View>(R.id.profileTab).setOnClickListener {
            findNavController().navigate(R.id.action_history_to_profile)
        }

        loadHistory(view)
    }

    private fun loadHistory(view: View) {
        val container = view.findViewById<LinearLayout>(R.id.historyContainer)

        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getHistory(TriageSession.patientId)
                if (resp.isSuccessful) {
                    val list = resp.body() ?: emptyList()
                    container.removeAllViews()

                    if (list.isEmpty()) {
                        val empty = TextView(requireContext()).apply {
                            text = "No history yet.\nComplete a triage to get started."
                            gravity = Gravity.CENTER
                            setPadding(0, 60, 0, 0)
                            setTextColor(resources.getColor(R.color.slate, null))
                        }
                        container.addView(empty)
                        return@launch
                    }

                    // Group by month label
                    var lastMonth = ""
                    list.forEach { item ->
                        val monthLabel = formatMonthLabel(item.sessionDate)
                        if (monthLabel != lastMonth) {
                            lastMonth = monthLabel
                            val header = TextView(requireContext()).apply {
                                text = monthLabel
                                setTextColor(resources.getColor(R.color.fog, null))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                val p = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.WRAP_CONTENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                                p.topMargin = 18.dp()
                                layoutParams = p
                            }
                            container.addView(header)
                        }

                        val severityIcon = when (item.severityLevel?.lowercase()) {
                            "high risk"   -> "🔴"
                            "medium risk" -> "🟠"
                            else          -> "🟢"
                        }
                        val visitStatus = when (item.visited) {
                            "Y" -> "  ✅ Visited"
                            "N" -> "  ❌ Not visited"
                            else -> ""
                        }

                        val card = TextView(requireContext()).apply {
                            text = "${formatDay(item.sessionDate)}   ${item.primaryCondition ?: "—"}\n" +
                                   "${item.recommendedSpecialty ?: ""}$visitStatus\n" +
                                   "$severityIcon ${item.severityLevel ?: ""}"
                            setBackgroundResource(R.drawable.bg_card_white)
                            setTextColor(resources.getColor(R.color.ink, null))
                            setTypeface(null, android.graphics.Typeface.BOLD)
                            setPadding(16.dp(), 14.dp(), 16.dp(), 14.dp())
                            gravity = Gravity.CENTER_VERTICAL
                            val p = LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                90.dp()
                            )
                            p.topMargin = 8.dp()
                            layoutParams = p
                        }
                        container.addView(card)
                    }
                }
            } catch (e: Exception) {
                val errView = TextView(requireContext()).apply {
                    text = "Could not load history.\nCheck your connection."
                    gravity = Gravity.CENTER
                    setPadding(0, 60, 0, 0)
                    setTextColor(resources.getColor(R.color.slate, null))
                }
                container.removeAllViews()
                container.addView(errView)
            }
        }
    }

    private fun formatMonthLabel(date: String?): String {
        if (date.isNullOrBlank()) return "UNKNOWN DATE"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d = sdf.parse(date) ?: return date
            java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
                .format(d).uppercase()
        } catch (_: Exception) { date }
    }

    private fun formatDay(date: String?): String {
        if (date.isNullOrBlank()) return "—"
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val d = sdf.parse(date) ?: return date
            java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault()).format(d)
        } catch (_: Exception) { date }
    }

    private fun Int.dp() = (this * resources.displayMetrics.density).toInt()
}
