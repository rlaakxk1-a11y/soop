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
 @Before public void setup(){MainActivity.clientFactory=()->new SoopClient(){@Override public List<ContentItem> home(int p){return Collections.singletonList(new ContentItem("demo","demo","테스트 방송","닉네임","","12",false));}@Override public List<ContentItem> station(String user,String type,int p){return Collections.emptyList();}};controller=Robolectric.buildActivity(MainActivity.class).setup();}
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private boolean contains(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(contains(((ViewGroup)v).getChildAt(i),label))return true;return false;}
 private boolean hasWeb(View v){if(v instanceof WebView)return true;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)if(hasWeb(((ViewGroup)v).getChildAt(i)))return true;return false;}
 @Test public void homeIsNativeAndHasMobileNavigation()throws Exception {for(int n=0;n<15;n++){Thread.sleep(30);Shadows.shadowOf(Looper.getMainLooper()).idle();}View root=controller.get().getWindow().getDecorView();assertTrue(contains(root,"인기 LIVE"));assertTrue(contains(root,"검색"));assertTrue(contains(root,"방송국"));assertTrue(contains(root,"MY"));assertFalse(hasWeb(root));}
 @Test public void dockedChatLeavesRoomForVideoAndCanHide(){WatchLayout layout=new WatchLayout(controller.get());layout.position="right";layout.fraction=.4f;int w=View.MeasureSpec.makeMeasureSpec(1000,View.MeasureSpec.EXACTLY),h=View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY);layout.measure(w,h);layout.layout(0,0,1000,600);assertEquals(600,layout.video.getWidth());assertEquals(400,layout.chat.getWidth());layout.hidden=true;layout.measure(w,h);layout.layout(0,0,1000,600);assertEquals(1000,layout.video.getWidth());assertEquals(View.GONE,layout.chat.getVisibility());}
}
