package me.timschneeberger.rootlessjamesdsp.liveprog

interface IPropertyCompanion {
    val definitionRegex: Regex
    fun parse(line: String, contents: String): EelBaseProperty?
}