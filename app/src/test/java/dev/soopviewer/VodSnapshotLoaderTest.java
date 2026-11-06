package dev.soopviewer;
import android.graphics.*;
import android.os.Looper;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,shadows=RegionDecoderShadow.class) @GraphicsMode(GraphicsMode.Mode.NATIVE)
public class VodSnapshotLoaderTest {
 private VodSnapshotLoader loader;private byte[] sheet;private final AtomicInteger downloads=new AtomicInteger();
 private final VodPreview.Track track=new VodPreview.Track("https://videoimg.sooplive.com/snapshot?rowKey=fixture&column=",900000);
 @Before public void setup(){Bitmap b=Bitmap.createBitmap(100,100,Bitmap.Config.ARGB_8888);Canvas c=new Canvas(b);Paint p=new Paint();for(int y=0;y<10;y++)for(int x=0;x<10;x++){p.setColor(Color.rgb(x*24,y*24,50));c.drawRect(x*10,y*10,x*10+10,y*10+10,p);}ByteArrayOutputStream out=new ByteArrayOutputStream();b.compress(Bitmap.CompressFormat.PNG,100,out);sheet=out.toByteArray();}
 @After public void cleanup(){if(loader!=null)loader.close();}
 private void until(BooleanSupplier done)throws Exception{for(int i=0;i<200&&!done.getAsBoolean();i++){Thread.sleep(10);Shadows.shadowOf(Looper.getMainLooper()).idle();}assertTrue("async preview completed",done.getAsBoolean());}
 private void client(CountDownLatch started,CountDownLatch release,int status){loader=new VodSnapshotLoader(new OkHttpClient.Builder().addInterceptor(chain->{downloads.incrementAndGet();if(started!=null)started.countDown();if(release!=null)try{if(!release.await(3,TimeUnit.SECONDS))throw new IOException("test timeout");}catch(InterruptedException e){throw new IOException(e);}return new Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(status).message("fixture").body(ResponseBody.create(sheet,MediaType.get("image/png"))).build();}).build(),"https://vod.sooplive.com/player/123");}
 @Test public void regionDecodeSelectsTheRequestedCellRatherThanTheWholeContactSheet()throws Exception{Bitmap tile=VodSnapshotLoader.tile(sheet,VodPreview.frame(track,23*3000));assertEquals(10,tile.getWidth());assertEquals(10,tile.getHeight());assertEquals(Color.rgb(72,48,50),tile.getPixel(5,5));}
 @Test public void movingWithinOneSheetReusesTheDownloadedImage()throws Exception{client(null,null,200);List<Bitmap> results=new ArrayList<>();loader.request(VodPreview.frame(track,0),results::add);until(()->results.size()==1);loader.request(VodPreview.frame(track,3000),results::add);until(()->results.size()==2);assertEquals(1,downloads.get());assertNotEquals(results.get(0).getPixel(5,5),results.get(1).getPixel(5,5));}
 @Test public void fastScrubbingWhileDownloadingDeliversOnlyTheNewestCell()throws Exception{CountDownLatch started=new CountDownLatch(1),release=new CountDownLatch(1);client(started,release,200);List<String> events=new ArrayList<>();List<Bitmap> images=new ArrayList<>();loader.request(VodPreview.frame(track,0),b->events.add("old"));assertTrue(started.await(2,TimeUnit.SECONDS));loader.request(VodPreview.frame(track,59*3000),b->{events.add("new");images.add(b);});release.countDown();until(()->!events.isEmpty());assertEquals(Collections.singletonList("new"),events);assertEquals(1,downloads.get());assertEquals(Color.rgb(216,120,50),images.get(0).getPixel(5,5));}
 @Test public void missingSheetReturnsNoImageAndDoesNotRepeatedlyDownloadWhileDragging()throws Exception{client(null,null,404);AtomicInteger completed=new AtomicInteger();loader.request(VodPreview.frame(track,0),b->{assertNull(b);completed.incrementAndGet();});until(()->completed.get()==1);loader.request(VodPreview.frame(track,3000),b->{assertNull(b);completed.incrementAndGet();});until(()->completed.get()==2);assertEquals(1,downloads.get());}
}
