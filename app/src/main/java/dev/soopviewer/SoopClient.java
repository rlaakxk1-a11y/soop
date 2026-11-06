package dev.soopviewer;
import android.webkit.CookieManager;
import okhttp3.*;
import org.json.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class SoopClient {
 public volatile boolean ads=true;
 private final Map<String,String> mediaReferers=Collections.synchronizedMap(new LinkedHashMap<String,String>(){@Override protected boolean removeEldestEntry(Map.Entry<String,String> eldest){return size()>16;}});
 private String rememberMedia(String media,String referer){mediaReferers.put(media,referer);return media;}
 public Map<String,String> mediaHeaders(ContentItem item,String media){return MediaRequestHeaders.forRequest(media,mediaReferers.getOrDefault(media,item.vod?item.pageUrl():"https://m.sooplive.com/"));}

 private volatile String playbackDiagnostic="아직 생방송 재생 요청이 없습니다";
 public String playbackDiagnostic(){return playbackDiagnostic;}
 private synchronized void trace(String message){playbackDiagnostic=(playbackDiagnostic+"\n"+message);if(playbackDiagnostic.length()>2500)playbackDiagnostic=playbackDiagnostic.substring(playbackDiagnostic.length()-2500);}
 private void beginPlayback(){playbackDiagnostic="SOOP Viewer "+BuildConfig.VERSION_NAME+" · 생방송 연결";}
 public static final class HttpStatusException extends IOException{
  public final int status;public HttpStatusException(int status){super("서비스 응답 오류 (HTTP "+status+")");this.status=status;}
 }
 private static int httpStatus(Throwable failure){for(Throwable t=failure;t!=null;t=t.getCause())if(t instanceof HttpStatusException)return ((HttpStatusException)t).status;return 0;}
 private static boolean authorizationFailure(Throwable failure){int status=httpStatus(failure);for(Throwable t=failure;t!=null;t=t.getCause())if(t instanceof PlaybackAuthorizationException)return true;return status==401||status==403;}
 public static final class IdentityVerificationException extends IOException{IdentityVerificationException(){super("공식 본인 · 성인 인증 상태를 확인해야 합니다. 로그인 후 본인 인증을 진행하세요.");}}
 private static final class PlaybackAuthorizationException extends IOException{PlaybackAuthorizationException(String message){super(message);}}
 private IOException stageFailure(String stage,Throwable failure){String reason=httpStatus(failure)>0?"HTTP "+httpStatus(failure):failure instanceof JSONException?"응답 형식 오류":failure instanceof java.net.SocketTimeoutException?"연결 시간 초과":"연결 또는 재생 목록 오류";trace(stage+": "+reason);return new IOException(stage+" · "+reason,failure);}
 private JSONObject playbackJson(String stage,String url,String form,String referer)throws IOException{
  try{return json(url,form,referer);}catch(IOException|JSONException e){throw stageFailure(stage,e);}catch(Exception e){throw stageFailure(stage,e);}
 }
 private String playbackText(String stage,String url)throws IOException{
  return playbackText(stage,url,"https://m.sooplive.com/");
 }
 private String playbackText(String stage,String url,String referer)throws IOException{
  try{return text(url,null,referer);}catch(Exception e){throw stageFailure(stage,e);}
 }
 public static String mobileWebCdn(String cdn){
  // gs_cdn is the native/cast family, not a browser HLS delivery endpoint.
  // Keep global and subscription-specific assignments unchanged.
  if(Arrays.asList("gs_cdn","gs_cdn_pc_web","gs_cdn_mobile_app").contains(cdn))return "gs_cdn_mobile_web";
  if(Arrays.asList("lg_cdn","lg_cdn_pc_web","lg_cdn_mobile_app").contains(cdn))return "lg_cdn_mobile_web";
  return cdn;
 }

 private SessionCookies sessions;
 public void attachSessions(SessionCookies sessions){this.sessions=sessions;}
 public final OkHttpClient http;
 public SoopClient(){
  http=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(20,TimeUnit.SECONDS).callTimeout(30,TimeUnit.SECONDS).followSslRedirects(false)
   .cookieJar(new CookieJar(){
    private final ThreadLocal<Integer> requestEpoch=new ThreadLocal<>();
    @Override public List<Cookie> loadForRequest(HttpUrl url){
     if(sessions!=null){sessions.awaitReady();requestEpoch.set(sessions.epoch());}
     String raw=CookieManager.getInstance().getCookie(url.toString());List<Cookie> list=new ArrayList<>();
     if(raw!=null)for(String field:raw.split(";")){int n=field.indexOf('=');if(n>0)try{list.add(new Cookie.Builder().name(field.substring(0,n).trim()).value(field.substring(n+1).trim()).hostOnlyDomain(url.host()).path("/").secure().build());}catch(IllegalArgumentException ignored){}}
     return list;
    }
    @Override public void saveFromResponse(HttpUrl url,List<Cookie> cookies){if(sessions!=null){Integer epoch=requestEpoch.get();sessions.receive(url,cookies,epoch==null?-1:epoch);}else for(Cookie c:cookies)CookieManager.getInstance().setCookie(url.toString(),c.toString());}
   }).addInterceptor(chain->{Request r=chain.request();if(ads&&AdRules.blocked(r.url().host()))return new Response.Builder().request(r).protocol(Protocol.HTTP_1_1).code(204).message("Filtered").body(ResponseBody.create(new byte[0],null)).build();return chain.proceed(r);}).build();
 }
 public String text(String url,String form,String referer)throws Exception{
  if(ServiceUrls.https(url).isEmpty())throw new IOException("HTTPS 주소만 사용할 수 있습니다");
  Request.Builder b=new Request.Builder().url(url);for(Map.Entry<String,String> header:MediaRequestHeaders.forRequest(url,referer).entrySet())b.header(header.getKey(),header.getValue());
  if(form!=null)b.post(RequestBody.create(form,MediaType.get("application/x-www-form-urlencoded; charset=UTF-8")));
  try(Response r=http.newCall(b.build()).execute()){
   if(!r.isSuccessful())throw new HttpStatusException(r.code());if(r.body()==null)throw new IOException("빈 응답입니다");
   try(InputStream in=r.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] data=new byte[8192];int n;while((n=in.read(data))!=-1){if(out.size()+n>8*1024*1024)throw new IOException("응답 크기가 너무 큽니다");out.write(data,0,n);}return out.toString("UTF-8");}
  }
 }
 public JSONObject json(String url,String form,String ref)throws Exception{return new JSONObject(text(url,form,ref));}
 public static String encode(String s){try{return java.net.URLEncoder.encode(s,"UTF-8");}catch(Exception e){throw new IllegalArgumentException(e);}}
 public LoginResult login(String user,String password)throws Exception{
  if(user.trim().isEmpty()||password.isEmpty())throw new IllegalArgumentException("아이디와 비밀번호를 입력하세요");
  // Credentials go only to SOOP's official HTTPS login endpoint; never stored or logged.
  return LoginResult.parse(json("https://login.sooplive.com/app/LoginAction.php","szWork=login&szType=json&szUid="+encode(user.trim())+"&szPassword="+encode(password)+"&isSaveId=false&isSavePw=false&isSaveJoin=false&isLoginRetain=Y","https://login.sooplive.com/afreeca/login.php"));
 }
 public String loggedInUser()throws Exception{JSONObject channel=json("https://afevent2.sooplive.com/api/get_private_info.php",null,"https://m.sooplive.com/").optJSONObject("CHANNEL");return channel==null?"":channel.optString("LOGIN_ID","").trim();}
 public List<ContentItem> liveList(String category,String order,int page)throws Exception{
  BrowseOptions.checked(BrowseOptions.LIVE_KEYS,order);if(category.isEmpty()&&order.equals("view_cnt"))return home(page);if(!category.isEmpty()&&order.equals("view_cnt"))return liveCategory(category,page);
  if(!category.isEmpty()&&!category.matches("[0-9]+"))throw new IllegalArgumentException("카테고리를 확인하세요");
  String select=category.isEmpty()?"selectType=action&selectValue=all":"selectType=cate&selectValue="+category;
  return ContentItem.parse(json("https://live.sooplive.com/api/main_broad_list_api.php?"+select+"&orderType="+encode(order)+"&pageNo="+page+"&lang=ko_KR",null,"https://m.sooplive.com/"),false);
 }
 public List<ContentItem> search(String keyword,boolean vod,int page,String order)throws Exception{
  BrowseOptions.checked(BrowseOptions.SEARCH_KEYS,order);if(order.equals("score"))return search(keyword,vod,page);
  return ContentItem.parse(json("https://sch.sooplive.com/api.php?l=DF&m="+(vod?"vodSearch":"liveSearch")+"&c=UTF-8&w=webk&isMobile=0&onlyParent=1&szType=json&szOrder="+encode(order)+"&szKeyword="+encode(keyword)+"&nPageNo="+page+"&nListCnt=30&tab="+(vod?"vod":"live")+"&location=total_search&isHashSearch=0&v="+(vod?"5.0":"2.0"),null,"https://www.sooplive.com/"),vod);
 }
 public List<ContentItem> station(String user,String type,int page,String order)throws Exception{
  BrowseOptions.checked(BrowseOptions.VIDEO_KEYS,order);if(order.equals("reg_date"))return station(user,type,page);
  if(!ServiceUrls.userId(user)||!Arrays.asList("all","review","clip","normal").contains(type))throw new IllegalArgumentException("방송국 정보를 확인하세요");
  return ContentItem.parse(json("https://chapi.sooplive.com/api/"+user+"/vods/"+type+"?page="+page+"&per_page=30&orderby="+encode(order),null,ServiceUrls.station(user)),true,user);
 }
 public void recordDecodedVideo(int width,int height){trace("실제 디코딩: "+width+"×"+height);}
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
 public List<ContentItem> searchStreamers(String keyword)throws Exception{return mobileSearch(keyword,"streamer",1,"score",20).items;}
 public SearchPage searchPage(String keyword,String mode,int page,String order)throws Exception{
  if(!Arrays.asList("all","streamer","live","vod").contains(mode)||page<1)throw new IllegalArgumentException("검색 조건을 확인하세요");
  BrowseOptions.checked(BrowseOptions.SEARCH_KEYS,order);
  if(!mode.equals("all"))return mobileSearch(keyword,mode,page,order,20);
  List<ContentItem> items=new ArrayList<>();for(String type:new String[]{"streamer","live","vod"})items.addAll(mobileSearch(keyword,type,1,"score",type.equals("streamer")?3:6).items);
  return new SearchPage(items,false);
 }
 private SearchPage mobileSearch(String keyword,String mode,int page,String order,int count)throws Exception{
  String method=mode.equals("streamer")?"bjSearch":mode.equals("live")?"liveSearch":"vodSearch";
  String actualOrder=order.equals("reg_date")?(mode.equals("live")?"broad_start":"reg_datetime"):order.equals("view_cnt")&&mode.equals("live")?"pc_total_view_cnt":order;
  String form="m="+method+"&v="+(mode.equals("vod")?"5.0":"1.0")+"&w=webm&isMobile=1&t=json&c=UTF-8&szKeyword="+encode(keyword)+"&szOrder="+actualOrder+"&nPageNo="+page+"&nListCnt="+count+"&location=total_search&tab="+(mode.equals("streamer")?"bj":mode)+"&align_type=list";
  return SearchPage.parse(json("https://sch.sooplive.com/api.php",form,"https://m.sooplive.com/"),mode);
 }
 public List<ContentItem> station(String user,String type,int page)throws Exception{
  if(!ServiceUrls.userId(user)||!Arrays.asList("all","review","clip","normal").contains(type))throw new IllegalArgumentException("방송국 ID를 확인하세요");
  return ContentItem.parse(json("https://chapi.sooplive.com/api/"+user+"/vods/"+type+"?page="+page+"&per_page=30&orderby=reg_date",null,ServiceUrls.station(user)),true,user);
 }
 public PlaybackInfo resolve(ContentItem item)throws Exception{
  if(item.vod)return PlaybackInfo.vod(json("https://api.m.sooplive.com/station/video/a/view","nTitleNo="+encode(item.id)+"&nApiLevel=10",item.pageUrl()));
  beginPlayback();JSONObject mobile;try{mobile=mobileWatch(item);}catch(IdentityVerificationException denied){return resolveVerifiedDesktop(item,denied);}
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
  JSONObject response=playbackJson("방송 정보","https://api.m.sooplive.com/broad/a/watch","bj_id="+encode(item.user)+"&broad_no="+(item.id.matches("[0-9]+")?encode(item.id):"")+"&agent=web&player_type=webm&mode=live","https://m.sooplive.com/");
  JSONObject data=response.optJSONObject("data");
  if(response.optInt("result")!=1||data==null||data.optInt("code",0)<0){String message=data==null?"방송 정보를 확인할 수 없습니다":data.optString("message","로그인·시청 권한 또는 방송 상태를 확인하세요");String lower=message.toLowerCase(Locale.ROOT);if(lower.contains("verify your identity")||message.contains("본인인증")||message.contains("본인 인증")||message.contains("성인인증")||message.contains("성인 인증")){trace("모바일 시청 권한: 본인 · 성인 인증 확인 필요");throw new IdentityVerificationException();}throw new IOException(message);}
  if(data.optString("broad_no").isEmpty())throw new IOException("방송 중이 아닙니다");trace("방송 원본: "+data.optString("resolution","미제공")+" · 서버 지역: "+data.optString("country_code","미제공"));return data;
 }
 private boolean manualCanUseWeb(JSONObject channel,String quality){if(quality.equals("auto")||quality.equals("master"))return true;for(LiveQuality q:LiveQuality.offered(channel))if(q.id.equals(quality)&&q.height>0&&q.height<=540)return true;return false;}
 private String mobileStream(ContentItem item,JSONObject mobile,String quality)throws Exception{return mobileStream(item,mobile,quality,false);}
 private String mobileStream(ContentItem item,JSONObject mobile,String quality,boolean webOnly)throws Exception{return mobileStream(item,mobile,quality,webOnly,true);}
 private String mobileStream(ContentItem item,JSONObject mobile,String quality,boolean webOnly,boolean allowWebFallback)throws Exception{
  if(!quality.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("화질 정보가 올바르지 않습니다");
  boolean cast=!webOnly&&(mobile.optBoolean("is_chromecast",false)||mobile.optInt("is_chromecast",0)==1);
  String castKey=cast?mobile.optString("chromecast_authentication_key"):"",castCdn=cast?mobile.optString("cdn_chromecast"):"",spare=cast?mobile.optString("chromecast_authentication_spare_key"):"";
  trace("요청 프리셋: "+quality+" · 기본 캐스트 키 "+(!castKey.isEmpty())+" · 예비 캐스트 키 "+(!spare.isEmpty()));
  IOException castFailure=null;boolean highQualityDenied=false;
  if(!castCdn.isEmpty()){
   String[][] attempts={{castKey,"고화질 기본","master"},{castKey,"고화질 직접","direct"},{spare,"고화질 예비","master"}};
   for(String[] attempt:attempts){if(attempt[0].isEmpty())continue;if(attempt[1].equals("고화질 예비")&&spare.equals(castKey))continue;boolean direct=attempt[2].equals("direct");if(direct&&(quality.equals("auto")||quality.equals("master")))continue;
    try{return mobileManifest(mobile,quality,attempt[0],castCdn,attempt[1],false,direct);}catch(IOException e){castFailure=e;trace(attempt[1]+" 실패");if(authorizationFailure(e)){highQualityDenied=true;break;}}
   }
  }
  if((castFailure!=null||castKey.isEmpty()||castCdn.isEmpty())&&!highQualityDenied&&!webOnly&&!quality.equals("auto")&&!quality.equals("master")){
   try{return desktopHighQuality(item,mobile,quality);}catch(IOException e){castFailure=e;trace("화질별 웹 인증 경로 실패 · 다른 고화질 경로를 재시도하지 않습니다");}
  }
  if(!allowWebFallback&&!webOnly){if(castFailure!=null)throw new IOException("요청 화질 연결 실패 · 현재 재생을 유지합니다",castFailure);throw new IOException("서버가 고화질 재생 인증을 제공하지 않았습니다");}
  if(castFailure!=null)trace("고화질 경로 실패 · 별도로 발급된 모바일 웹 권한으로 재생 확인");
  String webKey=mobile.optString("hls_authentication_key"),webCdn=mobile.optString("cdn");
  if(webKey.isEmpty()||webCdn.isEmpty()){if(castFailure!=null)throw castFailure;throw new IOException("생방송 시청 인증을 받지 못했습니다");}
  try{return mobileManifest(mobile,quality,webKey,webCdn,"모바일 웹",true,false);}catch(IOException e){if(castFailure!=null)e.addSuppressed(castFailure);throw e;}
 }
 /** Request an independent, quality-specific web entitlement after a transport failure. */
 private String desktopHighQuality(ContentItem item,JSONObject mobile,String quality)throws IOException{
  String endpoint="https://live.sooplive.com/afreeca/player_live_api.php",bno=mobile.optString("broad_no");
  JSONObject response=playbackJson("웹 방송 정보",endpoint,"bid="+encode(item.user)+"&bno="+encode(bno)+"&type=live&player_type=html5&stream_type=common",item.pageUrl());
  JSONObject channel=response.optJSONObject("CHANNEL");
  if(channel==null||channel.optInt("RESULT")!=1)throw new PlaybackAuthorizationException("웹 방송의 로그인·시청 권한을 확인하세요");
  if(!channel.optString("BNO").equals(bno))throw new IOException("방송 정보가 변경되었습니다 · 다시 연결하세요");
  return desktopMedia(item,channel,bno,quality);
 }
 private PlaybackInfo resolveVerifiedDesktop(ContentItem item,IdentityVerificationException denied)throws Exception{
  // The mobile API can require a separate identity session. Only the PC server's
  // independently granted, validated entitlement may authorize this route.
  try{JSONObject response=playbackJson("웹 시청 권한 확인","https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&bno="+(item.id.matches("[0-9]+")?encode(item.id):"")+"&type=live&player_type=html5&stream_type=common",item.pageUrl());JSONObject channel=response.optJSONObject("CHANNEL");
   if(channel==null||channel.optInt("RESULT")!=1||!channel.optString("BNO").matches("[0-9]+")||(item.id.matches("[0-9]+")&&!channel.optString("BNO").equals(item.id)))throw denied;
   PlaybackInfo info=new PlaybackInfo();info.live=channel;info.user=item.user;info.title=channel.optString("TITLE",item.title);info.qualities.addAll(LiveQuality.offered(channel));info.selectedQuality=LiveQuality.preferred(info.qualities);channel.put("_VERIFIED_WEB",true);info.media.add(desktopMedia(item,channel,channel.optString("BNO"),info.selectedQuality));trace("웹 서버 시청 인증 및 재생 목록 확인 완료");return info;
  }catch(Exception failed){trace("웹 시청 권한을 확인하지 못했습니다 · 공식 본인 인증 필요");throw denied;}
 }
 private String desktopMedia(ContentItem item,JSONObject channel,String bno,String quality)throws IOException{
  String endpoint="https://live.sooplive.com/afreeca/player_live_api.php";
  boolean offered=false;for(LiveQuality q:LiveQuality.offered(channel))if(q.id.equals(quality))offered=true;
  if(!offered)throw new IOException("웹 서버가 요청 화질을 제공하지 않습니다");
  JSONObject token=playbackJson("화질별 웹 인증",endpoint,"bid="+encode(item.user)+"&bno="+encode(bno)+"&type=aid&stream_type=common&quality="+encode(quality)+"&player_type=html5&mode=landing&from_api=0",item.pageUrl()).optJSONObject("CHANNEL");
  trace("화질별 웹 인증 응답: "+(token==null?"없음":token.optInt("RESULT"))+" · 인증 발급 "+(token!=null&&!token.optString("AID").isEmpty()));
  if(token==null||token.optInt("RESULT")!=1||token.optString("AID").isEmpty())throw new PlaybackAuthorizationException("요청 화질의 웹 시청 인증을 받지 못했습니다");
  String cdn=channel.optString("CDN");
  if(Arrays.asList("lg_cdn","lg_cdn_mobile_web","lg_cdn_mobile_app").contains(cdn))cdn="lg_cdn_pc_web";
  if(Arrays.asList("gs_cdn","gs_cdn_mobile_web","gs_cdn_mobile_app").contains(cdn))cdn="gs_cdn_pc_web";
  if(!cdn.matches("[A-Za-z0-9_\\-]+"))throw new IOException("웹 재생 서버 정보가 없습니다");
  String manager=ServiceUrls.https(channel.optString("RMD"));
  if(manager.isEmpty()||!ServiceUrls.official(java.net.URI.create(manager).getHost()))throw new IOException("웹 재생 서버 정보를 확인할 수 없습니다");
  if(!java.net.URI.create(manager).getPath().endsWith(".html"))manager=manager.replaceAll("/$","")+"/broad_stream_assign.html";
  trace("화질별 웹 인증 완료 · 프리셋: "+quality+" · CDN: "+cdn);
  JSONObject assignment=playbackJson("웹 서버 할당",HttpUrl.get(manager).newBuilder().setQueryParameter("return_type",cdn).setQueryParameter("broad_key",bno+"-common-"+quality+"-hls").build().toString(),null,item.pageUrl());
  String media=ServiceUrls.https(assignment.optString("view_url"));if(media.isEmpty())throw new IOException("웹 재생 주소가 없습니다");
  trace("화질별 웹 서버: "+java.net.URI.create(media).getHost());
  String base=HttpUrl.get(media).newBuilder().setQueryParameter("aid",token.optString("AID")).build().toString();
  trace("웹 영상 요청: PC User-Agent · Origin/Referer play.sooplive.com");
  String selected;try{selected=LiveManifest.select(base,playbackText("웹 화질 재생 목록",base,item.pageUrl()),quality);}catch(IOException e){throw stageFailure("웹 화질 선택",e);}
  if(!selected.equals(base))LiveManifest.select(selected,playbackText("웹 영상 목록",selected,item.pageUrl()),"auto");
  trace("화질별 웹 재생 주소 확인 완료");return rememberMedia(selected,item.pageUrl());
 }
 private String mobileManifest(JSONObject mobile,String quality,String aid,String assignedCdn,String route,boolean allowLower,boolean direct)throws IOException{
  String cdn=mobileWebCdn(assignedCdn);if(!cdn.matches("[A-Za-z0-9_\\-]+"))throw new IOException("재생 서버 정보가 올바르지 않습니다");
  trace(route+" CDN: "+assignedCdn+" → "+cdn);
  String manager=ServiceUrls.https(mobile.optString(direct?"resource_manager_url":"mobileweb_resource_manager_url"));
  if(manager.isEmpty())manager=ServiceUrls.https(mobile.optString(direct?"mobileweb_resource_manager_url":"resource_manager_url"));
  if(manager.isEmpty()||!ServiceUrls.official(java.net.URI.create(manager).getHost()))throw new IOException("재생 서버 정보를 확인할 수 없습니다");
  JSONObject assigned=playbackJson(route+" 서버 할당",HttpUrl.get(manager).newBuilder().setQueryParameter("return_type",cdn).setQueryParameter("broad_key",mobile.optString("broad_no")+"-common-"+(direct?quality:"master")+"-hls").build().toString(),null,"https://m.sooplive.com/");
  String media=ServiceUrls.https(assigned.optString("view_url"));if(media.isEmpty())throw new IOException(route+" · 재생 주소가 없습니다");
  trace(route+" 서버: "+java.net.URI.create(media).getHost());
  String master=HttpUrl.get(media).newBuilder().setQueryParameter("aid",aid).build().toString();
  String selected;
  String masterBody=playbackText(route+" 재생 목록",master);
  try{selected=LiveManifest.select(master,masterBody,quality);}catch(IOException e){if(!allowLower)throw stageFailure(route+" 화질 선택",e);try{selected=LiveManifest.bestAvailable(master,masterBody);trace("웹 권한에서 제공하는 화질로 재생 · 실제 해상도 확인 필요");}catch(IOException invalid){throw stageFailure(route+" 화질 선택",invalid);}}
  // A valid master does not prove that the signed child playlist is usable.
  if(!selected.equals(master)){
   String child=playbackText(route+" 영상 목록",selected);
   try{LiveManifest.select(selected,child,"auto");}catch(IOException e){throw stageFailure(route+" 영상 목록",e);}
  }
  trace(route+" 재생 주소 확인 완료");return rememberMedia(selected,"https://m.sooplive.com/");
 }
 public String fallbackLiveStream(ContentItem item,JSONObject channel,String quality)throws Exception{
  beginPlayback();trace("플레이어 오류 후 요청 화질로 인증을 갱신하여 1회 복구");if(channel.optBoolean("_VERIFIED_WEB"))return desktopHighQuality(item,new JSONObject().put("broad_no",channel.optString("BNO")),quality);return mobileStream(item,mobileWatch(item),quality,false);
 }

 public String liveStream(ContentItem item,JSONObject channel,String quality)throws Exception{
  if(channel.optBoolean("_VERIFIED_WEB"))return desktopHighQuality(item,new JSONObject().put("broad_no",channel.optString("BNO")),quality);
  if(channel.optBoolean("_MOBILE")){beginPlayback();return mobileStream(item,mobileWatch(item),quality,false,manualCanUseWeb(channel,quality));}
  if(!quality.matches("[A-Za-z0-9_\\-]+"))throw new IllegalArgumentException("화질 정보가 올바르지 않습니다");
  String bno=channel.optString("BNO");JSONObject token=json("https://live.sooplive.com/afreeca/player_live_api.php","bid="+encode(item.user)+"&bno="+encode(bno)+"&type=aid&stream_type=common&quality="+encode(quality)+"&player_type=html5&mode=landing&from_api=0",item.pageUrl()).optJSONObject("CHANNEL");
  if(token==null||token.optString("AID").isEmpty())throw new IOException("이 화질의 재생 인증을 받지 못했습니다. 계정 권한을 확인하세요");
  String base=ServiceUrls.https(channel.optString("RMD"));if(base.isEmpty()||!ServiceUrls.official(java.net.URI.create(base).getHost()))base="https://livestream-manager.sooplive.com";
  // Select the web stream for the known mobile CDN families; keep unknown
  // server assignments intact instead of guessing a different CDN.
  String cdn=channel.optString("CDN");if(cdn.isEmpty()||cdn.startsWith("gs_cdn"))cdn="gs_cdn_pc_web";else if(cdn.contains("lg_cdn"))cdn="lg_cdn_pc_web";
  JSONObject assigned=json(base.replaceAll("/$","")+"/broad_stream_assign.html?return_type="+encode(cdn)+"&broad_key="+encode(bno+"-common-"+quality+"-hls"),null,item.pageUrl());
  String media=ServiceUrls.https(assigned.optString("view_url"));if(media.isEmpty())throw new IOException("재생 주소를 받지 못했습니다");return rememberMedia(HttpUrl.get(media).newBuilder().setQueryParameter("aid",token.optString("AID")).build().toString(),item.pageUrl());
 }

}
