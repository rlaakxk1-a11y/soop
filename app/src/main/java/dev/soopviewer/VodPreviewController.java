package dev.soopviewer;

import android.graphics.*;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.ui.TimeBar;
import java.util.function.LongSupplier;

@androidx.annotation.OptIn(markerClass=androidx.media3.common.util.UnstableApi.class)
public final class VodPreviewController implements TimeBar.OnScrubListener,AutoCloseable {
 public interface Resolver {VodPreview.Frame at(long positionMs);}
 private final FrameLayout host,bubble,imageBox;private final View bar;private final TimeBar timeBar;private final SnapshotSource images;
 private final Resolver resolver;private final LongSupplier duration;private final ImageView image;private final TextView time,state;
 private final Handler main=new Handler(Looper.getMainLooper());private Runnable load;private long serial,position;private boolean showing,closed;private String selected="";
 private final View.OnLayoutChangeListener layout=(v,l,t,r,b,ol,ot,or,ob)->place();
 public VodPreviewController(FrameLayout host,View bar,SnapshotSource images,Resolver resolver,LongSupplier duration){
  this.host=host;this.bar=bar;this.timeBar=(TimeBar)bar;this.images=images;this.resolver=resolver;this.duration=duration;
  bubble=new FrameLayout(host.getContext());bubble.setTag("watch.preview");bubble.setClickable(false);bubble.setElevation(dp(8));
  GradientDrawable background=new GradientDrawable();background.setColor(0xf51b1d23);background.setCornerRadius(dp(7));background.setStroke(dp(1),0xff7d8390);bubble.setBackground(background);bubble.setPadding(dp(3),dp(3),dp(3),dp(3));
  LinearLayout box=new LinearLayout(host.getContext());box.setOrientation(LinearLayout.VERTICAL);bubble.addView(box,new FrameLayout.LayoutParams(-1,-1));
  imageBox=new FrameLayout(host.getContext());image=new ImageView(host.getContext());image.setTag("watch.preview.image");image.setScaleType(ImageView.ScaleType.FIT_CENTER);imageBox.addView(image,new FrameLayout.LayoutParams(-1,-1));
  state=new TextView(host.getContext());state.setTextColor(Color.WHITE);state.setTextSize(11);state.setGravity(Gravity.CENTER);imageBox.addView(state,new FrameLayout.LayoutParams(-1,-1));box.addView(imageBox,new LinearLayout.LayoutParams(-1,dp(100)));
  time=new TextView(host.getContext());time.setTag("watch.preview.time");time.setTextColor(Color.WHITE);time.setTextSize(12);time.setGravity(Gravity.CENTER);box.addView(time,new LinearLayout.LayoutParams(-1,dp(26)));
  host.addView(bubble,new FrameLayout.LayoutParams(dp(196),dp(132)));bubble.setVisibility(View.GONE);host.addOnLayoutChangeListener(layout);timeBar.addListener(this);
 }
 private int dp(float value){return (int)(value*host.getResources().getDisplayMetrics().density+.5f);}
 @Override public void onScrubStart(TimeBar bar,long position){if(closed)return;showing=true;selected="";bubble.setVisibility(View.VISIBLE);bubble.bringToFront();select(position,true);}
 @Override public void onScrubMove(TimeBar bar,long position){if(showing)select(position,false);}
 @Override public void onScrubStop(TimeBar bar,long position,boolean canceled){hide();}
 private void select(long position,boolean immediate){
  this.position=Math.max(0,position);time.setText(WatchHistory.time(this.position));image.setContentDescription("미리보기 "+time.getText());place();
  VodPreview.Frame frame=resolver.at(this.position);String key=frame==null?"unavailable":frame.key();if(key.equals(selected))return;selected=key;long request=++serial;
  if(load!=null)main.removeCallbacks(load);image.setImageDrawable(null);state.setVisibility(View.VISIBLE);state.setText(frame==null?"미리보기 없음":"불러오는 중…");
  if(frame==null){images.cancel();return;}
  load=()->images.request(frame,bitmap->{if(closed||!showing||request!=serial)return;image.setImageBitmap(bitmap);state.setVisibility(bitmap==null?View.VISIBLE:View.GONE);if(bitmap==null)state.setText("미리보기 없음");place();});
  if(immediate)load.run();else main.postDelayed(load,80);
 }
 private void place(){
  if(!showing||host.getWidth()==0||host.getHeight()==0)return;int[] h=new int[2],b=new int[2];host.getLocationInWindow(h);bar.getLocationInWindow(b);int top=b[1]-h[1];
  int width=Math.max(dp(64),Math.min(dp(196),host.getWidth()-dp(16))),imageHeight=Math.max(dp(24),Math.min(width*9/16,Math.min(host.getHeight()-dp(40),top-dp(40)))),height=imageHeight+dp(32);
  FrameLayout.LayoutParams size=(FrameLayout.LayoutParams)bubble.getLayoutParams();if(size.width!=width||size.height!=height){size.width=width;size.height=height;bubble.setLayoutParams(size);}LinearLayout.LayoutParams imageSize=(LinearLayout.LayoutParams)imageBox.getLayoutParams();if(imageSize.height!=imageHeight){imageSize.height=imageHeight;imageBox.setLayoutParams(imageSize);}
  long total=duration.getAsLong();float fraction=total>0?Math.max(0,Math.min(1,(float)position/total)):.5f;
  float center=b[0]-h[0]+bar.getPaddingLeft()+fraction*Math.max(1,bar.getWidth()-bar.getPaddingLeft()-bar.getPaddingRight());
  bubble.setX(Math.max(dp(4),Math.min(host.getWidth()-width-dp(4),center-width/2f)));bubble.setY(Math.max(dp(4),Math.min(host.getHeight()-height-dp(4),top-height-dp(8))));
 }
 public void hide(){showing=false;serial++;selected="";if(load!=null)main.removeCallbacks(load);images.cancel();image.setImageDrawable(null);bubble.setVisibility(View.GONE);}
 @Override public void close(){if(closed)return;closed=true;hide();timeBar.removeListener(this);host.removeOnLayoutChangeListener(layout);host.removeView(bubble);images.close();}
}
