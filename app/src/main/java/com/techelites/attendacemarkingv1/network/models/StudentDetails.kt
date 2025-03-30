package com.techelites.attendacemarkingv1.network.models

data class StudentDetails(
    val name: String = "",
    val college: String = "",
    val department: String = "",
    val year: Int = 0,
    val section: String = "",
    val photoUrl: String = "",
    val uid: String? = null,
    val usn: String? = null,
    val message: String = "",
    val userType: String = "",
    val additionalInfo: MutableMap<String, String> = mutableMapOf()
)