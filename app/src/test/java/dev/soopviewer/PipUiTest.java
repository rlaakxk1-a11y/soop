package dev.soopviewer;

import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=31)
public class PipUiTest {
 private ActivityController<MainActivity> controller;private boolean released,paused;
 @Before public void setup(){
  RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();
  MainActivity.clientFactory=()->new SoopClient(){@Override public List<ContentItem> home(int page){return Collections.emptyList();}};
  controller=Robolectric.buildActivity(MainActivity.class).setup();
  ReflectionHelpers.callInstanceMethod(app(),"buildWatchUi",ReflectionHelpers.ClassParameter.from(ContentItem.class,new ContentItem("1","demo","LIVE","닉","","",false)));
  ExoPlayer player=(ExoPlayer)java.lang.reflect.Proxy.newProxyInstance(ExoPlayer.class.getClassLoader(),new Class<?>[]{ExoPlayer.class},(proxy,method,args)->{
   switch(method.getName()){case "isPlaying":return !paused;case "getVideoSize":return new VideoSize(1920,1080);case "release":released=true;return null;case "pause":paused=true;return null;default:return null;}
  });ReflectionHelpers.setField(app(),"player",player);
 }
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private MainActivity app(){return controller.get();}
 private WatchLayout watch(){return ReflectionHelpers.getField(app(),"watch");}
 private FrameLayout body(){return ReflectionHelpers.getField(app(),"body");}
 private void measure(int w,int h){View root=ReflectionHelpers.getField(app(),"root");root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);}
 private void mini(){ReflectionHelpers.callInstanceMethod(app(),"minimizePlayback");}
 @Test public void miniPlayerExpandsToVideoOnlyAndRestoresItsBrowseLayout(){
  mini();WatchLayout video=watch();video.setTranslationX(-30);video.setTranslationY(-40);ViewGroup.LayoutParams old=video.getLayoutParams();View browse=body().getChildAt(0);
  app().onPictureInPictureModeChanged(true,new Configuration());measure(360,203);
  assertEquals(360,video.video.getWidth());assertEquals(203,video.video.getHeight());assertEquals(View.GONE,browse.getVisibility());
  for(String f:new String[]{"header","nav","miniFrame"})assertEquals(f,View.GONE,((View)ReflectionHelpers.getField(app(),f)).getVisibility());
  assertEquals(0,video.getTranslationX(),0);assertEquals(0,video.getTranslationY(),0);assertFalse(released);
  app().onPictureInPictureModeChanged(false,new Configuration());
  assertSame(old,video.getLayoutParams());assertEquals(-30,video.getTranslationX(),0);assertEquals(-40,video.getTranslationY(),0);assertEquals(View.VISIBLE,browse.getVisibility());assertTrue(video.miniPlayer);assertFalse(video.pictureInPicture);
 }
 @Test public void normalWatchRestoresChatAndKeepsTheSamePlayer(){
  Object player=ReflectionHelpers.getField(app(),"player");WatchLayout video=watch();video.position="left";video.fraction=.3f;
  app().onPictureInPictureModeChanged(true,new Configuration());measure(360,203);assertEquals(View.GONE,video.chat.getVisibility());
  app().onPictureInPictureModeChanged(false,new Configuration());measure(800,450);
  assertEquals("left",video.position);assertEquals(.3f,video.fraction,0);assertEquals(View.VISIBLE,video.chat.getVisibility());assertSame(player,ReflectionHelpers.getField(app(),"player"));assertFalse(released);
 }
 @Test public void homeGesturePreparesVideoBeforeAutomaticPip(){mini();app().onUserLeaveHint();measure(360,203);assertTrue(watch().pictureInPicture);assertEquals(360,watch().video.getWidth());assertEquals(View.GONE,body().getChildAt(0).getVisibility());}
 @Test public void cancelledPipRestoresTheMiniPlayerOnResume(){mini();ViewGroup.LayoutParams old=watch().getLayoutParams();app().onUserLeaveHint();controller.pause().resume();assertFalse(watch().pictureInPicture);assertSame(old,watch().getLayoutParams());assertEquals(View.VISIBLE,body().getChildAt(0).getVisibility());}
 @Test public void releasedPlaybackDisablesStaleAutoEnter(){PictureInPictureParams active=ReflectionHelpers.callInstanceMethod(app(),"pipParams");assertTrue(active.isAutoEnterEnabled());ReflectionHelpers.callInstanceMethod(app(),"releaseWatch");PictureInPictureParams cleared=ReflectionHelpers.callInstanceMethod(app(),"pipParams");assertFalse(cleared.isAutoEnterEnabled());assertTrue(released);}
 @Test public void manifestHandlesPipScreenLayoutChangesWithoutRecreatingTheActivity()throws Exception{android.content.pm.ActivityInfo info=app().getPackageManager().getActivityInfo(app().getComponentName(),0);assertTrue((info.configChanges&android.content.pm.ActivityInfo.CONFIG_SCREEN_LAYOUT)!=0);assertTrue((info.configChanges&android.content.pm.ActivityInfo.CONFIG_SMALLEST_SCREEN_SIZE)!=0);}
 @Test public void pipPauseKeepsVideoPlaying(){app().onPictureInPictureModeChanged(true,new Configuration());controller.pause();assertFalse(paused);}
}
