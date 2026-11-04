package dev.soopviewer;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
 private WebView web;
 private LinearLayout root;
 private TextView status;
 private Button toggle;
 private boolean hidden;
 private int percent;
 private String script;
 private String position;
 private volatile boolean ads;
 private View custom;
 private WebChromeClient.CustomViewCallback customCallback;
 private android.content.SharedPreferences prefs;

 @Override public void onCreate(Bundle saved) {
  super.onCreate(saved);
  prefs = getSharedPreferences("layout", MODE_PRIVATE);
  position = prefs.getString("position", "auto");
  ads = prefs.getBoolean("ads", true);
  hidden = prefs.getBoolean("hidden", false);
  percent = PanelState.clamp(prefs.getInt("percent", 35));
  try (java.io.InputStream in = getAssets().open("chat-layout.js")) {
   script = new String(readBytes(in), StandardCharsets.UTF_8);
  } catch (Exception e) { throw new IllegalStateException(e); }
  root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
  root.setBackgroundColor(Color.rgb(11,18,32));
  root.setOnApplyWindowInsetsListener((v,i) -> { v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom()); return i; });
  setContentView(root);
  TextView title = new TextView(this); title.setText("SOOP Viewer  /  비공식 · 미리보기"); title.setTextColor(Color.WHITE); title.setPadding(16,12,16,8); root.addView(title);
  LinearLayout tabs = new LinearLayout(this); root.addView(tabs);
  button(tabs,"생방송", () -> open("https://m.sooplive.co.kr/"));
  button(tabs,"다시보기", () -> open("https://vod.sooplive.co.kr/"));
  button(tabs,"검색", this::search);
  button(tabs,"메뉴", this::menu);
  LinearLayout controls = new LinearLayout(this); controls.setGravity(Gravity.CENTER_VERTICAL); root.addView(controls);
  toggle = button(controls,hidden ? "채팅 표시" : "채팅 숨김", this::toggleChat);
  SeekBar slider = new SeekBar(this); slider.setMax(50); slider.setProgress(percent-15); slider.setContentDescription("채팅 크기 15에서 65퍼센트");
  controls.addView(slider,new LinearLayout.LayoutParams(0,52,1));
  slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
   public void onProgressChanged(SeekBar s,int value,boolean user){ percent=PanelState.clamp(value+15); if(user){if("floating".equals(position))position="auto";persist();apply();} }
   public void onStartTrackingTouch(SeekBar s){} public void onStopTrackingTouch(SeekBar s){}
  });
  status = new TextView(this); status.setTextColor(Color.LTGRAY); status.setPadding(16,4,16,8); root.addView(status);
  web = new WebView(this); root.addView(web,new LinearLayout.LayoutParams(-1,0,1));
  WebSettings settings=web.getSettings(); settings.setJavaScriptEnabled(true); settings.setDomStorageEnabled(true); settings.setAllowFileAccess(false); settings.setAllowContentAccess(false); settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW); settings.setMediaPlaybackRequiresUserGesture(true);
  web.setWebViewClient(new WebViewClient(){
   @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest r){
    if(ads && !r.isForMainFrame() && AdRules.blocked(r.getUrl().getHost())) return new WebResourceResponse("text/plain","UTF-8",new java.io.ByteArrayInputStream(new byte[0]));
    return null;
   }
   @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest r){
    Uri u=r.getUrl(); if("https".equals(u.getScheme()) && PanelState.trusted(u.getHost())) return false;
    if(r.isForMainFrame()) new AlertDialog.Builder(MainActivity.this).setMessage("외부 브라우저에서 이 링크를 열까요?\n"+u.getHost()).setPositiveButton("열기",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(ActivityNotFoundException e){status.setText("열 수 있는 앱이 없습니다.");}}).setNegativeButton("취소",null).show();
    return true;
   }
   @Override public void onPageFinished(WebView v,String url){ apply(); }
   @Override public void onReceivedError(WebView v,WebResourceRequest r,WebResourceError e){if(r.isForMainFrame())status.setText("연결 실패 · 메뉴에서 새로고침 또는 데모를 선택하세요.");}
  });
  web.setWebChromeClient(new WebChromeClient(){
   @Override public void onShowCustomView(View v,CustomViewCallback cb){if(custom!=null){cb.onCustomViewHidden();return;} custom=v;customCallback=cb;root.setVisibility(View.GONE); addContentView(v,new ViewGroup.LayoutParams(-1,-1));}
   @Override public void onHideCustomView(){exitFullScreen();}
  });
  if(saved==null || web.restoreState(saved)==null) open("https://m.sooplive.co.kr/");
 }
 private Button button(LinearLayout row,String text,Runnable action){Button b=new Button(this);b.setText(text);b.setTextSize(12);row.addView(b,new LinearLayout.LayoutParams(0,-2,1));b.setOnClickListener(v->action.run());return b;}
 private static byte[] readBytes(java.io.InputStream in) throws java.io.IOException { java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream(); byte[] buffer=new byte[8192]; int count; while((count=in.read(buffer))!=-1)out.write(buffer,0,count); return out.toByteArray(); }
 private void toggleChat(){web.evaluateJavascript("window.viewerController ? window.viewerController.state.position : null", value->{if("\"floating\"".equals(value))position="floating";hidden=!hidden;toggle.setText(hidden?"채팅 표시":"채팅 숨김");persist();apply();});}
 private void persist(){prefs.edit().putBoolean("hidden",hidden).putInt("percent",percent).putString("position",position).putBoolean("ads",ads).apply();}
 private void open(String url){web.loadUrl(url);status.setText("공식 웹페이지 연결 중…");}
 private void demo(){try(java.io.InputStream in=getAssets().open("demo.html")){web.loadDataWithBaseURL("https://demo.invalid/",new String(readBytes(in),StandardCharsets.UTF_8),"text/html","UTF-8",null);}catch(Exception e){status.setText("데모 로드 실패");}}
 private void apply(){
  String u=web.getUrl(); if(u==null)return;
  if(!u.startsWith("https://demo.invalid/") && !PanelState.trusted(Uri.parse(u).getHost()))return;
  web.evaluateJavascript(script+";window.viewerApply("+hidden+","+percent+",\""+position+"\","+ads+");", result -> status.setText("true".equals(result) ? "채팅 "+(hidden?"숨김":percent+"%")+" · 실험 기능" : "채팅 자동 감지 대기 · 메뉴에서 위치 설정 가능"));
 }
 private void search(){EditText input=new EditText(this);input.setSingleLine();input.setHint("방송 / 스트리머 검색");new AlertDialog.Builder(this).setTitle("SOOP 검색").setView(input).setPositiveButton("검색",(d,w)->{String q=input.getText().toString().trim();if(!q.isEmpty())open("https://m.sooplive.co.kr/search?szKeyword="+Uri.encode(q));}).setNegativeButton("취소",null).show();}
 private void menu(){new AlertDialog.Builder(this).setItems(new String[]{"채팅 위치", ads?"광고 차단 끄기":"광고 차단 켜기", "새로고침", "SOOP 링크 열기", "레이아웃 데모", "안내"},(d,w)->{switch(w){case 0:placement();break;case 1:ads=!ads;persist();apply();web.reload();break;case 2:web.reload();break;case 3:link();break;case 4:demo();break;case 5:new AlertDialog.Builder(this).setMessage("비공식 SOOP 웹 뷰어입니다. 채팅창의 이동 손잡이를 드래그하거나 모서리를 끌어 크기를 조절하세요. 채팅 구조에 따라 감지되지 않을 수 있습니다. 광고 차단은 일부 광고 서버와 배너에 적용되며 영상에 포함된 광고 제거는 보장하지 않습니다. 전체화면 영상에서는 웹 채팅이 표시되지 않을 수 있습니다.").setPositiveButton("확인",null).show();}}).show();}
 private void placement(){String[] values={"auto","left","right","top","bottom","floating"};new AlertDialog.Builder(this).setTitle("채팅 위치").setItems(new String[]{"자동","왼쪽","오른쪽","위쪽","아래쪽","자유 이동"},(d,w)->{position=values[w];persist();apply();}).show();}
 private void link(){EditText input=new EditText(this);input.setHint("https://vod.sooplive.co.kr/…");new AlertDialog.Builder(this).setTitle("공식 링크").setView(input).setPositiveButton("열기",(d,w)->{Uri u=Uri.parse(input.getText().toString().trim());if("https".equals(u.getScheme())&&PanelState.trusted(u.getHost()))open(u.toString());else status.setText("HTTPS SOOP 공식 주소를 입력하세요.");}).setNegativeButton("취소",null).show();}
 private void exitFullScreen(){if(custom!=null){((ViewGroup)custom.getParent()).removeView(custom);custom=null;root.setVisibility(View.VISIBLE);customCallback.onCustomViewHidden();customCallback=null;}}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);web.postDelayed(this::apply,300);}
 @Override public void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);web.saveState(b);}
 @Override public void onBackPressed(){if(custom!=null)exitFullScreen();else if(web.canGoBack())web.goBack();else super.onBackPressed();}
 @Override public void onPause(){web.onPause();super.onPause();}
 @Override public void onResume(){super.onResume();if(web!=null)web.onResume();}
 @Override public void onDestroy(){web.destroy();super.onDestroy();}
}
