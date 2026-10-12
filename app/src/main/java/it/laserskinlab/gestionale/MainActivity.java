package it.laserskinlab.gestionale;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.webkit.*;
import android.widget.*;
import android.view.*;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

public final class MainActivity extends Activity {
 private static final String HOME="https://www.laserskinlab.it/gestionale/";
 private WebView web;
 private ValueCallback<Uri[]> chooser;
 private ProgressBar loading;
 private SwipeRefreshLayout swipe;
 private static final int PICK=2341;
 private boolean trusted(Uri u) {
  if(u==null||!"https".equalsIgnoreCase(u.getScheme()))return false;
  String host=u.getHost();
  return host!=null&&(host.equalsIgnoreCase("laserskinlab.it")||host.equalsIgnoreCase("www.laserskinlab.it"))&&(u.getPort()==-1||u.getPort()==443);
 }
 private void external(Uri uri) {
  try {
   String s=uri.getScheme();
   if(s!=null&&(s.equals("https")||s.equals("http")||s.equals("tel")||s.equals("mailto")))startActivity(new Intent(Intent.ACTION_VIEW,uri));
  }catch(Exception ex){Toast.makeText(this,"Collegamento non disponibile",Toast.LENGTH_SHORT).show();}
 }
 private void toast(String msg){Toast.makeText(this,msg,Toast.LENGTH_LONG).show();}
 private String current(){String u=web.getUrl();return u!=null&&trusted(Uri.parse(u))?u:HOME;}
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
  // La dashboard web contiene già la sua navigazione: nessuna barra duplicata.
  getWindow().setStatusBarColor(Color.rgb(250,248,246));
  getWindow().setNavigationBarColor(Color.rgb(250,248,246));
  getWindow().getDecorView().setSystemUiVisibility(
      View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
  FrameLayout root=new FrameLayout(this);
  root.setBackgroundColor(Color.rgb(250,248,246));
  if(android.os.Build.VERSION.SDK_INT>=30){
    getWindow().setDecorFitsSystemWindows(false);
    root.setOnApplyWindowInsetsListener((v,insets)->{
      android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
      v.setPadding(0,bars.top,0,bars.bottom);
      return insets;
    });
  } else {
    root.setFitsSystemWindows(true);
  }
  swipe=new SwipeRefreshLayout(this);
  swipe.setColorSchemeColors(Color.rgb(179,136,113));
  web=new WebView(this);
  swipe.addView(web,new android.view.ViewGroup.LayoutParams(-1,-1));
  swipe.setOnChildScrollUpCallback((parent,child)->web.canScrollVertically(-1));
  swipe.setOnRefreshListener(()->web.reload());
  root.addView(swipe,new FrameLayout.LayoutParams(-1,-1));
  loading=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);
  root.addView(loading,new FrameLayout.LayoutParams(-1,4,Gravity.TOP));
  setContentView(root);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSaveFormData(false);s.setSupportMultipleWindows(false);s.setSafeBrowsingEnabled(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){if(swipe!=null)swipe.setRefreshing(false);}
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){if(trusted(req.getUrl()))return false;external(req.getUrl());return true;}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){if(req.isForMainFrame()){if(swipe!=null)swipe.setRefreshing(false);toast("Connessione non disponibile. Scorri verso il basso per riprovare.");}}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onProgressChanged(WebView w,int p){loading.setProgress(p);loading.setVisibility(p>=100?View.GONE:View.VISIBLE);}
   @Override public boolean onShowFileChooser(WebView w,ValueCallback<Uri[]> cb,FileChooserParams params){
    if(chooser!=null)chooser.onReceiveValue(null);chooser=cb;
    try{Intent i=params.createIntent();i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);return true;}
    catch(Exception e){chooser=null;toast("Impossibile scegliere un file");return false;}
   }
  });
  web.setDownloadListener((url,userAgent,disposition,mime,size)->{
   if(url.startsWith("blob:")||url.startsWith("data:")){
    new AlertDialog.Builder(this).setTitle("Download PDF e QR").setMessage("Questo file viene generato dal gestionale. Apri la pagina in Chrome e ripeti il download. Potrebbe essere necessario effettuare nuovamente l'accesso.").setPositiveButton("Apri Chrome",(d,w)->external(Uri.parse(current()))).setNegativeButton("Annulla",null).show();return;
   }
   if(!trusted(Uri.parse(url))){external(Uri.parse(url));return;}
   try{
    DownloadManager.Request r=new DownloadManager.Request(Uri.parse(url));
    String cookies=CookieManager.getInstance().getCookie(url);if(cookies!=null)r.addRequestHeader("Cookie",cookies);
    if(userAgent!=null)r.addRequestHeader("User-Agent",userAgent);
    String file=URLUtil.guessFileName(url,disposition,mime).replaceAll("[^a-zA-Z0-9._-]","_");
    if(file.length()==0)file="documento";
    r.setTitle(file);r.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
    r.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS,file);
    ((DownloadManager)getSystemService(Context.DOWNLOAD_SERVICE)).enqueue(r);
    toast("Download avviato");
   }catch(Exception e){toast("Download non riuscito. Usa il browser.");}
  });
  if(state==null)web.loadUrl(HOME);else web.restoreState(state);
 }
 @Override protected void onActivityResult(int code,int result,Intent data){
  super.onActivityResult(code,result,data);
  if(code==PICK&&chooser!=null){chooser.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(result,data));chooser=null;}
 }
 @Override public void onBackPressed(){if(web!=null&&web.canGoBack())web.goBack();else new AlertDialog.Builder(this).setMessage("Chiudere il gestionale?").setPositiveButton("Chiudi",(d,w)->finish()).setNegativeButton("Annulla",null).show();}
 @Override protected void onSaveInstanceState(Bundle out){web.saveState(out);super.onSaveInstanceState(out);}
 @Override protected void onDestroy(){if(chooser!=null){chooser.onReceiveValue(null);chooser=null;}if(web!=null){web.destroy();web=null;}super.onDestroy();}
}