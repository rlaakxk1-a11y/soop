package dev.soopviewer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import android.webkit.CookieManager;
import okhttp3.Cookie;
import okhttp3.HttpUrl;
import org.json.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;

/** App-private encrypted session continuity. Passwords are never included. */
final class SessionCookies {
 interface Vault {String seal(String value)throws Exception;String open(String value)throws Exception;}
 static final String[] ORIGINS={"https://sooplive.com/","https://www.sooplive.com/","https://m.sooplive.com/","https://login.sooplive.com/afreeca/login.php","https://member.sooplive.com/","https://member.m.sooplive.com/","https://auth.m.sooplive.com/","https://api.m.sooplive.com/broad/a/watch","https://afevent2.sooplive.com/api/get_private_info.php","https://live.sooplive.com/afreeca/player_live_api.php","https://play.sooplive.com/","https://vod.sooplive.com/","https://chapi.sooplive.com/","https://api-channel.sooplive.com/"};
 private final SharedPreferences prefs;private final Vault vault;private final Handler main=new Handler(Looper.getMainLooper());
 private final CountDownLatch ready=new CountDownLatch(1);private volatile int epoch=0;private volatile boolean enabled;private final Map<String,String> nativeCookies=new LinkedHashMap<>();
 SessionCookies(Context context){this(context.getSharedPreferences("session",Context.MODE_PRIVATE),new AndroidVault());}
 SessionCookies(SharedPreferences prefs,Vault vault){this.prefs=prefs;this.vault=vault;restore();}
 int epoch(){return epoch;}
 private void restore(){
  try{String encoded=prefs.getString("encrypted","");if(encoded.isEmpty()){ready.countDown();return;}JSONObject saved=new JSONObject(vault.open(encoded));enabled=true;
   JSONObject nativeSaved=saved.optJSONObject("native");if(nativeSaved!=null)for(Iterator<String> it=nativeSaved.keys();it.hasNext();){String key=it.next();nativeCookies.put(key,nativeSaved.getString(key));}
   List<String[]> pending=new ArrayList<>();JSONObject snapshots=saved.optJSONObject("web");
   if(snapshots!=null)for(String url:ORIGINS){String raw=snapshots.optString(url);for(String part:raw.split(";")){Cookie cookie=Cookie.parse(HttpUrl.get(url),part.trim()+"; Path=/; Secure; HttpOnly");if(cookie!=null)pending.add(new String[]{url,cookie.toString()});}}
   for(Map.Entry<String,String> entry:nativeCookies.entrySet()){String url=entry.getKey().split("\\n",2)[0];Cookie cookie=Cookie.parse(HttpUrl.get(url),entry.getValue());if(cookie!=null)pending.add(new String[]{url,cookie.toString()});}
   if(pending.isEmpty()){ready.countDown();return;}java.util.concurrent.atomic.AtomicInteger left=new java.util.concurrent.atomic.AtomicInteger(pending.size());for(String[] entry:pending)CookieManager.getInstance().setCookie(entry[0],entry[1],ok->{if(left.decrementAndGet()==0){CookieManager.getInstance().flush();ready.countDown();}});
  }catch(Exception ignored){prefs.edit().clear().commit();nativeCookies.clear();enabled=false;ready.countDown();}
 }
 void awaitReady(){if(Looper.myLooper()==Looper.getMainLooper())return;try{if(!ready.await(10,TimeUnit.SECONDS))throw new IllegalStateException("로그인 복원 중입니다. 다시 시도하세요");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("로그인 복원이 취소되었습니다",e);}}
 void accepted(){enabled=true;checkpoint();}
 // Reconcile WebView changes (including account switching and server deletion)
 // before writing native cookie metadata captured by OkHttp.
 private void reconcileNative(){synchronized(nativeCookies){Iterator<Map.Entry<String,String>> entries=nativeCookies.entrySet().iterator();while(entries.hasNext()){Map.Entry<String,String> entry=entries.next();String url=entry.getKey().split("\\n",2)[0];Cookie cookie=Cookie.parse(HttpUrl.get(url),entry.getValue());if(cookie==null){entries.remove();continue;}if(cookie.expiresAt()<=System.currentTimeMillis())continue;String raw=CookieManager.getInstance().getCookie(url);boolean same=false,changed=false;if(raw!=null)for(String field:raw.split(";")){int n=field.indexOf('=');if(n>0&&field.substring(0,n).trim().equals(cookie.name())){changed=true;if(field.substring(n+1).trim().equals(cookie.value()))same=true;}}if(same)continue;if(changed)entries.remove();else entry.setValue(cookie.toString()+"; Max-Age=0");}}}
 void checkpoint(){if(Looper.myLooper()!=Looper.getMainLooper()){main.post(this::checkpoint);return;}if(!enabled)return;try{reconcileNative();JSONObject web=new JSONObject();for(String url:ORIGINS){String raw=CookieManager.getInstance().getCookie(url);if(raw==null)continue;StringBuilder remaining=new StringBuilder();for(String part:raw.split(";")){int split=part.indexOf('=');if(split<1)continue;String name=part.substring(0,split).trim();boolean known=false;synchronized(nativeCookies){for(Map.Entry<String,String> record:nativeCookies.entrySet()){Cookie cookie=Cookie.parse(HttpUrl.get(record.getKey().split("\\n",2)[0]),record.getValue());if(cookie!=null&&cookie.name().equals(name)&&cookie.matches(HttpUrl.get(url))){known=true;break;}}}if(!known){if(remaining.length()>0)remaining.append("; ");remaining.append(part.trim());}}if(remaining.length()>0)web.put(url,remaining.toString());}JSONObject nativeSaved=new JSONObject();synchronized(nativeCookies){for(Map.Entry<String,String> e:nativeCookies.entrySet())nativeSaved.put(e.getKey(),e.getValue());}String encrypted=vault.seal(new JSONObject().put("web",web).put("native",nativeSaved).toString());prefs.edit().putString("encrypted",encrypted).commit();CookieManager.getInstance().flush();}catch(Exception ignored){/* Never fall back to plaintext. The server session remains usable in memory. */}}
 void receive(HttpUrl url,List<Cookie> cookies,int requestEpoch){
  if(requestEpoch!=epoch||!ServiceUrls.official(url.host())||cookies.isEmpty())return;
  CountDownLatch applied=new CountDownLatch(1);java.util.concurrent.atomic.AtomicBoolean accepted=new java.util.concurrent.atomic.AtomicBoolean(true);
  Runnable apply=()->{
   if(requestEpoch!=epoch){applied.countDown();return;}
   java.util.concurrent.atomic.AtomicInteger left=new java.util.concurrent.atomic.AtomicInteger(cookies.size());
   for(Cookie cookie:cookies){
    String key=url.scheme()+"://"+url.host()+cookie.path()+"\n"+cookie.name()+"\n"+cookie.domain();
    synchronized(nativeCookies){nativeCookies.put(key,cookie.toString());}
    CookieManager.getInstance().setCookie(url.toString(),cookie.toString(),ok->{
     if(!Boolean.TRUE.equals(ok))accepted.set(false);
     if(left.decrementAndGet()==0){if(requestEpoch==epoch){CookieManager.getInstance().flush();checkpoint();}applied.countDown();}
    });
   }
  };
  if(Looper.myLooper()==Looper.getMainLooper())apply.run();else{main.post(apply);try{if(!applied.await(10,TimeUnit.SECONDS)||!accepted.get())throw new IllegalStateException("로그인 쿠키 저장에 실패했습니다");}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException(e);}}
 }
 void clear(){epoch++;enabled=false;synchronized(nativeCookies){nativeCookies.clear();}prefs.edit().clear().commit();}
 private static final class AndroidVault implements Vault{
  private javax.crypto.SecretKey key()throws Exception{KeyStore store=KeyStore.getInstance("AndroidKeyStore");store.load(null);String alias="soop.session.aes.v1";if(store.containsAlias(alias))return (javax.crypto.SecretKey)store.getKey(alias,null);KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");generator.init(new KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());return generator.generateKey();}
  public String seal(String value)throws Exception{Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.ENCRYPT_MODE,key());return Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)+":"+Base64.encodeToString(cipher.doFinal(value.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);}
  public String open(String value)throws Exception{String[] parts=value.split(":",2);Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),StandardCharsets.UTF_8);}
 }
}
