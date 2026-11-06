package dev.soopviewer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import android.os.Looper;
import android.view.*;
import android.webkit.WebView;
import android.widget.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28)
public class NativeUiTest {
 private ActivityController<MainActivity> controller;
 @Before public void setup(){org.robolectric.RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();MainActivity.clientFactory=()->new SoopClient(){@Override public List<ContentItem> home(int p){return Collections.singletonList(new ContentItem("demo","demo","테스트 방송","닉네임","","12",false));}@Override public List<ContentItem> station(String user,String type,int p){return Collections.emptyList();}@Override public org.json.JSONArray channelPosts(String user){return new org.json.JSONArray();}@Override public org.json.JSONArray categories()throws Exception{return new org.json.JSONArray("[{category_name:'게임',child:[{category_no:'00040019',category_name:'리그 오브 레전드'}]}]");}};controller=Robolectric.buildActivity(MainActivity.class).setup();}
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private boolean contains(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),label))return true;return false;}
 private boolean hasWeb(View v){if(v instanceof WebView)return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasWeb(((ViewGroup)v).getChildAt(i)))return true;return false;}
 @Test public void homeIsNativeAndHasMobileNavigation()throws Exception {for(int n=0;n<15;n++){Thread.sleep(30);Shadows.shadowOf(Looper.getMainLooper()).idle();}View root=controller.get().getWindow().getDecorView();assertTrue(contains(root,"추천"));assertTrue(contains(root,"라이브"));assertTrue(contains(root,"기록"));assertTrue(contains(root,"탐색"));assertTrue(contains(root,"방송국"));assertTrue(contains(root,"MY"));assertFalse(hasWeb(root));}

 @Test public void stationHasNativeHomeVideosAndPostsTabs(){MainActivity a=controller.get();org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"station",org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"demo"),org.robolectric.util.ReflectionHelpers.ClassParameter.from(String.class,"home"));View root=a.getWindow().getDecorView();for(String tab:new String[]{"홈","동영상","다시보기","클립","게시글"})assertTrue(tab,contains(root,tab));assertFalse(hasWeb(root));}
 @Test public void exploreUsesServerCategories()throws Exception{MainActivity a=controller.get();org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"explore");for(int i=0;i<15;i++){Thread.sleep(25);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue(contains(a.getWindow().getDecorView(),"리그 오브 레전드"));}
 @Test public void dockedChatLeavesRoomForVideoAndCanHide(){WatchLayout layout=new WatchLayout(controller.get());layout.position="right";layout.fraction=.4f;int w=View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),h=View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY);layout.measure(w,h);layout.layout(0,0,1000,600);assertEquals("docked video width",600,layout.video.getWidth());assertEquals("docked chat width",400,layout.chat.getWidth());layout.hidden=true;layout.requestLayout();layout.measure(w,h);layout.layout(0,0,1000,600);assertEquals("hidden chat releases video width",1000,layout.video.getWidth());assertEquals(View.GONE,layout.chat.getVisibility());}
 @Test public void pictureInPictureUsesTheEntireSmallWindowForVideo(){WatchLayout layout=new WatchLayout(controller.get());layout.mobilePortrait=true;layout.pictureInPicture=true;TextView title=new TextView(controller.get());title.setText("영상 제목");layout.details.addView(title);measureWatch(layout,360,203);assertEquals(360,layout.video.getWidth());assertEquals(203,layout.video.getHeight());assertEquals(View.GONE,layout.chat.getVisibility());assertEquals(View.GONE,layout.details.getVisibility());assertEquals(View.GONE,layout.resizeRail.getVisibility());}
 @Test public void inAppMiniPlayerKeepsVideoVisibleAboveTheBrowseList(){WatchLayout layout=new WatchLayout(controller.get());layout.mobilePortrait=true;layout.miniPlayer=true;TextView title=new TextView(controller.get());title.setText("영상 제목");layout.details.addView(title);measureWatch(layout,224,154);assertEquals(224,layout.video.getWidth());assertEquals(154,layout.video.getHeight());assertEquals(View.GONE,layout.chat.getVisibility());assertEquals(View.GONE,layout.details.getVisibility());assertEquals(View.GONE,layout.resizeRail.getVisibility());}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View hit=find(((ViewGroup)v).getChildAt(i),label);if(hit!=null)return hit;}return null;}
 @Test public void historyTabLoadsPersistedVodWithoutNetwork(){
  MainActivity a=controller.get();ContentItem item=new ContentItem("123","demo","보던 영상","닉","","",true);HistoryStore saved=new HistoryStore(a.getSharedPreferences("viewer",0));saved.save(new WatchHistory.Entry(item,1,42000,600000,342000,1234,false));
  // Recreate the Activity just as a later launch would load its preferences.
  controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity.class).setup();View root=controller.get().getWindow().getDecorView();find(root,"기록").performClick();
  assertTrue(contains(root,"최근 시청 1개 · 누르면 이어보기 · 길게 누르면 관리"));assertFalse(hasWeb(root));assertTrue(find(root,"기록").isSelected());
  HistoryStore reopened=new HistoryStore(controller.get().getSharedPreferences("viewer",0));assertEquals(42000,reopened.find("123").positionMs);assertEquals(1,reopened.find("123").part);
 }
 @Test public void leavingPlayerSavesActualFileAndPosition(){
  MainActivity a=controller.get();attachFakePlayback(a,42000,true);
  find(a.getWindow().getDecorView(),"기록").performClick();
  WatchHistory.Entry saved=new HistoryStore(a.getSharedPreferences("viewer",0)).find("321");assertNotNull(saved);assertEquals(1,saved.part);assertEquals(42000,saved.positionMs);assertEquals(342000,saved.elapsedMs);
 }
 @Test public void loadingBeforePlaybackDoesNotOverwriteEarlierPosition(){
  MainActivity a=controller.get();HistoryStore store=org.robolectric.util.ReflectionHelpers.getField(a,"history");store.save(new WatchHistory.Entry(new ContentItem("321","demo","영상","","","",true),1,42000,600000,342000,1,false));
  attachFakePlayback(a,0,false);find(a.getWindow().getDecorView(),"기록").performClick();
  assertEquals(42000,new HistoryStore(a.getSharedPreferences("viewer",0)).find("321").positionMs);
 }
 private void attachFakePlayback(MainActivity activity,long position,boolean started){
  androidx.media3.exoplayer.ExoPlayer fake=(androidx.media3.exoplayer.ExoPlayer)java.lang.reflect.Proxy.newProxyInstance(androidx.media3.exoplayer.ExoPlayer.class.getClassLoader(),new Class<?>[]{androidx.media3.exoplayer.ExoPlayer.class},(proxy,method,args)->{
   switch(method.getName()){case "getCurrentMediaItemIndex":return 1;case "getCurrentPosition":return position;case "getPlaybackState":return androidx.media3.common.Player.STATE_READY;case "isPlaying":return false;default:return null;}
  });
  PlaybackInfo info=new PlaybackInfo();info.title="영상";info.media.add("https://cdn.example/one");info.media.add("https://cdn.example/two");info.durations.add(300000L);info.durations.add(300000L);
  org.robolectric.util.ReflectionHelpers.setField(activity,"player",fake);org.robolectric.util.ReflectionHelpers.setField(activity,"info",info);org.robolectric.util.ReflectionHelpers.setField(activity,"current",new ContentItem("321","demo","영상","","","",true));org.robolectric.util.ReflectionHelpers.setField(activity,"historyStarted",started);
 }
 @Test public void immersiveWatchHasNoReservedTitleToolbarOrSystemInsets(){
  MainActivity a=controller.get();ContentItem item=new ContentItem("demo","demo","영상 제목","닉","","",false);
  org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"buildWatchUi",org.robolectric.util.ReflectionHelpers.ClassParameter.from(ContentItem.class,item));assertTrue(contains(a.getWindow().getDecorView(),"작은 화면"));
  org.robolectric.util.ReflectionHelpers.callInstanceMethod(a,"fullscreen");WatchLayout layout=org.robolectric.util.ReflectionHelpers.getField(a,"watch");measureWatch(layout,1000,600);
  assertEquals(0,layout.video.getTop());assertEquals(600,layout.video.getHeight());assertEquals(800,layout.video.getWidth());assertEquals(View.GONE,layout.details.getVisibility());
  for(String field:new String[]{"header","nav","playerChrome","chatStatus"}){View v=org.robolectric.util.ReflectionHelpers.getField(a,field);assertEquals(field,View.GONE,v.getVisibility());}
  assertTrue((a.getWindow().getAttributes().flags&WindowManager.LayoutParams.FLAG_FULLSCREEN)!=0);View root=org.robolectric.util.ReflectionHelpers.getField(a,"root");assertEquals(0,root.getPaddingTop());assertEquals(0,root.getPaddingBottom());
  layout.hidden=true;layout.requestLayout();measureWatch(layout,1000,600);assertEquals(1000,layout.video.getWidth());assertEquals(600,layout.video.getHeight());
 }
 @Test public void dockedChatBoundaryDragShrinksToEightPercentWithoutFloating(){WatchLayout layout=new WatchLayout(controller.get());layout.immersive=true;layout.position="right";layout.fraction=.20f;measureWatch(layout,1000,600);
  View rail=layout.resizeRail;long time=android.os.SystemClock.uptimeMillis();android.view.MotionEvent down=android.view.MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,0,100,0),move=android.view.MotionEvent.obtain(time,time+20,MotionEvent.ACTION_MOVE,180,100,0),up=android.view.MotionEvent.obtain(time,time+40,MotionEvent.ACTION_UP,180,100,0);rail.dispatchTouchEvent(down);rail.dispatchTouchEvent(move);rail.dispatchTouchEvent(up);down.recycle();move.recycle();up.recycle();measureWatch(layout,1000,600);
  assertEquals("right",layout.position);assertEquals(80,layout.chat.getWidth());assertEquals(920,layout.video.getWidth());assertEquals(0,layout.video.getTop());
 }
 @Test public void portraitWatchKeepsSixteenNineVideoAboveDetailsAndChat(){WatchLayout layout=new WatchLayout(controller.get());layout.mobilePortrait=true;TextView title=new TextView(controller.get());title.setText("영상 제목");layout.details.addView(title);measureWatch(layout,400,850);assertEquals(225,layout.video.getHeight());assertEquals(225,layout.details.getTop());assertEquals(layout.details.getBottom(),layout.chat.getTop());assertEquals(850,layout.chat.getBottom());}
 private void measureWatch(WatchLayout layout,int width,int height){layout.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));layout.layout(0,0,width,height);}
 @Test public void fullHdSelectionRequestsItsOwnAuthorizedWebStream()throws Exception{
  final boolean[] authorized={false};SoopClient client=new SoopClient(){@Override public org.json.JSONObject json(String url,String form,String ref)throws Exception{if(form!=null){assertTrue(form.contains("quality=fullhd"));assertTrue(form.contains("type=aid"));authorized[0]=true;return new org.json.JSONObject("{CHANNEL:{AID:'fixture-token'}}");}assertTrue(authorized[0]);assertTrue(url.contains("return_type=gs_cdn_pc_web"));assertTrue(url.contains("12-common-fullhd-hls"));return new org.json.JSONObject("{view_url:'https://cdn.example/live.m3u8?existing=1'}");}};
  String url=client.liveStream(new ContentItem("demo","demo","LIVE","","","",false),new org.json.JSONObject("{BNO:'12',RMD:'https://livestream-manager.sooplive.com',CDN:'gs_cdn_mobile_web'}"),"fullhd");assertTrue(url.contains("existing=1"));assertTrue(url.contains("aid=fixture-token"));
 }
 @Test(expected=java.io.IOException.class) public void unavailableHighQualityDoesNotBypassAuthorization()throws Exception{SoopClient client=new SoopClient(){@Override public org.json.JSONObject json(String url,String form,String ref)throws Exception{return new org.json.JSONObject("{CHANNEL:{RESULT:-6}}");}};client.liveStream(new ContentItem("demo","demo","LIVE","","","",false),new org.json.JSONObject("{BNO:'12'}"),"fullhd");}
}
