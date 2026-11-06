package dev.soopviewer;

import android.graphics.*;
import android.os.*;
import android.util.LruCache;
import okhttp3.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/** One coalesced download, a bounded compressed-image cache, and region decoding of only one tile. */
public final class VodSnapshotLoader implements SnapshotSource {
 private static final int MAX_BYTES=8*1024*1024;
 private final OkHttpClient http;private final String referer;private final Handler main=new Handler(Looper.getMainLooper());
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final LruCache<String,byte[]> sheets=new LruCache<String,byte[]>(MAX_BYTES){@Override protected int sizeOf(String key,byte[] value){return value.length;}};
 private final Map<String,Long> failed=Collections.synchronizedMap(new LinkedHashMap<String,Long>(){@Override protected boolean removeEldestEntry(Map.Entry<String,Long> e){return size()>24;}});
 private long sequence;private Pending pending;private boolean running,closed;private Call active;private String activeUrl="";
 private static final class Pending {final long id;final VodPreview.Frame frame;final Callback callback;Pending(long id,VodPreview.Frame frame,Callback callback){this.id=id;this.frame=frame;this.callback=callback;}}
 public VodSnapshotLoader(OkHttpClient http,String referer){this.http=http;this.referer=referer;}
 public synchronized void request(VodPreview.Frame frame,Callback callback){
  if(closed)return;pending=new Pending(++sequence,frame,callback);
  if(active!=null&&!activeUrl.equals(frame.url))active.cancel();
  if(!running){running=true;worker.execute(this::drain);}
 }
 public synchronized void cancel(){sequence++;pending=null;if(active!=null)active.cancel();}
 private void drain(){
  while(true){Pending task;synchronized(this){task=pending;pending=null;if(closed||task==null){running=false;return;}}
   Bitmap result=null;
   try{
    byte[] bytes=sheets.get(task.frame.url);if(bytes==null){Long until=failed.get(task.frame.url);if(until!=null&&until>SystemClock.elapsedRealtime())throw new IOException("미리보기 없음");bytes=fetch(task.frame.url);sheets.put(task.frame.url,bytes);}
    synchronized(this){if(pending!=null&&pending.frame.url.equals(task.frame.url)){task=pending;pending=null;}}
    result=tile(bytes,task.frame);
   }catch(IOException|RuntimeException error){synchronized(this){if(!closed&&task.id==sequence)failed.put(task.frame.url,SystemClock.elapsedRealtime()+60000);}}
   final Bitmap bitmap=result;final Pending complete=task;main.post(()->{synchronized(VodSnapshotLoader.this){if(closed||complete.id!=sequence)return;}complete.callback.complete(bitmap);});
  }
 }
 private byte[] fetch(String url)throws IOException{
  Request.Builder request=new Request.Builder().url(url);for(Map.Entry<String,String> h:MediaRequestHeaders.forRequest(url,referer).entrySet())request.header(h.getKey(),h.getValue());
  Call call=http.newCall(request.build());synchronized(this){if(closed)throw new IOException("closed");active=call;activeUrl=url;}
  try(Response response=call.execute()){
   if(!response.isSuccessful()||response.body()==null)throw new IOException("미리보기 응답 오류");
   try(InputStream in=response.body().byteStream();ByteArrayOutputStream out=new ByteArrayOutputStream()){
    byte[] buffer=new byte[8192];int count;while((count=in.read(buffer))!=-1){if(out.size()+count>MAX_BYTES)throw new IOException("미리보기 크기 제한");out.write(buffer,0,count);}return out.toByteArray();
   }
  }finally{synchronized(this){if(active==call){active=null;activeUrl="";}}}
 }
 static Bitmap tile(byte[] bytes,VodPreview.Frame frame)throws IOException{
  BitmapRegionDecoder decoder=BitmapRegionDecoder.newInstance(bytes,0,bytes.length,false);
  if(decoder==null)throw new IOException("미리보기 이미지 없음");
  try{int width=decoder.getWidth()/VodPreview.GRID,height=decoder.getHeight()/VodPreview.GRID;if(width<=0||height<=0)throw new IOException("미리보기 이미지 크기 오류");int x=frame.column()*width,y=frame.row()*height;BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(width/options.inSampleSize>512||height/options.inSampleSize>512)options.inSampleSize*=2;return decoder.decodeRegion(new Rect(x,y,x+width,y+height),options);}finally{decoder.recycle();}
 }
 @Override public synchronized void close(){closed=true;cancel();worker.shutdownNow();sheets.evictAll();failed.clear();}
}
