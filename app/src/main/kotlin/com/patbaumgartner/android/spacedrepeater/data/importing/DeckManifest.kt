package com.patbaumgartner.android.spacedrepeater.data.importing

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeckManifest(
    val name: String,
    val description: String = "",
    @SerialName("cardCount")
    val declaredCardCount: Int? = null,
    val tags: List<String> = emptyList(),
    val version: Int = 1,
)
