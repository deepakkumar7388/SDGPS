package com.example.digitalpass

import com.google.gson.annotations.SerializedName

data class SyncInterInstitutionalGatePassResponse(
    @SerializedName("updatedGatePasses")
    val updatedGatePasses: List<HashMap<String, String>>,

    @SerializedName("deletedGatePassIds")
    val deletedGatePassIds: List<Int>,

    @SerializedName("hasMore")
    val hasMore: Boolean,

    @SerializedName("serverTime")
    val serverTime: Long
)
