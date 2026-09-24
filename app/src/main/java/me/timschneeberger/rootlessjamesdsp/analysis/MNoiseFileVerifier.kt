package me.timschneeberger.rootlessjamesdsp.analysis

object MNoiseFileVerifier {
    const val OFFICIAL_SHA256 = "50bfcd4e5b9b7fcb07a0d34079198d076f978434c2971d80472c4c4ade66ec15"
    const val OFFICIAL_MD5 = "6539f08317d36216c3e0c37cf68c2b38"

    fun isVerifiedMNoise(sha256: String, md5: String?): Boolean {
        return sha256.equals(OFFICIAL_SHA256, ignoreCase = true) ||
            md5?.equals(OFFICIAL_MD5, ignoreCase = true) == true
    }
}
