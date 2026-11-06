package dev.soopviewer;

import org.json.*;
import java.util.*;

/** Local VOD metadata and playback positions only; never stores media URLs or credentials. */
public final class WatchHistory {
 public static final int LIMIT=100;
 private final LinkedHashMap<String,Entry> entries=new LinkedHashMap<>();
 public static final class Entry {
  public final ContentItem item;
  public final int part;
  public final long positionMs,totalMs,elapsedMs,updatedAt;
  public final boolean completed;
  public Entry(ContentItem item,int part,long positionMs,long totalMs,long elapsedMs,long updatedAt,boolean completed){
   this.item=item;this.part=Math.max(0,part);this.positionMs=Math.max(0,positionMs);this.totalMs=Math.max(0,totalMs);this.elapsedMs=Math.max(0,elapsedMs);this.updatedAt=updatedAt;this.completed=completed;
  }
  public int progress(){return totalMs<=0?0:(int)Math.min(100,100d*elapsedMs/totalMs);}
  public int resumePart(int count){return completed||part>=count?0:part;}
  public long resumePosition(List<Long> durations){
   if(completed||part>=durations.size())return 0;
   long duration=durations.get(part);return duration>0?Math.min(positionMs,Math.max(0,duration-1000)):positionMs;
  }
  public String label(){return completed?"시청 완료 · 다시 보기":time(totalMs>0?elapsedMs:positionMs)+"부터 이어보기";}
  public String timeLabel(){return totalMs>0?time(elapsedMs)+" / "+time(totalMs):time(positionMs);}
  private JSONObject json()throws JSONException {
   JSONObject o=new JSONObject();o.put("id",item.id);o.put("user",item.user);o.put("title",item.title);o.put("nick",item.nick);o.put("thumb",item.thumb);o.put("part",part);o.put("position",positionMs);o.put("total",totalMs);o.put("elapsed",elapsedMs);o.put("updated",updatedAt);o.put("completed",completed);return o;
  }
 }
 public static String time(long ms){long sec=Math.max(0,ms)/1000;return sec>=3600?String.format(Locale.ROOT,"%d:%02d:%02d",sec/3600,sec/60%60,sec%60):String.format(Locale.ROOT,"%d:%02d",sec/60,sec%60);}
 public void put(Entry e){if(!e.item.vod||!e.item.id.matches("[0-9]+"))return;entries.remove(e.item.id);entries.put(e.item.id,e);while(entries.size()>LIMIT)entries.remove(entries.keySet().iterator().next());}
 public Entry find(String id){return entries.get(id);}
 public void remove(String id){entries.remove(id);}
 public void clear(){entries.clear();}
 public List<Entry> recent(){List<Entry> list=new ArrayList<>(entries.values());Collections.reverse(list);return list;}
 public String serialize(){try{JSONArray data=new JSONArray();for(Entry e:entries.values())data.put(e.json());return new JSONObject().put("version",1).put("entries",data).toString();}catch(JSONException e){throw new IllegalStateException(e);}}
 public static WatchHistory restore(String json){
  WatchHistory history=new WatchHistory();try{JSONObject root=new JSONObject(json);if(root.optInt("version")!=1)return history;JSONArray data=root.optJSONArray("entries");if(data==null)return history;
   for(int i=0;i<data.length();i++){JSONObject o=data.optJSONObject(i);if(o==null)continue;String id=o.optString("id"),user=o.optString("user");if(!id.matches("[0-9]+")||!user.isEmpty()&&!ServiceUrls.userId(user))continue;
    ContentItem item=new ContentItem(id,user,o.optString("title"),o.optString("nick"),ServiceUrls.https(o.optString("thumb")),"",true);
    history.put(new Entry(item,o.optInt("part"),o.optLong("position"),o.optLong("total"),o.optLong("elapsed"),o.optLong("updated"),o.optBoolean("completed")));
   }
  }catch(JSONException ignored){}return history;
 }
}
