package dev.soopviewer;
import org.json.*;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class SearchTest {
 @Test public void mobileStreamerResultPreservesProfileAndBroadcastState()throws Exception{
  SearchPage p=SearchPage.parse(new JSONObject("{result:1,data:{has_more_list:true,groups:[{content_type:'bj',contents:[{user_id:'b13246',user_nick:'타요~',station_name:'타요TV',thumbnail:'https://profile.img.sooplive.com/LOGO/b1/b13246/b13246.jpg',favorite_cnt:'303354',broad_no:null},{user_id:'yangdoki',user_nick:'양도끼',broad_no:'298377285'}]}]}}"),"streamer");
  assertTrue(p.hasMore);assertEquals(2,p.items.size());ContentItem first=p.items.get(0);assertTrue(first.streamer);assertTrue(first.thumb.endsWith("b13246.jpg"));assertEquals("303354",first.favoriteCount);assertEquals("",first.liveBroadcast);assertEquals("298377285",p.items.get(1).liveBroadcast);
 }
 @Test public void liveAndVodMobileGroupsParseActualFields()throws Exception{
  SearchPage live=SearchPage.parse(new JSONObject("{result:1,data:{has_more_list:false,groups:[{content_type:'live',contents:[{user_id:'demo',broad_no:'123',title:'방송',thumbnail:'https://liveimg.sooplive.com/m/123',view_cnt:12}]}]}}"),"live");assertEquals("123",live.items.get(0).id);assertEquals("12",live.items.get(0).viewers);assertFalse(live.items.get(0).streamer);
  SearchPage vod=SearchPage.parse(new JSONObject("{result:1,data:{groups:[{content_type:'vod',contents:[{user_id:'demo',title_no:'456',title:'영상',duration:3661000}]}]}}"),"vod");assertTrue(vod.items.get(0).vod);assertEquals("1:01:01",vod.items.get(0).duration);
 }
 @Test public void integratedSearchKeepsTheSamePersonsProfileAndLiveBroadcast()throws Exception{
  ContentItem person=new ContentItem("123","demo","닉","닉","","",false);person.streamer=true;ContentItem live=new ContentItem("123","demo","방송","닉","","",false);
  assertEquals(2,BrowseOptions.additions(Collections.emptyList(),Arrays.asList(person,live)).size());
 }
 @Test public void searchUsesMobileContractAndRealPagination()throws Exception{
  final List<String> requests=new ArrayList<>();SoopClient c=new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertEquals("https://sch.sooplive.com/api.php",url);assertEquals("https://m.sooplive.com/",ref);requests.add(form);return new JSONObject("{result:1,data:{has_more_list:false,groups:[]}}");}};
  c.searchPage("타요","streamer",2,"score");c.searchPage("타요","live",1,"view_cnt");c.searchPage("타요","live",1,"reg_date");c.searchPage("타요","vod",1,"reg_date");
  assertTrue(requests.get(0).contains("nPageNo=2"));assertTrue(requests.get(0).contains("nListCnt=20"));assertTrue(requests.get(0).contains("m=bjSearch"));assertTrue(requests.get(0).contains("isMobile=1"));assertTrue(requests.get(1).contains("szOrder=pc_total_view_cnt"));assertTrue(requests.get(2).contains("szOrder=broad_start"));assertTrue(requests.get(3).contains("szOrder=reg_datetime"));
 }
 @Test public void integratedSearchRequestsAllThreeScopesWithoutMixingTheirRows()throws Exception{
  final Set<String> methods=new HashSet<>();SoopClient c=new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{methods.add(okhttp3.HttpUrl.get("https://fixture/?"+form).queryParameter("m"));return new JSONObject("{result:1,data:{has_more_list:true,groups:[]}}");}};
  assertFalse(c.searchPage("타요","all",1,"score").hasMore);assertEquals(new HashSet<>(Arrays.asList("bjSearch","liveSearch","vodSearch")),methods);
 }
 @Test public void legacyStationLogoIsRecognized()throws Exception{assertEquals("https://profile.img.sooplive.com/demo.jpg",ContentItem.parse(new JSONObject("{DATA:[{user_id:'demo',station_logo:'https://profile.img.sooplive.com/demo.jpg'}]}"),false).get(0).thumb);}
 @Test public void officialHttpThumbnailIsUpgradedToHttpsWithoutAllowingCleartext(){assertEquals("https://iflv14.sooplive.com/image.jpg",ServiceUrls.image("http://iflv14.sooplive.com/image.jpg"));assertEquals("",ServiceUrls.image("http://unknown.example/image.jpg"));}
 @Test(expected=java.io.IOException.class) public void rejectedSearchIsNotShownAsAnEmptySuccess()throws Exception{SearchPage.parse(new JSONObject("{result:-1,data:{groups:[]}}"),"streamer");}
}
