package dev.soopviewer;
import java.util.*;
public final class ChatTimeline {
 public static long chunkStart(long positionMs,int chunkSeconds){long seconds=Math.max(1,chunkSeconds);return Math.max(0,positionMs)/(seconds*1000)*seconds;}
 public static List<ChatMessage> visible(List<ChatMessage> messages,long positionMs){
  List<ChatMessage> out=new ArrayList<>();long end=Math.max(0,positionMs),start=Math.max(0,end-60000);
  for(ChatMessage m:messages)if(m.timeMs>=start&&m.timeMs<=end)out.add(m);
  return out.size()<=150?out:new ArrayList<>(out.subList(out.size()-150,out.size()));
 }
}
