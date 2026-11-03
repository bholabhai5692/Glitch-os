package com.example.pipeline

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

class DeepAIWebViewClient(
    private val webView: WebView?
) {
    private val handler = Handler(Looper.getMainLooper())

    companion object {
        private const val TAG = "GLITCHImageEngine"

        suspend fun create(context: Context): DeepAIWebViewClient = withContext(Dispatchers.Main) {
            try {
                DeepAIWebViewClient(WebView(context))
            } catch (e: Throwable) {
                Log.w(TAG, "WebView init fallback: ${e.message}")
                try {
                    DeepAIWebViewClient(WebView(context.applicationContext))
                } catch (t: Throwable) {
                    Log.w(TAG, "WebView completely unavailable: ${t.message}")
                    DeepAIWebViewClient(null)
                }
            }
        }
    }

    init {
        setupWebView()
    }

    private fun createOptimizedWebViewClient(onFinished: (() -> Unit)?): WebViewClient {
        return object : WebViewClient() {
            override fun onRenderProcessGone(
                view: WebView?,
                detail: android.webkit.RenderProcessGoneDetail?
            ): Boolean {
                val didCrash = detail?.didCrash() ?: false
                Log.w(TAG, "DeepAI WebView renderer process gone (didCrash=$didCrash). Handled cleanly to protect application process.")
                onFinished?.invoke()
                return true
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                onFinished?.invoke()
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: android.webkit.WebResourceRequest?
            ): android.webkit.WebResourceResponse? {
                val reqUrl = request?.url?.toString() ?: ""
                val lowercaseUrl = reqUrl.lowercase()
                
                // Fast-block heavy ad networks and external analytics
                val shouldBlock = lowercaseUrl.contains("google-analytics") ||
                        lowercaseUrl.contains("googlesyndication") ||
                        lowercaseUrl.contains("doubleclick.net") ||
                        lowercaseUrl.contains("amazon-adsystem") ||
                        lowercaseUrl.contains("adnxs.com")
                
                if (shouldBlock) {
                    return android.webkit.WebResourceResponse(
                        "text/plain",
                        "UTF-8",
                        java.io.ByteArrayInputStream("".toByteArray())
                    )
                }
                
                return super.shouldInterceptRequest(view, request)
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        val wv = webView ?: return
        try {
            // Enforce software rendering to prevent Mesa GPU rendernode crashes in containerized/headless environments
            wv.setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.databaseEnabled = true
            wv.settings.loadsImagesAutomatically = true
            wv.settings.allowContentAccess = true
            wv.settings.allowFileAccess = true
            wv.settings.userAgentString =
                "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36"
            try {
                CookieManager.getInstance().setAcceptCookie(true)
                CookieManager.getInstance().setAcceptThirdPartyCookies(wv, true)
            } catch (e: Throwable) {
                Log.w(TAG, "CookieManager setup warning: ${e.message}")
            }
            wv.webViewClient = createOptimizedWebViewClient(null)
        } catch (e: Throwable) {
            Log.w(TAG, "setupWebView failed: ${e.message}")
        }
    }

    /**
     * Generate one original AI image using pure DOM Automation Scraper.
     * Flow: Open URL -> Paste Prompt into DOM -> Click Submit -> Live Check DOM -> Fetch & Download Image.
     */
    suspend fun generateImage(
        prompt: String,
        output: File,
        pollinationsEnabled: Boolean = true,
        globalFallbackEnabled: Boolean = false,
        onStepLog: ((String) -> Unit)? = null
    ) {
        fun log(msg: String) {
            Log.d(TAG, msg)
            println(msg)
            onStepLog?.invoke(msg)
        }

        require(prompt.isNotBlank()) {
            "GLITCH ImageEngine prompt is empty"
        }

        output.parentFile?.mkdirs()
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                log("[GLITCH Quantum Vision] DOM Scraper Render Attempt ${attempt + 1}/3 starting...")
                generateInternal(prompt, output, onStepLog)

                if (!output.exists() || output.length() < 5_000) {
                    throw IllegalStateException(
                        "GLITCH Quantum Vision output image is empty or invalid"
                    )
                }

                log("[GLITCH Quantum Vision SUCCESS] Saved AI visual frame successfully (${output.length()} bytes)")
                return
            } catch (e: Exception) {
                lastError = e
                log("[GLITCH Quantum Vision ERROR] Attempt ${attempt + 1} failed: ${e.message}")
                if (attempt < 2) {
                    log("Retrying over the current network; this app does not switch VPN servers.")
                    delay(100L)
                }
            }
        }

        if (output.exists() && output.length() >= 5_000) {
            log("[GLITCH Quantum Vision SUCCESS] Visual frame finalized (${output.length()} bytes)")
            return
        }

        throw IllegalStateException(
            "GLITCH Image Engine DOM automation failed: ${lastError?.message ?: "Unable to render image"}",
            lastError
        )
    }

    private val USER_AGENTS = listOf(
        "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro Build/UQ1A.240105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.6261.64 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 13; Samsung Galaxy S23 Ultra Build/TP1A.220624.014) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 12; OnePlus 10 Pro Build/NE2211) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; Xiaomi 14 Pro Build/UQ1A.240105.004) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.6167.101 Mobile Safari/537.36",
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_3 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"
    )

    private suspend fun clearSessionAndRotateUA() = withContext(Dispatchers.Main) {
        val wv = webView ?: return@withContext
        try {
            try {
                val cookieManager = CookieManager.getInstance()
                cookieManager.removeAllCookies(null)
                cookieManager.flush()
            } catch (e: Throwable) {}
            wv.clearCache(true)
            wv.clearHistory()
            try {
                android.webkit.WebStorage.getInstance().deleteAllData()
            } catch (e: Throwable) {}
            val randomUa = USER_AGENTS.random()
            wv.settings.userAgentString = randomUa
            Log.d(TAG, "[GLITCH ImageEngine] WebView session reset with UA: $randomUa")
        } catch (e: Exception) {
            Log.e(TAG, "Error performing WebView session reset", e)
        }
    }

    private suspend fun generateInternal(
        prompt: String,
        outputFile: File,
        onStepLog: ((String) -> Unit)?
    ) {
        fun log(msg: String) {
            Log.d(TAG, msg)
            println(msg)
            onStepLog?.invoke(msg)
        }

        log("[GLITCH Quantum Vision] Resetting session for peak neural performance...")
        clearSessionAndRotateUA()

        log("[GLITCH Quantum Vision] STEP 1/3: Connecting to DeepAI Neural Engine...")
        loadUrl("https://deepai.org/chat")

        log("[GLITCH Quantum Vision] STEP 2/3: Initializing neural prompt & dispatching generation...")
        val oldImages = getCurrentImages()
        waitForElement("#generate-textarea", 15_000)
        setPrompt(prompt)
        click("#modelSubmitButton")

        log("[GLITCH Quantum Vision] Waiting for neural image rendering & synthesis...")
        val imageUrl = waitForNewImage(oldImages = oldImages, timeout = 45_000)
            ?: throw IllegalStateException("Neural image rendering timed out or returned empty result")

        log("[GLITCH Quantum Vision] STEP 3/3: Downloading high-definition visual asset...")
        downloadImage(imageUrl, outputFile)
    }

    private suspend fun loadUrl(url: String) {
        val wv = webView ?: throw IllegalStateException("WebView not available")
        val done = CompletableDeferred<Boolean>()
        withContext(Dispatchers.Main) {
            try {
                wv.webViewClient = createOptimizedWebViewClient {
                    if (!done.isCompleted) {
                        done.complete(true)
                    }
                }
                wv.loadUrl(url)
            } catch (e: Throwable) {
                if (!done.isCompleted) done.complete(false)
            }
        }
        kotlin.runCatching {
            kotlinx.coroutines.withTimeout(15_000) {
                done.await()
            }
        }
        delay(1000)
    }

    private suspend fun waitForElement(selector: String, timeout: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val exists = evalBoolean(
                """
                (() => {
                    const el = document.querySelector('#generate-textarea') ||
                               document.querySelector('textarea[name="text"]') ||
                               document.querySelector('.model-input-text-input') ||
                               document.querySelector('#persistentChatbox') ||
                               document.querySelector('textarea');
                    return !!el;
                })();
                """.trimIndent()
            )
            if (exists) {
                return
            }
            delay(300)
        }
        throw IllegalStateException("DeepAI input element not found")
    }

    private suspend fun setPrompt(prompt: String) {
        val safePrompt = JSONObject.quote(prompt)
        val result = evalBoolean(
            """
            (() => {
                const box = document.querySelector('#generate-textarea') ||
                            document.querySelector('textarea[name="text"]') ||
                            document.querySelector('.model-input-text-input') ||
                            document.querySelector('#persistentChatbox') ||
                            document.querySelector('textarea');
                if (!box) {
                    return false;
                }
                box.focus();
                if (box.tagName === 'TEXTAREA' || box.tagName === 'INPUT') {
                    box.value = $safePrompt;
                } else {
                    box.innerText = $safePrompt;
                }
                box.dispatchEvent(new Event('input', { bubbles: true }));
                box.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
            })();
            """.trimIndent()
        )
        if (!result) {
            throw IllegalStateException("Could not enter DeepAI prompt")
        }
    }

    private suspend fun waitForButton(selector: String, timeout: Long) {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val enabled = evalBoolean(
                """
                (() => {
                    const btn = document.querySelector('#modelSubmitButton') ||
                                document.querySelector('#mobileSubmitButton') ||
                                document.querySelector('#chatSubmitButton') ||
                                document.querySelector('button[type="submit"]') ||
                                document.querySelector('.pill-submit-button') ||
                                document.querySelector('button');
                    if (!btn) {
                        return false;
                    }
                    return !btn.disabled;
                })();
                """.trimIndent()
            )
            if (enabled) {
                return
            }
            delay(300)
        }
        throw IllegalStateException("DeepAI send button not enabled")
    }

    private suspend fun click(selector: String) {
        val clicked = evalBoolean(
            """
            (() => {
                if (typeof handleSubmitAction === 'function') {
                    handleSubmitAction();
                    return true;
                }
                if (typeof textModelSubmit === 'function') {
                    textModelSubmit();
                    return true;
                }
                const btn = document.querySelector('#modelSubmitButton') ||
                            document.querySelector('#mobileSubmitButton') ||
                            document.querySelector('#chatSubmitButton') ||
                            document.querySelector('button[type="submit"]') ||
                            document.querySelector('.pill-submit-button') ||
                            document.querySelector('button');
                if (btn && !btn.disabled) {
                    btn.click();
                    return true;
                }
                const box = document.querySelector('#generate-textarea') || document.querySelector('textarea');
                if (box) {
                    const evt = new KeyboardEvent('keydown', {
                        key: 'Enter',
                        code: 'Enter',
                        keyCode: 13,
                        which: 13,
                        bubbles: true
                    });
                    box.dispatchEvent(evt);
                    return true;
                }
                return false;
            })();
            """.trimIndent()
        )
        if (!clicked) {
            throw IllegalStateException("Could not trigger generation on DeepAI")
        }
    }

    private suspend fun getCurrentImages(): List<String> {
        val raw = eval(
            """
            (() => {
                const urls = [];
                const images = Array.from(document.querySelectorAll('#main-image, .try-it-result-area img, .image-output img, .markdownContainer img, .generated-chat-image, img'));
                for (const img of images) {
                    const src = img.src || img.getAttribute('data-src') || '';
                    if (src && (
                        src.includes('job-view-file') || 
                        src.includes('outputs/output.jpg') ||
                        src.includes('api.deepai.org') ||
                        src.startsWith('blob:')
                    ) && !src.includes('avatar') && !src.includes('logo') && !src.includes('pixel')) {
                        urls.push(src);
                    }
                }
                return JSON.stringify(urls);
            })();
            """.trimIndent()
        )
        return parseStringArray(raw)
    }

    private suspend fun waitForNewImage(
        oldImages: List<String>,
        timeout: Long
    ): String {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val errorMsg = eval(
                """
                (() => {
                    const errEl = document.getElementById('tryItResultError') || document.querySelector('.tryItResultError');
                    if (errEl && errEl.innerText && errEl.innerText.trim().length > 0) {
                        return errEl.innerText.trim();
                    }
                    const text = document.body ? document.body.innerText : '';
                    if (text.includes('anonymous limit') || text.includes('reached the anonymous limit') || text.includes('Please log in')) {
                        return "You have reached the anonymous limit. Please log in to continue using image generation.";
                    }
                    return '';
                })();
                """.trimIndent()
            )
            if (errorMsg.isNotBlank()) {
                throw IllegalStateException("DeepAI Limit Reached: $errorMsg")
            }

            // Check main-image element first
            val mainImgSrc = eval(
                """
                (() => {
                    const img = document.getElementById('main-image') || document.querySelector('.try-it-result-area img');
                    if (img && img.src && !img.src.includes('placeholder') && !img.src.includes('blank') && (img.src.startsWith('http') || img.src.startsWith('blob:'))) {
                        return img.src;
                    }
                    return '';
                })();
                """.trimIndent()
            )
            if (mainImgSrc.isNotBlank() && !oldImages.contains(mainImgSrc)) {
                return mainImgSrc
            }

            val currentImages = getCurrentImages()
            val newImages = currentImages.filterNot { oldImages.contains(it) }
            val deepAiUrl = newImages.lastOrNull {
                it.contains("job-view-file") || it.contains("outputs/output.jpg") || it.contains("api.deepai.org")
            } ?: newImages.lastOrNull()

            if (deepAiUrl != null && deepAiUrl.isNotBlank() && !deepAiUrl.contains("avatar") && !deepAiUrl.contains("logo")) {
                return deepAiUrl
            }

            delay(500)
        }

        throw IllegalStateException("DeepAI image generation timeout")
    }

    private suspend fun downloadImage(
        imageUrl: String,
        output: File
    ) = withContext(Dispatchers.IO) {
        if (output.exists()) {
            output.delete()
        }

        if (imageUrl.startsWith("blob:")) {
            val base64 = eval(
                """
                (async () => {
                    const response = await fetch('$imageUrl');
                    const blob = await response.blob();
                    return new Promise((resolve) => {
                        const reader = new FileReader();
                        reader.onloadend = () => resolve(reader.result.split(',')[1]);
                        reader.readAsDataURL(blob);
                    });
                })();
                """.trimIndent()
            )
            if (base64.isBlank()) throw IllegalStateException("Failed to extract blob")
            val bytes = android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
            output.writeBytes(bytes)
            if (output.length() < 10_000) throw IllegalStateException("Blob image is too small")
            return@withContext
        }

        val connection = URL(imageUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            val cookies = CookieManager.getInstance().getCookie(imageUrl)
            if (!cookies.isNullOrBlank()) {
                connection.setRequestProperty("Cookie", cookies)
            }
            connection.connect()
            val code = connection.responseCode
            if (code !in 200..299) {
                throw IllegalStateException("Image download HTTP error: $code")
            }
            connection.inputStream.use { input ->
                output.outputStream().use { outputStream ->
                    val buffer = ByteArray(16 * 1024)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count == -1) {
                            break
                        }
                        if (count > 0) {
                            outputStream.write(buffer, 0, count)
                            total += count
                        }
                    }
                    outputStream.flush()
                    if (total < 10_000) {
                        throw IllegalStateException("DeepAI returned empty image")
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun eval(script: String): String {
        val wv = webView ?: return ""
        val result = CompletableDeferred<String>()
        withContext(Dispatchers.Main) {
            try {
                wv.evaluateJavascript(script) { value ->
                    val decoded = try {
                        if (value == null || value == "null") {
                            ""
                        } else {
                            JSONObject("""{"v":$value}""").getString("v")
                        }
                    } catch (_: Exception) {
                        value ?: ""
                    }
                    if (!result.isCompleted) result.complete(decoded)
                }
            } catch (e: Throwable) {
                if (!result.isCompleted) result.complete("")
            }
        }
        return result.await()
    }

    private suspend fun evalBoolean(script: String): Boolean {
        return eval(script).equals("true", ignoreCase = true)
    }

    private fun parseStringArray(raw: String): List<String> {
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i)
                    if (value.isNotBlank()) {
                        add(value)
                    }
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
