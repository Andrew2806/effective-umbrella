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
import java.util.Locale;

public final class MainActivity extends Activity {
 private static final String HOME="https://www.laserskinlab.it/gestionale/";
 private WebView web;
 private ValueCallback<Uri[]> chooser;
 private ProgressBar loading;
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
  LinearLayout layout=new LinearLayout(this);layout.setOrientation(1);layout.setBackgroundColor(Color.WHITE);
  LinearLayout bar=new LinearLayout(this);bar.setBackgroundColor(Color.rgb(18,74,114));bar.setGravity(Gravity.CENTER_VERTICAL);
  TextView title=new TextView(this);title.setText("Laser & Skin Lab");title.setTextColor(Color.WHITE);title.setTextSize(17);title.setPadding(18,0,0,0);bar.addView(title,new LinearLayout.LayoutParams(0,54,1));
  Button reload=new Button(this);reload.setText("↻");reload.setOnClickListener(v->web.reload());bar.addView(reload);
  Button browser=new Button(this);browser.setText("Browser");browser.setOnClickListener(v->external(Uri.parse(current())));bar.addView(browser);
  layout.addView(bar);
  FrameLayout frame=new FrameLayout(this);
  web=new WebView(this);frame.addView(web,new FrameLayout.LayoutParams(-1,-1));
  loading=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);frame.addView(loading,new FrameLayout.LayoutParams(-1,5,Gravity.TOP));
  layout.addView(frame,new LinearLayout.LayoutParams(-1,0,1));setContentView(layout);
  WebSettings s=web.getSettings();s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setAllowFileAccess(false);s.setAllowContentAccess(true);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);s.setSaveFormData(false);s.setSupportMultipleWindows(false);s.setSafeBrowsingEnabled(true);
  CookieManager.getInstance().setAcceptCookie(true);CookieManager.getInstance().setAcceptThirdPartyCookies(web,false);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest req){if(trusted(req.getUrl()))return false;external(req.getUrl());return true;}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){if(req.isForMainFrame())toast("Connessione non disponibile. Tocca ↻ per riprovare.");}
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