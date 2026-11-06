package dev.soopviewer;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class SearchUiTest {
 private ActivityController<MainActivity> controller;private final List<String> calls=Collections.synchronizedList(new ArrayList<>());
 @Before public void setup()throws Exception{
  RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();
  MainActivity.clientFactory=()->new SoopClient(){
   @Override public List<ContentItem> home(int p){return Collections.emptyList();}
   @Override public List<ContentItem> station(String id,String type,int p){return Collections.emptyList();}
   @Override public org.json.JSONArray channelPosts(String id){return new org.json.JSONArray();}
   @Override public SearchPage searchPage(String q,String mode,int page,String order){calls.add(q+":"+mode+":"+page);ContentItem person=new ContentItem("111","demo","테스트 스트리머","테스트 스트리머","","",false);person.streamer=true;person.favoriteCount="1234";person.liveBroadcast="111";person.section="스트리머";ContentItem live=new ContentItem("111","demo","현재 라이브 제목","테스트 스트리머","","100",false);live.section="LIVE";ContentItem vod=new ContentItem("222","demo","다시보기 제목","테스트 스트리머","","55",true);vod.section="VOD";
    if(mode.equals("all"))return new SearchPage(Arrays.asList(person,live,vod),false);
    if(mode.equals("streamer")){if(page==1)return new SearchPage(Collections.singletonList(person),true);ContentItem next=new ContentItem("other","other","다음 스트리머","다음 스트리머","","",false);next.streamer=true;return new SearchPage(Collections.singletonList(next),false);}
    return new SearchPage(Collections.singletonList(mode.equals("vod")?vod:live),false);
   }
  };controller=Robolectric.buildActivity(MainActivity.class).setup();ReflectionHelpers.setField(app(),"keyword","타요");ReflectionHelpers.callInstanceMethod(app(),"search");idle();
 }
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private MainActivity app(){return controller.get();}private View root(){return ReflectionHelpers.getField(app(),"root");}
 private void idle()throws Exception{for(int i=0;i<20;i++){Thread.sleep(15);Shadows.shadowOf(Looper.getMainLooper()).idle();}root().measure(View.MeasureSpec.makeMeasureSpec(412,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(892,View.MeasureSpec.EXACTLY));root().layout(0,0,412,892);}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View hit=find(((ViewGroup)v).getChildAt(i),label);if(hit!=null)return hit;}return null;}
 @SuppressWarnings("unchecked") private List<ContentItem> rows(){return ReflectionHelpers.getField(app(),"rows");}
 @Test public void defaultIntegratedSearchKeepsProfileLiveAndVodAndShowsTheirSections(){assertEquals("all",ReflectionHelpers.getField(app(),"searchMode"));assertEquals(3,rows().size());assertNotNull(find(root(),"@demo"));assertNotNull(find(root(),"즐겨찾기 1,234명"));assertNotNull(find(root(),"현재 라이브 제목"));assertNotNull(find(root(),"다시보기 제목"));assertTrue(find(root(),"통합").isSelected());}
 @Test public void streamerTabLoadsMoreThanTheFirstPage()throws Exception{find(root(),"스트리머").performClick();idle();find(root(),"더 보기").performClick();idle();assertTrue(calls.contains("타요:streamer:2"));assertEquals(2,rows().size());assertEquals("other",rows().get(1).user);assertEquals(View.GONE,((View)ReflectionHelpers.getField(app(),"more")).getVisibility());}
 @Test public void profileClickOpensNativeChannelHome()throws Exception{((View)find(root(),"@demo").getParent().getParent().getParent()).performClick();idle();assertEquals("station",ReflectionHelpers.getField(app(),"screen"));assertEquals("demo",ReflectionHelpers.getField(app(),"stationId"));}
 @Test public void tabSwitchUsesCurrentInputRatherThanThePreviousSubmittedQuery()throws Exception{((EditText)root().findViewWithTag("search.query")).setText("양도끼");find(root(),"VOD").performClick();idle();assertTrue(calls.contains("양도끼:vod:1"));assertEquals("양도끼",((EditText)root().findViewWithTag("search.query")).getText().toString());}
}
