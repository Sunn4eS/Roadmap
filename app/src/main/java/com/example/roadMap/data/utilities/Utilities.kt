package com.example.roadMap.data.utilities

fun hashPassword(password: String): String {
    val bytes = password.toByteArray()
    val md = java.security.MessageDigest.getInstance("SHA-256")
    val digest = md.digest(bytes)
    return digest.joinToString("") { "%02x".format(it) }
}
fun comparePassword(inputPassword: String, hashedPassword: String): Boolean {
    val inputHash = hashPassword(inputPassword) // Используйте ту же функцию хеширования, что и при регистрации
    return inputHash == hashedPassword
}