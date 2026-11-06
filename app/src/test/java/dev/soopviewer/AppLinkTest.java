package dev.soopviewer;
import org.junit.Test;
import static org.junit.Assert.*;
public class AppLinkTest {
 @Test public void liveLinksKeepTheBroadcastNumber(){AppLink link=AppLink.parse("https://play.sooplive.com/demo/123");assertEquals(AppLink.Kind.LIVE,link.kind);assertEquals("demo",link.id);assertEquals("123",link.detail);}
 @Test public void mobileHashPlayerLinksOpenNativePlayback(){assertEquals(AppLink.Kind.LIVE,AppLink.parse("https://m.sooplive.co.kr/#/player/demo/123").kind);}
 @Test public void vodLegacyAndCurrentRoutesWork(){assertEquals("123",AppLink.parse("https://vod.sooplive.com/player/123").id);assertEquals("456",AppLink.parse("https://vod.sooplive.co.kr/PLAYER/STATION/456").id);}
 @Test public void channelRoutesSelectTheirNativeTab(){assertEquals("home",AppLink.parse("https://www.sooplive.com/station/demo").detail);assertEquals("review",AppLink.parse("https://www.sooplive.com/station/demo/vod/review").detail);assertEquals("posts",AppLink.parse("https://www.sooplive.com/station/demo/post/123").detail);}
 @Test public void searchDecodesOnlyItsQueryValue(){AppLink link=AppLink.parse("https://m.sooplive.com/#/search?keyword=%EA%B2%8C%EC%9E%84");assertEquals(AppLink.Kind.SEARCH,link.kind);assertEquals("게임",link.id);}
 @Test public void authPagesAreNotMistakenForStations(){assertEquals(AppLink.Kind.AUTH,AppLink.parse("https://login.sooplive.com/afreeca/login.php").kind);assertEquals(AppLink.Kind.AUTH,AppLink.parse("https://member.sooplive.com/app/find_id_pw.php").kind);assertEquals(AppLink.Kind.NONE,AppLink.parse("https://www.sooplive.com/catch").kind);}
 @Test public void callbackHomeIsOnlyARouteNotProofOfLogin(){assertEquals(AppLink.Kind.HOME,AppLink.parse("https://m.sooplive.com/?soop_viewer_auth=complete").kind);}
 @Test public void lookalikeHostsUserInfoPortsAndOtherSchemesAreRejected(){for(String url:new String[]{"https://play.sooplive.com.evil.example/demo","https://evil.example/play.sooplive.com/demo","https://user@play.sooplive.com/demo","https://play.sooplive.com:444/demo","javascript:alert(1)","intent://demo","file:///station/demo"})assertEquals(url,AppLink.Kind.NONE,AppLink.parse(url).kind);}
 @Test public void extraVerificationStaysOnRecognizedHttpsHosts(){assertTrue(AppLink.authHost("https://accounts.kakao.com/login"));assertFalse(AppLink.authHost("https://accounts.kakao.com.evil.example/login"));assertFalse(AppLink.authHost("http://login.sooplive.com/"));}
}
