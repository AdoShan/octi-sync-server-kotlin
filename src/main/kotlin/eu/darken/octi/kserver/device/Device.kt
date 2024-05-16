package eu.darken.octi.kserver.device

import eu.darken.octi.kserver.account.AccountId
import eu.darken.octi.kserver.common.generateRandomKey
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import java.nio.file.Path
import java.time.Instant
import java.util.*


data class Device(
    val data: Data,
    val path: Path,
    val accountId: AccountId,
    val sync: Mutex = Mutex(),
) {
    fun isAuthorized(credentials: DeviceCredentials): Boolean {
        return accountId == credentials.accountId && data.password == credentials.devicePassword
    }

    val id: DeviceId
        get() = data.id

    val password: String
        get() = data.password

    val label: String?
        get() = data.label

    val version: String?
        get() = data.version

    @Serializable
    data class Data(
        @Contextual val id: DeviceId,
        val password: String = generateRandomKey(),
        val label: String?,
        val version: String?,
        @Contextual val addedAt: Instant = Instant.now(),
    )
}

typealias DeviceId = UUID
