package dev.soopviewer;
import org.junit.Test;
import org.json.*;
import java.io.IOException;
import static org.junit.Assert.*;

public class LivePlaybackTest {
 private static final String MASTER="#EXTM3U\n#EXT-X-STREAM-INF:NAME=original,BANDWIDTH=8000000,RESOLUTION=1920x1080\nauth_playlist.m3u8?aid=signed-original&keep=1\n#EXT-X-STREAM-INF:NAME=hd,RESOLUTION=960x540\nauth_playlist.m3u8?aid=signed-hd\n";
 @Test public void pinsServerSignedVariantWithoutOverwritingItsToken()throws Exception{
  assertEquals("https://cdn.example/live/auth_playlist.m3u8?aid=signed-original&keep=1",LiveManifest.select("https://cdn.example/live/master.m3u8?aid=cast",MASTER,"original"));
 }
 @Test(expected=IOException.class) public void doesNotInventUnavailableQuality()throws Exception{LiveManifest.select("https://cdn.example/master",MASTER,"hd4k");}
 @Test public void autoRetainsAdaptiveMaster()throws Exception{assertEquals("https://cdn.example/master",LiveManifest.select("https://cdn.example/master",MASTER,"auto"));}
 private SoopClient fixture(boolean cast){return new SoopClient(){
  @Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(url.contains("/broad/a/watch")){assertTrue(form.contains("player_type=webm"));assertFalse(form.contains("confirm_adult"));return new JSONObject("{result:1,data:{broad_no:'987',broad_title:'LIVE',is_chromecast:"+cast+",chromecast_authentication_key:'cast-key',hls_authentication_key:'web-540-key',cdn_chromecast:'gs_cdn_mobile_app',cdn:'gs_cdn_mobile_web',mobileweb_resource_manager_url:'https://livestream-manager.sooplive.com/broad_stream_assign.html',viewpreset:[{name:'original',label_resolution:'1080'},{name:'hd',label_resolution:'540'}]}}");}
   if(url.contains("player_live_api"))return new JSONObject("{CHANNEL:{RESULT:1,CHATNO:'42',CHDOMAIN:'chat-demo.sooplive.com',CHPT:8040}}");
   assertTrue(url.contains("987-common-master-hls"));assertTrue(url.contains(cast?"return_type=gs_cdn_mobile_app":"return_type=gs_cdn_mobile_web"));return new JSONObject("{view_url:'https://cdn.example/live/master.m3u8?keep=1'}");
  }
  @Override public String text(String url,String form,String ref){assertTrue(url.contains(cast?"aid=cast-key":"aid=web-540-key"));assertTrue(url.contains("keep=1"));return cast?MASTER:"#EXTM3U\n#EXTINF:2,\n960x540/segment.ts\n";}
 };}
 @Test public void resolveUsesAuthorizedMobileCastKeyAndPreservesChat()throws Exception{
  PlaybackInfo p=fixture(true).resolve(new ContentItem("987","demo","LIVE","demo","","",false));assertEquals("original",p.selectedQuality);assertTrue(p.media.get(0).contains("aid=signed-original"));assertFalse(p.media.get(0).contains("cast-key"));assertEquals("42",p.live.optString("CHATNO"));assertTrue(p.live.optBoolean("_MOBILE"));
 }
 @Test public void absentCastEntitlementUsesOnlyWebKey()throws Exception{PlaybackInfo p=fixture(false).resolve(new ContentItem("987","demo","LIVE","demo","","",false));assertTrue(p.media.get(0).contains("aid=web-540-key"));}
 @Test(expected=IOException.class) public void mobilePermissionFailureStopsBeforeCdn()throws Exception{
  new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("/broad/a/watch"));return new JSONObject("{result:0,data:{code:-3002,message:'Login required'}}");}}.resolve(new ContentItem("987","demo","LIVE","demo","","",false));
 }
}
