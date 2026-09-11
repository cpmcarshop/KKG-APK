package com.cpmcarshop.app;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private LinearLayout nav;
    private String currentTab = "home";
    private float downX, downY;
    private boolean edgeSwipe;
    private final String HOME = "https://cpmcarshop.github.io/";
    private final String[] ids = {"home","search","makers","ranking","mypage"};
    private final String[] labels = {"⌂\nホーム","⌕\n検索","🚗\nメーカー","★\nランキング","☻\nマイページ"};

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setStatusBarColor(Color.rgb(10,12,27));
        getWindow().setNavigationBarColor(Color.rgb(10,12,27));
        buildUi();
        setupWebView();
        web.loadUrl(HOME);
    }

    private void buildUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.rgb(10,12,27));
        web = new WebView(this);
        root.addView(web, new FrameLayout.LayoutParams(-1,-1));

        nav = new LinearLayout(this); nav.setOrientation(LinearLayout.HORIZONTAL); nav.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable(); bg.setColor(Color.rgb(17,24,43)); bg.setCornerRadius(dp(22)); bg.setStroke(dp(1),Color.rgb(38,52,82)); nav.setBackground(bg);
        nav.setPadding(dp(5),dp(5),dp(5),dp(5));
        FrameLayout.LayoutParams np = new FrameLayout.LayoutParams(-1,dp(68),Gravity.BOTTOM); np.setMargins(dp(8),0,dp(8),dp(8)); root.addView(nav,np);
        for(int i=0;i<ids.length;i++) addNavItem(ids[i],labels[i]);
        setContentView(root); updateNav();
    }

    private void addNavItem(final String id, String text) {
        TextView v = new TextView(this); v.setText(text); v.setTextColor(Color.LTGRAY); v.setTextSize(11); v.setGravity(Gravity.CENTER); v.setLines(2); v.setFontFeatureSettings("kern");
        v.setPadding(2,2,2,2); v.setClickable(true);
        v.setOnClickListener(x -> switchTab(id));
        nav.addView(v,new LinearLayout.LayoutParams(0,-1,1));
    }

    private void updateNav() {
        for(int i=0;i<nav.getChildCount();i++) {
            TextView v=(TextView)nav.getChildAt(i); boolean sel=ids[i].equals(currentTab);
            GradientDrawable d=new GradientDrawable(); d.setCornerRadius(dp(18)); d.setColor(sel?Color.rgb(23,57,74):Color.TRANSPARENT); v.setBackground(d); v.setTextColor(sel?Color.rgb(0,210,255):Color.LTGRAY);
        }
    }

    private void switchTab(String id) {
        currentTab=id; updateNav();
        web.animate().alpha(0.15f).translationX(10).setDuration(90).withEndAction(() -> {
            String js="(function(){try{if(typeof switchTab==='function'){switchTab('"+id+"');}else{return false;} return true;}catch(e){return false;}})()";
            web.evaluateJavascript(js, value -> web.animate().alpha(1f).translationX(0).setDuration(170).start());
        }).start();
    }

    private void setupWebView() {
        WebSettings s=web.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setDatabaseEnabled(true); s.setBuiltInZoomControls(false); s.setDisplayZoomControls(false); s.setSupportZoom(false); s.setLoadWithOverviewMode(false); s.setUseWideViewPort(false); s.setMediaPlaybackRequiresUserGesture(false);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER); web.setBackgroundColor(Color.rgb(10,12,27));
        web.setWebViewClient(new WebViewClient(){
            @Override public void onPageFinished(WebView view,String url){ injectMobileUi(); }
            @Override public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest r){
                Uri u=r.getUrl(); String host=u.getHost();
                if(host!=null && (host.equals("cpmcarshop.github.io") || host.endsWith(".cpmcarshop.github.io"))) return false;
                if("http".equals(u.getScheme()) || "https".equals(u.getScheme())) { startActivity(new Intent(Intent.ACTION_VIEW,u)); return true; }
                return false;
            }
        });
        web.setWebChromeClient(new WebChromeClient());
    }

    private void injectMobileUi(){
        String js=""+
        "(function(){if(document.getElementById('android-mobile-style'))return;"+
        "var s=document.createElement('style');s.id='android-mobile-style';s.textContent=`"+
        "html,body{overscroll-behavior-y:none;-webkit-tap-highlight-color:transparent;}"+
        "body{padding-bottom:82px!important;}"+
        "header{padding:11px 14px!important;}header h1{font-size:1.2rem!important;letter-spacing:1px!important;}"+
        ".container{padding:12px 10px 20px!important;max-width:none!important;}"+
        ".card-grid{grid-template-columns:1fr!important;gap:12px!important;}"+
        ".card{border-radius:14px!important;} .card-img{height:190px!important;}"+
        ".btn,.tab-button,button,input,select,textarea{min-height:44px!important;}"+
        ".tab-menu{display:none!important;}"+
        ".modal-content{width:calc(100% - 24px)!important;max-height:88vh!important;padding:16px!important;border-radius:18px!important;}"+
        "a,button{touch-action:manipulation;}"+
        ".android-page-in{animation:androidPageIn .18s ease-out;}@keyframes androidPageIn{from{opacity:.25;transform:translateX(10px)}to{opacity:1;transform:none}}`;
        document.head.appendChild(s);"+
        "document.querySelectorAll('.tab-button').forEach(function(b){b.addEventListener('click',function(){var x=document.querySelector('.tab-content.active');if(x){x.classList.remove('android-page-in');void x.offsetWidth;x.classList.add('android-page-in');}})});"+
        "})();";
        web.evaluateJavascript(js,null);
    }

    @Override public boolean dispatchTouchEvent(android.view.MotionEvent e){
        if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getX();downY=e.getY();edgeSwipe=downX<dp(28) && downY>dp(60);}
        else if(e.getAction()==MotionEvent.ACTION_UP && edgeSwipe){float dx=e.getX()-downX;float dy=Math.abs(e.getY()-downY);if(dx>dp(100)&&dy<dp(80)){if(web.canGoBack())web.goBack(); else if(!currentTab.equals("home"))switchTab("home");return true;}}
        return super.dispatchTouchEvent(e);
    }

    @Override public void onBackPressed(){
        if(web.canGoBack()){web.goBack();return;}
        if(!currentTab.equals("home")){switchTab("home");return;}
        super.onBackPressed();
    }
    @Override protected void onDestroy(){if(web!=null)web.destroy();super.onDestroy();}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
