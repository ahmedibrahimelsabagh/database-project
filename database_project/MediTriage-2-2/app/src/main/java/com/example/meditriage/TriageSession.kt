package com.example.meditriage

object TriageSession {
    var patientId: Int = -1
    var firstName: String = ""
    var lastName: String = ""
    var email: String = ""
    var phone: String = ""
    var bloodType: String = ""
    var dateOfBirth: String = ""
    var gender: String = ""

    var answers: List<String> = emptyList()
    var currentSessionId: Int = -1
    var currentDiagnosisId: Int = -1

    var aiResult: TriageAiResult? = null

    // Matched doctor from Oracle
    var matchedDoctor: DoctorResponse? = null

    fun isLoggedIn() = patientId != -1

    fun clear() {
        patientId = -1
        firstName = ""
        lastName = ""
        email = ""
        phone = ""
        bloodType = ""
        dateOfBirth = ""
        gender = ""
        answers = emptyList()
        currentSessionId = -1
        currentDiagnosisId = -1
        aiResult = null
        matchedDoctor = null
    }
}
