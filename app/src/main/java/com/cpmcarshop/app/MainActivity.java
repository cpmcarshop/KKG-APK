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
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

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
        popupDialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);

        LinearLayout outer = new LinearLayout(this);
        outer.setOrientation(LinearLayout.VERTICAL);
        outer.setPadding(dp(5), dp(5), dp(5), dp(5));
        outer.setBackground(glassDrawable(Color.rgb(10, 58, 105), Color.argb(210, 90, 215, 255), 28));

        LinearLayout titleBar = new LinearLayout(this);
        titleBar.setGravity(Gravity.CENTER_VERTICAL);
        titleBar.setPadding(dp(14), 0, dp(8), 0);
        titleBar.setBackground(glassDrawable(Color.argb(210, 15, 91, 150), Color.argb(100, 150, 235, 255), 20));

        TextView title = new TextView(this);
        title.setText("架空車街");
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        titleBar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));

        TextView close = new TextView(this);
        close.setText("×");
        close.setTextColor(Color.WHITE);
        close.setTextSize(27);
        close.setGravity(Gravity.CENTER);
        close.setBackground(glassDrawable(Color.argb(190, 22, 105, 165), Color.argb(130, 170, 240, 255), 20));
        close.setOnClickListener(v -> closePopupWindow());
        titleBar.addView(close, new LinearLayout.LayoutParams(dp(46), dp(44)));
        outer.addView(titleBar, new LinearLayout.LayoutParams(-1, dp(52)));

        popupWeb = new WebView(this);
        setupWebView(popupWeb);
        popupWeb.setBackgroundColor(Color.TRANSPARENT);
        LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(-1, 0, 1);
        wp.topMargin = dp(5);
        outer.addView(popupWeb, wp);

        popupDialog.setContentView(outer);
        popupDialog.setOnDismissListener(d -> {
            if (popupWeb != null) {
                popupWeb.stopLoading();
                popupWeb.destroy();
                popupWeb = null;
            }
            popupDialog = null;
        });
        popupDialog.setCanceledOnTouchOutside(false);
        popupDialog.setCancelable(true);
        popupDialog.show();

        if (popupDialog.getWindow() != null) {
            popupDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            popupDialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            android.view.WindowManager.LayoutParams lp = popupDialog.getWindow().getAttributes();
            lp.dimAmount = 0.62f;
            popupDialog.getWindow().setAttributes(lp);
            popupDialog.getWindow().setLayout((int)(getResources().getDisplayMetrics().widthPixels * 0.94f),
                    (int)(getResources().getDisplayMetrics().heightPixels * 0.90f));
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
                ".container{padding-top:36px!important;padding-left:10px!important;padding-right:10px!important;padding-bottom:24px!important;max-width:none!important;}" +
                "header{padding:14px 14px!important;}" +
                "header h1{font-size:1.2rem!important;letter-spacing:1px!important;}" +
                ".card-grid{grid-template-columns:1fr!important;gap:12px!important;}" +
                ".card,.sub-item-box,.notif-box,.btn,button,.tab-button,input,select,textarea{" +
                "border-radius:16px!important;" +
                "border:1px solid rgba(180,235,255,.20)!important;" +
                "background:linear-gradient(135deg,rgba(255,255,255,.13),rgba(120,190,255,.045))!important;" +
                "box-shadow:inset 0 1px 0 rgba(255,255,255,.24),inset 0 -1px 0 rgba(0,0,0,.16),0 10px 28px rgba(0,0,0,.20)!important;" +
                "backdrop-filter:blur(18px) saturate(145%)!important;-webkit-backdrop-filter:blur(18px) saturate(145%)!important;}" +
                ".modal{display:none;position:fixed!important;inset:0!important;width:100%!important;height:100%!important;box-sizing:border-box!important;" +
                "background:rgba(2,12,32,.68)!important;backdrop-filter:blur(12px) saturate(135%)!important;" +
                "-webkit-backdrop-filter:blur(12px) saturate(135%)!important;z-index:10000!important;" +
                "pointer-events:auto!important;align-items:center!important;justify-content:center!important;overflow-y:auto!important;padding:18px!important;}" +
                ".modal-content{position:relative!important;box-sizing:border-box!important;width:calc(100% - 4px)!important;max-width:650px!important;max-height:88vh!important;" +
                "overflow-y:auto!important;overflow-x:hidden!important;padding:22px 18px 20px!important;" +
                "border-radius:24px!important;border:1px solid rgba(105,220,255,.55)!important;" +
                "background:linear-gradient(145deg,rgba(18,67,112,.97),rgba(8,31,66,.97) 55%,rgba(8,22,48,.98))!important;" +
                "box-shadow:0 24px 70px rgba(0,0,0,.55),inset 0 1px 0 rgba(255,255,255,.22),inset 0 -1px 0 rgba(0,0,0,.25)!important;" +
                "backdrop-filter:blur(22px) saturate(140%)!important;-webkit-backdrop-filter:blur(22px) saturate(140%)!important;" +
                "pointer-events:auto!important;touch-action:auto!important;}" +
                ".modal-content:before{content:none!important;}" +
                ".modal *{pointer-events:auto!important;}" +
                ".modal input,.modal textarea,.modal select,.modal button,.modal a{touch-action:auto!important;}" +
                ".android-modal-close{position:absolute!important;top:10px!important;right:10px!important;width:40px!important;height:40px!important;" +
                "padding:0!important;margin:0!important;z-index:10002!important;border-radius:50%!important;" +
                "border:1px solid rgba(180,235,255,.45)!important;background:rgba(20,94,150,.9)!important;" +
                "color:#fff!important;font-size:24px!important;line-height:38px!important;text-align:center!important;" +
                "box-shadow:0 6px 18px rgba(0,0,0,.3)!important;}" +
                ".modal-content h3:first-child,.modal-content h2:first-child{padding-right:48px!important;}" +
                ".btn,.tab-button,button{min-height:44px!important;}" +
                ".tab-menu{display:none!important;}" +
                "a,button{touch-action:manipulation;}" +
                ".android-page-in{animation:androidPageIn .18s ease-out;}" +
                "@keyframes androidPageIn{from{opacity:.25;transform:translateX(8px)}to{opacity:1;transform:none}}";

        String js =
                "(function(){" +
                "try{" +
                "var old=document.getElementById('android-mobile-style');if(old)old.remove();" +
                "var s=document.createElement('style');s.id='android-mobile-style';s.textContent=" + jsQuote(css) + ";" +
                "document.head.appendChild(s);" +
                // Remove the previous animated glass sweep layer completely.
                "document.querySelectorAll('.android-glass-layer').forEach(function(x){x.remove();});" +
                "var closeAll=function(){document.querySelectorAll('.modal').forEach(function(m){" +
                "var st=getComputedStyle(m);if(st.display!=='none'&&st.visibility!=='hidden'){" +
                "var id=m.id;if(typeof window.closeModal==='function'&&id){try{window.closeModal(id);}catch(e){m.style.display='none';}}else{m.style.display='none';}" +
                "}});};" +
                "var install=function(){document.querySelectorAll('.modal').forEach(function(m){" +
                "m.style.pointerEvents='auto';" +
                "var c=m.querySelector('.modal-content');" +
                "if(c&&!c.querySelector('.android-modal-close')){" +
                "var b=document.createElement('button');b.type='button';b.className='android-modal-close';b.setAttribute('aria-label','閉じる');b.textContent='×';" +
                "b.onclick=function(ev){ev.preventDefault();ev.stopPropagation();if(typeof window.closeModal==='function'&&m.id){try{window.closeModal(m.id);return;}catch(e){}}m.style.display='none';};" +
                "c.insertBefore(b,c.firstChild);}" +
                "if(!m.__androidModalBound){m.__androidModalBound=true;m.addEventListener('click',function(ev){" +
                "if(ev.target===m){ev.preventDefault();if(typeof window.closeModal==='function'&&m.id){try{window.closeModal(m.id);return;}catch(e){}}m.style.display='none';}" +
                "},false);}" +
                "});};" +
                "install();" +
                "if(!window.__androidModalObserver){window.__androidModalObserver=new MutationObserver(function(){install();});window.__androidModalObserver.observe(document.body,{childList:true,subtree:true});}" +
                "}catch(e){}" +
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

        // まずサイト内モーダルだけを閉じる。ボタンを探してクリックしないので、
        // 「保存」「確定」などの操作が戻るボタンで実行されることを防ぐ。
        String js =
                "(function(){" +
                "var ms=document.querySelectorAll('.modal');" +
                "for(var i=ms.length-1;i>=0;i--){var m=ms[i],st=getComputedStyle(m);" +
                "if(st.display!=='none'&&st.visibility!=='hidden'&&parseFloat(st.opacity||'1')>0){" +
                "if(typeof window.closeModal==='function'&&m.id){try{window.closeModal(m.id);return 'modal';}catch(e){}}" +
                "m.style.display='none';return 'modal';}}" +
                "return 'page';})()";
        web.evaluateJavascript(js, value -> {
            if ("\"modal\"".equals(value)) return;
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
