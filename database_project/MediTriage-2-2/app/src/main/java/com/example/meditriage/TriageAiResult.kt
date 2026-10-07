package com.example.meditriage

data class TriageAiResult(
    val severity: String,
    val possibleCondition: String,
    val confidence: String,
    val advice: String,
    val recommendedDoctor: String
)