package me.timschneeberger.rootlessjamesdsp.model

import java.io.Serializable
import java.util.*

data class GraphicEqNode(
    val freq: Double,
    val gain: Double,
    val uuid: UUID = UUID.randomUUID() /* Uniquely identifies one node during an editing session */
) : Serializable