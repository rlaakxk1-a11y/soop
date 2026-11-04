package dev.soopviewer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.view.*;
import android.widget.*;

/** Video fills the immersive canvas; a docked chat can shrink without becoming an overlay. */
public final class WatchLayout extends FrameLayout {
 public final FrameLayout video;
 public final LinearLayout chat,details;
 public final View resizeRail;
 public boolean hidden=false,immersive=false,mobilePortrait=false,pictureInPicture=false,miniPlayer=false;
 public String position="auto";
 public float fraction=.20f;
 Runnable onChanged;
 float x=.08f,y=.35f,w=.40f,h=.4f;
 private int detailHeight;
 public WatchLayout(Context c){
  super(c);setBackgroundColor(android.graphics.Color.BLACK);
  video=new FrameLayout(c);chat=new LinearLayout(c);chat.setOrientation(LinearLayout.VERTICAL);
  details=new LinearLayout(c);details.setOrientation(LinearLayout.VERTICAL);
  addView(video);addView(chat);addView(details);
  resizeRail=new View(c){final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);@Override protected void onDraw(Canvas canvas){paint.setColor(0xff7a7d85);float cx=getWidth()/2f,cy=getHeight()/2f;if(side())canvas.drawRoundRect(cx-dp(1.5f),cy-dp(22),cx+dp(1.5f),cy+dp(22),dp(2),dp(2),paint);else canvas.drawRoundRect(cx-dp(22),cy-dp(1.5f),cx+dp(22),cy+dp(1.5f),dp(2),dp(2),paint);}};
  resizeRail.setContentDescription("채팅 폭 조절 · 경계를 좌우로 드래그");resizeRail.setTag("chat.resizeRail");addView(resizeRail);gesture(resizeRail,true);
 }
 private int dp(float value){return (int)(value*getResources().getDisplayMetrics().density+.5f);}
 public static float clamp(float value){return Math.max(.08f,Math.min(.50f,value));}
 public void placement(String value){position=value;requestLayout();}
 public void size(float value){fraction=clamp(value);if(position.equals("floating"))position="auto";requestLayout();}
 private boolean portrait(){return mobilePortrait&&!immersive&&getMeasuredWidth()<getMeasuredHeight();}
 private String resolved(){return position.equals("auto")?(immersive||getMeasuredWidth()>getMeasuredHeight()?"right":"bottom"):position;}
 private boolean side(){return resolved().equals("left")||resolved().equals("right");}
 public void gesture(View handle,boolean resize){
  handle.setOnTouchListener(new View.OnTouchListener(){float sx,sy,left,top,width,height;
   @Override public boolean onTouch(View v,MotionEvent e){
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){sx=e.getRawX();sy=e.getRawY();left=chat.getLeft();top=chat.getTop();width=chat.getWidth();height=chat.getHeight();v.getParent().requestDisallowInterceptTouchEvent(true);return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_MOVE){float W=Math.max(1,getWidth()),H=Math.max(1,getHeight()),dx=e.getRawX()-sx,dy=e.getRawY()-sy;
     if(resize&&!position.equals("floating")){String p=resolved();fraction=clamp(side()?(width+(p.equals("right")?-dx:dx))/W:(height+(p.equals("bottom")?-dy:dy))/H);}
     else {position="floating";float nw=resize?width+dx:width,nh=resize?height+dy:height;nw=Math.max(Math.min(dp(80),W),Math.min(W,nw));nh=Math.max(Math.min(dp(80),H),Math.min(H,nh));x=Math.max(0,Math.min(W-nw,resize?left:left+dx))/W;y=Math.max(0,Math.min(H-nh,resize?top:top+dy))/H;w=nw/W;h=nh/H;}
     requestLayout();return true;
    }
    if(e.getActionMasked()==MotionEvent.ACTION_UP){v.performClick();if(onChanged!=null)onChanged.run();return true;}return e.getActionMasked()==MotionEvent.ACTION_CANCEL;
   }
  });
 }
 private void measureExact(View v,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(0,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(0,h),MeasureSpec.EXACTLY));}
 @Override protected void onMeasure(int ws,int hs){
  int W=MeasureSpec.getSize(ws),H=MeasureSpec.getSize(hs);setMeasuredDimension(W,H);fraction=clamp(fraction);
  if(pictureInPicture||miniPlayer){details.setVisibility(GONE);measureExact(video,W,H);measureExact(chat,0,0);measureExact(resizeRail,0,0);return;}
  if(portrait()){
   int vh=Math.min(H,W*9/16);details.setVisibility(VISIBLE);details.measure(MeasureSpec.makeMeasureSpec(W,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(0,H-vh),MeasureSpec.AT_MOST));detailHeight=details.getMeasuredHeight();measureExact(video,W,vh);measureExact(chat,W,Math.max(0,H-vh-detailHeight));measureExact(resizeRail,0,0);return;
  }
  details.setVisibility(GONE);detailHeight=0;int cw=W,ch=H,vw=W,vh=H;
  if(!hidden){if(position.equals("floating")){cw=(int)(W*w);ch=(int)(H*h);}else if(side()){cw=(int)(W*fraction);vw=W-cw;}else{ch=(int)(H*fraction);vh=H-ch;}}
  measureExact(video,vw,vh);measureExact(chat,cw,ch);measureExact(resizeRail,side()?dp(18):W,side()?H:dp(18));
 }
 @Override protected void onLayout(boolean changed,int a,int b,int c,int d){
  int W=getWidth(),H=getHeight();chat.setVisibility(hidden||pictureInPicture||miniPlayer?GONE:VISIBLE);resizeRail.setVisibility(!hidden&&!pictureInPicture&&!miniPlayer&&!position.equals("floating")&&!portrait()?VISIBLE:GONE);
  if(pictureInPicture||miniPlayer){video.layout(0,0,W,H);details.layout(0,0,0,0);chat.layout(0,0,0,0);return;}
  if(portrait()){int vh=video.getMeasuredHeight();video.layout(0,0,W,vh);details.layout(0,vh,W,vh+detailHeight);chat.layout(0,vh+detailHeight,W,H);return;}
  if(hidden){video.layout(0,0,W,H);return;}int cw=chat.getMeasuredWidth(),ch=chat.getMeasuredHeight(),r=dp(9);String p=resolved();
  switch(p){
   case "left":chat.layout(0,0,cw,H);video.layout(cw,0,W,H);resizeRail.layout(cw-r,0,cw+r,H);break;
   case "right":video.layout(0,0,W-cw,H);chat.layout(W-cw,0,W,H);resizeRail.layout(W-cw-r,0,W-cw+r,H);break;
   case "top":chat.layout(0,0,W,ch);video.layout(0,ch,W,H);resizeRail.layout(0,ch-r,W,ch+r);break;
   case "bottom":video.layout(0,0,W,H-ch);chat.layout(0,H-ch,W,H);resizeRail.layout(0,H-ch-r,W,H-ch+r);break;
   default:video.layout(0,0,W,H);int left=Math.max(0,Math.min(W-cw,(int)(x*W))),top=Math.max(0,Math.min(H-ch,(int)(y*H)));chat.layout(left,top,left+cw,top+ch);
  }
 }
}
