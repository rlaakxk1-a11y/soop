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

 @Test public void readsNumericMobileResolutionAndLegacyPresetNames()throws Exception{
  List<LiveQuality> q=LiveQuality.offered(new JSONObject("{VIEWPRESET:[{name:'sd'},{name:'hd'},{name:'hd4k'},{name:'original',label_resolution:'1080'}]}"));
  assertEquals(1080,q.get(0).height);assertEquals(720,q.get(1).height);assertEquals(540,q.get(2).height);assertEquals(360,q.get(3).height);
 }
}
