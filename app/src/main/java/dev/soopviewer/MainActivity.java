package dev.soopviewer;

import android.os.*;
import android.content.*;
import android.content.res.Configuration;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.datasource.okhttp.OkHttpDataSource;
import androidx.media3.ui.PlayerView;
import com.bumptech.glide.Glide;
import org.json.*;
import java.util.*;
import java.util.concurrent.*;

@androidx.annotation.OptIn(markerClass = androidx.media3.common.util.UnstableApi.class)
public class MainActivity extends AppCompatActivity {
 static java.util.function.Supplier<SoopClient> clientFactory=SoopClient::new;
 private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService io=Executors.newFixedThreadPool(4);
 private final int bg=Color.rgb(11,16,27),panel=Color.rgb(22,30,46),muted=Color.rgb(157,170,193),blue=Color.rgb(80,124,255);
 private LinearLayout root,header,nav;private FrameLayout body;private TextView heading;
 private android.content.SharedPreferences prefs;private SoopClient api;
 private int generation=0,page=1;private String screen="home",stationId="",stationType="review",keyword="";private boolean searchVod=false,full=false,resumePlay=false;
 private final ArrayList<ContentItem> rows=new ArrayList<>();private Cards cards;private TextView listStatus;private Button more;
 private ExoPlayer player;private WatchLayout watch;private TextView playbackStatus,chatStatus;private ChatRows chatRows;private RecyclerView chatList;private ContentItem current;
 private ContentItem returnAfterLogin;
 private PlaybackInfo info;private LiveChat liveChat;private WebView web;
 private final Map<String,List<ChatMessage>> chatCache=new LinkedHashMap<>();private String chatLoading="",chatFailed="",chatSignature="";

 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);prefs=getSharedPreferences("viewer",MODE_PRIVATE);api=clientFactory.get();api.ads=prefs.getBoolean("ads",true);CookieManager.getInstance().setAcceptCookie(true);
  root=column();root.setBackgroundColor(bg);root.setOnApplyWindowInsetsListener((v,i)->{v.setPadding(i.getSystemWindowInsetLeft(),i.getSystemWindowInsetTop(),i.getSystemWindowInsetRight(),i.getSystemWindowInsetBottom());return i;});setContentView(root);
  header=row();header.setPadding(dp(16),dp(8),dp(12),dp(8));heading=text("SOOP viewer",23,Color.WHITE);heading.setTypeface(null,Typeface.BOLD);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));header.addView(button("계정",this::account));root.addView(header);
  body=new FrameLayout(this);root.addView(body,new LinearLayout.LayoutParams(-1,0,1));
  nav=row();nav.setPadding(dp(4),dp(4),dp(4),dp(4));for(String[] t:new String[][]{{"홈","home"},{"검색","search"},{"방송국","stations"},{"MY","my"}}){String dest=t[1];Button b=button(t[0],()->navigate(dest));nav.addView(b,new LinearLayout.LayoutParams(0,dp(52),1));}root.addView(nav);home();
 }
 private void navigate(String dest){switch(dest){case "home":home();break;case "search":search();break;case "stations":stations();break;default:account();}}
 private int dp(float n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private LinearLayout column(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.VERTICAL);return l;}
 private LinearLayout row(){LinearLayout l=new LinearLayout(this);l.setOrientation(LinearLayout.HORIZONTAL);l.setGravity(Gravity.CENTER_VERTICAL);return l;}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setTextColor(Color.WHITE);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setOnClickListener(v->action.run());return b;}
 private void pill(View v,int color){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(12));v.setBackground(d);}
 private void note(LinearLayout host,String s){TextView t=text(s,13,muted);t.setPadding(dp(16),dp(10),dp(16),dp(10));host.addView(t);}
 private void screen(String name,String title){generation++;releaseWatch();closeWeb();screen=name;body.removeAllViews();heading.setText(title);header.setVisibility(View.VISIBLE);nav.setVisibility(View.VISIBLE);if(full){full=false;setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);}}
 private LinearLayout listShell(String title,String name){screen(name,title);LinearLayout box=column();body.addView(box,new FrameLayout.LayoutParams(-1,-1));listStatus=text("불러오는 중…",13,muted);listStatus.setPadding(dp(16),dp(8),dp(16),dp(8));box.addView(listStatus);rows.clear();cards=new Cards();RecyclerView list=new RecyclerView(this);list.setLayoutManager(new LinearLayoutManager(this));list.setAdapter(cards);box.addView(list,new LinearLayout.LayoutParams(-1,0,1));more=button("더 보기",()->loadList(true));box.addView(more);return box;}
 private void home(){listShell("인기 LIVE","home");page=1;loadList(false);}
 private void search(){
  LinearLayout box=listShell("검색","search");LinearLayout tools=column();EditText input=new EditText(this);input.setSingleLine();input.setText(keyword);input.setTextColor(Color.WHITE);input.setHintTextColor(muted);input.setHint("방송 제목 / 스트리머");input.setPadding(dp(16),0,dp(16),0);tools.addView(input);
  LinearLayout types=row();Button live=button("LIVE",()->{keyword=input.getText().toString().trim();searchVod=false;page=1;loadList(false);});Button vod=button("VOD",()->{keyword=input.getText().toString().trim();searchVod=true;page=1;loadList(false);});Button channel=button("방송국 ID",()->channelDialog());types.addView(live,new LinearLayout.LayoutParams(0,-2,1));types.addView(vod,new LinearLayout.LayoutParams(0,-2,1));types.addView(channel,new LinearLayout.LayoutParams(0,-2,1));tools.addView(types);box.addView(tools,0);input.setOnEditorActionListener((v,a,e)->{keyword=input.getText().toString().trim();page=1;loadList(false);return true;});
  if(keyword.isEmpty()){listStatus.setText("검색어를 입력하고 LIVE 또는 VOD를 선택하세요");more.setVisibility(View.GONE);}else loadList(false);
 }
 private void station(String user,String type){
  if(!ServiceUrls.userId(user)){toast("방송국 ID를 확인하세요");return;}stationId=user;stationType=type;LinearLayout box=listShell(user+" 방송국","station");
  LinearLayout actions=row();actions.addView(button("생방송",()->watch(new ContentItem(user,user,user+" LIVE",user,"","",false))),new LinearLayout.LayoutParams(0,-2,1));actions.addView(button("즐겨찾기 ☆",()->favorite(user)),new LinearLayout.LayoutParams(0,-2,1));box.addView(actions,0);
  LinearLayout tabs=row();String[] labels={"다시보기","전체 VOD","클립"},values={"review","all","clip"};for(int n=0;n<labels.length;n++){String value=values[n];tabs.addView(button(labels[n],()->station(user,value)),new LinearLayout.LayoutParams(0,-2,1));}box.addView(tabs,1);page=1;loadList(false);
 }
 private void stations(){screen("stations","방송국");LinearLayout box=column();body.addView(box);note(box,"즐겨찾는 방송국에서 생방송과 다시보기를 확인하세요.");box.addView(button("＋ 방송국 ID로 찾기",this::channelDialog));Set<String> saved=new TreeSet<>(prefs.getStringSet("favorites",Collections.emptySet()));if(saved.isEmpty())note(box,"아직 저장한 방송국이 없습니다. 방송 카드의 방송국 버튼으로 방문해 보세요.");for(String id:saved)box.addView(button(id,()->station(id,"review")));}
 private void channelDialog(){EditText input=new EditText(this);input.setSingleLine();input.setHint("SOOP 방송국 ID");new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("방송국 찾기").setView(input).setPositiveButton("열기",(d,w)->station(input.getText().toString().trim(),"review")).setNegativeButton("취소",null).show();}
 private void favorite(String id){Set<String> saved=new HashSet<>(prefs.getStringSet("favorites",Collections.emptySet()));boolean add=!saved.remove(id);if(add)saved.add(id);prefs.edit().putStringSet("favorites",saved).apply();toast(add?"방송국 저장됨":"방송국 저장 해제");}
 private void loadList(boolean next){
  if(screen.equals("search")&&keyword.isEmpty()){listStatus.setText("검색어를 입력하세요");return;}
  if(next)page++;else rows.clear();int request=++generation,requestedPage=page;String kind=screen,user=stationId,type=stationType,q=keyword;boolean vod=searchVod;
  more.setText("더 보기");more.setOnClickListener(v->loadList(true));more.setEnabled(false);more.setVisibility(View.VISIBLE);listStatus.setText("불러오는 중…");if(cards!=null)cards.notifyDataSetChanged();
  io.execute(()->{try{List<ContentItem> found=kind.equals("station")?api.station(user,type,requestedPage):kind.equals("search")?api.search(q,vod,requestedPage):api.home(requestedPage);
   main.post(()->{if(request!=generation)return;int start=rows.size();rows.addAll(found);cards.notifyItemRangeInserted(start,found.size());listStatus.setText((kind.equals("search")?(vod?"VOD":"LIVE")+" 검색 · ":"")+rows.size()+"개"+(found.isEmpty()?" · 결과가 없습니다":""));more.setEnabled(true);more.setVisibility(found.isEmpty()?View.GONE:View.VISIBLE);});
  }catch(Exception e){main.post(()->{if(request!=generation)return;if(next)page=Math.max(1,page-1);listStatus.setText("목록을 가져오지 못했습니다 · "+safeError(e));more.setText("다시 시도");more.setEnabled(true);more.setOnClickListener(v->loadList(false));});}});
 }
 private String safeError(Exception e){return e instanceof java.io.IOException||e instanceof IllegalArgumentException?String.valueOf(e.getMessage()):"서비스 응답을 확인할 수 없습니다";}
 private final class Cards extends RecyclerView.Adapter<Card>{
  public Card onCreateViewHolder(ViewGroup parent,int type){LinearLayout card=column();card.setPadding(dp(14),dp(10),dp(14),dp(16));ImageView image=new ImageView(MainActivity.this);image.setScaleType(ImageView.ScaleType.CENTER_CROP);pill(image,panel);card.addView(image,new LinearLayout.LayoutParams(-1,dp(190)));TextView title=text("",17,Color.WHITE);title.setMaxLines(2);title.setPadding(0,dp(8),0,0);card.addView(title);LinearLayout meta=row();TextView by=text("",12,muted);meta.addView(by,new LinearLayout.LayoutParams(0,-2,1));Button channel=button("방송국",()->{});meta.addView(channel);card.addView(meta);return new Card(card,image,title,by,channel);}
  public void onBindViewHolder(Card holder,int pos){ContentItem item=rows.get(pos);holder.title.setText(html(item.title));holder.meta.setText((item.vod?"VOD":"● LIVE")+" · "+html(item.nick.isEmpty()?item.user:item.nick)+(item.viewers.isEmpty()?"":" · "+item.viewers));Glide.with(MainActivity.this).load(item.thumb).centerCrop().placeholder(android.R.drawable.ic_media_play).into(holder.image);holder.itemView.setOnClickListener(v->watch(item));holder.channel.setOnClickListener(v->station(item.user,"review"));}
  public int getItemCount(){return rows.size();}
 }
 private static final class Card extends RecyclerView.ViewHolder{final ImageView image;final TextView title,meta;final Button channel;Card(View v,ImageView i,TextView t,TextView m,Button c){super(v);image=i;title=t;meta=m;channel=c;}}
 private String html(String s){return android.text.Html.fromHtml(s,android.text.Html.FROM_HTML_MODE_LEGACY).toString();}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_SHORT).show();}
 private void watch(ContentItem item){
  screen("watch",item.vod?"다시보기":"LIVE");current=item;int request=generation;
  LinearLayout box=column();body.addView(box,new FrameLayout.LayoutParams(-1,-1));TextView title=text(html(item.title),16,Color.WHITE);title.setMaxLines(2);title.setPadding(dp(16),dp(4),dp(16),dp(4));box.addView(title);
  LinearLayout controls=row();controls.addView(button("화질",this::quality),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("채팅",this::chatSettings),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("전체화면",this::fullscreen),new LinearLayout.LayoutParams(0,-2,1));controls.addView(button("방송국",()->{if(current!=null)station(current.user,"review");}),new LinearLayout.LayoutParams(0,-2,1));box.addView(controls);
  playbackStatus=text("재생 정보를 불러오는 중…",12,muted);playbackStatus.setPadding(dp(16),0,dp(16),dp(6));box.addView(playbackStatus);
  watch=new WatchLayout(this);watch.hidden=prefs.getBoolean("chatHidden",false);watch.position=prefs.getString("chatPosition","auto");watch.fraction=prefs.getFloat("chatFraction",.35f);watch.x=prefs.getFloat("chatX",.08f);watch.y=prefs.getFloat("chatY",.35f);watch.w=prefs.getFloat("chatW",.7f);watch.h=prefs.getFloat("chatH",.4f);watch.onChanged=this::saveChat;box.addView(watch,new LinearLayout.LayoutParams(-1,0,1));watch.chat.setBackgroundColor(panel);
  TextView handle=text("⠿ 채팅 이동",13,Color.WHITE);handle.setPadding(dp(12),dp(8),dp(12),dp(8));handle.setBackgroundColor(Color.rgb(34,49,72));watch.chat.addView(handle);watch.gesture(handle,false);
  chatStatus=text(item.vod?"재생 위치에 맞춰 채팅을 불러옵니다":"채팅 연결 대기 · 읽기 전용",11,muted);chatStatus.setPadding(dp(10),dp(4),dp(10),dp(4));watch.chat.addView(chatStatus);
  chatList=new RecyclerView(this);chatList.setLayoutManager(new LinearLayoutManager(this));chatRows=new ChatRows();chatList.setAdapter(chatRows);watch.chat.addView(chatList,new LinearLayout.LayoutParams(-1,0,1));
  TextView grip=text("크기 조절 ◢",12,muted);grip.setGravity(Gravity.END);grip.setPadding(dp(8),dp(4),dp(8),dp(4));watch.chat.addView(grip);watch.gesture(grip,true);
  io.execute(()->{try{PlaybackInfo loaded=api.resolve(item);main.post(()->{if(request!=generation)return;info=loaded;if(current.user.isEmpty()&&ServiceUrls.userId(loaded.user))current=new ContentItem(item.id,loaded.user,item.title,item.nick,item.thumb,item.viewers,item.vod);startPlayer(item,request);});}catch(Exception e){main.post(()->{if(request!=generation)return;playbackStatus.setText(e instanceof org.json.JSONException||e.getMessage()==null?"재생 정보를 확인할 수 없습니다":e.getMessage());LinearLayout error=column();note(error,"로그인 상태·시청 권한을 확인하거나 공식 화면에서 재생해 보세요.");error.addView(button("로그인 / 인증",()->officialLogin()));error.addView(button("다시 시도",()->watch(item)));error.addView(button("공식 화면 열기",()->official(item.pageUrl(),"공식 재생 화면")));watch.video.addView(error);});}});
 }
 private void startPlayer(ContentItem item,int request){
  OkHttpDataSource.Factory source=new OkHttpDataSource.Factory(api.http).setDefaultRequestProperties(Collections.singletonMap("Referer",item.pageUrl()));
  player=new ExoPlayer.Builder(this).setMediaSourceFactory(new DefaultMediaSourceFactory(source)).build();player.setAudioAttributes(new androidx.media3.common.AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(),true);player.setHandleAudioBecomingNoisy(true);
  PlayerView view=new PlayerView(this);view.setPlayer(player);view.setKeepScreenOn(true);view.setShowNextButton(info.media.size()>1);view.setShowPreviousButton(info.media.size()>1);watch.video.addView(view,new FrameLayout.LayoutParams(-1,-1));
  List<MediaItem> media=new ArrayList<>();for(String url:info.media){MediaItem.Builder b=new MediaItem.Builder().setUri(url);if(!item.vod||url.contains(".m3u8"))b.setMimeType(MimeTypes.APPLICATION_M3U8);media.add(b.build());}player.setMediaItems(media);
  player.addListener(new Player.Listener(){
   @Override public void onPlaybackStateChanged(int state){if(request!=generation)return;playbackStatus.setText(state==Player.STATE_BUFFERING?"버퍼링 중…":state==Player.STATE_ENDED?"재생 완료": "앱 플레이어 · "+(item.vod?"VOD":"LIVE"));}
   @Override public void onPlayerError(PlaybackException error){if(request!=generation)return;playbackStatus.setText("재생 실패 · 로그인/권한 또는 네트워크 상태를 확인하세요");new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this).setTitle("재생할 수 없습니다").setMessage("공식 화면에서 재생 상태를 확인하거나 다시 시도할 수 있습니다.").setPositiveButton("공식 화면",(d,w)->official(item.pageUrl(),"공식 재생 화면")).setNeutralButton("다시 시도",(d,w)->watch(item)).setNegativeButton("닫기",null).show();}
  });
  player.prepare();player.play();
  if(item.vod)main.post(vodTick);else try{liveChat=new LiveChat(api,info.live,item.user,main,batch->{if(request==generation){chatRows.append(batch);chatList.scrollToPosition(Math.max(0,chatRows.messages.size()-1));}},status->{if(request==generation)chatStatus.setText(status);});}catch(Exception e){chatStatus.setText("채팅에 연결할 수 없습니다 · 방송별 지원 확인 필요");}
 }
 private void quality(){
  if(player==null){toast("영상을 불러온 뒤 사용할 수 있습니다");return;}
  List<String> labels=new ArrayList<>();List<TrackSelectionOverride> choices=new ArrayList<>();labels.add("자동");choices.add(null);Set<Integer> heights=new HashSet<>();
  for(Tracks.Group group:player.getCurrentTracks().getGroups())if(group.getType()==C.TRACK_TYPE_VIDEO)for(int i=0;i<group.length;i++){
   Format f=group.getTrackFormat(i);if(group.isTrackSupported(i)&&f.height>0&&heights.add(f.height)){labels.add(f.height+"p"+(f.frameRate>0?" · "+Math.round(f.frameRate)+"fps":""));choices.add(new TrackSelectionOverride(group.getMediaTrackGroup(),i));}
  }
  new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("방송에서 제공하는 화질").setItems(labels.toArray(new String[0]),(d,w)->{if(player==null)return;TrackSelectionParameters.Builder b=player.getTrackSelectionParameters().buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO);if(choices.get(w)!=null)b.setOverrideForType(choices.get(w));player.setTrackSelectionParameters(b.build());}).show();
 }
 private void chatSettings(){
  if(watch==null)return;String[] labels={watch.hidden?"채팅 표시":"채팅 숨김","자동 배치","왼쪽","오른쪽","위쪽","아래쪽","자유 이동","크기 설정"};
  new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("채팅창 설정").setItems(labels,(d,n)->{if(watch==null)return;if(n==0)watch.hidden=!watch.hidden;else if(n==7){SeekBar size=new SeekBar(this);size.setMax(50);size.setProgress((int)(watch.fraction*100)-15);new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("채팅 크기 · 15~65%").setView(size).setPositiveButton("적용",(a,b)->{if(watch!=null){watch.size((size.getProgress()+15)/100f);saveChat();}}).show();return;}else watch.placement(new String[]{"auto","left","right","top","bottom","floating"}[n-1]);watch.requestLayout();saveChat();}).show();
 }
 private void saveChat(){if(watch!=null)prefs.edit().putBoolean("chatHidden",watch.hidden).putString("chatPosition",watch.position).putFloat("chatFraction",watch.fraction).putFloat("chatX",watch.x).putFloat("chatY",watch.y).putFloat("chatW",watch.w).putFloat("chatH",watch.h).apply();}
 private void fullscreen(){full=!full;header.setVisibility(full?View.GONE:View.VISIBLE);nav.setVisibility(full?View.GONE:View.VISIBLE);setRequestedOrientation(full?ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);}
 private final Runnable vodTick=new Runnable(){public void run(){if(player==null||info==null||current==null||!current.vod)return;updateVodChat();main.postDelayed(this,500);}};
 private void updateVodChat(){
  int part=player.getCurrentMediaItemIndex();long pos=player.getCurrentPosition();if(part<0||part>=info.chat.size())return;String url=info.chat.get(part);if(url.isEmpty()){chatStatus.setText("이 영상에는 제공되는 다시보기 채팅이 없습니다");return;}
  long chunk=ChatTimeline.chunkStart(pos,info.chatSeconds);String key=part+":"+chunk;List<ChatMessage> messages=chatCache.get(key);
  if(messages==null){if(key.equals(chatFailed)){chatStatus.setText("채팅을 불러오지 못했습니다 · 화면을 다시 열면 재시도합니다");return;}if(!key.equals(chatLoading)){chatLoading=key;chatStatus.setText("재생 위치의 채팅 불러오는 중…");int request=generation;String referer=current.pageUrl();io.execute(()->{try{List<ChatMessage> found=ChatParser.xml(api.text(url+(url.contains("?")?"&":"?")+"startTime="+chunk,null,referer));main.post(()->{if(request!=generation)return;chatCache.put(key,found);if(chatCache.size()>8)chatCache.remove(chatCache.keySet().iterator().next());if(key.equals(chatLoading))chatLoading="";});}catch(Exception e){main.post(()->{if(request!=generation)return;chatFailed=key;if(key.equals(chatLoading))chatLoading="";});}});}return;}
  List<ChatMessage> visible=ChatTimeline.visible(messages,pos);String signature=key+":"+visible.size()+":"+(visible.isEmpty()?"":visible.get(visible.size()-1).timeMs+visible.get(visible.size()-1).text);
  if(!signature.equals(chatSignature)){chatRows.replace(visible);chatSignature=signature;chatList.scrollToPosition(Math.max(0,visible.size()-1));}chatStatus.setText("다시보기 채팅 · 영상 위치와 동기화 · 읽기 전용");
 }
 private final class ChatRows extends RecyclerView.Adapter<ChatHolder>{
  final List<ChatMessage> messages=new ArrayList<>();public ChatHolder onCreateViewHolder(ViewGroup p,int t){TextView v=text("",13,Color.WHITE);v.setPadding(dp(10),dp(5),dp(10),dp(5));return new ChatHolder(v);}public void onBindViewHolder(ChatHolder h,int n){ChatMessage m=messages.get(n);h.label.setText(m.nick+"  "+m.text);}public int getItemCount(){return messages.size();}
  void append(List<ChatMessage> batch){messages.addAll(batch);if(messages.size()>200)messages.subList(0,messages.size()-200).clear();notifyDataSetChanged();}void replace(List<ChatMessage> batch){messages.clear();messages.addAll(batch);notifyDataSetChanged();}
 }
 private static final class ChatHolder extends RecyclerView.ViewHolder{final TextView label;ChatHolder(TextView t){super(t);label=t;}}
 private void releaseWatch(){saveChat();main.removeCallbacks(vodTick);if(liveChat!=null){liveChat.close();liveChat=null;}if(player!=null){player.release();player=null;}info=null;current=null;watch=null;chatCache.clear();chatLoading="";chatFailed="";chatSignature="";}
 private void account(){
  screen("my","MY · 내 계정");LinearLayout box=column();body.addView(box);note(box,"본인 SOOP 계정으로 로그인하세요. 로그인과 성인 인증은 공식 화면에서 진행합니다.");String cookies=CookieManager.getInstance().getCookie("https://www.sooplive.com/");note(box,cookies==null||cookies.isEmpty()?"저장된 웹 세션 없음":"저장된 웹 세션 있음 · 로그인 및 인증 여부는 서비스에서 확인합니다");box.addView(button("SOOP 공식 로그인",this::officialLogin));box.addView(button("SOOP 링크 열기",this::linkDialog));box.addView(button(api.ads?"일부 광고 차단: 켜짐":"일부 광고 차단: 꺼짐",()->{api.ads=!api.ads;prefs.edit().putBoolean("ads",api.ads).apply();account();}));box.addView(button("로그아웃 · 웹 세션 삭제",()->new androidx.appcompat.app.AlertDialog.Builder(this).setMessage("이 앱에 저장된 SOOP 웹 세션을 삭제할까요?").setPositiveButton("삭제",(d,w)->{api.http.dispatcher().cancelAll();CookieManager.getInstance().removeAllCookies(ok->{CookieManager.getInstance().flush();WebStorage.getInstance().deleteAllData();account();});}).setNegativeButton("취소",null).show()));note(box,"비공식 개인 뷰어 0.2.0\n홈·방송국·플레이어는 앱 화면입니다. 로그인과 서비스 제한 확인에는 공식 화면을 사용합니다. 일부 기능은 서비스 변경에 따라 동작하지 않을 수 있습니다.");
 }
 private void officialLogin(){ContentItem retry=current;official("https://login.sooplive.com/afreeca/login.php?szFrom=full&request_uri="+Uri.encode("https://www.sooplive.com/"),"SOOP 공식 로그인");returnAfterLogin=retry;}
 private void official(String url,String title){
  returnAfterLogin=null;screen("web",title);LinearLayout box=column();body.addView(box,new FrameLayout.LayoutParams(-1,-1));note(box,"공식 SOOP 화면 · 완료 후 ‘앱으로 돌아가기’를 누르세요");box.addView(button("앱으로 돌아가기",()->{CookieManager.getInstance().flush();ContentItem retry=returnAfterLogin;returnAfterLogin=null;if(retry!=null)watch(retry);else home();}));
  web=new WebView(this);WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);CookieManager.getInstance().setAcceptThirdPartyCookies(web,true);
  web.setWebViewClient(new WebViewClient(){
   @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest r){Uri u=r.getUrl();if("https".equals(u.getScheme())&&ServiceUrls.official(u.getHost()))return false;if(!r.isForMainFrame())return true;if("https".equals(u.getScheme()))new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this).setMessage("외부 공식 인증 화면을 브라우저에서 열까요?\n"+u.getHost()).setPositiveButton("열기",(d,w)->{try{startActivity(new Intent(Intent.ACTION_VIEW,u));}catch(ActivityNotFoundException e){toast("열 수 있는 브라우저가 없습니다");}}).setNegativeButton("취소",null).show();return true;}
   @Override public void onPageFinished(WebView v,String u){CookieManager.getInstance().flush();}
  });box.addView(web,new LinearLayout.LayoutParams(-1,0,1));web.loadUrl(url);
 }
 private void linkDialog(){EditText input=new EditText(this);input.setHint("SOOP 생방송 / VOD 주소");new androidx.appcompat.app.AlertDialog.Builder(this).setTitle("공식 링크 열기").setView(input).setPositiveButton("열기",(d,w)->{
  try{Uri u=Uri.parse(input.getText().toString().trim());if(!"https".equals(u.getScheme())||!ServiceUrls.official(u.getHost()))throw new IllegalArgumentException();List<String> seg=u.getPathSegments();String host=u.getHost();if(host.startsWith("vod.")&&seg.size()>=2&&seg.get(0).equalsIgnoreCase("player")&&seg.get(1).matches("[0-9]+")){watch(new ContentItem(seg.get(1),"","다시보기","","","",true));}else if(host.startsWith("play.")&&!seg.isEmpty()&&ServiceUrls.userId(seg.get(0))){String user=seg.get(0);watch(new ContentItem(user,user,user+" LIVE",user,"","",false));}else throw new IllegalArgumentException();}catch(Exception e){toast("SOOP 공식 방송 또는 VOD 링크를 입력하세요");}
 }).setNegativeButton("취소",null).show();}
 private void closeWeb(){if(web!=null){web.stopLoading();if(web.getParent() instanceof ViewGroup)((ViewGroup)web.getParent()).removeView(web);web.destroy();web=null;}}
 @Override public void onConfigurationChanged(Configuration c){super.onConfigurationChanged(c);if(watch!=null)watch.requestLayout();}
 @Override public void onBackPressed(){if(full){fullscreen();return;}if(web!=null&&web.canGoBack()){web.goBack();return;}if(!screen.equals("home")){home();return;}super.onBackPressed();}
 @Override public void onPause(){if(player!=null){resumePlay=player.isPlaying();player.pause();}if(web!=null)web.onPause();CookieManager.getInstance().flush();super.onPause();}
 @Override public void onResume(){super.onResume();if(web!=null)web.onResume();if(player!=null&&resumePlay){player.play();resumePlay=false;}}
 @Override public void onDestroy(){generation++;releaseWatch();closeWeb();io.shutdownNow();super.onDestroy();}
}
