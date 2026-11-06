package dev.soopviewer;

import java.io.IOException;
import java.util.*;
import org.json.*;

/** Official mobile search groups, including paging information and distinct streamer rows. */
public final class SearchPage {
 public final List<ContentItem> items;
 public final boolean hasMore;
 public SearchPage(List<ContentItem> items,boolean hasMore){this.items=items;this.hasMore=hasMore;}
 public static SearchPage parse(JSONObject response,String mode)throws Exception{
  JSONObject data=response.optJSONObject("data");
  if(response.optInt("result")!=1||data==null)throw new IOException("검색 결과를 확인할 수 없습니다");
  JSONArray groups=data.optJSONArray("groups");if(groups==null)throw new IOException("검색 결과 형식이 변경되었습니다");
  List<ContentItem> out=new ArrayList<>();
  for(int i=0;i<groups.length();i++){
   JSONObject group=groups.optJSONObject(i);if(group==null)continue;
   String type=group.optString("content_type");boolean streamer=mode.equals("streamer");
   if(!type.isEmpty()&&!type.equals(streamer?"bj":mode))continue;
   JSONArray contents=group.optJSONArray("contents");if(contents==null)continue;
   List<ContentItem> parsed=ContentItem.parse(new JSONObject().put("DATA",contents),mode.equals("vod"));
   for(ContentItem item:parsed){
    item.streamer=streamer;item.section=streamer?"스트리머":mode.equals("live")?"LIVE":"VOD";
    // Mobile search durations are milliseconds; station and legacy list contracts differ.
    if(item.vod)for(int j=0;j<contents.length();j++){JSONObject raw=contents.optJSONObject(j);if(raw!=null&&item.id.equals(raw.optString("title_no"))){String duration=raw.optString("duration");if(duration.matches("[0-9]+"))item.duration=ContentItem.duration(new JSONObject().put("total_file_duration",duration));break;}}
    out.add(item);
   }
  }
  return new SearchPage(out,data.optBoolean("has_more_list",false));
 }
}
