package dev.soopviewer;
import org.junit.Test;
import org.json.JSONObject;
import java.util.*;
import static org.junit.Assert.*;
public class BrowseApiTest {
 @Test public void viewerAscendingIsServerWideAndKeepsCategoryAndPage()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("orderType=view_cnt_asc"));assertTrue(url.contains("selectValue=00040019"));assertTrue(url.contains("pageNo=2"));return new JSONObject("{broad:[]}");}}.liveList("00040019","view_cnt_asc",2);}
 @Test public void newestLiveUsesTheActualServerOrder()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("orderType=broad_start"));return new JSONObject("{broad:[]}");}}.liveList("","broad_start",1);}
 @Test public void channelViewsUseReadCountRatherThanIgnoredViewCountParameter()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("/demo/vods/review"));assertTrue(url.contains("orderby=read_cnt"));return new JSONObject("{data:[]}");}}.station("demo","review",1,"read_cnt");}
 @Test public void searchOrderChangesActualQuery()throws Exception{new SoopClient(){@Override public JSONObject json(String url,String form,String ref)throws Exception{assertTrue(url.contains("szOrder=reg_date"));assertTrue(url.contains("m=vodSearch"));return new JSONObject("{DATA:[]}");}}.search("게임",true,1,"reg_date");}
 @Test public void paginationDeduplicatesWithoutReorderingServerResults(){ContentItem a=new ContentItem("1","a","","","","",false),b=new ContentItem("2","b","","","","",false);List<ContentItem> added=BrowseOptions.additions(Collections.singletonList(a),Arrays.asList(a,b,b));assertEquals(1,added.size());assertSame(b,added.get(0));}
 @Test(expected=IllegalArgumentException.class) public void unsupportedOrderCannotLookLikeAWorkingOption(){BrowseOptions.checked(BrowseOptions.LIVE_KEYS,"broad_start_asc");}
}
