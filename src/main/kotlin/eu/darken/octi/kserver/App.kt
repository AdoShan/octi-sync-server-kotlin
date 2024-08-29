package eu.darken.octi.kserver

import eu.darken.octi.kserver.common.AppScope
import eu.darken.octi.kserver.common.RateLimitConfig
import eu.darken.octi.kserver.common.debug.logging.ConsoleLogger
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.*
import eu.darken.octi.kserver.common.debug.logging.log
import eu.darken.octi.kserver.common.debug.logging.logTag
import java.nio.file.Path
import java.time.Duration
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.absolute
import kotlin.reflect.full.memberProperties


class App @Inject constructor(
    val appScope: AppScope,
    private val server: Server,
) {

    fun launch() {
        server.start()
    }

    fun isRunning(): Boolean {
        return server.isRunning()
    }

    fun shutdown() {
        server.stop()
    }

    data class Config(
        val isDebug: Boolean = false,
        val port: Int,
        val dataPath: Path,
        val rateLimit: RateLimitConfig? = RateLimitConfig(),
        val payloadLimit: Long? = 128 * 1024L,
        val accountGCInterval: Duration = Duration.ofMinutes(10),
        val shareExpiration: Duration = Duration.ofMinutes(60),
        val shareGCInterval: Duration = Duration.ofMinutes(10),
        val deviceExpiration: Duration = Duration.ofDays(90),
        val deviceGCInterval: Duration = Duration.ofMinutes(10),
        val moduleExpiration: Duration = Duration.ofDays(90),
        val moduleGCInterval: Duration = Duration.ofMinutes(10),
    )

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            log(TAG, INFO) { "Program arguments: ${args.joinToString()}" }

            val config = Config(
                isDebug = args.any { it.startsWith("--debug") },
                port = args
                    .singleOrNull { it.startsWith("--port") }
                    ?.substringAfter('=')?.toInt()
                    ?: 8080,
                dataPath = args
                    .single { it.startsWith("--datapath") }
                    .let { Path(it.substringAfter('=')) }
                    .absolute(),
                rateLimit = if (args.any { it.startsWith("--disable-rate-limits") }) {
                    null
                } else {
                    RateLimitConfig()
                },
            )

            createComponent(config).application().launch()
        }

        fun createComponent(config: Config): AppComponent {
            log(TAG, INFO) { "SERVER BUILD: ${BuildInfo.GIT_SHA} (${BuildInfo.GIT_DATE})" }

            log(TAG, INFO) { "App config is\n---" }
            Config::class.memberProperties.forEach { prop -> log(TAG, INFO) { "${prop.name}: ${prop.get(config)}" } }
            log(TAG, INFO) { "---" }

            if (config.isDebug) {
                ConsoleLogger.logLevel = VERBOSE
                log(TAG, VERBOSE) { "Debug mode is active" }
                log(TAG, DEBUG) { "Debug mode is active" }
                log(TAG, INFO) { "Debug mode is active" }
            } else {
                ConsoleLogger.logLevel = INFO
                log(TAG, INFO) { "Debug mode disabled" }
            }

            return DaggerAppComponent.builder().config(config).build()
        }

        private val TAG = logTag("App")
    }
}