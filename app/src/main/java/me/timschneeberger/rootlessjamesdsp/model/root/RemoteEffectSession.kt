package me.timschneeberger.rootlessjamesdsp.model.root

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.timschneeberger.rootlessjamesdsp.interop.JamesDspRemoteEngine
import me.timschneeberger.rootlessjamesdsp.model.IEffectSession

data class RemoteEffectSession(
    override var packageName: String,
    override var uid: Int,
    var effect: JamesDspRemoteEngine?
) : CoroutineScope by CoroutineScope(Dispatchers.Default), IEffectSession {
    override fun toString(): String {
        return "package=$packageName; uid=$uid"
    }
}