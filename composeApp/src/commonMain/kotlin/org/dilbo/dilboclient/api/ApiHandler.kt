package org.dilbo.dilboclient.api
/**
 *
 *       dilbo - digital logbook for Rowing and Canoeing
 *       -----------------------------------------------
 *       https://www.dilbo.org
 *
 * Copyright  2023-2024  Martin Glade
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import androidx.compose.material3.Icon
import dilboclient.composeapp.generated.resources.Res
import dilboclient.composeapp.generated.resources.connection_alert
import dilboclient.composeapp.generated.resources.connection_busy
import dilboclient.composeapp.generated.resources.connection_idle
import io.ktor.client.HttpClient
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.parameters
import io.ktor.utils.io.charsets.Charsets
import kotlinx.datetime.Clock
import org.dilbo.dilboclient.api.Container.ResultType
import org.dilbo.dilboclient.app.FormHandler
import org.dilbo.dilboclient.composable.Stage
import org.dilbo.dilboclient.localhostUrl
import org.dilbo.dilboclient.tfyh.control.Logger
import org.dilbo.dilboclient.tfyh.control.LoggerSeverity
import org.dilbo.dilboclient.tfyh.data.Codec
import org.dilbo.dilboclient.tfyh.data.Config
import org.dilbo.dilboclient.tfyh.data.DataBase
import org.dilbo.dilboclient.tfyh.data.SettingsLoader
import org.dilbo.dilboclient.tfyh.util.LocalCache
import org.dilbo.dilboclient.tfyh.util.Timer
import org.dilbo.dilboclient.tfyh.util.User
import org.jetbrains.compose.resources.DrawableResource
import kotlin.math.min

/**
 * Queueing and handling of transaction requests for communication with the dilbo server.
 * Compile and send messages, asynchronously collect responses and communication errors,
 * parse responses and trigger response processing.
 */

class ApiHandler private constructor() {

    companion object {

        const val API_VERSION = 4
        const val CACHE_PATH_PENDING = "Api/pending/"
        const val CACHE_PATH_DONE = "Api/done/"
        const val CACHE_PATH_FAILED = "Api/failed/"

        // the #MESSAGE_SEPARATOR_STRING must not contain any regex special
        // character
        // because it is used in "split()" calls as "regex" argument.
        const val MESSAGE_SEPARATOR_STRING = "\n-#|#-\n"
        const val MS_REPLACEMENT_STRING = "\n_#|#_\n"

        const val API_POST_URI = "/api/post_tx.php"
        private const val API_UPDATE_CHECK_URI = "/api/update_check.php"
        private const val API_PENDING_MILLIS = 500L
        private const val API_PAUSE_AFTER_TIMEOUT_MILLIS = 10000L
        private const val TX_COUNT_PER_CONTAINER = 10

        internal val apiHandler = ApiHandler()
        fun getInstance() = apiHandler
    }

    private var apiServerUrl: String = ""
    private var apiServerVerified: Boolean = false
    var loginRetryActive: Boolean = false
    // the apiSessionId will be provided by the server. It may contain the user password in plain
    // text at session start. The user password will not be stored.
    private var apiSessionId: String = ""
    private var apiUserId: Int = -1
    internal var connected = false
    private var connectionResult: Int = 0

    // last data update and session activity timestamps (seconds)
    private var lastUpdateCheck = 0L
    var lastSessionActivity = 0L
    private var lastSessionRegenerate = 0L
    // parameters as provided by the server on a session transaction
    var welcomeMessage = "no server yet connected."
    private var lastContainerSuccessful = true
    val settingsLoader = SettingsLoader()

    // The following synchronisation defaults must never be 0.
    // download synchronisation period (seconds)
    private var updateCheckPeriod = 90
    private var updatePeriod = 900
    // session inactivity and lifetime timeouts in seconds
    private var keepAlivePeriod = 600  // 10 minutes
    private var sessionLifetime = 10800  // 3 hours


    // transaction queues. Transactions wait in the pending-queue, are processed in the busy-queue
    // and moved to the failed-queue on errors, else dropped after being processed.
    internal val pendingQueue: MutableList<Transaction> = mutableListOf()
    internal var lastContainerResult: Container.Result = ResultType.UNDEFINED.result

    // the first request always goes with lowest version, then the client shall
    // max out the version based on the server response
    private var transactionId = 42
    // the operation can be paused upon overload detection or timeout
    private var paused = 0L // wait time in milliseconds

    private var container = Container.getInstance()
    private var timer: Timer = Timer(::onTimerEvent)
    private var httpClient: HttpClient? = null

    internal val config = Config.getInstance()
    internal val localCache = LocalCache.getInstance()
    internal val logger = Logger("apiLogger")

