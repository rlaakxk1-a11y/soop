package dev.soopviewer;
import android.os.Handler;
import okhttp3.*;
import okio.ByteString;
import org.json.JSONObject;
import java.util.List;
import java.util.function.Consumer;

/** Read-only SOOP chat subscription; no chat posting or account impersonation. */
public final class LiveChat extends WebSocketListener {
 private final Handler main;private final Consumer<List<ChatMessage>> messages;private final Consumer<String> status;
 private WebSocket socket;private volatile boolean stopped=false;private final String room;
 private final Runnable heartbeat=new Runnable(){public void run(){if(!stopped&&socket!=null){socket.send(ChatParser.packet(0,"\f"));main.postDelayed(this,20000);}}};
 public LiveChat(SoopClient client,JSONObject channel,String user,Handler main,Consumer<List<ChatMessage>> messages,Consumer<String> status)throws Exception{
  this.main=main;this.messages=messages;this.status=status;room=channel.optString("CHATNO");String host=channel.optString("CHDOMAIN");
  if(host.isEmpty()){String[] ip=channel.optString("CHIP").split("\\.");if(ip.length==4){StringBuilder code=new StringBuilder();for(String n:ip){int part=Integer.parseInt(n);if(part<0||part>255)throw new Exception();code.append(String.format(java.util.Locale.ROOT,"%02X",part));}host="chat-"+code+".sooplive.com";}}
  int port=channel.optInt("CHPT")+1;if(room.isEmpty()||!ServiceUrls.official(host)||port<2||port>65535)throw new Exception("채팅 연결 정보가 없습니다");
  socket=client.http.newWebSocket(new Request.Builder().url("wss://"+host+":"+port+"/Websocket/"+user).header("Origin","https://play.sooplive.com").header("Sec-WebSocket-Protocol","chat").build(),this);
 }
 @Override public void onOpen(WebSocket ws,Response response){if(stopped){ws.close(1000,"Closed");return;}ws.send(ChatParser.packet(1,"\f\f\f16\f"));main.postDelayed(()->{if(!stopped){ws.send(ChatParser.packet(2,"\f"+room+"\f\f\f\f\f"));status.accept("채팅 연결 대기 · 읽기 전용");heartbeat.run();}},200);}
 @Override public void onMessage(WebSocket ws,String text){deliver(text.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
 @Override public void onMessage(WebSocket ws,ByteString bytes){deliver(bytes.toByteArray());}
 private void deliver(byte[] data){List<ChatMessage> batch=ChatParser.packets(data);if(!batch.isEmpty())main.post(()->{if(!stopped){status.accept("실시간 채팅 · 읽기 전용");messages.accept(batch);}});}
 @Override public void onFailure(WebSocket ws,Throwable t,Response response){main.post(()->{if(!stopped)status.accept("채팅 연결 실패 · 시청 화면을 다시 열어주세요");});}
 @Override public void onClosed(WebSocket ws,int code,String reason){main.post(()->{if(!stopped)status.accept("채팅 연결 종료");});}
 public void close(){stopped=true;main.removeCallbacks(heartbeat);if(socket!=null)socket.cancel();}
}
