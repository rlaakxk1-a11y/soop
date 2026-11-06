package dev.soopviewer;
import org.junit.Test;
import org.json.JSONObject;
import static org.junit.Assert.*;

public class VodPreviewTest {
 private VodPreview.Track track(){return new VodPreview.Track("https://videoimg.sooplive.com/php/SnapshotLoad.php?rowKey=fixture_t&column=",18000000);}
 @Test public void firstSheetUsesColumnOneAndThreeSecondFrames(){VodPreview.Frame zero=VodPreview.frame(track(),0),next=VodPreview.frame(track(),8999);assertTrue(zero.url.endsWith("column=1"));assertEquals(0,zero.cell);assertEquals(2,next.cell);assertEquals(6000,next.sampleMs);}
 @Test public void fiveMinuteBoundaryChangesSheetAndResetsTheGridCell(){VodPreview.Frame last=VodPreview.frame(track(),299999),next=VodPreview.frame(track(),300000);assertEquals(99,last.cell);assertEquals(9,last.row());assertEquals(9,last.column());assertTrue(next.url.endsWith("column=2"));assertEquals(0,next.cell);}
 @Test public void finalPartialSheetNeverRequestsAFrameBeyondThePublishedDuration(){VodPreview.Frame f=VodPreview.frame(new VodPreview.Track(track().url,17999167),999999999);assertTrue(f.url.endsWith("column=60"));assertEquals(98,f.cell);assertEquals(17994000,f.sampleMs);}
 @Test public void missingSnapshotDoesNotSubstituteTheRepresentativeCover(){assertNull(VodPreview.frame(new VodPreview.Track("",30000),10000));assertNull(VodPreview.frame(new VodPreview.Track(track().url,0),10000));}
 @Test public void metadataKeepsPreviewTracksAlignedWithAcceptedMediaFiles()throws Exception{PlaybackInfo p=PlaybackInfo.vod(new JSONObject("{data:{files:[{file:'https://cdn.example/a.m3u8',duration:30000,snapshot:'https://videoimg.sooplive.com/snapshot?rowKey=a_t&column='},{file:'http://bad.example/b.m3u8',duration:60000,snapshot:'https://videoimg.sooplive.com/wrong'},{file:'https://cdn.example/c.m3u8',duration:90000,file_info_key:'fixture_c'}]}}"));assertEquals(2,p.previews.size());assertEquals(30000,p.previews.get(0).durationMs);assertEquals(90000,p.previews.get(1).durationMs);assertTrue(p.previews.get(1).url.contains("fixture_c_t"));}
 @Test public void independentSourceFilesHaveIndependentSnapshotPages(){VodPreview.Frame a=VodPreview.frame(track(),60000),b=VodPreview.frame(new VodPreview.Track("https://videoimg.sooplive.com/snapshot?rowKey=part2_t&column=",900000),60000);assertEquals(a.cell,b.cell);assertNotEquals(a.url,b.url);}
}