    /**
     * Set the http client handler
     */
    fun setHttpClient(httpClient: HttpClient) { this.httpClient = httpClient }
    fun getUrl() = this.apiServerUrl
    fun setUrl(url: String) { this.apiServerUrl = url }
    fun getApiStatus(): DrawableResource {
        return when (lastContainerResult.code) {
            20, 22 ->
                if (pendingQueue.size > 0) Res.drawable.connection_busy
                else Res.drawable.connection_idle
            else -> Res.drawable.connection_alert
        }
    }
    /**
     * Set the server Url and check whether it exists. Leave url empty to use the configured value
     */
    suspend fun connect(url: String = ""): Int {
        val httpClient = this.httpClient ?: return 0
        var urlToCheck = url.ifEmpty { config.getItem(".app.server.url").valueCsv() }
        if (urlToCheck.endsWith("/"))
            urlToCheck = urlToCheck.substring(0, urlToCheck.length - 1)
        if ((urlToCheck == "http://127.0.0.1") || (urlToCheck == "http://localhost"))
            urlToCheck = localhostUrl() // this replaces the localhost for emulators
        // send
        try {
            val updateCheckResponse = httpClient.get("$urlToCheck/api/update_check.php")
            if (updateCheckResponse.status.value in 200..299) {
                apiServerUrl = urlToCheck
                connected = true
            }
            return updateCheckResponse.status.value
        } catch (_:Exception) {}
        return 999
    }
    /**
     * Set the credentials. Use the apiUserId and the apiSessionId var to store the password.
     * This will soon be replaced after the first session response received.
     */
    fun setCredentials(apiUserId: Int, apiPassword: String) {
        this.apiUserId = apiUserId
        this.apiSessionId = apiPassword
    }

    /**
     * start the transaction queue. This will 1. read all pending transactions from the local cache,
     * prepend the session start transaction, trigger the settings loader and the data base loader.
     * The httpClient must be set to get any action done.
     */
    fun start() {
        // initialize the httpClient, depending on the context
        logger.setLocale(config.language(), config.timeZone())
        this.container.setLogger(logger)

        // add all stored pending write transactions to the queue
        val localCache = LocalCache.getInstance()
        val localCacheKeys = localCache.keys()
        val pendingPrefix = "$CACHE_PATH_PENDING/tx-"
        for (cacheFileName in localCacheKeys) {
            if (cacheFileName.startsWith(pendingPrefix)) {
                val tx = Transaction.readStored(cacheFileName.replace(pendingPrefix, "")
                    .toInt(), this)
                // add, if it is a write transaction
                if ((tx != null) &&
                    ((tx.type == Transaction.TxType.INSERT) ||
                    (tx.type == Transaction.TxType.UPDATE) ||
                    (tx.type == Transaction.TxType.DELETE))) {
                    pendingQueue.add(tx)
                } else {
                    // remove, if not a write transaction
                    localCache.removeItem(cacheFileName)
                }
            }
        }
        // restore the original sequence, since files may be sorted by name.
        pendingQueue.sortBy { it.transactionId }

        // get the first transactionId. That will continue to increment over the application lifetime
        transactionId = try {
            localCache.getItem("$CACHE_PATH_PENDING/tx-max").toInt()
        } catch (e: Exception) {
            42
        }

        // set url and credentials
        apiServerUrl = apiServerUrl.ifEmpty { config.getItem(".app.server.url").valueCsv() }
        // start the queue with a session start.
        // Create the transaction
        val txStart = addNewTxToPending(Transaction.TxType.SESSION, "start", emptyMap())
        // put to front, in case stored transaction have already been added to the queue
        pendingQueue.remove(txStart)
        pendingQueue.add(0,txStart)

        // add the settings loader trigger
        settingsLoader.requestModified()
        // add the data base loader
        DataBase.getInstance().load()

        // start the timer
        timer.start(API_PENDING_MILLIS)
    }

    /**
     * Update all api parameters received as a session response.
     */
    fun onSessionResponse(sessionResponse: String) {
        val responseElements = Codec.splitCsvRow(sessionResponse, ";")
        val apiParams = mutableMapOf<String,String>()
        for (responseElement in responseElements)
            if (responseElement.indexOf('=') > 0)
                apiParams[responseElement.split('=')[0]] = responseElement.split('=')[1]

        fun updateInt(orig: Int, new: String?) = try { new?.toInt() } catch (e: Exception) { null } ?: orig

        // Synchronisation periods.
        updateCheckPeriod = updateInt(updateCheckPeriod, apiParams["update_check_period"])
        updatePeriod = updateInt(updatePeriod, apiParams["update_period"])
        // Session management.
        keepAlivePeriod = updateInt(keepAlivePeriod, apiParams["keep_alive_period"])
        sessionLifetime = updateInt(sessionLifetime, apiParams["session_lifetime"])
        apiSessionId = apiParams["api_session_id"] ?: ""
        welcomeMessage = apiParams["server_welcome_message"] ?: ""
        // User information and permissions
        val apiUserCsv = apiParams["api_user"] ?: ""
        User.getInstance().set(apiUserCsv)
    }

