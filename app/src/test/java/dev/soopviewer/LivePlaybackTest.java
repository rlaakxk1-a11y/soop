package dev.soopviewer;
import org.junit.Test;
import org.json.*;
import java.io.IOException;
import static org.junit.Assert.*;

public class LivePlaybackTest {
 private static final String MASTER="#EXTM3U\n#EXT-X-STREAM-INF:NAME=original,BANDWIDTH=8000000,RESOLUTION=1920x1080\nauth_playlist.m3u8?aid=signed-original&keep=1\n#EXT-X-STREAM-INF:NAME=hd,RESOLUTION=960x540\nauth_playlist.m3u8?aid=signed-hd\n";
 private static final String MEDIA="#EXTM3U\n#EXTINF:2,\nsegment.ts\n";
 private static final ContentItem ITEM=new ContentItem("987","demo","LIVE","demo","","",false);
 @Test public void pinsServerSignedVariantWithoutOverwritingItsToken()throws Exception{assertEquals("https://cdn.example/live/auth_playlist.m3u8?aid=signed-original&keep=1",LiveManifest.select("https://cdn.example/live/master.m3u8?aid=cast",MASTER,"original"));}
 @Test(expected=IOException.class) public void doesNotInventUnavailableQuality()throws Exception{LiveManifest.select("https://cdn.example/master",MASTER,"hd4k");}
 @Test public void autoRetainsAdaptiveMaster()throws Exception{assertEquals("https://cdn.example/master",LiveManifest.select("https://cdn.example/master",MASTER,"auto"));}
 private static class Fixture extends SoopClient{
  boolean cast=true,malformedCast=false;String cdn="gs_cdn";int castStatus=0,childStatus=0,webStatus=0,castRequests=0,webRequests=0,childRequests=0,assignments=0,assignmentFailure=0;
  @Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(url.contains("/broad/a/watch")){assertTrue(form.contains("player_type=webm"));assertFalse(form.contains("confirm_adult"));return new JSONObject("{result:1,data:{broad_no:'987',broad_title:'LIVE',is_chromecast:"+cast+",chromecast_authentication_key:'cast-key',hls_authentication_key:'web-540-key',cdn_chromecast:'"+cdn+"',cdn:'"+cdn+"',mobileweb_resource_manager_url:'https://livestream-manager.sooplive.com/broad_stream_assign.html',viewpreset:[{name:'original',label_resolution:'1080'},{name:'hd',label_resolution:'540'}]}}");}
   if(url.contains("player_live_api"))return new JSONObject("{CHANNEL:{RESULT:1,CHATNO:'42',CHDOMAIN:'chat-demo.sooplive.com',CHPT:8040}}");
   assignments++;if(assignments==1&&assignmentFailure!=0)throw new HttpStatusException(assignmentFailure);
   assertTrue(url.contains("987-common-master-hls"));assertTrue(url.contains("return_type="+SoopClient.mobileWebCdn(cdn)));return new JSONObject("{view_url:'https://cdn.example/live/master.m3u8?keep=1'}");
  }
  @Override public String text(String url,String form,String ref)throws Exception{
   if(url.contains("aid=signed-original")){childRequests++;if(childStatus>0)throw new HttpStatusException(childStatus);return MEDIA;}
   if(url.contains("aid=cast-key")){castRequests++;if(castStatus>0)throw new HttpStatusException(castStatus);return malformedCast?"<html>bad gateway</html>":MASTER;}
   assertTrue(url.contains("aid=web-540-key"));webRequests++;if(webStatus>0)throw new HttpStatusException(webStatus);return MEDIA;
  }
 }
 @Test public void domesticCdnUsesMobileWebDeliveryAndPreservesChat()throws Exception{
  Fixture f=new Fixture();PlaybackInfo p=f.resolve(ITEM);assertEquals("gs_cdn_mobile_web",SoopClient.mobileWebCdn("gs_cdn"));assertEquals("original",p.selectedQuality);assertTrue(p.media.get(0).contains("aid=signed-original"));assertEquals(1,f.childRequests);assertEquals(0,f.webRequests);assertEquals("42",p.live.optString("CHATNO"));assertTrue(p.live.optBoolean("_MOBILE"));
 }
 @Test public void globalAndRestrictedCdnAssignmentsRemainIntact(){assertEquals("gcp_cdn",SoopClient.mobileWebCdn("gcp_cdn"));assertEquals("gs_cdn_mobile_app_subscribe",SoopClient.mobileWebCdn("gs_cdn_mobile_app_subscribe"));assertEquals("lg_cdn_mobile_web",SoopClient.mobileWebCdn("lg_cdn"));}
 @Test public void absentCastEntitlementUsesOnlyWebKey()throws Exception{Fixture f=new Fixture();f.cast=false;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(0,f.castRequests);}
 @Test public void castHttp400FallsBackToIndependentlyIssuedWebKey()throws Exception{Fixture f=new Fixture();f.castStatus=400;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(1,f.castRequests);assertEquals(1,f.webRequests);assertTrue(f.playbackDiagnostic().contains("HTTP 400"));assertFalse(f.playbackDiagnostic().contains("cast-key"));assertFalse(f.playbackDiagnostic().contains("web-540-key"));assertFalse(f.playbackDiagnostic().contains("https://"));}
 @Test public void signedChildHttp400IsDetectedBeforeStartingPlayer()throws Exception{Fixture f=new Fixture();f.childStatus=400;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(1,f.childRequests);assertEquals(1,f.webRequests);}
 @Test public void assignmentFailureCanRecoverOnWebRoute()throws Exception{Fixture f=new Fixture();f.assignmentFailure=503;assertTrue(f.resolve(ITEM).media.get(0).contains("aid=web-540-key"));assertEquals(2,f.assignments);}
 @Test public void malformedCastManifestCanRecoverOnWebRoute()throws Exception{Fixture f=new Fixture();f.malformedCast=true;assertTrue(f.resolve(ITEM).media.get(0).contains("aid=web-540-key"));}
 @Test public void authorizationFailureNeverTriggersAnotherRoute()throws Exception{for(int status:new int[]{401,403}){Fixture f=new Fixture();f.castStatus=status;try{f.resolve(ITEM);fail("must stop on authorization rejection");}catch(IOException expected){assertEquals(0,f.webRequests);assertEquals(1,f.assignments);}}}
 @Test public void twoBrokenRoutesReportTheFailedStage()throws Exception{Fixture f=new Fixture();f.castStatus=400;f.webStatus=400;try{f.resolve(ITEM);fail("expected failure");}catch(IOException expected){assertTrue(expected.getMessage().contains("모바일 웹 재생 목록"));assertTrue(expected.getMessage().contains("HTTP 400"));assertEquals(1,f.webRequests);}}
 @Test public void playerRecoveryUsesOnlyFreshWebEntitlement()throws Exception{Fixture f=new Fixture();String url=f.fallbackLiveStream(ITEM,new JSONObject(),"original");assertTrue(url.contains("aid=web-540-key"));assertEquals(0,f.castRequests);assertEquals(1,f.webRequests);}
 @Test(expected=IOException.class) public void mobilePermissionFailureStopsBeforeCdn()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("/broad/a/watch"));return new JSONObject("{result:0,data:{code:-3002,message:'Login required'}}");}}.resolve(ITEM);}
}
