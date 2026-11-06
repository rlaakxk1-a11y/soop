package dev.soopviewer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import org.robolectric.shadows.ShadowDialog;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class BrowseUiTest {
 private ActivityController<MainActivity> controller;private Client client;
 private static class Client extends SoopClient{
  final List<String> calls=Collections.synchronizedList(new ArrayList<>());boolean failPageTwo=false;
  @Override public List<ContentItem> liveList(String category,String order,int page)throws Exception{calls.add("live:"+category+":"+order+":"+page);if(page==2&&failPageTwo){failPageTwo=false;throw new java.io.IOException("fixture timeout");}ContentItem a=new ContentItem(order.equals("view_cnt_asc")?"low":"high","demo","테스트","닉","",order.equals("view_cnt_asc")?"1":"100",false);return page==1?Collections.singletonList(a):Arrays.asList(a,new ContentItem("page2","demo2","다음 방송","닉2","","7",false));}
  @Override public List<ContentItem> station(String id,String type,int page,String order){calls.add("video:"+id+":"+type+":"+order+":"+page);return Collections.singletonList(new ContentItem(order.equals("read_cnt")?"999":"123",id,"동영상","닉","","",true));}
  @Override public List<ContentItem> search(String q,boolean vod,int page,String order){calls.add("search:"+q+":"+vod+":"+order+":"+page);return Collections.emptyList();}
  @Override public List<ContentItem> searchStreamers(String q){return Collections.emptyList();}
  @Override public org.json.JSONArray channelPosts(String id){return new org.json.JSONArray();}
  @Override public String loggedInUser(){return "";}
 }
 @Before public void setup()throws Exception{RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();client=new Client();MainActivity.clientFactory=()->client;controller=Robolectric.buildActivity(MainActivity.class).setup();idle();}
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private void idle()throws Exception{for(int i=0;i<20;i++){Thread.sleep(15);Shadows.shadowOf(Looper.getMainLooper()).idle();}}
 private MainActivity app(){return controller.get();}private View root(){return app().getWindow().getDecorView();}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View f=find(((ViewGroup)v).getChildAt(i),label);if(f!=null)return f;}return null;}
 private void choose(String tag,int index)throws Exception{root().findViewWithTag(tag).performClick();androidx.appcompat.app.AlertDialog dialog=(androidx.appcompat.app.AlertDialog)ShadowDialog.getLatestDialog();ListView list=dialog.getListView();list.performItemClick(null,index,index);idle();}
 @SuppressWarnings("unchecked") private List<ContentItem> rows(){return ReflectionHelpers.getField(app(),"rows");}
 @Test public void sortMenuChangesTheServerRequestAndPersistsSelection()throws Exception{choose("live.sort",1);assertTrue(client.calls.contains("live::view_cnt_asc:1"));assertEquals("1",rows().get(0).viewers);assertEquals("view_cnt_asc",app().getSharedPreferences("viewer",0).getString("liveOrder",""));controller.pause().stop().destroy();controller=Robolectric.buildActivity(MainActivity.class).setup();idle();assertEquals("view_cnt_asc",ReflectionHelpers.getField(app(),"liveOrder"));}
 @Test public void changingSortKeepsTheSelectedCategory()throws Exception{ReflectionHelpers.setField(app(),"categoryId","00040019");ReflectionHelpers.setField(app(),"categoryTitle","게임");ReflectionHelpers.callInstanceMethod(app(),"homeLive");idle();choose("live.sort",1);assertTrue(client.calls.contains("live:00040019:view_cnt_asc:1"));find(root(),"전체").performClick();idle();assertTrue(client.calls.contains("live::view_cnt_asc:1"));}
 @Test public void failedNextPageRetriesSamePageAndKeepsExistingRows()throws Exception{client.failPageTwo=true;find(root(),"더 보기").performClick();idle();assertEquals(1,rows().size());assertEquals(1,(int)ReflectionHelpers.getField(app(),"page"));find(root(),"다시 시도").performClick();idle();assertEquals(2,rows().size());assertEquals("page2",rows().get(1).id);assertEquals(2,(int)ReflectionHelpers.getField(app(),"page"));assertEquals(2,Collections.frequency(client.calls,"live::view_cnt:2"));}
 @Test public void channelSortKeepsItsReplayTabAndChangesOrder()throws Exception{ReflectionHelpers.callInstanceMethod(app(),"station",ReflectionHelpers.ClassParameter.from(String.class,"demo"),ReflectionHelpers.ClassParameter.from(String.class,"review"));idle();choose("video.sort",2);assertTrue(client.calls.contains("video:demo:review:read_cnt:1"));assertEquals("999",rows().get(0).id);assertEquals("review",ReflectionHelpers.getField(app(),"stationType"));}
 @Test public void searchSortKeepsTheQueryAndVodTab()throws Exception{ReflectionHelpers.setField(app(),"keyword","테스트");ReflectionHelpers.setField(app(),"searchMode","vod");ReflectionHelpers.setField(app(),"searchVod",true);ReflectionHelpers.callInstanceMethod(app(),"search");idle();choose("search.sort",1);assertTrue(client.calls.contains("search:테스트:true:reg_date:1"));assertEquals("테스트",((EditText)root().findViewWithTag("search.query")).getText().toString());}
 @Test public void changingMiniPlayerSortDoesNotReleasePlayback()throws Exception{final boolean[] released={false};androidx.media3.exoplayer.ExoPlayer player=(androidx.media3.exoplayer.ExoPlayer)java.lang.reflect.Proxy.newProxyInstance(androidx.media3.exoplayer.ExoPlayer.class.getClassLoader(),new Class<?>[]{androidx.media3.exoplayer.ExoPlayer.class},(proxy,method,args)->{if(method.getName().equals("release"))released[0]=true;return null;});ReflectionHelpers.setField(app(),"player",player);ReflectionHelpers.setField(app(),"miniPlayer",true);choose("live.sort",1);assertSame(player,ReflectionHelpers.getField(app(),"player"));assertFalse(released[0]);ReflectionHelpers.setField(app(),"player",null);ReflectionHelpers.setField(app(),"miniPlayer",false);}
 @Test public void liveFavoriteStarUpdatesWithoutLeavingPlayer(){ContentItem item=new ContentItem("1","demo","방송","닉","","",false);ReflectionHelpers.callInstanceMethod(app(),"buildWatchUi",ReflectionHelpers.ClassParameter.from(ContentItem.class,item));Button star=root().findViewWithTag("favorite.demo");star.performClick();assertEquals("★",star.getText().toString());assertEquals("watch",ReflectionHelpers.getField(app(),"screen"));star.performClick();assertEquals("☆",star.getText().toString());}
}
