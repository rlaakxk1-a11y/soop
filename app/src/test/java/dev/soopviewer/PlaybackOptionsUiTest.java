package dev.soopviewer;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.view.*;
import android.widget.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=31)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class PlaybackOptionsUiTest {
 private ActivityController<MainActivity> controller;private ExoPlayer player;private PlayerView view;
 @Before public void setup(){RuntimeEnvironment.getApplication().getSharedPreferences("viewer",0).edit().clear().commit();MainActivity.clientFactory=()->new SoopClient(){@Override public List<ContentItem> home(int page){return Collections.emptyList();}};controller=Robolectric.buildActivity(MainActivity.class).setup();ReflectionHelpers.callInstanceMethod(app(),"buildWatchUi",ReflectionHelpers.ClassParameter.from(ContentItem.class,new ContentItem("123","demo","VOD","닉","","",true)));player=new ExoPlayer.Builder(app()).build();view=new PlayerView(app());view.setPlayer(player);ReflectionHelpers.setField(app(),"player",player);ReflectionHelpers.setField(app(),"playerView",view);watch().video.addView(view,0,new FrameLayout.LayoutParams(-1,-1));layout();}
 @After public void cleanup(){controller.pause().stop().destroy();MainActivity.clientFactory=SoopClient::new;}
 private MainActivity app(){return controller.get();}private View root(){return app().getWindow().getDecorView();}private WatchLayout watch(){return ReflectionHelpers.getField(app(),"watch");}
 private void layout(){View root=ReflectionHelpers.getField(app(),"root");root.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(450,View.MeasureSpec.EXACTLY));root.layout(0,0,800,450);}
 private View find(View v,String label){if(v instanceof TextView&&((TextView)v).getText().toString().equals(label))return v;if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++){View hit=find(((ViewGroup)v).getChildAt(i),label);if(hit!=null)return hit;}return null;}
 private View options(){root().findViewWithTag("watch.options").performClick();return ShadowDialog.getLatestDialog().getWindow().getDecorView();}
 @Test public void scaleMenuActuallyChangesThePlayerAndPersistsTheChoice(){find(options(),"화면 비율").performClick();androidx.appcompat.app.AlertDialog d=(androidx.appcompat.app.AlertDialog)ShadowDialog.getLatestDialog();d.getListView().performItemClick(null,1,1);assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM,view.getResizeMode());assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM,app().getSharedPreferences("viewer",0).getInt("videoScale",-1));}
 @Test public void lockBlocksVideoTouchAndBackUnlocksWithoutLeavingTheBroadcast(){final int[] touches={0};view.setOnTouchListener((v,e)->{touches[0]++;return true;});int original=app().getRequestedOrientation();find(options(),"영상 터치·회전 잠금").performClick();assertNotNull(root().findViewWithTag("watch.unlock"));assertEquals(ActivityInfo.SCREEN_ORIENTATION_LOCKED,app().getRequestedOrientation());layout();for(int action:new int[]{MotionEvent.ACTION_DOWN,MotionEvent.ACTION_UP}){MotionEvent e=MotionEvent.obtain(1,1,action,100,100,0);watch().video.dispatchTouchEvent(e);e.recycle();}assertEquals(0,touches[0]);app().onBackPressed();assertNull(root().findViewWithTag("watch.unlock"));assertEquals(original,app().getRequestedOrientation());assertEquals("watch",ReflectionHelpers.getField(app(),"screen"));assertSame(player,ReflectionHelpers.getField(app(),"player"));}
 @Test public void pipUsesOriginalAspectAndRestoresTheChosenZoomAfterReturning(){app().getSharedPreferences("viewer",0).edit().putInt("videoScale",AspectRatioFrameLayout.RESIZE_MODE_ZOOM).commit();ReflectionHelpers.callInstanceMethod(app(),"applyVideoScale");assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM,view.getResizeMode());app().onPictureInPictureModeChanged(true,new Configuration());assertEquals(AspectRatioFrameLayout.RESIZE_MODE_FIT,view.getResizeMode());app().onPictureInPictureModeChanged(false,new Configuration());assertEquals(AspectRatioFrameLayout.RESIZE_MODE_ZOOM,view.getResizeMode());}
 @Test public void holdGestureCanBeDisabledFromTheActualSettingsMenu(){find(options(),"길게 눌러 2배속 · 켜짐").performClick();assertFalse(app().getSharedPreferences("viewer",0).getBoolean("holdToSpeed",true));}
}
