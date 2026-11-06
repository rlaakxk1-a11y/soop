package dev.soopviewer;
/** Conservative host rules; never block first-party media URLs by substring. */
public final class AdRules {
 public static boolean blocked(String host) {
  if(host==null)return false;
  host=host.toLowerCase(java.util.Locale.ROOT);
  for(String domain:new String[]{"doubleclick.net","googlesyndication.com","googleadservices.com"})
   if(host.equals(domain)||host.endsWith("."+domain))return true;
  return false;
 }
}
