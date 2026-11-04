package dev.soopviewer;
public final class PanelState {
 public static int clamp(int percent) { return Math.max(15, Math.min(65, percent)); }
 public static boolean trusted(String host) {
  return host != null && (host.equals("sooplive.co.kr") || host.endsWith(".sooplive.co.kr"));
 }
}
