package dev.soopviewer;
import android.content.Context;
import android.view.*;
import android.widget.*;

/** Native split layout; in floating mode only the chat overlays the video. */
public final class WatchLayout extends FrameLayout {
 public final FrameLayout video;public final LinearLayout chat;
 public boolean hidden=false;public String position="auto";public float fraction=.35f;
 Runnable onChanged;
 float x=.08f,y=.35f,w=.7f,h=.4f;
 public WatchLayout(Context c){super(c);video=new FrameLayout(c);chat=new LinearLayout(c);chat.setOrientation(LinearLayout.VERTICAL);addView(video);addView(chat);}
 public void placement(String value){position=value;requestLayout();}
 public void size(float value){fraction=Math.max(.15f,Math.min(.65f,value));if(position.equals("floating"))position="auto";requestLayout();}
 public void gesture(View handle,boolean resize){
  handle.setOnTouchListener(new View.OnTouchListener(){float startX,startY,left,top,width,height;
   public boolean onTouch(View v,android.view.MotionEvent e){
    if(e.getActionMasked()==MotionEvent.ACTION_DOWN){startX=e.getRawX();startY=e.getRawY();left=chat.getLeft();top=chat.getTop();width=chat.getWidth();height=chat.getHeight();v.getParent().requestDisallowInterceptTouchEvent(true);return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_MOVE){position="floating";float W=Math.max(1,getWidth()),H=Math.max(1,getHeight()),dx=e.getRawX()-startX,dy=e.getRawY()-startY;
     float nw=resize?width+dx:width,nh=resize?height+dy:height;nw=Math.max(Math.min(180*getResources().getDisplayMetrics().density,W),Math.min(W,nw));nh=Math.max(Math.min(120*getResources().getDisplayMetrics().density,H),Math.min(H,nh));
     x=Math.max(0,Math.min(W-nw,resize?left:left+dx))/W;y=Math.max(0,Math.min(H-nh,resize?top:top+dy))/H;w=nw/W;h=nh/H;requestLayout();return true;}
    if(e.getActionMasked()==MotionEvent.ACTION_UP){v.performClick();if(onChanged!=null)onChanged.run();return true;}return e.getActionMasked()==MotionEvent.ACTION_CANCEL;
   }
  });
 }
 @Override protected void onMeasure(int ws,int hs){int W=MeasureSpec.getSize(ws),H=MeasureSpec.getSize(hs);setMeasuredDimension(W,H);String p=resolved(W,H);int cw=W,ch=H,vw=W,vh=H;
  if(!hidden){if(p.equals("floating")){cw=(int)(W*w);ch=(int)(H*h);}else if(p.equals("left")||p.equals("right")){cw=(int)(W*fraction);vw=W-cw;}else{ch=(int)(H*fraction);vh=H-ch;}}
  video.measure(MeasureSpec.makeMeasureSpec(vw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(vh,MeasureSpec.EXACTLY));chat.measure(MeasureSpec.makeMeasureSpec(cw,MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(ch,MeasureSpec.EXACTLY));
 }
 private String resolved(int W,int H){return position.equals("auto")?(W>H?"right":"bottom"):position;}
 @Override protected void onLayout(boolean changed,int a,int b,int c,int d){int W=getWidth(),H=getHeight();chat.setVisibility(hidden?GONE:VISIBLE);if(hidden){video.layout(0,0,W,H);return;}int cw=chat.getMeasuredWidth(),ch=chat.getMeasuredHeight();
  switch(resolved(W,H)){
   case "left":chat.layout(0,0,cw,H);video.layout(cw,0,W,H);break;
   case "right":video.layout(0,0,W-cw,H);chat.layout(W-cw,0,W,H);break;
   case "top":chat.layout(0,0,W,ch);video.layout(0,ch,W,H);break;
   case "bottom":video.layout(0,0,W,H-ch);chat.layout(0,H-ch,W,H);break;
   default:video.layout(0,0,W,H);int left=Math.max(0,Math.min(W-cw,(int)(x*W))),top=Math.max(0,Math.min(H-ch,(int)(y*H)));chat.layout(left,top,left+cw,top+ch);
  }
 }
}
