package dev.soopviewer;
import org.junit.Test;
import org.json.*;
import java.io.IOException;
import static org.junit.Assert.*;

public class LivePlaybackTest {
 private static final String MASTER="#EXTM3U\n#EXT-X-STREAM-INF:NAME=original,BANDWIDTH=8000000,RESOLUTION=1920x1080\nauth_playlist.m3u8?aid=signed-original&keep=1\n#EXT-X-STREAM-INF:NAME=hd,RESOLUTION=960x540\nauth_playlist.m3u8?aid=signed-hd\n";
 private static final String WEB_MASTER="#EXTM3U\n#EXT-X-STREAM-INF:NAME=hd,RESOLUTION=960x540\nauth_playlist.m3u8?aid=signed-web-hd\n";
 private static final String MEDIA="#EXTM3U\n#EXTINF:2,\nsegment.ts\n";
 private static final ContentItem ITEM=new ContentItem("987","demo","LIVE","demo","","",false);
 @Test public void pinsServerSignedVariantWithoutOverwritingItsToken()throws Exception{assertEquals("https://cdn.example/live/auth_playlist.m3u8?aid=signed-original&keep=1",LiveManifest.select("https://cdn.example/live/master.m3u8?aid=cast",MASTER,"original"));}
 @Test(expected=IOException.class) public void doesNotInventUnavailableQuality()throws Exception{LiveManifest.select("https://cdn.example/master",MASTER,"hd4k");}
 @Test public void autoRetainsAdaptiveMaster()throws Exception{assertEquals("https://cdn.example/master",LiveManifest.select("https://cdn.example/master",MASTER,"auto"));}
 private static class Fixture extends SoopClient{
  boolean cast=true,malformedCast=false,webMultivariant=false;String spare="";boolean directSucceeds=false;String cdn="gs_cdn";int castStatus=0,childStatus=0,webStatus=0,castRequests=0,webRequests=0,childRequests=0,assignments=0,assignmentFailure=0;
  @Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(url.contains("/broad/a/watch")){assertTrue(form.contains("player_type=webm"));assertFalse(form.contains("confirm_adult"));return new JSONObject("{result:1,data:{broad_no:'987',broad_title:'LIVE',is_chromecast:"+cast+",chromecast_authentication_key:'cast-key',chromecast_authentication_spare_key:'"+spare+"',hls_authentication_key:'web-540-key',cdn_chromecast:'"+cdn+"',cdn:'"+cdn+"',mobileweb_resource_manager_url:'https://livestream-manager.sooplive.com/broad_stream_assign.html',viewpreset:[{name:'original',label_resolution:'1080'},{name:'hd',label_resolution:'540'}]}}");}
   if(url.contains("player_live_api"))return new JSONObject("{CHANNEL:{RESULT:1,CHATNO:'42',CHDOMAIN:'chat-demo.sooplive.com',CHPT:8040}}");
   assignments++;if(assignments==1&&assignmentFailure!=0)throw new HttpStatusException(assignmentFailure);
   assertTrue(url.contains("987-common-master-hls")||url.contains("987-common-original-hls"));assertTrue(url.contains("return_type="+SoopClient.mobileWebCdn(cdn)));return new JSONObject("{view_url:'https://cdn.example/live/"+(url.contains("-original-hls")?"direct":"master")+".m3u8?keep=1'}");
  }
  @Override public String text(String url,String form,String ref)throws Exception{
   if(url.contains("aid=signed-web-hd"))return MEDIA;
   if(url.contains("aid=signed-original")){childRequests++;if(childStatus>0)throw new HttpStatusException(childStatus);return MEDIA;}
   if(url.contains("aid=spare-key"))return MASTER;
   if(url.contains("aid=cast-key")){castRequests++;if(directSucceeds&&url.contains("direct.m3u8"))return MEDIA;if(castStatus>0)throw new HttpStatusException(castStatus);return malformedCast?"<html>bad gateway</html>":MASTER;}
   assertTrue(url.contains("aid=web-540-key"));webRequests++;if(webStatus>0)throw new HttpStatusException(webStatus);return webMultivariant?WEB_MASTER:MEDIA;
  }
 }
 @Test public void domesticCdnUsesMobileWebDeliveryAndPreservesChat()throws Exception{
  Fixture f=new Fixture();PlaybackInfo p=f.resolve(ITEM);assertEquals("gs_cdn_mobile_web",SoopClient.mobileWebCdn("gs_cdn"));assertEquals("original",p.selectedQuality);assertTrue(p.media.get(0).contains("aid=signed-original"));assertEquals(1,f.childRequests);assertEquals(0,f.webRequests);assertEquals("42",p.live.optString("CHATNO"));assertTrue(p.live.optBoolean("_MOBILE"));
 }
 @Test public void globalAndRestrictedCdnAssignmentsRemainIntact(){assertEquals("gcp_cdn",SoopClient.mobileWebCdn("gcp_cdn"));assertEquals("gs_cdn_mobile_app_subscribe",SoopClient.mobileWebCdn("gs_cdn_mobile_app_subscribe"));assertEquals("lg_cdn_mobile_web",SoopClient.mobileWebCdn("lg_cdn"));}
 @Test public void absentCastEntitlementUsesOnlyWebKey()throws Exception{Fixture f=new Fixture();f.cast=false;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(0,f.castRequests);}
 @Test public void castHttp400FallsBackToIndependentlyIssuedWebKey()throws Exception{Fixture f=new Fixture();f.castStatus=400;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(2,f.castRequests);assertEquals(1,f.webRequests);assertTrue(f.playbackDiagnostic().contains("HTTP 400"));assertFalse(f.playbackDiagnostic().contains("cast-key"));assertFalse(f.playbackDiagnostic().contains("web-540-key"));assertFalse(f.playbackDiagnostic().contains("https://"));}
 @Test public void webOnly540VariantRemainsPlayableAfterOriginalFails()throws Exception{Fixture f=new Fixture();f.castStatus=400;f.webMultivariant=true;assertTrue(f.resolve(ITEM).media.get(0).contains("aid=signed-web-hd"));assertTrue(f.playbackDiagnostic().contains("웹 권한에서 제공하는 화질"));}
 @Test public void bestAvailableSelectsActualHighestAllowedVariant()throws Exception{assertEquals("https://cdn.example/live/auth_playlist.m3u8?aid=signed-original&keep=1",LiveManifest.bestAvailable("https://cdn.example/live/master",MASTER));}
 @Test public void signedChildHttp400IsDetectedBeforeStartingPlayer()throws Exception{Fixture f=new Fixture();f.childStatus=400;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("aid=web-540-key"));assertEquals(2,f.childRequests);assertEquals(1,f.webRequests);}
 @Test public void directAssignmentRecoversAfterMasterAssignmentFailure()throws Exception{Fixture f=new Fixture();f.assignmentFailure=503;assertTrue(f.resolve(ITEM).media.get(0).contains("aid=signed-original"));assertEquals(2,f.assignments);}
 @Test public void malformedCastManifestCanRecoverOnWebRoute()throws Exception{Fixture f=new Fixture();f.malformedCast=true;assertTrue(f.resolve(ITEM).media.get(0).contains("aid=web-540-key"));}
 @Test public void highQualityDenialStopsHigherQualityRetriesButAllowsTheIndependentMobileGrant()throws Exception{for(int status:new int[]{401,403}){Fixture f=new Fixture();f.castStatus=status;assertTrue(f.resolve(ITEM).media.get(0).contains("web-540-key"));assertEquals(1,f.castRequests);assertEquals(1,f.webRequests);assertEquals(2,f.assignments);}}

 @Test public void twoBrokenRoutesReportTheFailedStage()throws Exception{Fixture f=new Fixture();f.castStatus=400;f.webStatus=400;try{f.resolve(ITEM);fail("expected failure");}catch(IOException expected){assertTrue(expected.getMessage().contains("모바일 웹 재생 목록"));assertTrue(expected.getMessage().contains("HTTP 400"));assertEquals(1,f.webRequests);}}
 @Test public void playerRecoveryUsesOnlyFreshWebEntitlement()throws Exception{Fixture f=new Fixture();String url=f.fallbackLiveStream(ITEM,new JSONObject(),"original");assertTrue(url.contains("aid=web-540-key"));assertEquals(0,f.castRequests);assertEquals(1,f.webRequests);}
 @Test public void directPresetRecoversAnUnsupportedMaster()throws Exception{Fixture f=new Fixture();f.cdn="lg_cdn_mobile_web";f.castStatus=400;f.directSucceeds=true;PlaybackInfo p=f.resolve(ITEM);assertTrue(p.media.get(0).contains("direct.m3u8"));assertEquals(0,f.webRequests);}
 @Test public void serverIssuedSpareKeyIsTriedBeforeWebDowngrade()throws Exception{Fixture f=new Fixture();f.castStatus=400;f.spare="spare-key";assertTrue(f.resolve(ITEM).media.get(0).contains("aid=signed-original"));assertEquals(0,f.webRequests);}
 @Test public void explicit1080SelectionDoesNotSilentlyReturn540()throws Exception{Fixture f=new Fixture();f.castStatus=400;try{f.liveStream(ITEM,new JSONObject("{_MOBILE:true,VIEWPRESET:[{name:'original',height:1080}]}"),"original");fail("must report failed manual selection");}catch(IOException expected){assertEquals(0,f.webRequests);}}
 private static class DesktopFixture extends Fixture{
  int desktopTokens=0,tokenResult=1;boolean desktopAvailable=true;String requestedPreset="";
  DesktopFixture(){castStatus=400;cdn="lg_cdn_mobile_web";}
  @Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(url.contains("player_live_api")){
    if(form.contains("type=aid")){desktopTokens++;assertTrue(form.contains("quality=original"));assertTrue(form.contains("player_type=html5"));return new JSONObject("{CHANNEL:{RESULT:"+tokenResult+",AID:'quality-specific-web-key'}}");}
    return new JSONObject("{CHANNEL:{RESULT:1,BNO:'987',CDN:'lg_cdn',RMD:'https://livestream-manager.sooplive.com',VIEWPRESET:"+(desktopAvailable?"[{name:'original',label_resolution:'1080'}]":"[]")+"}}");
   }
   if(url.contains("return_type=lg_cdn_pc_web")){requestedPreset=okhttp3.HttpUrl.get(url).queryParameter("broad_key");return new JSONObject("{view_url:'https://cdn.example/web-quality.m3u8'}");}
   return super.json(url,form,ref);
  }
  @Override public String text(String url,String form,String ref)throws Exception{if(url.contains("aid=quality-specific-web-key")){assertEquals(ITEM.pageUrl(),ref);return MEDIA;}return super.text(url,form,ref);}
 }
 @Test public void castTransportFailureCanUseAnIndependentlyIssuedQualitySpecificWebToken()throws Exception{DesktopFixture f=new DesktopFixture();String media=f.resolve(ITEM).media.get(0);assertTrue(media.contains("quality-specific-web-key"));assertEquals(1,f.desktopTokens);assertEquals("987-common-original-hls",f.requestedPreset);assertEquals(0,f.webRequests);assertTrue(f.playbackDiagnostic().contains("화질별 웹 인증 완료"));assertFalse(f.playbackDiagnostic().contains("quality-specific-web-key"));}
 @Test public void manual1080CanRecoverOnQualitySpecificWebEntitlement()throws Exception{DesktopFixture f=new DesktopFixture();assertTrue(f.liveStream(ITEM,new JSONObject("{_MOBILE:true,VIEWPRESET:[{name:'original',height:1080}]}"),"original").contains("quality-specific-web-key"));assertEquals(0,f.webRequests);}
 @Test public void rejectedWebHighQualityDoesNotDiscardTheIndependentlyGrantedMobileStream()throws Exception{DesktopFixture f=new DesktopFixture();f.tokenResult=-6;assertTrue(f.resolve(ITEM).media.get(0).contains("web-540-key"));assertEquals(1,f.desktopTokens);assertEquals(1,f.webRequests);}

 @Test public void unofferedWebPresetIsNeverRequested()throws Exception{DesktopFixture f=new DesktopFixture();f.desktopAvailable=false;assertTrue(f.resolve(ITEM).media.get(0).contains("web-540-key"));assertEquals(0,f.desktopTokens);}
 @Test(expected=IOException.class) public void mobilePermissionFailureStopsBeforeCdn()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("/broad/a/watch"));return new JSONObject("{result:0,data:{code:-3002,message:'Login required'}}");}}.resolve(ITEM);}
}
