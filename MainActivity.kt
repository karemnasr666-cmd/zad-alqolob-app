package com.zadalqolob.app

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebSettings
import android.widget.Toast

class MainActivity : Activity() {
    private lateinit var webView: WebView
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private var pendingSaveBytes: ByteArray? = null
    private var pendingSaveName = "zad-alqolob-backup.json"

    companion object {
        private const val FILE_CHOOSER_REQUEST = 4101
        private const val CREATE_FILE_REQUEST = 4102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webView = WebView(this)
        setContentView(webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadsImagesAutomatically = true
            javaScriptCanOpenWindowsAutomatically = true
            setSupportMultipleWindows(false)
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        webView.addJavascriptInterface(AndroidBridge(), "ZadAndroid")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(
                view: WebView?, callback: ValueCallback<Array<Uri>>?,
                params: FileChooserParams?
            ): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                return try {
                    val intent = params?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                    }
                    startActivityForResult(intent, FILE_CHOOSER_REQUEST)
                    true
                } catch (_: Exception) {
                    fileCallback = null
                    false
                }
            }
        }
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val scheme = request?.url?.scheme ?: return false
                return scheme !in listOf("https", "http", "file", "content", "about")
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                installBackupDownloadBridge()
            }
        }
        webView.loadUrl("file:///android_asset/index.html")
    }

    
override fun onBackPressed() {
    webView.evaluateJavascript(
        """(function(){
            var s = document.querySelectorAll('.show');
            if (s.length) {
                document.dispatchEvent(
                    new KeyboardEvent('keydown', {key:'Escape'})
                );
                return 'handled';
            }
            return 'none';
        })()"""
    ) { result ->
        if (result == "\"none\"" || result == "null") {
            if (webView.canGoBack()) {
                webView.goBack()
            } else {
                super.onBackPressed()
            }
        }
    }
}

        webView.evaluateJavascript(
            """(function(){var s=document.querySelectorAll('.show');if(s.length){document.dispatchEvent(new KeyboardEvent('keydown',{key:'Escape'}));return 'handled';}return 'none';})()"""
        ) { result ->
            if (result == "\\"none\\"" || result == "null") {
                if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
            }
        }
    }

    private fun private fun installBackupDownloadBridge() {
    val js = """
        (function(){
          if(window.__zadAndroidBackupHook)return;
          window.__zadAndroidBackupHook=true;
          document.addEventListener('click',function(ev){
            var a=ev.target && ev.target.closest ? ev.target.closest('a[download]') : null;
            if(!a  !a.href  !a.href.startsWith('blob:'))return;
            ev.preventDefault();
            ev.stopPropagation();
            fetch(a.href).then(function(r){return r.blob();}).then(function(b){
              var fr=new FileReader();
              fr.onload=function(){
                var s=String(fr.result'').split(',')[1]  '';
                if(window.ZadAndroid)window.ZadAndroid.saveBackup(a.download || 'zad-alqolob-backup.json',s);
              };
              fr.readAsDataURL(b);
            }).catch(function(){});
          },true);
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}{
        val js = """
            (function(){
              if(window.__zadAndroidBackupHook)return;
              window.__zadAndroidBackupHook=true;
              document.addEventListener('click',function(ev){
                var a=ev.target && ev.target.closest ? ev.target.closest('a[download]') : null;
                if(!a || !a.href || !a.href.startsWith('blob:'))return;
                ev.preventDefault(); ev.stopPropagation();
                fetch(a.href).then(function(r){return r.blob();}).then(function(b){
                  var fr=new FileReader();
                  fr.onload=function(){
                    var s=String(fr.result||'').split(',')[1]||'';
                    if(window.ZadAndroid)window.ZadAndroid.saveBackup(a.download||'zad-alqolob-backup.json',s);
                  };
                  fr.readAsDataURL(b);
                }).catch(function(){});
              },true);
            })();
        """.trimIndent()
        webView.evaluateJavascript(js, null)
    }

    inner class AndroidBridge {
        @JavascriptInterface
        fun saveBackup(name: String, base64: String) {
            try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                runOnUiThread {
                    pendingSaveBytes = bytes
                    pendingSaveName = name.substringAfterLast('/').ifBlank { "zad-alqolob-backup.json" }
                    val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                        addCategory(Intent.CATEGORY_OPENABLE)
                        type = "application/json"
                        putExtra(Intent.EXTRA_TITLE, pendingSaveName)
                    }
                    startActivityForResult(intent, CREATE_FILE_REQUEST)
                }
            } catch (_: Exception) {
                runOnUiThread {
                    Toast.makeText(this@MainActivity, "تعذر تجهيز ملف النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    @Deprecated("Deprecated Android callback retained for broad device compatibility")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == Activity.RESULT_OK) {
                WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            } else null
            fileCallback?.onReceiveValue(result)
            fileCallback = null
        } else if (requestCode == CREATE_FILE_REQUEST) {
            if (resultCode == Activity.RESULT_OK && data?.data != null && pendingSaveBytes != null) {
                try {
                    contentResolver.openOutputStream(data.data!!)?.use { it.write(pendingSaveBytes!!) }
                    Toast.makeText(this, "تم حفظ النسخة الاحتياطية", Toast.LENGTH_LONG).show()
                } catch (_: Exception) {
                    Toast.makeText(this, "تعذر حفظ الملف", Toast.LENGTH_LONG).show()
                }
            }
            pendingSaveBytes = null
        }
    }

    override fun onDestroy() {
        fileCallback?.onReceiveValue(null)
        webView.removeJavascriptInterface("ZadAndroid")
        webView.destroy()
        super.onDestroy()
    }
}
