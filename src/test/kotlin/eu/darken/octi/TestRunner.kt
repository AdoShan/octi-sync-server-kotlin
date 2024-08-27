package eu.darken.octi

import eu.darken.octi.kserver.App
import eu.darken.octi.kserver.DaggerAppComponent
import eu.darken.octi.kserver.common.debug.logging.Logging.Priority.VERBOSE
import eu.darken.octi.kserver.common.debug.logging.log
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.concurrent.thread
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively

@OptIn(ExperimentalPathApi::class)
abstract class TestRunner {

    val baseConfig = App.Config(
        dataPath = Path("./build/tmp/testdatapath/${UUID.randomUUID()}"),
        port = 8080,
        isDebug = true,
        rateLimit = null,
    )

    data class TestEnvironment(
        val config: App.Config,
        val app: App,
        val http: HttpClient,
    )

    fun runTest2(
        appConfig: App.Config = baseConfig,
        keepData: Boolean = false,
        before: (App.Config) -> TestEnvironment = {
            Files.createDirectories(it.dataPath)

            val app = DaggerAppComponent.builder().config(appConfig).build().application()
            thread { app.launch() }

            while (!app.isRunning()) Thread.sleep(100)

            val client = HttpClient(CIO) {
                defaultRequest {
                    url {
                        protocol = URLProtocol.HTTP
                        host = "127.0.0.1"
                        port = appConfig.port
                    }
                }
            }

            TestEnvironment(appConfig, app, client)
        },
        after: TestEnvironment.() -> Unit = {
            http.close()
            app.shutdown()
            if (!keepData) {
                log(VERBOSE) { "Cleaning up stored data" }
                appConfig.dataPath.deleteRecursively()
            }
        },
        test: suspend TestEnvironment.() -> Unit
    ) {
        val env = before(appConfig)
        log(VERBOSE) { "Running test with environment $env" }
        try {
            runTest { test(env) }
        } finally {
            after(env)
            log(VERBOSE) { "Test is done $env" }
        }
    }

    fun TestEnvironment.getAccountPath(credentials: Credentials): Path {
        return config.dataPath.resolve("accounts").resolve(credentials.account)
    }

    fun TestEnvironment.getSharesPath(credentials: Credentials): Path {
        return getAccountPath(credentials).resolve("shares")
    }
}