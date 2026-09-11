package com.cpmcarshop.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.ServiceWorkerClient;
import android.webkit.ServiceWorkerController;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String HOME = "https://cpmcarshop.github.io/";
    private static final String SITE_HOST = "cpmcarshop.github.io";

    private WebView web;
    private LinearLayout nav;
    private String currentTab = "home";
    private float downX, downY;
    private boolean edgeSwipe;
    private Dialog popupDialog;
    private WebView popupWeb;

    private final String[] ids = {"home", "search", "makers", "ranking", "mypage"};
    private final String[] labels = {"ホーム", "車両検索", "メーカー一覧", "ランキング", "マイページ・店舗管理"};

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.rgb(7, 10, 22));
        getWindow().setNavigationBarColor(Color.rgb(7, 10, 22));

        buildUi();
        setupWebView(web);
        configureServiceWorker();
        web.loadUrl(HOME);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(7, 10, 22));

        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1, -1));

        nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(7), dp(7), dp(7), dp(7));
        nav.setBackground(glassDrawable(Color.argb(178, 19, 28, 49), Color.argb(105, 120, 220, 255), 24));

        FrameLayout.LayoutParams navParams = new FrameLayout.LayoutParams(-1, dp(68), Gravity.BOTTOM);
        navParams.setMargins(dp(8), 0, dp(8), dp(8));
        root.addView(nav, navParams);

        for (int i = 0; i < ids.length; i++) addNavItem(ids[i], labels[i]);

        setContentView(root);
        updateNav();
    }

    private GradientDrawable glassDrawable(int fill, int stroke, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(fill);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), stroke);
        return d;
    }

    private void addNavItem(final String id, String text) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(Color.LTGRAY);
        v.setTextSize(11);
        v.setGravity(Gravity.CENTER);
        v.setMaxLines(2);
        v.setPadding(dp(2), dp(2), dp(2), dp(2));
        v.setClickable(true);
        v.setOnClickListener(view -> switchTab(id));
        nav.addView(v, new LinearLayout.LayoutParams(0, -1, 1));
    }

    private void updateNav() {
        for (int i = 0; i < nav.getChildCount(); i++) {
            TextView v = (TextView) nav.getChildAt(i);
            boolean selected = ids[i].equals(currentTab);
            v.setBackground(glassDrawable(
                    selected ? Color.argb(165, 35, 92, 116) : Color.TRANSPARENT,
                    selected ? Color.argb(170, 100, 225, 255) : Color.TRANSPARENT,
                    18));
            v.setTextColor(selected ? Color.rgb(150, 240, 255) : Color.LTGRAY);
        }
    }

    private void switchTab(String id) {
        currentTab = id;
        updateNav();
        web.animate()
                .alpha(0.15f)
                .translationX(dp(8))
                .setDuration(80)
                .withEndAction(() -> {
                    String js = "(function(){try{" +
                            "if(typeof switchTab==='function'){switchTab(" + jsQuote(id) + ");return true;}" +
                            "return false;}catch(e){return false;}})()";
                    web.evaluateJavascript(js, value -> web.animate()
                            .alpha(1f)
                            .translationX(0)
                            .setDuration(150)
                            .start());
                }).start();
    }

    private void setupWebView(WebView view) {
        WebSettings s = view.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(false);
        s.setLoadWithOverviewMode(false);
        s.setUseWideViewPort(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);

        CookieManager cookies = CookieManager.getInstance();
        cookies.setAcceptCookie(true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            cookies.setAcceptThirdPartyCookies(view, true);
        }

        view.setOverScrollMode(View.OVER_SCROLL_NEVER);
        view.setBackgroundColor(Color.TRANSPARENT);
        view.setWebViewClient(new SiteWebViewClient());
        view.setWebChromeClient(new SiteChromeClient());
    }

    private void configureServiceWorker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ServiceWorkerController.getInstance().setServiceWorkerClient(new ServiceWorkerClient() {
                @Override
                public WebResourceResponse shouldInterceptRequest(WebResourceRequest request) {
                    return null;
                }
            });
        }
    }

    private class SiteWebViewClient extends WebViewClient {
        @Override
        public void onPageFinished(WebView view, String url) {
            injectMobileUi(view);
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            return handleUrl(view, request.getUrl());
        }

        @SuppressWarnings("deprecation")
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return handleUrl(view, Uri.parse(url));
        }

        private boolean handleUrl(WebView view, Uri uri) {
            String scheme = uri.getScheme();
            if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                // Webサイトのリンク、新規タブ、別ドメインも基本的にアプリ内WebViewで開く。
                return false;
            }

            try {
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            } catch (Exception ignored) {
                return true;
            }
        }
    }

    private class SiteChromeClient extends WebChromeClient {
        @Override
        public boolean onCreateWindow(WebView view, boolean isDialog, boolean isUserGesture, android.os.Message resultMsg) {
            openPopupWindow(resultMsg);
            return true;
        }

        @Override
        public void onCloseWindow(WebView window) {
            closePopupWindow();
        }
    }

    private void openPopupWindow(android.os.Message resultMsg) {
        if (popupDialog != null && popupDialog.isShowing()) closePopupWindow();

        popupDialog = new Dialog(this);
        popupDialog.getWindow();
        popupDialog.setTitle("");

        FrameLayout box = new FrameLayout(this);
        box.setBackgroundColor(Color.rgb(7, 10, 22));

        popupWeb = new WebView(this);
        setupWebView(popupWeb);
        box.addView(popupWeb, new FrameLayout.LayoutParams(-1, -1));

        TextView close = new TextView(this);
        close.setText("閉じる");
        close.setTextColor(Color.WHITE);
        close.setTextSize(13);
        close.setGravity(Gravity.CENTER);
        close.setPadding(dp(14), 0, dp(14), 0);
        close.setBackground(glassDrawable(Color.argb(185, 28, 40, 64), Color.argb(120, 150, 230, 255), 18));
        close.setOnClickListener(v -> closePopupWindow());
        FrameLayout.LayoutParams cp = new FrameLayout.LayoutParams(dp(82), dp(44), Gravity.TOP | Gravity.END);
        cp.setMargins(0, dp(10), dp(10), 0);
        box.addView(close, cp);

        popupDialog.setContentView(box);
        if (popupDialog.getWindow() != null) {
            popupDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            popupDialog.getWindow().setLayout(-1, -1);
        }
        popupDialog.setOnDismissListener(d -> {
            if (popupWeb != null) {
                popupWeb.stopLoading();
                popupWeb.destroy();
                popupWeb = null;
            }
            popupDialog = null;
        });
        popupDialog.show();
        if (popupDialog.getWindow() != null) {
            popupDialog.getWindow().setLayout(-1, -1);
        }

        WebView.WebViewTransport transport = (WebView.WebViewTransport) resultMsg.obj;
        transport.setWebView(popupWeb);
        resultMsg.sendToTarget();
    }

    private void closePopupWindow() {
        if (popupDialog != null && popupDialog.isShowing()) popupDialog.dismiss();
    }

    private void injectMobileUi(WebView view) {
        String css =
                "html,body{overscroll-behavior-y:none;-webkit-tap-highlight-color:transparent;}" +
                "body{padding-bottom:84px!important;}" +
                ".container{padding-top:24px!important;padding-left:10px!important;padding-right:10px!important;padding-bottom:24px!important;max-width:none!important;}" +
                "header{padding:14px 14px!important;}" +
                "header h1{font-size:1.2rem!important;letter-spacing:1px!important;}" +
                ".card-grid{grid-template-columns:1fr!important;gap:12px!important;}" +
                ".card,.modal-content,.sub-item-box,.notif-box,.btn,button,.tab-button,input,select,textarea{" +
                "border-radius:16px!important;" +
                "border:1px solid rgba(180,235,255,.20)!important;" +
                "background:linear-gradient(135deg,rgba(255,255,255,.13),rgba(120,190,255,.045))!important;" +
                "box-shadow:inset 0 1px 0 rgba(255,255,255,.24),inset 0 -1px 0 rgba(0,0,0,.16),0 10px 28px rgba(0,0,0,.20)!important;" +
                "backdrop-filter:blur(18px) saturate(145%)!important;-webkit-backdrop-filter:blur(18px) saturate(145%)!important;}" +
                ".card,.modal-content{position:relative;overflow:hidden;}" +
                ".card:before,.modal-content:before{content:'';position:absolute;inset:-40%;pointer-events:none;background:linear-gradient(115deg,transparent 35%,rgba(255,255,255,.16) 47%,transparent 59%);transform:translateX(-35%) rotate(8deg);animation:glassSweep 5.5s ease-in-out infinite;}" +
                "@keyframes glassSweep{0%,45%{transform:translateX(-55%) rotate(8deg);opacity:0}55%{opacity:1}100%{transform:translateX(55%) rotate(8deg);opacity:0}}" +
                ".btn,.tab-button,button{min-height:44px!important;}" +
                ".tab-menu{display:none!important;}" +
                ".modal{background:rgba(2,5,15,.56)!important;backdrop-filter:blur(14px) saturate(135%)!important;-webkit-backdrop-filter:blur(14px) saturate(135%)!important;}" +
                ".modal-content{width:calc(100% - 24px)!important;max-height:88vh!important;padding:18px!important;}" +
                "a,button{touch-action:manipulation;}" +
                ".android-page-in{animation:androidPageIn .18s ease-out;}" +
                "@keyframes androidPageIn{from{opacity:.25;transform:translateX(8px)}to{opacity:1;transform:none}}";

        String js =
                "(function(){" +
                "var old=document.getElementById('android-mobile-style');" +
                "if(old)old.remove();" +
                "var s=document.createElement('style');s.id='android-mobile-style';" +
                "s.textContent=" + jsQuote(css) + ";document.head.appendChild(s);" +
                "if(!document.getElementById('android-glass-layer')){" +
                "var c=document.createElement('canvas');c.id='android-glass-layer';" +
                "c.style='position:fixed;inset:0;width:100%;height:100%;pointer-events:none;z-index:-1;opacity:.48';" +
                "document.body.prepend(c);" +
                "try{" +
                "var gl=c.getContext('webgl2')||c.getContext('webgl');" +
                "if(gl){c.width=innerWidth*devicePixelRatio;c.height=innerHeight*devicePixelRatio;gl.viewport(0,0,c.width,c.height);" +
                "var vs=gl.createShader(gl.VERTEX_SHADER);gl.shaderSource(vs,'attribute vec2 p;void main(){gl_Position=vec4(p,0.,1.);}');gl.compileShader(vs);" +
                "var fs=gl.createShader(gl.FRAGMENT_SHADER);gl.shaderSource(fs,'precision mediump float;uniform float t;uniform vec2 r;void main(){vec2 u=gl_FragCoord.xy/r;float a=.5+.5*sin(t*.55+u.x*7.0);float b=.5+.5*cos(t*.38+u.y*9.0);vec3 col=vec3(.05+.04*a,.09+.08*b,.18+.12*a);gl_FragColor=vec4(col,.72);}');gl.compileShader(fs);" +
                "var pr=gl.createProgram();gl.attachShader(pr,vs);gl.attachShader(pr,fs);gl.linkProgram(pr);gl.useProgram(pr);" +
                "var buf=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buf);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array([-1,-1,1,-1,-1,1,1,1]),gl.STATIC_DRAW);var loc=gl.getAttribLocation(pr,'p');gl.enableVertexAttribArray(loc);gl.vertexAttribPointer(loc,2,gl.FLOAT,false,0,0);" +
                "var ut=gl.getUniformLocation(pr,'t'),ur=gl.getUniformLocation(pr,'r');" +
                "function draw(){if(!document.body.contains(c))return;gl.uniform1f(ut,performance.now()/1000);gl.uniform2f(ur,c.width,c.height);gl.drawArrays(gl.TRIANGLE_STRIP,0,4);requestAnimationFrame(draw);}draw();" +
                "}}catch(e){}" +
                "}" +
                "})();";
        view.evaluateJavascript(js, null);
    }

    private String jsQuote(String value) {
        StringBuilder out = new StringBuilder(value.length() + 2);
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '\\': out.append("\\\\"); break;
                case '"': out.append("\\\""); break;
                case '\n': out.append("\\n"); break;
                case '\r': out.append("\\r"); break;
                case '\t': out.append("\\t"); break;
                case '\b': out.append("\\b"); break;
                case '\f': out.append("\\f"); break;
                default: out.append(c);
            }
        }
        out.append('"');
        return out.toString();
    }

    @Override
    public void onBackPressed() {
        if (popupDialog != null && popupDialog.isShowing()) {
            popupDialog.dismiss();
            return;
        }

        // モーダルが開いている場合はページ履歴を戻さず、サイト側のモーダルだけ閉じる。
        String js =
                "(function(){" +
                "var m=document.querySelectorAll('.modal');" +
                "for(var i=m.length-1;i>=0;i--){var x=m[i],s=getComputedStyle(x);" +
                "if(s.display!=='none'&&s.visibility!=='hidden'&&parseFloat(s.opacity||'1')>0){" +
                "var b=x.querySelector('[onclick*=" + jsQuote("closeModal") + "],.close,.modal-close,button.btn-secondary');" +
                "if(b){b.click();}else{x.style.display='none';}" +
                "return 'modal';}}return 'page';})()";
        web.evaluateJavascript(js, value -> {
            if (value != null && value.contains("modal")) return;
            if (web.canGoBack()) {
                web.goBack();
            } else if (!currentTab.equals("home")) {
                switchTab("home");
            } else {
                MainActivity.super.onBackPressed();
            }
        });
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent e) {
        if (e.getAction() == MotionEvent.ACTION_DOWN) {
            downX = e.getX();
            downY = e.getY();
            edgeSwipe = downX < dp(28) && downY > dp(60);
        } else if (e.getAction() == MotionEvent.ACTION_UP && edgeSwipe) {
            float dx = e.getX() - downX;
            float dy = Math.abs(e.getY() - downY);
            if (dx > dp(100) && dy < dp(80)) {
                onBackPressed();
                return true;
            }
        }
        return super.dispatchTouchEvent(e);
    }

    @Override
    protected void onDestroy() {
        closePopupWindow();
        if (web != null) {
            web.stopLoading();
            web.destroy();
        }
        super.onDestroy();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
