package dev.soopviewer;

import java.io.IOException;
import java.net.URI;
import java.util.regex.*;

/** Select server-issued variants without rebuilding or replacing their signed query strings. */
public final class LiveManifest {
 private LiveManifest(){}
 public static String select(String base,String body,String quality)throws IOException{
  if(!body.trim().startsWith("#EXTM3U"))throw new IOException("생방송 재생 목록을 확인할 수 없습니다");
  if(quality.equals("auto")||quality.equals("master")||!body.contains("#EXT-X-STREAM-INF:"))return base;
  String[] lines=body.split("\\r?\\n");
  Pattern name=Pattern.compile("(?:^|,)NAME=(?:\"([^\"]+)\"|([^,]+))");
  for(int i=0;i<lines.length;i++)if(lines[i].startsWith("#EXT-X-STREAM-INF:")){
   Matcher match=name.matcher(lines[i].substring(lines[i].indexOf(':')+1));
   if(match.find()&&quality.equals(match.group(1)!=null?match.group(1):match.group(2))){
    for(int j=i+1;j<lines.length;j++){String line=lines[j].trim();if(line.isEmpty())continue;if(line.startsWith("#EXT-X-STREAM-INF:"))break;if(line.startsWith("#"))continue;
     String url=ServiceUrls.https(URI.create(base).resolve(line).toString());if(!url.isEmpty())return url;break;
    }
   }
  }
  throw new IOException("서버가 선택한 화질을 제공하지 않습니다");
 }
 /** The web entitlement may advertise only hd (540p), even after original was requested. */
 public static String bestAvailable(String base,String body)throws IOException{
  select(base,body,"auto");if(!body.contains("#EXT-X-STREAM-INF:"))return base;
  String best="",first="";int bestHeight=-1;Pattern resolution=Pattern.compile("(?:^|,)RESOLUTION=[0-9]+[xX]([0-9]+)");String[] lines=body.split("\\r?\\n");
  for(int i=0;i<lines.length;i++)if(lines[i].startsWith("#EXT-X-STREAM-INF:")){
   Matcher match=resolution.matcher(lines[i].substring(lines[i].indexOf(':')+1));int height=match.find()?Integer.parseInt(match.group(1)):0;
   for(int j=i+1;j<lines.length;j++){String line=lines[j].trim();if(line.isEmpty())continue;if(line.startsWith("#EXT-X-STREAM-INF:"))break;if(line.startsWith("#"))continue;
    String uri=ServiceUrls.https(URI.create(base).resolve(line).toString());if(!uri.isEmpty()){if(first.isEmpty())first=uri;if(height<=1080&&height>bestHeight){best=uri;bestHeight=height;}}break;
   }
  }
  if(!best.isEmpty())return best;if(!first.isEmpty())return first;throw new IOException("재생 가능한 영상 목록이 없습니다");
 }

}
