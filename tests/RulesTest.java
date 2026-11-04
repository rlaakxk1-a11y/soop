import dev.soopviewer.*;
public final class RulesTest {
 static void check(boolean x){if(!x)throw new AssertionError();}
 public static void main(String[] args){
  check(PanelState.clamp(-1)==15); check(PanelState.clamp(99)==65);
  check(PanelState.trusted("vod.sooplive.co.kr"));check(!PanelState.trusted("sooplive.co.kr.evil.example"));check(!PanelState.trusted(null));
  check(AdRules.blocked("ad.doubleclick.net"));check(!AdRules.blocked("doubleclick.net.evil.example"));check(!AdRules.blocked("vod.sooplive.co.kr"));check(!AdRules.blocked(null));
  System.out.println("9 Java rule assertions passed");
 }
}