    /**
     * Add a transaction based on the tx type and parameters to the pending
     * queue. Set atFront to true, to add at front, else the new transaction is
     * appended.
     *
     * @params String type transaction type.
     * @params String tableName the table name or the list name
     * @params String record as object of strings
     * @return added transaction.
     */
    fun addNewTxToPending (txType: Transaction.TxType, tableName: String,
                           record: Map<String, String>): Transaction {
        // prepare the transaction cache. create a clone of the empty
        // transaction, set values and push it to the queue.
        transactionId++
        val tx = Transaction(
            apiHandler = this,
            transactionId = transactionId,
            type = txType,
            tableName = tableName,
            record = record
        )
        pendingQueue.add(tx)
        tx.writeStored(CACHE_PATH_PENDING)
        logger.log(LoggerSeverity.INFO, "ApiHandler.addNewTxToPending",
            "#${tx.transactionId} ${tx.type} ${tx.tableName}: Pending queue ++: " + pendingQueue.size)
        return tx
    }

    /**
     * After a transaction has been processed it must be removed.
     */
    fun removeTxFromPending(tx: Transaction) {
        pendingQueue.remove(tx)
        tx.deleteStored(CACHE_PATH_PENDING)
    }

    /**
     * Check for last other write access and start reload of tables, if true.
     */
    private suspend fun updateCheck(): String {
        val httpClient = this.httpClient ?: return ""
        val response = try {
            httpClient.post(
                urlString = (config.getItem(".app.server.url").valueCsv()) + API_UPDATE_CHECK_URI
            ) {
                parameter("lowa", apiSessionId)
            }
        } catch (e: Exception) {
            return "ERROR: some internet connection error"
        }
        // TODO: parse the time returned and trigger an update
        return if (response.status.value == 200) response.bodyAsText()
        else "ERROR: " + response.status.toString()

     }

    /**
     * Check the queue, when the pending timer fires. If no transaction is busy,
     * send the first transaction in the queue.
     */
    private suspend fun onTimerEvent () {

        // check the apiServerUrl, if not yet done (for manually entered URL)
        if (!apiServerVerified && !loginRetryActive) {
            val urlCheck = connect(apiServerUrl)
            if ((urlCheck <= 299) && (urlCheck >= 200))
                apiServerVerified = true
            else {
                loginRetryActive = true
                FormHandler.loginDo()
            }
        }
        if (!apiServerVerified) return

        // start transaction, if not busy nor enabled and a transaction is
        // pending
        val nowSeconds = Clock.System.now().epochSeconds
        if ((nowSeconds * 1000) > paused) {

            // send transactions, if idle
            if (container.transactions.isEmpty() && pendingQueue.isNotEmpty())
                processNextApiContainer()

            // trigger session regeneration, if due
            val regenerateDueAt = lastSessionRegenerate + (sessionLifetime * 0.9)
            if ((lastSessionRegenerate > 0) && ((nowSeconds) > regenerateDueAt)) {
                // the first session start sets the lastSessionRegenerate value.
                // Wait for this to happen before starting session regeneration
                addNewTxToPending(Transaction.TxType.SESSION, "regenerate", emptyMap())
            }

            // keep session alive
            if ((nowSeconds) > (lastSessionActivity + (keepAlivePeriod / 2)) ) {
                lastSessionActivity = nowSeconds
                addNewTxToPending (Transaction.TxType.NOP, "", mapOf("sleep" to "0"))
            }

            // schedule synchronization check
            else if (nowSeconds > (lastUpdateCheck + updateCheckPeriod)) {
                lastUpdateCheck = nowSeconds
                updateCheck()
            }
        }
    }

