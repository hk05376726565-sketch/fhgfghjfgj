package com.kanal38.app

import android.annotation.SuppressLint
import android.app.Activity
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.Toast

class MainActivity : Activity() {

    companion object {
        private const val EMBED_URL = "https://www.canlitv.diy/embed/?id=58"
        private const val ALLOWED_HOST = "canlitv.diy"
    }

    private lateinit var logo: ImageView
    private lateinit var playerContainer: FrameLayout
    private lateinit var fullscreenContainer: FrameLayout

    private var webView: WebView? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        logo = findViewById(R.id.logo)
        playerContainer = findViewById(R.id.playerContainer)
        fullscreenContainer = findViewById(R.id.fullscreenContainer)

        logo.setOnClickListener { startPlayback() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun startPlayback() {
        if (webView != null) return

        logo.visibility = View.GONE
        playerContainer.visibility = View.VISIBLE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        val wv = WebView(this)
        wv.setBackgroundColor(Color.BLACK)
        wv.layoutParams = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        wv.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            javaScriptCanOpenWindowsAutomatically = false
            setSupportMultipleWindows(false)
            loadWithOverviewMode = true
            useWideViewPort = true
        }

        wv.webViewClient = object : WebViewClient() {
            // Sayfa dışına (reklam/yönlendirme) çıkılmasını engelle
            override fun shouldOverrideUrlLoading(
                view: WebView,
                request: WebResourceRequest
            ): Boolean {
                val host = request.url.host ?: return true
                return !(host == ALLOWED_HOST || host.endsWith(".$ALLOWED_HOST"))
            }

            override fun onReceivedError(
                view: WebView,
                request: WebResourceRequest,
                error: WebResourceError
            ) {
                if (request.isForMainFrame) {
                    Toast.makeText(
                        this@MainActivity,
                        "Yayın açılamadı, internet bağlantınızı kontrol edin.",
                        Toast.LENGTH_LONG
                    ).show()
                    stopPlayback()
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            // Oynatıcının tam ekran butonu için
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) {
                    callback.onCustomViewHidden()
                    return
                }
                customView = view
                customViewCallback = callback
                fullscreenContainer.addView(view)
                fullscreenContainer.visibility = View.VISIBLE
                playerContainer.visibility = View.GONE
                enterImmersive()
            }

            override fun onHideCustomView() {
                hideCustomView()
            }
        }

        playerContainer.addView(wv)
        webView = wv
        wv.loadUrl(EMBED_URL)
    }

    private fun hideCustomView() {
        val view = customView ?: return
        fullscreenContainer.removeView(view)
        fullscreenContainer.visibility = View.GONE
        playerContainer.visibility = View.VISIBLE
        customViewCallback?.onCustomViewHidden()
        customView = null
        customViewCallback = null
        exitImmersive()
    }

    private fun stopPlayback() {
        hideCustomView()
        webView?.let { wv ->
            wv.stopLoading()
            wv.loadUrl("about:blank")
            playerContainer.removeView(wv)
            wv.removeAllViews()
            wv.destroy()
        }
        webView = null
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        playerContainer.visibility = View.GONE
        logo.visibility = View.VISIBLE
    }

    @Suppress("DEPRECATION")
    private fun enterImmersive() {
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    @Suppress("DEPRECATION")
    private fun exitImmersive() {
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when {
            customView != null -> hideCustomView()
            webView != null -> stopPlayback()   // yayından logo ekranına dön
            else -> finish()
        }
    }

    // Uygulama arka plana alınınca yayın tamamen durur, hiçbir şey arka planda çalışmaz.
    override fun onStop() {
        super.onStop()
        stopPlayback()
    }

    override fun onDestroy() {
        super.onDestroy()
        webView?.destroy()
        webView = null
    }
}
