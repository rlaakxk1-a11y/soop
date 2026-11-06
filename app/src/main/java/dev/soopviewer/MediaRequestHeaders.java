package dev.soopviewer;
import java.net.URI;
import java.util.*;

/** Keep entitlement requests, playlists and media segments in the same SOOP client profile. */
public final class MediaRequestHeaders {
 public static final String MOBILE_UA="Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36";
 public static final String DESKTOP_UA="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36";
 private static String host(String url){try{return URI.create(url).getHost();}catch(Exception e){return "";}}
 public static Map<String,String> forRequest(String url,String referer){
  String refHost=host(referer),target=host(url);boolean mobile="m.sooplive.com".equals(refHost)||"m.sooplive.co.kr".equals(refHost);
  boolean desktop="play.sooplive.com".equals(refHost)||"play.sooplive.co.kr".equals(refHost)||!mobile&&ServiceUrls.official(target)&&(target.startsWith("live.")||target.startsWith("livestream-manager."));
  Map<String,String> out=new LinkedHashMap<>();out.put("User-Agent",desktop?DESKTOP_UA:MOBILE_UA);if(referer!=null)out.put("Referer",referer);
  if(desktop)out.put("Origin","https://play.sooplive.com");else if(mobile)out.put("Origin","https://m.sooplive.com");else if("vod.sooplive.com".equals(refHost)||"vod.sooplive.co.kr".equals(refHost))out.put("Origin","https://vod.sooplive.com");
  return Collections.unmodifiableMap(out);
 }
}
