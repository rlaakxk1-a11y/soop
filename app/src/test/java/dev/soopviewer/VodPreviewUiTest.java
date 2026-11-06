package dev.soopviewer;
import android.app.Activity;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.ui.DefaultTimeBar;
import java.io.*;
import java.time.Duration;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,qualifiers="mdpi") @GraphicsMode(GraphicsMode.Mode.NATIVE)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class VodPreviewUiTest {
 private ActivityController<Activity> activity;private FrameLayout host;private DefaultTimeBar bar;private VodPreviewController preview;private Images images;
 private final List<Long> positions=new ArrayList<>();private boolean unavailable;
 private static class Images implements SnapshotSource{final List<Callback> callbacks=new ArrayList<>();boolean auto,closed;int canceled;public void request(VodPreview.Frame f,Callback c){callbacks.add(c);if(auto)c.complete(bitmap());}public void cancel(){canceled++;}public void close(){closed=true;}}
 private static Bitmap bitmap(){Bitmap b=Bitmap.createBitmap(256,144,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint();p.setShader(new LinearGradient(0,0,256,144,0xff287e9c,0xff5a345c,Shader.TileMode.CLAMP));c.drawRect(0,0,256,144,p);p.setShader(null);p.setColor(Color.WHITE);p.setTextSize(20);c.drawText("Preview fixture",30,80,p);return b;}
 @Before public void setup(){activity=Robolectric.buildActivity(Activity.class).setup();host=new FrameLayout(activity.get());host.setBackgroundColor(Color.rgb(25,30,38));activity.get().setContentView(host);bar=new DefaultTimeBar(activity.get());bar.setDuration(40738600);FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(-1,48,Gravity.BOTTOM);lp.bottomMargin=40;host.addView(bar,lp);images=new Images();preview=new VodPreviewController(host,bar,images,p->{positions.add(p);return unavailable?null:VodPreview.frame(new VodPreview.Track("https://videoimg.sooplive.com/snapshot?rowKey=fixture&column=",40738600),p);},()->40738600);layout(800,450);}
 @After public void cleanup(){preview.close();activity.pause().stop().destroy();}
 private void layout(int width,int height){host.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));host.layout(0,0,width,height);}
 private View bubble(){return host.findViewWithTag("watch.preview");}private ImageView image(){return host.findViewWithTag("watch.preview.image");}
 private void touch(int action,float x){long t=SystemClock.uptimeMillis();MotionEvent e=MotionEvent.obtain(t,t,action,x,24,0);bar.dispatchTouchEvent(e);e.recycle();}
 @Test public void draggingTheActualTimeBarShowsAndMovesThenHidesThePreview(){images.auto=true;touch(MotionEvent.ACTION_DOWN,100);assertEquals(View.VISIBLE,bubble().getVisibility());touch(MotionEvent.ACTION_MOVE,650);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));layout(800,450);assertEquals(WatchHistory.time(positions.get(positions.size()-1)),((TextView)host.findViewWithTag("watch.preview.time")).getText().toString());assertNotNull(image().getDrawable());assertTrue(bubble().getX()>=0);assertTrue(bubble().getX()+bubble().getWidth()<=800);assertTrue(bubble().getY()+bubble().getHeight()<=450);touch(MotionEvent.ACTION_UP,650);assertEquals(View.GONE,bubble().getVisibility());assertNull(image().getDrawable());}
 @Test public void delayedOldImageCannotAppearUnderANewerTimestampOrAfterScrubStops(){preview.onScrubStart(bar,1000);SnapshotSource.Callback old=images.callbacks.get(0);preview.onScrubMove(bar,9000);Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100));old.complete(bitmap());assertNull(image().getDrawable());Bitmap latest=bitmap();images.callbacks.get(1).complete(latest);assertSame(latest,((BitmapDrawable)image().getDrawable()).getBitmap());preview.onScrubStop(bar,9000,true);images.callbacks.get(1).complete(latest);assertNull(image().getDrawable());assertEquals(View.GONE,bubble().getVisibility());}
 @Test public void unavailableSnapshotKeepsTheSeekTimeWithoutShowingAnUnrelatedCover(){unavailable=true;preview.onScrubStart(bar,23609000);assertEquals("6:33:29",((TextView)host.findViewWithTag("watch.preview.time")).getText().toString());assertTrue(images.callbacks.isEmpty());assertNull(image().getDrawable());assertEquals(View.VISIBLE,bubble().getVisibility());}
 @Test public void previewFitsPortraitAndLandscapeAndIsReleasedWithThePlayer()throws Exception{images.auto=true;preview.onScrubStart(bar,23609000);layout(800,450);capture("preview-landscape");layout(400,225);capture("preview-portrait");assertTrue(bubble().getX()+bubble().getWidth()<=400);assertTrue(bubble().getY()+bubble().getHeight()<=225);preview.close();assertTrue(images.closed);assertNull(host.findViewWithTag("watch.preview"));}
 private void capture(String name)throws Exception{layout(host.getWidth(),host.getHeight());Bitmap b=Bitmap.createBitmap(host.getWidth(),host.getHeight(),Bitmap.Config.ARGB_8888);host.draw(new Canvas(b));File dir=new File("build/reports/mobile-screenshots");dir.mkdirs();try(OutputStream out=new FileOutputStream(new File(dir,name+".png"))){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
}
