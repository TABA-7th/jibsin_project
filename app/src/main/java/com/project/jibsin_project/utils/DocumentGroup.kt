package com.project.jibsin_project.utils

data class DocumentGroup(
    val groupId: String = "",
    val userId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val status: String = "pending",
    val documentSets: Map<String, List<String>> = mapOf(
        "building_registry" to listOf(),
        "registry_document" to listOf(),
        "contract" to listOf()
    )
)