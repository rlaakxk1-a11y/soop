package dev.soopviewer;
import org.json.*;
import java.util.*;
public final class ContentItem {
 public final String id,user,title,nick,thumb,viewers; public final boolean vod;
 public ContentItem(String id,String user,String title,String nick,String thumb,String viewers,boolean vod){this.id=id;this.user=user;this.title=title;this.nick=nick;this.thumb=thumb;this.viewers=viewers;this.vod=vod;}
 static String value(JSONObject o,String...names){for(String n:names){Object x=o.opt(n);if(x!=null&&x!=JSONObject.NULL&&!x.toString().isEmpty())return x.toString();}return "";}
 public static List<ContentItem> parse(JSONObject root,boolean vod){return parse(root,vod,"");}
 public static List<ContentItem> parse(JSONObject root,boolean vod,String fallbackUser){
  JSONArray rows=root.optJSONArray("broad");if(rows==null)rows=root.optJSONArray("REAL_BROAD");if(rows==null)rows=root.optJSONArray("DATA");if(rows==null)rows=root.optJSONArray("data");
  if(rows==null&&root.optJSONObject("data")!=null)rows=root.optJSONObject("data").optJSONArray("list");
  if(rows==null)throw new IllegalArgumentException("목록 응답 형식이 변경되었습니다");
  List<ContentItem> items=new ArrayList<>();
  for(int i=0;i<rows.length();i++){
   JSONObject o=rows.optJSONObject(i);if(o==null)continue;
   String user=value(o,"user_id","bj_id","writer_id");if(user.isEmpty())user=fallbackUser;String id=value(o,"title_no","nTitleNo","vod_id");
   if(!ServiceUrls.userId(user)||vod&&!id.matches("[0-9]+"))continue;
   String nick=value(o,"user_nick","writer_nick","nick_name"),title=value(o,"broad_title","title_name","title");if(title.isEmpty())title=nick;
   items.add(new ContentItem(vod?id:user,user,title,nick,ServiceUrls.https(value(o,"broad_thumb","thumbnail","thumb","broad_img","thumb_url")),value(o,"total_view_cnt","view_cnt","current_view_cnt","view_count"),vod));
  }return items;
 }
 public String pageUrl(){return vod?"https://vod.sooplive.com/player/"+id:"https://play.sooplive.com/"+user;}
}
