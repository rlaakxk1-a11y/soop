package dev.soopviewer;
import android.webkit.CookieManager;
import okhttp3.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SoopClient {
 public volatile boolean ads=true;
 public final OkHttpClient http;
 public SoopClient(){
  http=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).followSslRedirects(false)
   .cookieJar(new CookieJar(){
    @Override public List<Cookie> loadForRequest(HttpUrl url){
     String raw=CookieManager.getInstance().getCookie(url.toString());List<Cookie> list=new ArrayList<>();
     if(raw!=null)for(String field:raw.split(";")){int n=field.indexOf('=');if(n>0)try{list.add(new Cookie.Builder().name(field.substring(0,n).trim()).value(field.substring(n+1).trim()).hostOnlyDomain(url.host()).path("/").secure().build());}catch(IllegalArgumentException ignored){}}
     return list;
    }
    @Override public void saveFromResponse(HttpUrl url,List<Cookie> cookies){for(Cookie c:cookies)CookieManager.getInstance().setCookie(url.toString(),c.toString());}
   }).addInterceptor(chain->{Request r=chain.request();if(ads&&AdRules.blocked(r.url().host()))return new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(204).message("Filtered").body(ResponseBody.create(new byte[0],null)).build();return chain.proceed(r);}).build();
 }
 public String text(String url,String form,String referer)throws Exception{
  if(ServiceUrls.https(url).isEmpty())throw new IOException("HTTPS 주소만 사용할 수 있습니다");
  Request.Builder b=new Request.Builder().url(url).header("User-Agent","Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36");
  if(referer!=null)b.header("Referer",referer);
  String host=java.net.URI.create(url).getHost();if(ServiceUrls.official(host)&&(host.startsWith("live.")||host.startsWith("livestream-manager."))){b.header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");b.header("Origin","https://play.sooplive.com");}
  if(form!=null)b.post(RequestBody.create(form,MediaType.get("application/x-www-form-urlencoded; charset=UTF-8")));
  try(Response r=http.newCall(b.build()).execute()){
   if(!r.isSuccessful())throw new IOException("서비스 응답 오류 (HTTP "+r.code()+")");if(r.body()==null)throw new IOException("빈 응답입니다");
   try(InputStream in=r.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("응답 크기가 너무 큽니다");out.write(data,0,n);}return out.toString("UTF-8");}
  }
 }
 public JSONObject json(String url,String form,String ref)throws Exception{return new JSONObject(text(url,form,ref));}
 public static String encode(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
 public List<ContentItem> home(int page)throws Exception{return ContentItem.parse(json("https://live.sooplive.com/api/main_broad_list_api.php?selectType=action&selectValue=all&orderType=view_cnt&pageNo="+page+"&lang=ko_KR",null,"https://www.sooplive.com/"),false);}
 public List<ContentItem> search(String keyword,boolean vod,int page)throws Exception{
  String url="https://sch.sooplive.com/api.php?l=DF&m="+(vod?"vodSearch":"liveSearch")+"&c=UTF-8&w=webk&isMobile=0&onlyParent=1&szType=json&szOrder=score&szKeyword="+encode(keyword)+"&nPageNo="+page+"&nListCnt=30&tab="+(vod?"vod":"live")+"&location=total_search&isHashSearch=0&v="+(vod?"5.0":"2.0");
  return ContentItem.parse(json(url,null,"https://www.sooplive.com/"),vod);
 }
 public List<ContentItem> searchStreamers(String keyword)throws Exception{
  String url="https://sch.sooplive.com/api.php?m=bjSearch&v=3.0&szOrder=score&szKeyword="+encode(keyword);
  return ContentItem.parse(json(url,null,"https://www.sooplive.com/"),false);
 }
 public List<ContentItem> station(String user,String type,int page)throws Exception{
  if(!ServiceUrls.userId(user)||!Arrays.asList("all","review","clip","normal").contains(type))throw new IllegalArgumentException("방송국 ID를 확인하세요");
  return ContentItem.parse(json("https://chapi.sooplive.com/api/"+user+"/vods/"+type+"?page="+page+"&per_page=30&orderby=reg_date",null,ServiceUrls.station(user)),true,user);
 }
 public PlaybackInfo resolve(ContentItem item)throws Exception{
  if(item.vod)return PlaybackInfo.vod(json("https://api.m.sooplive.com/station/video/a/view","nTitleNo="+encode(item.id)+"&nApiLevel=10",item.pageUrl()));
  // Do not pin the channel-info request to a made-up "master" preset. SOOP
  // returns VIEWPRESET here; the chosen preset is sent only in the AID request.
  JSONObject response=json("https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&type=live&player_type=html5&stream_type=common",item.pageUrl());
  JSONObject channel=response.optJSONObject("CHANNEL");if(channel==null)throw new IOException("방송 정보를 확인할 수 없습니다");
  if(channel.optInt("RESULT",-1)!=1||channel.optString("BNO").isEmpty())throw new IOException("방송 중이 아니거나 로그인·시청 권한이 필요합니다");
  if("Y".equals(channel.optString("BPWD")))throw new IOException("비밀번호 방송은 공식 화면에서 확인하세요");
  PlaybackInfo info=new PlaybackInfo();info.title=channel.optString("TITLE",item.title);info.user=item.user;info.live=channel;info.qualities.addAll(LiveQuality.offered(channel));info.selectedQuality=LiveQuality.preferred(info.qualities);
  try{info.media.add(liveStream(item,channel,info.selectedQuality));}catch(Exception first){if(info.selectedQuality.equals("master"))throw first;info.selectedQuality="master";info.media.add(liveStream(item,channel,"master"));}return info;
 }
 public String liveStream(ContentItem item,JSONObject channel,String quality)throws Exception{
  if(!quality.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("화질 정보가 올바르지 않습니다");
  String bno=channel.optString("BNO");JSONObject token=json("https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&bno="+encode(bno)+"&type=aid&stream_type=common&quality="+encode(quality)+"&player_type=html5&mode=landing&from_api=0",item.pageUrl()).optJSONObject("CHANNEL");
  if(token==null||token.optString("AID").isEmpty())throw new IOException("이 화질의 재생 인증을 받지 못했습니다. 계정 권한을 확인하세요");
  String base=ServiceUrls.https(channel.optString("RMD"));if(base.isEmpty()||!ServiceUrls.official(java.net.URI.create(base).getHost()))base="https://livestream-manager.sooplive.com";
  // Preserve SOOP's assigned CDN. Replacing unknown CDN values with the
  // desktop GS endpoint can make high-resolution live playlists unavailable.
  String cdn=channel.optString("CDN");if(cdn.isEmpty())cdn="gs_cdn_pc_web";else if(cdn.contains("lg_cdn"))cdn="lg_cdn_pc_web";
  JSONObject assigned=json(base.replaceAll("/$","")+"/broad_stream_assign.html?return_type="+encode(cdn)+"&broad_key="+encode(bno+"-common-"+quality+"-hls"),null,item.pageUrl());
  String media=ServiceUrls.https(assigned.optString("view_url"));if(media.isEmpty())throw new IOException("재생 주소를 받지 못했습니다");return HttpUrl.get(media).newBuilder().setQueryParameter("aid",token.optString("AID")).build().toString();
 }

}
