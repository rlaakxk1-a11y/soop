package dev.soopviewer;
import java.net.URI;
public final class ServiceUrls {
 public static boolean official(String host) {
  if(host==null)return false;host=host.toLowerCase(java.util.Locale.ROOT);
  return host.equals("sooplive.com")||host.endsWith(".sooplive.com")||host.equals("sooplive.co.kr")||host.endsWith(".sooplive.co.kr");
 }
 public static String https(String value){
  if(value==null)return "";if(value.startsWith("//"))value="https:"+value;
  try {URI u=URI.create(value);return "https".equalsIgnoreCase(u.getScheme())&&u.getHost()!=null&&u.getUserInfo()==null?value:"";}catch(Exception e){return "";}
 }
 public static boolean userId(String id){return id!=null&&id.matches("[A-Za-z0-9_\\-]{1,64}");}
 public static String station(String id){if(!userId(id))throw new IllegalArgumentException("Invalid channel ID");return "https://www.sooplive.com/station/"+id;}
}