    /**
     * Display a dialog with the http connection error.
     */
    private fun showHttpsError(error: String) {
        Stage.showDialog("Internet connection error: $error")
    }
    /**
     * Takes the pending transactions, builds a container, sends it, receives and processes the
     * server response. Returns the count of transactions which experience a temporary failure
     */
    private suspend fun processNextApiContainer() {

        val httpClient = this.httpClient ?: return
        if ((apiUserId < 0) && apiSessionId.isEmpty())
            return
        // prepare
        val txCnt = min(pendingQueue.size, TX_COUNT_PER_CONTAINER)
        container.getEmpty()

        // add as many transactions to the container as requested.
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        for (i in 0 ..< txCnt) {
            val tx = pendingQueue[i]
            tx.sentAt = nowMillis
            container.transactions += tx
        }

        val txc = container.build(apiUserId, apiSessionId)
        if (apiServerUrl.endsWith("/"))
            apiServerUrl.substring(0, apiServerUrl.length - 1)
        if ((apiServerUrl == "http://127.0.0.1") || (apiServerUrl == "http://localhost"))
            apiServerUrl = localhostUrl() // this replaces the localhost for emulators
        // send
        val response = try {
            httpClient.submitForm (
                    url = apiServerUrl + API_POST_URI,
                    formParameters = parameters {
                        append("txc", txc)
                    }
            )
        } catch (e: Exception) {
            if ((e is HttpRequestTimeoutException) ||
                (e is ConnectTimeoutException) ||
                (e is SocketTimeoutException)) {
                showHttpsError("TimeOutException.")
                logger.log(LoggerSeverity.ERROR,"ApiHandler.processNextApiContainer",
                    "$apiServerUrl: TimeOutException.")
                handleHttpsError(ResultType.INTERNET_CONNECTION_TIMEOUT.result)
            }
            else {
                logger.log(LoggerSeverity.ERROR,"ApiHandler.processNextApiContainer",
                    "$apiServerUrl: Connection failed.")
                showHttpsError("Exception raised during connection setup.")
                handleHttpsError(ResultType.INTERNET_CONNECTION_FAILED.result)
            }
            null
        }

        if (response != null) {
            when (response.status.value) {
                in 200..299 -> {
                    // no error on http-level. Handle response
                    lastContainerSuccessful = true
                    container.processResponse(response.bodyAsText(Charsets.UTF_8))
                }
                in 301..303 -> {
                    // explicit server error
                    container.cResultCode =
                        ResultType.HTTP_COMMUNICATION_ERROR.result.code
                    container.cResultMessage = "Page was moved. HTTP status code: " + response.status.value
                    showHttpsError("The page was moved. Maybe a redirect from http to https. Status code: "
                            + response.status.value +
                            ". See 'https://en.wikipedia.org/wiki/List_of_HTTP_status_codes' for details.")
                    logger.log(LoggerSeverity.ERROR,"ApiHandler.processNextApiContainer",
                        "$apiServerUrl: moved. Status = " + response.status.value)
                    apiServerUrl = ""
                    connected = false
                    handleHttpsError(ResultType.SERVER_ERROR.result)
                    FormHandler.loginDo()
                }
                in 500..599 -> {
                    // explicit server error
                    container.cResultCode =
                        ResultType.SERVER_ERROR.result.code
                    container.cResultMessage = "HTTP status code: " + response.status.value
                    showHttpsError("Server error. Status code: " + response.status.value +
                            ". See 'https://en.wikipedia.org/wiki/List_of_HTTP_status_codes' for details.")
                    logger.log(LoggerSeverity.ERROR,"ApiHandler.processNextApiContainer",
                        "$apiServerUrl: server error. Status = " + response.status.value)
                    handleHttpsError(ResultType.SERVER_ERROR.result)
                }
                else -> {
                    // some other http error code. Typically rare. May be a forced redirect to https
                    container.cResultCode =
                        ResultType.HTTP_COMMUNICATION_ERROR.result.code
                    container.cResultMessage = "HTTP status code: " + response.status.value +
                            ", response text = " + response.bodyAsText(Charsets.UTF_8)
                    showHttpsError("Undefined error. Status code: " + response.status.value +
                            ". See 'https://en.wikipedia.org/wiki/List_of_HTTP_status_codes' for details.")
                    logger.log(LoggerSeverity.ERROR,"ApiHandler.processNextApiContainer",
                        "$apiServerUrl: undefined error. Status = " + response.status.value)
                    handleHttpsError(ResultType.HTTP_COMMUNICATION_ERROR.result)
                }
            }
        }
    }

    /**
     * Handle the error for a transaction container. The transactions go to the
     * failed queue.
     */
    private fun handleHttpsError (result: Container.Result) {

        // set the container result
        container.cResultCode = result.code
        container.cResultMessage = result.message
        container.cParsingError = ""
        lastContainerResult = result
        lastContainerSuccessful = false
        container.logResponseError( "ApiHandler.handleHttpsError", result)

        // Pause the connection for some time before retry
        paused = Clock.System.now().toEpochMilliseconds() + API_PAUSE_AFTER_TIMEOUT_MILLIS
        // invalidate the container transactions and remove them to unlock the queue processing
        container.setFailedAndRemoveAllTransactions()
    }

}