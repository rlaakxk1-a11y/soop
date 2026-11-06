package dev.soopviewer;

import okhttp3.HttpUrl;

/** SOOP's provided snapshot sheets: 10 by 10 frames, one frame every three seconds. */
public final class VodPreview {
 public static final int GRID=10;public static final long STEP_MS=3000;
 public static final class Track {
  public final String url;public final long durationMs;
  public Track(String url,long durationMs){this.url=ServiceUrls.image(url);this.durationMs=Math.max(0,durationMs);}
 }
 public static final class Frame {
  public final String url;public final int cell;public final long sampleMs;
  Frame(String url,int cell,long sampleMs){this.url=url;this.cell=cell;this.sampleMs=sampleMs;}
  public int column(){return cell%GRID;}public int row(){return cell/GRID;}
  public String key(){return url+"#"+cell;}
 }
 public static Frame frame(Track track,long positionMs){
  if(track==null||track.url.isEmpty()||track.durationMs<=0)return null;
  long last=Math.max(0,track.durationMs/STEP_MS-1),index=Math.min(Math.max(0,positionMs)/STEP_MS,last);
  String url=HttpUrl.get(track.url).newBuilder().setQueryParameter("column",Long.toString(index/(GRID*GRID)+1)).build().toString();
  return new Frame(url,(int)(index%(GRID*GRID)),index*STEP_MS);
 }
}
