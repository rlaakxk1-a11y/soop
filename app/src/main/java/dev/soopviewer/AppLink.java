package dev.soopviewer;

import java.net.URI;
import java.net.URLDecoder;
import java.util.*;

/** Only known SOOP page routes become native navigation; authentication pages stay authentication. */
public final class AppLink {
 public enum Kind { NONE, HOME, EXPLORE, SEARCH, STATION, LIVE, VOD, AUTH }
 public final Kind kind;public final String id,detail;
 private AppLink(Kind kind,String id,String detail){this.kind=kind;this.id=id;this.detail=detail;}
 private static AppLink link(Kind kind,String id,String detail){return new AppLink(kind,id,detail);}
 private static String query(String raw,String key){if(raw==null)return "";for(String field:raw.split("&")){String[] pair=field.split("=",2);try{if(URLDecoder.decode(pair[0],"UTF-8").equals(key))return pair.length>1?URLDecoder.decode(pair[1],"UTF-8"):"";}catch(Exception ignored){}}return "";}
 private static boolean host(String host,String name){return host.equals(name+".sooplive.com")||host.equals(name+".sooplive.co.kr");}
 public static boolean authHost(String value){try{URI u=URI.create(value);String h=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||h==null||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443))return false;h=h.toLowerCase(Locale.ROOT);return ServiceUrls.official(h)||Arrays.asList("accounts.kakao.com","kauth.kakao.com","nid.naver.com","appleid.apple.com","accounts.google.com","nice.checkplus.co.kr","niceid.co.kr","cert.vno.co.kr","ipin.ok-name.co.kr").contains(h);}catch(Exception e){return false;}}
 public static AppLink parse(String value){
  AppLink none=link(Kind.NONE,"","");
  try{
   URI u=URI.create(value);String h=u.getHost();if(!"https".equalsIgnoreCase(u.getScheme())||h==null||u.getUserInfo()!=null||(u.getPort()!=-1&&u.getPort()!=443)||!ServiceUrls.official(h))return none;h=h.toLowerCase(Locale.ROOT);
   if(host(h,"login")||host(h,"member")||host(h,"member.m"))return link(Kind.AUTH,"","");
   String path=u.getPath()==null?"":u.getPath(),rawQuery=u.getRawQuery();
   if(host(h,"m")&&u.getRawFragment()!=null&&u.getRawFragment().startsWith("/")){URI fragment=URI.create(u.getRawFragment());path=fragment.getPath();rawQuery=fragment.getRawQuery();}
   String[] parts=Arrays.stream(path.split("/")).filter(p->!p.isEmpty()).toArray(String[]::new);
   if(host(h,"play")&&parts.length>=1&&ServiceUrls.userId(parts[0]))return link(Kind.LIVE,parts[0],parts.length>1&&parts[1].matches("[0-9]+")?parts[1]:parts[0]);
   if(host(h,"vod")){
    if(parts.length>=2&&parts[0].equalsIgnoreCase("player")&&parts[1].matches("[0-9]+"))return link(Kind.VOD,parts[1],"");
    if(parts.length>=3&&parts[0].equalsIgnoreCase("player")&&parts[1].equalsIgnoreCase("station")&&parts[2].matches("[0-9]+"))return link(Kind.VOD,parts[2],"");
   }
   if(!(host(h,"www")||host(h,"m")||h.equals("sooplive.com")||h.equals("sooplive.co.kr")))return none;
   if(parts.length==0||parts[0].equalsIgnoreCase("home"))return link(Kind.HOME,"","");
   if(parts[0].equalsIgnoreCase("search")){String keyword=query(rawQuery,"keyword");if(keyword.isEmpty())keyword=query(rawQuery,"szKeyword");return link(Kind.SEARCH,keyword,"");}
   if(parts[0].equalsIgnoreCase("explore")||parts[0].equalsIgnoreCase("category"))return link(Kind.EXPLORE,"","");
   if(parts[0].equalsIgnoreCase("player")&&parts.length>=2&&ServiceUrls.userId(parts[1]))return link(Kind.LIVE,parts[1],parts.length>2&&parts[2].matches("[0-9]+")?parts[2]:parts[1]);
   if(parts[0].equalsIgnoreCase("station")&&parts.length>=2&&ServiceUrls.userId(parts[1])){
    String section="home";if(parts.length>=3&&parts[2].equalsIgnoreCase("vod"))section=parts.length>=4&&Arrays.asList("all","review","clip","normal").contains(parts[3])?parts[3]:"all";
    if(parts.length>=3&&(parts[2].equalsIgnoreCase("post")||parts[2].equalsIgnoreCase("board")))section="posts";return link(Kind.STATION,parts[1],section);
   }
   if(parts.length==1&&ServiceUrls.userId(parts[0])&&!Arrays.asList("api","app","catch","esports","policy","download","service","login","logout","notice","help","event","my","join","guide").contains(parts[0].toLowerCase(Locale.ROOT)))return link(Kind.STATION,parts[0],"home");
   return none;
  }catch(Exception e){return none;}
 }
}
