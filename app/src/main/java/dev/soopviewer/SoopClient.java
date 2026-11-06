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
 public List<ContentItem> liveCategory(String id,int page)throws Exception{
  if(!id.matches("[0-9]+"))throw new IllegalArgumentException("카테고리를 확인하세요");return ContentItem.parse(json("https://live.sooplive.com/api/main_broad_list_api.php?selectType=cate&selectValue="+id+"&orderType=view_cnt&pageNo="+page+"&lang=ko_KR",null,"https://m.sooplive.com/"),false);
 }
 public JSONArray categories()throws Exception{JSONObject r=json("https://api.m.sooplive.com/application/live/a/category","szLang=ko_KR","https://m.sooplive.com/");JSONArray list=r.optJSONArray("data");if(list==null)throw new IOException("카테고리를 확인할 수 없습니다");return list;}
 public JSONArray channelPosts(String user)throws Exception{if(!ServiceUrls.userId(user))throw new IllegalArgumentException("방송국 ID를 확인하세요");JSONObject r=json("https://api-channel.sooplive.com/v1.1/channel/"+user+"/board?page=1&perPage=20",null,ServiceUrls.station(user));JSONArray list=r.optJSONArray("contents");if(list==null)throw new IOException("게시글을 확인할 수 없습니다");return list;}
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
  JSONObject mobile=mobileWatch(item);
  // Keep the existing web chat handshake, independently of media authentication.
  JSONObject channel=new JSONObject();
  try{JSONObject web=json("https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&bno="+encode(mobile.optString("broad_no"))+"&type=live&player_type=html5&stream_type=common",item.pageUrl()).optJSONObject("CHANNEL");if(web!=null&&web.optInt("RESULT")==1)channel=web;}catch(IOException ignored){}
  channel.put("_MOBILE",true);channel.put("BNO",mobile.optString("broad_no"));
  channel.put("VIEWPRESET",mobile.optJSONArray("viewpreset"));channel.put("RESOLUTION",mobile.optString("resolution"));
  if(channel.optString("CHATNO").isEmpty())channel.put("CHATNO",mobile.optString("chat_no"));
  if(channel.optString("CHIP").isEmpty()){String[] chat=mobile.optString("channel_info").split(":");if(chat.length==2){channel.put("CHIP",chat[0]);try{channel.put("CHPT",Integer.parseInt(chat[1]));}catch(NumberFormatException ignored){}}}
  PlaybackInfo info=new PlaybackInfo();info.title=mobile.optString("broad_title",item.title);info.user=item.user;info.live=channel;info.qualities.addAll(LiveQuality.offered(channel));info.selectedQuality=LiveQuality.preferred(info.qualities);
  info.media.add(mobileStream(item,mobile,info.selectedQuality));return info;
 }
 public JSONObject mobileWatch(ContentItem item)throws Exception{
  JSONObject response=json("https://api.m.sooplive.com/broad/a/watch","bj_id="+encode(item.user)+"&broad_no="+(item.id.matches("[0-9]+")?encode(item.id):"")+"&agent=web&player_type=webm&mode=live","https://m.sooplive.com/");
  JSONObject data=response.optJSONObject("data");
  if(response.optInt("result")!=1||data==null||data.optInt("code",0)<0)throw new IOException(data==null?"방송 정보를 확인할 수 없습니다":data.optString("message","로그인·시청 권한 또는 방송 상태를 확인하세요"));
  if(data.optString("broad_no").isEmpty())throw new IOException("방송 중이 아닙니다");return data;
 }
 private String mobileStream(ContentItem item,JSONObject mobile,String quality)throws Exception{
  // The app advertises a separate cast entitlement. The web HLS key can be
  // restricted to 540p even when VIEWPRESET lists 1080p. Never mix these keys.
  boolean cast=mobile.optBoolean("is_chromecast",false)||mobile.optInt("is_chromecast",0)==1;
  String aid=cast?mobile.optString("chromecast_authentication_key"):"";
  String cdn=cast?mobile.optString("cdn_chromecast"):"";
  if(aid.isEmpty()||cdn.isEmpty()){aid=mobile.optString("hls_authentication_key");cdn=mobile.optString("cdn");}
  if(aid.isEmpty()||!cdn.matches("[A-Za-z0-9_\\-]+"))throw new IOException("생방송 시청 인증을 받지 못했습니다");
  String manager=ServiceUrls.https(mobile.optString("mobileweb_resource_manager_url",mobile.optString("resource_manager_url")));
  if(manager.isEmpty()||!ServiceUrls.official(java.net.URI.create(manager).getHost()))throw new IOException("재생 서버 정보를 확인할 수 없습니다");
  JSONObject assigned=json(HttpUrl.get(manager).newBuilder().setQueryParameter("return_type",cdn).setQueryParameter("broad_key",mobile.optString("broad_no")+"-common-master-hls").build().toString(),null,"https://m.sooplive.com/");
  String media=ServiceUrls.https(assigned.optString("view_url"));if(media.isEmpty())throw new IOException("재생 주소를 받지 못했습니다");
  String master=HttpUrl.get(media).newBuilder().setQueryParameter("aid",aid).build().toString();
  return LiveManifest.select(master,text(master,null,"https://m.sooplive.com/"),quality);
 }

 public String liveStream(ContentItem item,JSONObject channel,String quality)throws Exception{
  if(channel.optBoolean("_MOBILE"))return mobileStream(item,mobileWatch(item),quality);
  if(!quality.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("화질 정보가 올바르지 않습니다");
  String bno=channel.optString("BNO");JSONObject token=json("https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&bno="+encode(bno)+"&type=aid&stream_type=common&quality="+encode(quality)+"&player_type=html5&mode=landing&from_api=0",item.pageUrl()).optJSONObject("CHANNEL");
  if(token==null||token.optString("AID").isEmpty())throw new IOException("이 화질의 재생 인증을 받지 못했습니다. 계정 권한을 확인하세요");
  String base=ServiceUrls.https(channel.optString("RMD"));if(base.isEmpty()||!ServiceUrls.official(java.net.URI.create(base).getHost()))base="https://livestream-manager.sooplive.com";
  // Select the web stream for the known mobile CDN families; keep unknown
  // server assignments intact instead of guessing a different CDN.
  String cdn=channel.optString("CDN");if(cdn.isEmpty()||cdn.startsWith("gs_cdn"))cdn="gs_cdn_pc_web";else if(cdn.contains("lg_cdn"))cdn="lg_cdn_pc_web";
  JSONObject assigned=json(base.replaceAll("/$","")+"/broad_stream_assign.html?return_type="+encode(cdn)+"&broad_key="+encode(bno+"-common-"+quality+"-hls"),null,item.pageUrl());
  String media=ServiceUrls.https(assigned.optString("view_url"));if(media.isEmpty())throw new IOException("재생 주소를 받지 못했습니다");return HttpUrl.get(media).newBuilder().setQueryParameter("aid",token.optString("AID")).build().toString();
 }

}
