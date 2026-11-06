package dev.soopviewer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import android.content.SharedPreferences;
import android.webkit.CookieManager;
import android.os.Looper;
import okhttp3.*;
import java.util.*;
import javax.crypto.*;
import javax.crypto.spec.*;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class) @Config(sdk=28)
public class SessionCookiesTest {
 private SharedPreferences prefs;private SessionCookies.Vault vault;
 @Before public void setup(){prefs=RuntimeEnvironment.getApplication().getSharedPreferences("session-test",0);prefs.edit().clear().commit();CookieManager.getInstance().removeAllCookies(null);vault=new SessionCookies.Vault(){final javax.crypto.SecretKey key=new SecretKeySpec(new byte[16],"AES");public String seal(String plain)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key);return Base64.getEncoder().encodeToString(cipher.getIV())+":"+Base64.getEncoder().encodeToString(cipher.doFinal(plain.getBytes("UTF-8")));}public String open(String encoded)throws Exception{String[] parts=encoded.split(":");Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key,new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),"UTF-8");}};}
 @After public void cleanup(){CookieManager.getInstance().removeAllCookies(null);prefs.edit().clear().commit();}
 @Test public void sessionSurvivesCookieManagerRestartAndIsNeverStoredAsPlaintext()throws Exception{SessionCookies first=new SessionCookies(prefs,vault);HttpUrl url=HttpUrl.get("https://login.sooplive.com/app/LoginAction.php");Cookie cookie=Cookie.parse(url,"session=fixture-secret-token; Domain=sooplive.com; Path=/; Secure; HttpOnly");first.receive(url,Collections.singletonList(cookie),first.epoch());first.accepted();String stored=prefs.getString("encrypted","");assertFalse(stored.isEmpty());assertFalse(stored.contains("fixture-secret-token"));CookieManager.getInstance().removeAllCookies(null);SessionCookies restored=new SessionCookies(prefs,vault);Shadows.shadowOf(Looper.getMainLooper()).idle();restored.awaitReady();// RoboCookieManager ignores Domain attributes. Verify restored origin plus
 // the persisted RFC cookie scope independently; real WebView honors Domain.
 assertTrue(CookieManager.getInstance().getCookie("https://login.sooplive.com/").contains("fixture-secret-token"));org.json.JSONObject nativeSaved=new org.json.JSONObject(vault.open(stored)).getJSONObject("native");Cookie savedCookie=Cookie.parse(url,nativeSaved.getString(nativeSaved.keys().next()));assertTrue(savedCookie.matches(HttpUrl.get("https://api.m.sooplive.com/")));assertFalse(savedCookie.matches(HttpUrl.get("https://unrelated.example/")));assertNull(CookieManager.getInstance().getCookie("https://unrelated.example/"));}
 @Test public void logoutDeletesSavedSessionAndRejectsInflightResponses(){SessionCookies session=new SessionCookies(prefs,vault);session.accepted();int before=session.epoch();session.clear();HttpUrl url=HttpUrl.get("https://login.sooplive.com/");session.receive(url,Collections.singletonList(Cookie.parse(url,"session=late; Path=/; Secure")),before);assertFalse(prefs.contains("encrypted"));String raw=CookieManager.getInstance().getCookie(url.toString());assertTrue(raw==null||!raw.contains("late"));}
 @Test public void revokedCookieIsNotResurrectedBySavedSnapshot()throws Exception{SessionCookies session=new SessionCookies(prefs,vault);HttpUrl url=HttpUrl.get("https://login.sooplive.com/");session.receive(url,Collections.singletonList(Cookie.parse(url,"session=valid; Domain=sooplive.com; Path=/; Secure")),session.epoch());session.accepted();session.receive(url,Collections.singletonList(Cookie.parse(url,"session=; Domain=sooplive.com; Path=/; Max-Age=0; Secure")),session.epoch());CookieManager.getInstance().removeAllCookies(null);new SessionCookies(prefs,vault);Shadows.shadowOf(Looper.getMainLooper()).idle();String raw=CookieManager.getInstance().getCookie("https://m.sooplive.com/");assertTrue(raw==null||!raw.contains("valid"));}
 @Test public void unreadableEncryptedSessionRequiresFreshLoginWithoutCrashing(){prefs.edit().putString("encrypted","corrupt").commit();new SessionCookies(prefs,vault);assertFalse(prefs.contains("encrypted"));}
 @Test public void switchingAccountInWebViewNeverRestoresThePreviousAccount()throws Exception{SessionCookies session=new SessionCookies(prefs,vault);HttpUrl url=HttpUrl.get("https://login.sooplive.com/");session.receive(url,Collections.singletonList(Cookie.parse(url,"session=old-account; Path=/; Secure")),session.epoch());session.accepted();CookieManager.getInstance().setCookie(url.toString(),"session=new-account; Path=/; Secure; HttpOnly");session.checkpoint();CookieManager.getInstance().removeAllCookies(null);new SessionCookies(prefs,vault);Shadows.shadowOf(Looper.getMainLooper()).idle();String raw=CookieManager.getInstance().getCookie(url.toString());assertNotNull(raw);assertTrue(raw.contains("new-account"));assertFalse(raw.contains("old-account"));}

}
