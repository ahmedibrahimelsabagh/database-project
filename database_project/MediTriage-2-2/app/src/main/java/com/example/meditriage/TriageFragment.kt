package com.example.meditriage

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class TriageFragment : Fragment(R.layout.fragment_triage) {

    private data class TriageQuestion(
        val icon: String,
        val question: String,
        val subtitle: String,
        val options: List<String>
    )

    private val questions = listOf(
        TriageQuestion(
            icon = "🤒",
            question = "Do you have a fever?",
            subtitle = "Select what best describes your temperature",
            options = listOf(
                "High fever above 38.5°C",
                "Mild fever 37.5°C - 38.5°C",
                "No fever"
            )
        ),
        TriageQuestion(
            icon = "😮‍💨",
            question = "Do you have breathing difficulty?",
            subtitle = "Choose the closest option",
            options = listOf(
                "Severe shortness of breath",
                "Mild breathing difficulty",
                "No breathing difficulty"
            )
        ),
        TriageQuestion(
            icon = "🤕",
            question = "Do you have pain?",
            subtitle = "Choose your pain level",
            options = listOf(
                "Severe pain",
                "Moderate pain",
                "Mild pain",
                "No pain"
            )
        ),
        TriageQuestion(
            icon = "🧠",
            question = "Do you feel dizzy or confused?",
            subtitle = "This helps detect emergency signs",
            options = listOf(
                "Very dizzy or confused",
                "Slight dizziness",
                "No dizziness or confusion"
            )
        ),
        TriageQuestion(
            icon = "🤢",
            question = "Do you have nausea or vomiting?",
            subtitle = "Select what applies to you",
            options = listOf(
                "Repeated vomiting",
                "Nausea only",
                "No nausea or vomiting"
            )
        ),
        TriageQuestion(
            icon = "🩸",
            question = "Do you have bleeding?",
            subtitle = "Choose the most accurate answer",
            options = listOf(
                "Heavy bleeding",
                "Minor bleeding",
                "No bleeding"
            )
        ),
        TriageQuestion(
            icon = "⏱️",
            question = "How long have you had these symptoms?",
            subtitle = "Duration helps estimate urgency",
            options = listOf(
                "Less than 24 hours",
                "1 to 3 days",
                "More than 3 days",
                "More than 1 week"
            )
        )
    )

    private var currentQuestionIndex = 0
    private var selectedAnswer: String? = null
    private val answers = mutableListOf<String>()

    private lateinit var questionCounterText: TextView
    private lateinit var questionIcon: TextView
    private lateinit var questionTitle: TextView
    private lateinit var questionSubtitle: TextView
    private lateinit var optionsContainer: LinearLayout
    private lateinit var progressBar: ProgressBar
    private lateinit var continueBtn: View

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        questionCounterText = view.findViewById(R.id.questionCounterText)
        questionIcon = view.findViewById(R.id.questionIcon)
        questionTitle = view.findViewById(R.id.questionTitle)
        questionSubtitle = view.findViewById(R.id.questionSubtitle)
        optionsContainer = view.findViewById(R.id.optionsContainer)
        progressBar = view.findViewById(R.id.progressBar)
        continueBtn = view.findViewById(R.id.continueBtn)

        view.findViewById<View>(R.id.backBtn).setOnClickListener {
            if (currentQuestionIndex > 0) {
                currentQuestionIndex--
                if (answers.isNotEmpty()) {
                    answers.removeAt(answers.lastIndex)
                }
                showQuestion()
            } else {
                findNavController().popBackStack()
            }
        }

        continueBtn.setOnClickListener {
            val answer = selectedAnswer

            if (answer == null) {
                Toast.makeText(requireContext(), "Please select an answer", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            answers.add(answer)

            if (currentQuestionIndex < questions.lastIndex) {
                currentQuestionIndex++
                showQuestion()
            } else {
                TriageSession.answers = answers.toList()
                findNavController().navigate(R.id.action_triage_to_analyzing)
            }
        }

        showQuestion()
    }

    private fun showQuestion() {
        selectedAnswer = null

        val question = questions[currentQuestionIndex]

        questionCounterText.text = "Question ${currentQuestionIndex + 1} of ${questions.size}"
        questionIcon.text = question.icon
        questionTitle.text = question.question
        questionSubtitle.text = question.subtitle

        val progress = (((currentQuestionIndex + 1).toDouble() / questions.size) * 100).toInt()
        progressBar.progress = progress

        optionsContainer.removeAllViews()

        question.options.forEach { optionText ->
            val optionView = createOptionView(optionText)

            optionView.setOnClickListener {
                selectOption(optionView, optionText)
            }

            optionsContainer.addView(optionView)
        }

        val buttonText = if (currentQuestionIndex == questions.lastIndex) {
            "Analyze Symptoms →"
        } else {
            "Continue →"
        }

        if (continueBtn is android.widget.Button) {
            (continueBtn as android.widget.Button).text = buttonText
        }
    }

    private fun createOptionView(optionText: String): TextView {
        val optionView = TextView(requireContext())

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            72.dp()
        )
        params.setMargins(0, 0, 0, 10.dp())

        optionView.layoutParams = params
        optionView.setBackgroundResource(R.drawable.bg_option_normal)
        optionView.gravity = android.view.Gravity.CENTER_VERTICAL
        optionView.setPadding(16.dp(), 0, 16.dp(), 0)
        optionView.text = optionText
        optionView.textSize = 14f
        optionView.setTextColor(resources.getColor(R.color.ink, null))
        optionView.setTypeface(null, android.graphics.Typeface.BOLD)

        return optionView
    }

    private fun selectOption(selectedView: View, answer: String) {
        for (i in 0 until optionsContainer.childCount) {
            optionsContainer.getChildAt(i).setBackgroundResource(R.drawable.bg_option_normal)
        }

        selectedView.setBackgroundResource(R.drawable.bg_option_selected)
        selectedAnswer = answer
    }

    private fun Int.dp(): Int {
        return (this * resources.displayMetrics.density).toInt()
    }
}