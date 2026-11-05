package dev.soopviewer;
import org.junit.Test;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;
public class LiveQualityTest {
 @Test public void prefersFullHdOverMobilePresets()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'sd',label:'540p'},{name:'hd',label:'720p'},{name:'original'},{name:'fullhd',label:'1080p'}]}"));assertEquals("fullhd",LiveQuality.preferred(q));assertEquals(1080,q.get(0).height);}
 @Test public void requestsOriginalIfResolutionIsNotAdvertised()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'hd'},{name:'original'},{name:'sd'}]}"));assertEquals("original",LiveQuality.preferred(q));assertEquals(0,q.get(0).height);assertEquals("원본",q.get(0).label);}
 @Test public void noInvented1080ForLowerResolutionSource()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{RESOLUTION:'1280x720',VIEWPRESET:[{name:'original'},{name:'sd'}]}"));assertEquals(720,q.get(0).height);assertTrue(q.stream().noneMatch(x->x.height==1080));}
 @Test public void readsSoopResolutionFieldForOriginalPreset()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'original',label:'원본',label_resolution:'1920x1080'},{name:'hd',label:'고화질',label_resolution:'1280x720'}]}"));assertEquals("original",LiveQuality.preferred(q));assertEquals(1080,q.get(0).height);assertEquals("1080p",q.get(0).label);}
 @Test public void recognizesResolutionEncodedInPresetName()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'FHD1080'},{name:'HD720'}]}"));assertEquals(1080,q.get(0).height);}
 @Test public void ignoresInvalidAndDuplicatePresets()throws Exception{List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'../bad'},{name:'fullhd'},{name:'fullhd'},{name:'auto'}]}"));assertEquals(1,q.size());assertEquals("master",LiveQuality.preferred(Collections.emptyList()));}
 @Test public void requestsBroadcastSpecificPresetsBeforeChoosingLiveQuality()throws Exception{
  SoopClient client=new SoopClient(){int liveCalls=0;@Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(form!=null&&form.contains("type=live")){liveCalls++;if(liveCalls==1)return new JSONObject("{CHANNEL:{RESULT:1,BNO:'987',RMD:'https://livestream-manager.sooplive.com',CDN:'gs_cdn',VIEWPRESET:[{name:'sd',label:'540p'}]}}");assertTrue(form.contains("bno=987"));return new JSONObject("{CHANNEL:{RESULT:1,BNO:'987',RMD:'https://livestream-manager.sooplive.com',CDN:'gs_cdn',VIEWPRESET:[{name:'sd',label:'540p'},{name:'original',label:'원본',label_resolution:'1920x1080'}]}}");}
   if(form!=null&&form.contains("type=aid")){assertTrue(form.contains("quality=original"));return new JSONObject("{CHANNEL:{AID:'test-aid'}}");}
   assertTrue(url.contains("987-common-original-hls"));assertTrue(url.contains("return_type=gs_cdn_pc_web"));
   return new JSONObject("{view_url:'https://cdn.example/live.m3u8'}");
  }};
  PlaybackInfo info=client.resolve(new ContentItem("demo","demo","LIVE","demo","","",false));assertEquals("original",info.selectedQuality);assertEquals(2,info.qualities.size());assertTrue(info.media.get(0).contains("aid=test-aid"));
 }
 @Test public void fallsBackToMasterOnlyWithoutAdvertisedPresets()throws Exception{
  SoopClient client=new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{
   if(form!=null&&form.contains("type=live"))return new JSONObject("{CHANNEL:{RESULT:1,BNO:'987'}}");
   if(form!=null&&form.contains("type=aid")){assertTrue(form.contains("quality=master"));return new JSONObject("{CHANNEL:{AID:'fixture'}}");}
   assertTrue(url.contains("987-common-master-hls"));return new JSONObject("{view_url:'https://cdn.example/live.m3u8'}");
  }};
  PlaybackInfo info=client.resolve(new ContentItem("987","demo","LIVE","demo","","",false));
  assertEquals("master",info.selectedQuality);assertTrue(info.qualities.isEmpty());
 }

}
