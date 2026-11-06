package dev.soopviewer;
import org.json.*;
import java.util.*;
public final class ContentItem {
 public String duration="";public final String id,user,title,nick,thumb,viewers; public final boolean vod;
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
   String user=value(o,"user_id","bj_id","writer_id");if(user.isEmpty())user=fallbackUser;String id=value(o,"title_no","nTitleNo","vod_id");String broadcast=value(o,"broad_no","broadNo","bno","BNO");
   if(!ServiceUrls.userId(user)||vod&&!id.matches("[0-9]+"))continue;
   String nick=value(o,"user_nick","writer_nick","nick_name"),title=value(o,"broad_title","title_name","title");if(title.isEmpty())title=nick;
   // SOOP's station API has returned several image key names over time. Some
   // list responses omit the thumbnail entirely; the UI then resolves it from
   // the canonical VOD detail response.
   String thumbnail=value(o,"broad_thumb","thumbnail","thumb","broad_img","thumb_url","title_thumb","title_thumbnail","title_image","title_img","list_img","img","bj_img","profile_image","profile_img","user_profile");
   JSONObject ucc=o.optJSONObject("ucc");if(vod&&ucc!=null){String videoThumb=value(ucc,"thumb","thumbnail");if(!videoThumb.isEmpty())thumbnail=videoThumb;}JSONObject nested=o.optJSONObject("thumbnail");if(thumbnail.isEmpty()&&nested!=null)thumbnail=value(nested,"url","src");
   items.add(new ContentItem(vod?id:(broadcast.isEmpty()?user:broadcast),user,title,nick,ServiceUrls.https(thumbnail),value(o,"total_view_cnt","view_cnt","current_view_cnt","view_count"),vod));
   items.get(items.size()-1).duration=duration(o);if(items.get(items.size()-1).duration.isEmpty()&&ucc!=null)items.get(items.size()-1).duration=duration(ucc);
  }return items;
 }
 public static String duration(JSONObject o){String raw=value(o,"duration","play_time","video_duration","total_file_duration","file_duration");if(raw.matches("[0-9]+:[0-9:]+"))return raw;try{long n=Long.parseLong(raw);if(o.has("total_file_duration"))n/=1000;if(n<=0)return "";return n>=3600?String.format(java.util.Locale.ROOT,"%d:%02d:%02d",n/3600,n/60%60,n%60):String.format(java.util.Locale.ROOT,"%d:%02d",n/60,n%60);}catch(Exception e){return "";}}
 public String pageUrl(){return vod?"https://vod.sooplive.com/player/"+id:"https://play.sooplive.com/"+user;}
}
