package dev.soopviewer;

import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class WatchHistoryTest {
 private ContentItem vod(String id){return new ContentItem(id,"demo","다시보기 "+id,"스트리머","https://img.example/thumb.jpg","",true);}
 private WatchHistory.Entry entry(String id,int part,long pos,boolean complete){return new WatchHistory.Entry(vod(id),part,pos,600000,300000+pos,1234,complete);}
 @Test public void survivesRestartWithMultipartPosition(){WatchHistory h=new WatchHistory();h.put(entry("123",1,42000,false));WatchHistory.Entry restored=WatchHistory.restore(h.serialize()).find("123");assertNotNull(restored);assertEquals(1,restored.resumePart(2));assertEquals(42000,restored.resumePosition(Arrays.asList(300000L,300000L)));assertEquals("5:42부터 이어보기",restored.label());assertEquals("다시보기 123",restored.item.title);}
 @Test public void mostRecentFirstWithoutDuplicates(){WatchHistory h=new WatchHistory();h.put(entry("1",0,1000,false));h.put(entry("2",0,1000,false));h.put(entry("1",0,2000,false));assertEquals(2,h.recent().size());assertEquals("1",h.recent().get(0).item.id);assertEquals(2000,h.find("1").positionMs);assertEquals("1",WatchHistory.restore(h.serialize()).recent().get(0).item.id);}
 @Test public void finishedAndRemovedPartsRestartAtBeginning(){WatchHistory.Entry complete=entry("1",1,300000,true);assertEquals(0,complete.resumePart(2));assertEquals(0,complete.resumePosition(Arrays.asList(300000L,300000L)));WatchHistory.Entry changed=entry("2",3,42000,false);assertEquals(0,changed.resumePart(2));assertEquals(0,changed.resumePosition(Arrays.asList(300000L,300000L)));}
 @Test public void shortenedFileClampsPositionAndUnknownDurationPreservesIt(){WatchHistory.Entry saved=entry("1",0,50000,false);assertEquals(19000,saved.resumePosition(Collections.singletonList(20000L)));assertEquals(50000,saved.resumePosition(Collections.singletonList(0L)));}
 @Test public void capsHistoryAt100AndAllowsDeletion(){WatchHistory h=new WatchHistory();for(int i=1;i<=105;i++)h.put(entry(""+i,0,1000,false));assertEquals(100,h.recent().size());assertNull(h.find("1"));h.remove("105");assertNull(WatchHistory.restore(h.serialize()).find("105"));h.clear();assertTrue(WatchHistory.restore(h.serialize()).recent().isEmpty());}
 @Test public void liveIsNotSavedAndMalformedDataIsTolerated(){WatchHistory h=new WatchHistory();h.put(new WatchHistory.Entry(new ContentItem("demo","demo","LIVE","","","",false),0,100,0,100,0,false));assertTrue(h.recent().isEmpty());assertTrue(WatchHistory.restore("bad JSON").recent().isEmpty());assertTrue(WatchHistory.restore("{\"version\":99}").recent().isEmpty());}
 @Test public void progressAndTimeLabels(){assertEquals(57,entry("1",1,42000,false).progress());assertEquals("1:02:03",WatchHistory.time(3723000));assertEquals("0:00",WatchHistory.time(-1));}
}
