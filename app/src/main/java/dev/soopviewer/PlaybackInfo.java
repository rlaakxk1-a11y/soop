package dev.soopviewer;
import org.json.*;
import java.util.*;
public final class PlaybackInfo {
 public final List<String> media=new ArrayList<>(),chat=new ArrayList<>();
 public final List<Long> durations=new ArrayList<>();public JSONObject live;public final List<LiveQuality> qualities=new ArrayList<>();public String selectedQuality="master";
 public String title="",user="",thumbnail="";public int chatSeconds=300;
 public static PlaybackInfo vod(JSONObject response)throws Exception{
  JSONObject d=response.optJSONObject("data");if(d==null)throw new Exception("다시보기 정보를 가져오지 못했습니다");
  if(d.optInt("code",0)<0)throw new Exception("계정 권한 또는 VOD 공개 상태를 확인하세요 ("+d.optInt("code")+")");
  if("notLogin".equals(d.optString("adult_status")))throw new Exception("로그인 및 공식 성인 인증이 필요합니다");
  // Additional subscriber authorization is left to the official player.
  if(!d.optString("sub_upload_type").isEmpty())throw new Exception("구독자 VOD는 공식 재생 화면에서 확인하세요");
  PlaybackInfo p=new PlaybackInfo();p.title=d.optString("title");p.user=d.optString("bj_id");p.thumbnail=ServiceUrls.https(d.optString("thumb"));p.chatSeconds=Math.max(1,d.optInt("chat_duration",300));
  JSONArray files=d.optJSONArray("files");if(files!=null)for(int i=0;i<files.length();i++){
   JSONObject f=files.optJSONObject(i);if(f==null)continue;String url=ServiceUrls.https(f.optString("file"));if(url.isEmpty())continue;
   p.media.add(url);p.chat.add(ServiceUrls.https(f.optString("chat")));p.durations.add(Math.max(0,f.optLong("duration")));
  }
  if(p.media.isEmpty())throw new Exception("재생 가능한 영상이 없습니다. 로그인 또는 공개 상태를 확인하세요");return p;
 }
}
