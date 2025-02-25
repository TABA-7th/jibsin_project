package com.project.jibsin_project.api

import com.project.jibsin_project.api.model.AnalysisRequest
import com.project.jibsin_project.api.model.AnalysisResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("ai/start_analysis/")
    suspend fun startAnalysis(@Body request: AnalysisRequest): AnalysisResponse
}