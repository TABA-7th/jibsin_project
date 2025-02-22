package com.project.jibsin_project.api.model

import com.google.gson.annotations.SerializedName

data class AnalysisRequest(
    @SerializedName("userId") val userId: String,
    @SerializedName("contractId") val contractId: String
    //@SerializedName("building_registry_url") val buildingRegistryUrl: String?,
    //@SerializedName("registry_document_url") val registryDocumentUrl: String?,
    //@SerializedName("contract_url") val contractUrl: String?
)