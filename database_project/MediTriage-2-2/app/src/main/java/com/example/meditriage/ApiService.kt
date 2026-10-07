package com.example.meditriage

import retrofit2.Response
import retrofit2.http.*

// ── Request bodies ──────────────────────────────────────

data class RegisterRequest(
    val firstName: String, val lastName: String, val email: String,
    val phone: String, val gender: String, val bloodType: String, val dateOfBirth: String
)
data class LoginRequest(val email: String, val phone: String)
data class SessionRequest(val patientId: Int)
data class SymptomAnswer(
    val symptomName: String, val answerValue: String,
    val severityScore: Int, val durationDays: Int
)
data class SymptomsRequest(val answers: List<SymptomAnswer>)
data class DiagnosisRequest(
    val sessionId: Int, val primaryCondition: String, val otherConditions: String,
    val severityLevel: String, val basicAdvice: String,
    val recommendedSpecialty: String, val confidenceScore: Double,
    val doctorId: Int?
)
data class HistoryRequest(
    val sessionId: Int, val doctorId: Int?, val visited: String,
    val visitDate: String?, val actualDiagnosis: String?,
    val symptomSummaries: List<String> = emptyList()
)
data class AuditLogRequest(
    val patientId: Int, val actionType: String,
    val tableName: String, val recordId: Int
)

// ── Response bodies ─────────────────────────────────────

data class RegisterResponse(val success: Boolean, val patientId: Int?)
data class LoginResponse(
    val success: Boolean, val patientId: Int?,
    val firstName: String?, val lastName: String?,
    val email: String?, val phone: String?,
    val bloodType: String?, val dateOfBirth: String?, val gender: String?
)
data class SessionResponse(val success: Boolean, val sessionId: Int?)
data class DiagnosisResponse(val success: Boolean, val diagnosisId: Int?)
data class GenericSuccess(val success: Boolean)

data class DoctorResponse(
    val doctorId: Int,
    val firstName: String?,
    val lastName: String?,
    val specialtyName: String?,
    val clinicName: String?,
    val clinicAddress: String?,
    val consultationFee: Double?,
    val availableDays: String?,
    val rating: Double?,
    val phone: String?
)

data class HistoryItem(
    val sessionId: Int, val sessionDate: String?,
    val primaryCondition: String?, val severityLevel: String?,
    val recommendedSpecialty: String?, val visited: String?,
    val visitDate: String?, val actualDiagnosis: String?,
    val symptomSummaries: List<String>? = null
)

// ── Interface ───────────────────────────────────────────

interface ApiService {

    @POST("patient")
    suspend fun registerPatient(@Body body: RegisterRequest): Response<RegisterResponse>

    @POST("patient/login")
    suspend fun loginPatient(@Body body: LoginRequest): Response<LoginResponse>

    @POST("session")
    suspend fun createSession(@Body body: SessionRequest): Response<SessionResponse>

    @PATCH("session/{id}/complete")
    suspend fun completeSession(@Path("id") id: Int): Response<GenericSuccess>

    @POST("session/{id}/symptoms")
    suspend fun saveSymptoms(
        @Path("id") sessionId: Int,
        @Body body: SymptomsRequest
    ): Response<GenericSuccess>

    @POST("diagnosis")
    suspend fun saveDiagnosis(@Body body: DiagnosisRequest): Response<DiagnosisResponse>

    @GET("history/{patientId}")
    suspend fun getHistory(@Path("patientId") patientId: Int): Response<List<HistoryItem>>

    @POST("history")
    suspend fun saveHistory(@Body body: HistoryRequest): Response<GenericSuccess>

    @POST("audit")
    suspend fun saveAuditLog(@Body body: AuditLogRequest): Response<GenericSuccess>

    // Get best matching doctor by specialty keyword
    @GET("doctors/specialty/{specialty}")
    suspend fun getDoctorBySpecialty(@Path("specialty") specialty: String): Response<List<DoctorResponse>>
}
