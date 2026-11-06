package dev.soopviewer;
import android.net.Uri;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import okhttp3.*;
import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

/** Runs the real OkHttp request builders and Media3 datasource against an in-memory transport. */
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public class PlaybackTransportTest {
 private static final ContentItem ITEM=new ContentItem("987","demo","LIVE","demo","","",false);
 private SoopClient client;private boolean denyDesktop;private final List<Request> calls=new ArrayList<>();
 @Before public void setup(){client=new SoopClient();OkHttpClient intercepted=client.http.newBuilder().addInterceptor(chain->{Request r=chain.request();calls.add(r);String url=r.url().toString(),body="",form="";int status=200;if(r.body()!=null){okio.Buffer b=new okio.Buffer();r.body().writeTo(b);form=b.readUtf8();}
  if(url.contains("/broad/a/watch"))body="{result:1,data:{broad_no:'987',is_chromecast:true,chromecast_authentication_key:'cast-key',hls_authentication_key:'mobile-key',cdn_chromecast:'lg_cdn_mobile_web',cdn:'lg_cdn_mobile_web',mobileweb_resource_manager_url:'https://livestream-manager.sooplive.com/broad_stream_assign.html',viewpreset:[{name:'original',label_resolution:'1080'}]}}";
  else if(url.contains("player_live_api"))body=form.contains("type=aid")?"{CHANNEL:{RESULT:1,AID:'pc-key'}}":"{CHANNEL:{RESULT:1,BNO:'987',CDN:'lg_cdn',RMD:'https://livestream-manager.sooplive.com',VIEWPRESET:[{name:'original',label_resolution:'1080'}]}}";
  else if(url.contains("broad_stream_assign"))body=url.contains("lg_cdn_pc_web")?"{view_url:'https://live-pcweb-kr-cdn-z02.sooplive.com/auth_playlist.m3u8'}":"{view_url:'https://live-mobileweb-kr-cdn-z02.sooplive.com/master.m3u8'}";
  else if(url.contains("live-pcweb-")){boolean desktop=MediaRequestHeaders.DESKTOP_UA.equals(r.header("User-Agent"))&&"https://play.sooplive.com".equals(r.header("Origin"))&&ITEM.pageUrl().equals(r.header("Referer"));status=desktop&&!denyDesktop?200:403;body=url.endsWith("segment.ts")?"segment bytes":"#EXTM3U\n#EXTINF:2,\nsegment.ts\n";}
  else if(url.contains("aid=cast-key"))status=400;
  else if(url.contains("aid=mobile-key"))body="#EXTM3U\n#EXT-X-STREAM-INF:NAME=hd,RESOLUTION=960x540\nchild.m3u8?aid=low-key\n";
  else body="#EXTM3U\n#EXTINF:2,\nsegment.ts\n";
  return new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(status).message(status==200?"OK":"Denied").body(ResponseBody.create(body,MediaType.get("text/plain"))).build();}).build();ReflectionHelpers.setField(client,"http",intercepted);}
 @Test public void desktopEntitlementPlaylistAndMedia3SegmentKeepTheSameDesktopHeaders()throws Exception{PlaybackInfo p=client.resolve(ITEM);String media=p.media.get(0);assertTrue(media.contains("pc-key"));Map<String,String> h=client.mediaHeaders(ITEM,media);assertEquals(MediaRequestHeaders.DESKTOP_UA,h.get("User-Agent"));assertEquals(ITEM.pageUrl(),h.get("Referer"));OkHttpDataSource data=new OkHttpDataSource.Factory(client.http).setDefaultRequestProperties(h).createDataSource();data.open(new DataSpec(Uri.parse("https://live-pcweb-kr-cdn-z02.sooplive.com/segment.ts")));data.close();Request segment=calls.get(calls.size()-1);assertEquals("https://play.sooplive.com",segment.header("Origin"));assertEquals(MediaRequestHeaders.DESKTOP_UA,segment.header("User-Agent"));assertTrue(client.playbackDiagnostic().contains("PC User-Agent"));}
 @Test public void deniedDesktopDeliveryStillAllowsTheSeparatelyGrantedMobile540Playback()throws Exception{denyDesktop=true;String media=client.resolve(ITEM).media.get(0);assertTrue(media.contains("low-key"));assertEquals(MediaRequestHeaders.MOBILE_UA,client.mediaHeaders(ITEM,media).get("User-Agent"));assertEquals(1,calls.stream().filter(r->r.url().host().contains("live-pcweb-")).count());}
 @Test public void manualHighQualityDenialNeverReplacesTheCurrentStreamWithMobile540()throws Exception{denyDesktop=true;try{client.liveStream(ITEM,new org.json.JSONObject("{_MOBILE:true,VIEWPRESET:[{name:'original',height:1080}]}"),"original");fail("manual selection must fail");}catch(java.io.IOException expected){assertFalse(calls.stream().anyMatch(r->"mobile-key".equals(r.url().queryParameter("aid"))));}}
 @Test public void vodRequestsKeepVodOriginAndNeverInheritThePreviousLiveProfile(){ContentItem vod=new ContentItem("123","demo","VOD","","","",true);Map<String,String> h=client.mediaHeaders(vod,"https://vod.example/video.m3u8");assertEquals("https://vod.sooplive.com",h.get("Origin"));assertEquals(vod.pageUrl(),h.get("Referer"));}
}
