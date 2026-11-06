package dev.soopviewer;
import java.util.*;

/** Translate one complete VOD clock to the service's per-file chat and history positions. */
public final class VodTimeline {
 public static final class Position {public final int part;public final long positionMs;public Position(int part,long positionMs){this.part=part;this.positionMs=Math.max(0,positionMs);}}
 public static boolean known(List<Long> durations,int count){if(count==0||durations.size()!=count)return false;for(long duration:durations)if(duration<=0)return false;return true;}
 public static long elapsed(List<Long> durations,int part,long position){long result=Math.max(0,position);for(int i=0;i<Math.min(Math.max(0,part),durations.size());i++)result+=Math.max(0,durations.get(i));return result;}
 public static Position locate(List<Long> durations,long elapsed){long left=Math.max(0,elapsed);for(int i=0;i<durations.size();i++){long duration=Math.max(0,durations.get(i));if(left<duration||i==durations.size()-1)return new Position(i,duration>0?Math.min(left,duration):left);left-=duration;}return new Position(0,left);}
}
