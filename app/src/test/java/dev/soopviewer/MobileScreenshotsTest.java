package dev.soopviewer;
import android.graphics.*;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import java.util.*;
import java.io.*;

/** Offline layout evidence. Fixture content; no account or live network requests. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,qualifiers="w412dp-h892dp-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class MobileScreenshotsTest {
 private ActivityController<MainActivity> controller;
 @Before public void setup(){RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();MainActivity.clientFactory=()->new SoopClient(){
  @Override public String loggedInUser(){return "";}
  @Override public List<ContentItem> home(int page){return Arrays.asList(new ContentItem("100","demo","오늘도 함께하는 라이브 방송","테스트 스트리머","","12345",false),new ContentItem("101","demo2","게임 이야기와 실시간 소통","두 번째 스트리머","","8492",false));}
  @Override public List<ContentItem> station(String id,String type,int page){return Arrays.asList(new ContentItem("123","demo","오늘의 다시보기 · 테스트 영상","테스트 스트리머","","",true));}
  @Override public org.json.JSONArray channelPosts(String id)throws Exception{return new org.json.JSONArray("[{titleName:'오늘 방송 안내',regDate:'2026-11-06',content:{textContent:'잠시 후 방송에서 만나요. 화면 검증용 예시입니다.'}}]");}
  @Override public SearchPage searchPage(String q,String mode,int page,String order){ContentItem p=new ContentItem("100","demo","테스트 스트리머","테스트 스트리머","","",false);p.streamer=true;p.stationName="테스트 방송국";p.favoriteCount="12345";p.liveBroadcast="100";p.section="스트리머";ContentItem live=new ContentItem("100","demo","오늘도 함께하는 라이브 방송","테스트 스트리머","","12345",false);live.section="LIVE";ContentItem vod=new ContentItem("123","demo","오늘의 다시보기 · 테스트 영상","테스트 스트리머","","321",true);vod.section="VOD";vod.duration="1:23:45";return new SearchPage(mode.equals("streamer")?Collections.singletonList(p):Arrays.asList(p,live,vod),mode.equals("streamer"));}
  @Override public org.json.JSONArray categories()throws Exception{return new org.json.JSONArray("[{category_name:'게임',child:[{category_no:'1',category_name:'리그 오브 레전드'},{category_no:'2',category_name:'배틀그라운드'}]},{category_name:'보이는 라디오',child:[{category_no:'3',category_name:'토크 / 캠방'},{category_no:'4',category_name:'먹방'}]}]");}
 };controller=Robolectric.buildActivity(MainActivity.class).setup();}
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private void capture(String name)throws Exception{for(int i=0;i<20;i++){Thread.sleep(20);Shadows.shadowOf(Looper.getMainLooper()).idle();}View view=ReflectionHelpers.getField(controller.get(),"root");view.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(892,View.MeasureSpec.EXACTLY));view.layout(0,0,412,892);Shadows.shadowOf(Looper.getMainLooper()).idle();view.measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(892,View.MeasureSpec.EXACTLY));view.layout(0,0,412,892);if(name.equals("home")){androidx.recyclerview.widget.RecyclerView list=findList(view);org.junit.Assert.assertNotNull(list);org.junit.Assert.assertEquals(412,list.getChildAt(0).getWidth());}Bitmap bitmap=Bitmap.createBitmap(412,892,Bitmap.Config.ARGB_8888);view.draw(new Canvas(bitmap));File dir=new File("build/reports/mobile-screenshots");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){bitmap.compress(Bitmap.CompressFormat.PNG,100,out);}}
 private androidx.recyclerview.widget.RecyclerView findList(View view){if(view instanceof androidx.recyclerview.widget.RecyclerView)return (androidx.recyclerview.widget.RecyclerView)view;if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++){androidx.recyclerview.widget.RecyclerView found=findList(((ViewGroup)view).getChildAt(i));if(found!=null)return found;}return null;}
 @Test public void homeAndStationAndExplore()throws Exception{capture("home");ReflectionHelpers.callInstanceMethod(controller.get(),"station",ReflectionHelpers.ClassParameter.from(String.class,"demo"),ReflectionHelpers.ClassParameter.from(String.class,"home"));capture("station");ReflectionHelpers.callInstanceMethod(controller.get(),"explore");capture("explore");ReflectionHelpers.callInstanceMethod(controller.get(),"officialLogin");capture("login");ReflectionHelpers.setField(controller.get(),"keyword","테스트");ReflectionHelpers.callInstanceMethod(controller.get(),"search");capture("search-all");ReflectionHelpers.setField(controller.get(),"searchMode","streamer");ReflectionHelpers.callInstanceMethod(controller.get(),"search");capture("search-streamer");}
}
