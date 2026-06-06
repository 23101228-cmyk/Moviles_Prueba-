package com.soltis.mya.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class BankCard(
    val bank: String,
    val type: String,
    val account: String,
    val cci: String
)

data class UserProfile(
    val fullName: String,
    val username: String,
    val email: String,
    val password: String,
    val phone: String = "",
    val yape: String = "",
    val plin: String = "",
    val bankCards: List<BankCard> = emptyList(),
    val balances: Map<String, Double> = emptyMap(),
    val heldBalances: Map<String, Double> = emptyMap()
)

class LocalUserStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        seedDemoUsersIfNeeded()
    }

    fun login(email: String, password: String): Boolean {
        val normalizedEmail = email.lowercase()
        val user = getUsers().firstOrNull { it.email.lowercase() == normalizedEmail && it.password == password }
        return if (user != null) {
            prefs.edit().putString(KEY_CURRENT_EMAIL, user.email).apply()
            true
        } else {
            false
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
    }

    fun registerUser(fullName: String, username: String, email: String, password: String): RegisterResult {
        val normalizedEmail = email.lowercase()
        val users = getUsers().toMutableList()
        if (users.any { it.email.lowercase() == normalizedEmail }) {
            return RegisterResult.EmailAlreadyExists
        }

        users.add(
            UserProfile(
                fullName = fullName,
                username = username,
                email = normalizedEmail,
                password = password,
                balances = zeroBalances(),
                heldBalances = zeroBalances()
            )
        )
        saveUsers(users)
        return RegisterResult.Success
    }

    fun getCurrentUser(): UserProfile? {
        val currentEmail = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        return getUsers().firstOrNull { it.email.equals(currentEmail, ignoreCase = true) }
    }

    fun getUserByEmail(email: String): UserProfile? {
        return getUsers().firstOrNull { it.email.equals(email, ignoreCase = true) }
    }

    fun updateCurrentUser(updated: UserProfile) {
        val currentEmail = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        val users = getUsers().map { user ->
            if (user.email.equals(currentEmail, ignoreCase = true)) {
                updated.copy(email = currentEmail, password = user.password, username = updated.username.ifBlank { user.username })
            } else {
                user
            }
        }
        saveUsers(users)
    }

    fun updateCurrentWallet(balances: Map<String, Double>, heldBalances: Map<String, Double>) {
        val current = getCurrentUser() ?: return
        updateCurrentUser(current.copy(balances = balances, heldBalances = heldBalances))
    }

    fun maskAccount(value: String): String {
        val digits = value.filter { it.isDigit() }
        if (digits.length <= 8) return digits.ifBlank { "Pendiente" }
        return "${digits.take(4)}${"*".repeat((digits.length - 8).coerceAtLeast(0))}${digits.takeLast(4)}"
    }

    fun maskCci(value: String): String {
        val digits = value.filter { it.isDigit() }
        if (digits.isBlank()) return "Pendiente"
        if (digits.length <= 4) return digits
        return "${"*".repeat(digits.length - 4)}${digits.takeLast(4)}"
    }

    private fun seedDemoUsersIfNeeded() {
        if (prefs.getBoolean(KEY_SEEDED, false) && prefs.getString(KEY_USERS, "[]") != "[]") return

        saveUsers(
            listOf(
                UserProfile(
                    fullName = "Kevin Quispe",
                    username = "kevinq",
                    email = "kevin.quispe@email.com",
                    password = "12345678",
                    phone = "+51 987 654 321",
                    yape = "987654321",
                    plin = "987654321",
                    bankCards = listOf(
                        BankCard(
                            bank = "BCP",
                            type = "Cuenta de ahorros",
                            account = "19112345678912",
                            cci = "00219100123456789012"
                        )
                    ),
                    balances = mapOf("PEN" to 8240.50, "USD" to 2150.00, "EUR" to 66.28),
                    heldBalances = mapOf("PEN" to 390.00, "USD" to 45.00, "EUR" to 0.0)
                ),
                UserProfile(
                    fullName = "Ana Torres",
                    username = "anatorres",
                    email = "ana.torres@email.com",
                    password = "12345678",
                    phone = "+51 955 120 888",
                    yape = "955120888",
                    plin = "955120888",
                    bankCards = listOf(
                        BankCard(
                            bank = "BBVA",
                            type = "Cuenta corriente",
                            account = "001112223333444455",
                            cci = "01100111222333344455"
                        )
                    ),
                    balances = mapOf("PEN" to 1200.00, "USD" to 350.00, "EUR" to 0.0),
                    heldBalances = zeroBalances()
                )
            )
        )
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }

    private fun getUsers(): List<UserProfile> {
        val raw = prefs.getString(KEY_USERS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return (0 until array.length()).map { index -> array.getJSONObject(index).toUserProfile() }
    }

    private fun saveUsers(users: List<UserProfile>) {
        val array = JSONArray()
        users.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_USERS, array.toString()).apply()
    }

    private fun zeroBalances() = mapOf("PEN" to 0.0, "USD" to 0.0, "EUR" to 0.0)

    private fun UserProfile.toJson(): JSONObject {
        return JSONObject()
            .put("fullName", fullName)
            .put("username", username)
            .put("email", email)
            .put("password", password)
            .put("phone", phone)
            .put("yape", yape)
            .put("plin", plin)
            .put("bankCards", JSONArray().also { cards ->
                bankCards.forEach { card ->
                    cards.put(
                        JSONObject()
                            .put("bank", card.bank)
                            .put("type", card.type)
                            .put("account", card.account)
                            .put("cci", card.cci)
                    )
                }
            })
            .put("balances", balances.toJsonObject())
            .put("heldBalances", heldBalances.toJsonObject())
    }

    private fun JSONObject.toUserProfile(): UserProfile {
        return UserProfile(
            fullName = optString("fullName"),
            username = optString("username"),
            email = optString("email"),
            password = optString("password"),
            phone = optString("phone"),
            yape = optString("yape"),
            plin = optString("plin"),
            bankCards = optJSONArray("bankCards").toBankCards(),
            balances = optJSONObject("balances").toBalanceMap(),
            heldBalances = optJSONObject("heldBalances").toBalanceMap()
        )
    }

    private fun JSONArray?.toBankCards(): List<BankCard> {
        if (this == null) return emptyList()
        return (0 until length()).map { index ->
            val card = getJSONObject(index)
            BankCard(
                bank = card.optString("bank"),
                type = card.optString("type"),
                account = card.optString("account"),
                cci = card.optString("cci")
            )
        }
    }

    private fun Map<String, Double>.toJsonObject(): JSONObject {
        val obj = JSONObject()
        forEach { (key, value) -> obj.put(key, value) }
        return obj
    }

    private fun JSONObject?.toBalanceMap(): Map<String, Double> {
        if (this == null) return zeroBalances()
        return mapOf(
            "PEN" to optDouble("PEN", 0.0),
            "USD" to optDouble("USD", 0.0),
            "EUR" to optDouble("EUR", 0.0)
        )
    }

    sealed class RegisterResult {
        data object Success : RegisterResult()
        data object EmailAlreadyExists : RegisterResult()
    }

    companion object {
        private const val PREFS_NAME = "p2p_local_users"
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_EMAIL = "current_email"
        private const val KEY_SEEDED = "seeded"
    }
}
