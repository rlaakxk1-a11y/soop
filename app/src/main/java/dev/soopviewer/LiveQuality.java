package dev.soopviewer;
import org.json.*;
import java.util.*;
public final class LiveQuality {
 public final String id,label;public final int height;
 public LiveQuality(String id,String label,int height){this.id=id;this.label=label;this.height=height;}
 public static List<LiveQuality> offered(JSONObject channel){
  List<LiveQuality> out=new ArrayList<>();JSONArray presets=channel.optJSONArray("VIEWPRESET");if(presets==null)presets=channel.optJSONArray("viewpreset");Set<String> seen=new HashSet<>();
  if(presets!=null)for(int i=0;i<presets.length();i++){JSONObject p=presets.optJSONObject(i);if(p==null)continue;String id=p.optString("name");if(!id.matches("[A-Za-z0-9_\\-]+")||id.equals("auto")||id.equals("master")||!seen.add(id))continue;
   String low=id.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]",""),label=p.optString("label",id);int height=p.optInt("height",0);
   // SOOP's live API also supplies label_resolution (for example 1920x1080).
   // Use it when the user-facing label is just "원본" or a preset name.
   if(height==0){String resolution=p.optString("label_resolution");java.util.regex.Matcher m=java.util.regex.Pattern.compile("(?i)(?:[0-9]{3,4})[x×]([0-9]{3,4})").matcher(resolution);if(m.find())try{height=Integer.parseInt(m.group(1));}catch(NumberFormatException ignored){}}
   for(int h:new int[]{1080,720,540,480,360,240})if(label.contains(""+h))height=h;
   if(height==0){if(low.matches(".*(?:fullhd|fhd|1080p?).*"))height=1080;else if(low.matches(".*(?:hd|720p?).*"))height=720;else if(low.matches(".*(?:sd|540p?).*"))height=540;else if(low.matches(".*(?:low|360p?).*"))height=360;else if(low.equals("original")||low.equals("source")){String r=channel.optString("RESOLUTION");if(r.matches("[0-9]+[xX×][0-9]+"))try{height=Integer.parseInt(r.split("[xX×]")[1]);}catch(NumberFormatException ignored){}}}
   out.add(new LiveQuality(id,height>0?height+"p":(low.equals("original")?"원본":label),height));
  }
  out.sort((a,b)->Integer.compare(rank(b),rank(a)));return out;
 }
 private static int rank(LiveQuality q){if(q.height>0&&q.height<=1080)return q.height+10000;if(q.id.equals("original")||q.id.equals("source"))return 11050;return q.height;}
 public static String preferred(List<LiveQuality> list){return list.isEmpty()?"master":list.get(0).id;}
}
