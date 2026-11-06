package dev.soopviewer;
import org.junit.Test;
import org.json.*;
import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class ServiceTest {
 @Test public void hostsAndLinks(){assertTrue(ServiceUrls.official("play.sooplive.com"));assertTrue(ServiceUrls.official("vod.sooplive.co.kr"));assertFalse(ServiceUrls.official("sooplive.com.evil.test"));assertFalse(ServiceUrls.official(null));assertEquals("",ServiceUrls.https("http://example.com/a"));assertEquals("https://img.example/a",ServiceUrls.https("//img.example/a"));assertFalse(ServiceUrls.userId("../escape"));assertEquals("https://www.sooplive.com/station/demo",ServiceUrls.station("demo"));}
 @Test public void catalogSchemas()throws Exception {
  JSONObject home=new JSONObject("{broad:[{user_id:'demo',broad_title:'방송',user_nick:'닉',broad_thumb:'//img.test/a',total_view_cnt:15}]}");List<ContentItem> rows=ContentItem.parse(home,false);assertEquals(1,rows.size());assertEquals("https://img.test/a",rows.get(0).thumb);assertEquals("15",rows.get(0).viewers);
  ContentItem live=ContentItem.parse(new JSONObject("{broad:[{user_id:'demo',broad_no:987,broad_title:'LIVE'}]}"),false).get(0);assertEquals("987",live.id);assertEquals("demo",live.user);
  List<ContentItem> vod=ContentItem.parse(new JSONObject("{data:[{user_id:'demo',title_no:123,title_name:'다시보기'}]}"),true);assertEquals("https://vod.sooplive.com/player/123",vod.get(0).pageUrl());
  assertTrue(ContentItem.parse(new JSONObject("{DATA:[]}"),true).isEmpty());
 }
 @Test(expected=IllegalArgumentException.class) public void changedCatalogSchemaFailsClearly()throws Exception {ContentItem.parse(new JSONObject("{}"),false);}
 @Test public void invalidCatalogRowsSkipped()throws Exception {assertTrue(ContentItem.parse(new JSONObject("{data:[{user_id:'../bad',title_no:3},{user_id:'demo',title_no:'oops'}]}"),true).isEmpty());}
 @Test public void vodPartsAndChat()throws Exception{
  PlaybackInfo p=PlaybackInfo.vod(new JSONObject("{data:{title:'영상',bj_id:'demo',files:[{file:'https://cdn.test/a.m3u8',chat:'https://cdn.test/chat',duration:300000},{file:'https://cdn.test/b.m3u8',duration:60000}]}}"));assertEquals(2,p.media.size());assertEquals(300000L,(long)p.durations.get(0));assertEquals("",p.chat.get(1));
 }
 @Test(expected=Exception.class) public void adultLoginRequired()throws Exception {PlaybackInfo.vod(new JSONObject("{data:{adult_status:'notLogin',files:[{file:'https://cdn.test/a'}]}}"));}
 @Test(expected=Exception.class) public void emptyPlaybackFails()throws Exception {PlaybackInfo.vod(new JSONObject("{data:{files:[]}}"));}
 @Test public void chatXmlSortedWithStickers()throws Exception {List<ChatMessage> rows=ChatParser.xml("<root><chat><t>2.5</t><n>한글</n><m>안녕 &amp; 반가워</m></chat><ogq><t>1</t><sn>닉</sn></ogq></root>");assertEquals(2,rows.size());assertEquals(1000,rows.get(0).timeMs);assertEquals("한글",rows.get(1).nick);assertEquals("안녕 & 반가워",rows.get(1).text);}
 @Test(expected=Exception.class) public void xmlEntitiesRejected()throws Exception {ChatParser.xml("<!DOCTYPE root [<!ENTITY x SYSTEM 'file:///etc/passwd'>]><root>&x;</root>");}
 @Test public void chatPacketLengthUsesUtf8Bytes(){String packet=ChatParser.packet(5,"\f한글\fuid\f\f\f\f닉\f");List<ChatMessage> rows=ChatParser.packets(packet.getBytes(StandardCharsets.UTF_8));assertEquals(1,rows.size());assertEquals("한글",rows.get(0).text);assertEquals("닉",rows.get(0).nick);assertTrue(ChatParser.packets(new byte[]{1,2,3}).isEmpty());byte[] truncated=Arrays.copyOf(packet.getBytes(StandardCharsets.UTF_8),15);assertTrue(ChatParser.packets(truncated).isEmpty());}
 @Test public void adDomainBoundaries(){assertTrue(AdRules.blocked("ad.doubleclick.net"));assertFalse(AdRules.blocked("doubleclick.net.example"));assertFalse(AdRules.blocked("live.sooplive.com"));}
 @Test public void chatSeeksUseTheCurrentFileClock(){List<ChatMessage> rows=Arrays.asList(new ChatMessage(1000,"a","first"),new ChatMessage(9000,"b","second"));assertEquals(2,ChatTimeline.visible(rows,10000).size());assertEquals(1,ChatTimeline.visible(rows,2000).size());assertEquals(0,ChatTimeline.chunkStart(299999,300));assertEquals(300,ChatTimeline.chunkStart(300000,300));assertEquals(0,ChatTimeline.chunkStart(-1,300));}
 @Test public void stationCanSupplyMissingBroadcasterId()throws Exception {assertEquals("demo",ContentItem.parse(new JSONObject("{data:[{title_no:123,title_name:'영상'}]}"),true,"demo").get(0).user);}
 @Test public void stationVodThumbnailAliasesAreRead()throws Exception {ContentItem row=ContentItem.parse(new JSONObject("{data:[{title_no:123,title_name:'영상',title_img:'//videoimg.sooplive.com/thumb.jpg'}]}"),true,"demo").get(0);assertEquals("https://videoimg.sooplive.com/thumb.jpg",row.thumb);}
 @Test public void vodDetailThumbnailIsAvailableForListFallback()throws Exception {PlaybackInfo p=PlaybackInfo.vod(new JSONObject("{data:{title:'영상',bj_id:'demo',thumb:'//videoimg.sooplive.com/thumb.jpg',files:[{file:'https://cdn.test/a.m3u8'}]}}"));assertEquals("https://videoimg.sooplive.com/thumb.jpg",p.thumbnail);}
 @Test public void durationFormatsHoursAndMilliseconds()throws Exception {assertEquals("2:03:04",ContentItem.duration(new JSONObject("{duration:7384}")));assertEquals("2:03:04",ContentItem.duration(new JSONObject("{total_file_duration:7384000}")));assertEquals("",ContentItem.duration(new JSONObject("{}")));}
}
