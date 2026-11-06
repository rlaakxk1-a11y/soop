package dev.soopviewer;
import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.source.*;
import java.util.*;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class VodMediaSource {
 public static MediaSource join(List<MediaSource> parts,List<Long> durations){
  if(!VodTimeline.known(durations,parts.size()))throw new IllegalArgumentException("VOD 파일 길이를 확인하세요");
  ConcatenatingMediaSource2.Builder builder=new ConcatenatingMediaSource2.Builder();
  for(int i=0;i<parts.size();i++)builder.add(parts.get(i),durations.get(i));
  return builder.build();
 }
 public static VodTimeline.Position position(PlaybackInfo info,Timeline timeline,int period,int window,long elapsed){
  if(!info.joinedVod)return new VodTimeline.Position(window,elapsed);
  if(timeline!=null&&timeline.getPeriodCount()==info.media.size()&&period>=0&&period<timeline.getPeriodCount()){
   long offset=timeline.getPeriod(period,new Timeline.Period()).getPositionInWindowMs();
   return new VodTimeline.Position(period,elapsed-offset);
  }
  return VodTimeline.locate(info.durations,elapsed);
 }
 public static VodTimeline.Position at(PlaybackInfo info,Timeline timeline,int currentWindow,long position){
  if(info.joinedVod&&timeline!=null&&timeline.getPeriodCount()==info.media.size()){
   for(int i=timeline.getPeriodCount()-1;i>=0;i--){long offset=timeline.getPeriod(i,new Timeline.Period()).getPositionInWindowMs();if(position>=offset)return new VodTimeline.Position(i,position-offset);}
  }
  if(!info.joinedVod&&timeline!=null&&timeline.getWindowCount()>1&&timeline.getWindowCount()<=100){
   List<Long> lengths=new ArrayList<>();for(int i=0;i<timeline.getWindowCount();i++)lengths.add(timeline.getWindow(i,new Timeline.Window()).getDurationMs());
   if(VodTimeline.known(lengths,info.media.size()))return VodTimeline.locate(lengths,position);
  }
  return info.joinedVod?VodTimeline.locate(info.durations,position):new VodTimeline.Position(currentWindow,position);
 }
 public static void updateDurations(PlaybackInfo info,Timeline timeline){
  if(!info.joinedVod||timeline.getWindowCount()!=1||timeline.getPeriodCount()!=info.media.size())return;
  for(int i=0;i<timeline.getPeriodCount();i++){long duration=timeline.getPeriod(i,new Timeline.Period()).getDurationMs();if(duration>0)info.durations.set(i,duration);}
 }
}
