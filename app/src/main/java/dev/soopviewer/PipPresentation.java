package dev.soopviewer;

import android.view.*;
import android.widget.FrameLayout;
import java.util.*;

/** Temporarily gives the actual video the entire Activity, including from the in-app mini-player. */
final class PipPresentation {
 private final WatchLayout watch;
 private final ViewGroup.LayoutParams layout;
 private final float translationX,translationY;
 private final Map<View,Integer> visibility=new LinkedHashMap<>();
 PipPresentation(ViewGroup body,WatchLayout watch,View header,View nav){
  this.watch=watch;layout=watch.getLayoutParams();translationX=watch.getTranslationX();translationY=watch.getTranslationY();
  hide(header);hide(nav);
  for(int i=0;i<body.getChildCount();i++)if(body.getChildAt(i)!=watch)hide(body.getChildAt(i));
  watch.setTranslationX(0);watch.setTranslationY(0);watch.pictureInPicture=true;
  watch.setLayoutParams(new FrameLayout.LayoutParams(-1,-1));watch.requestLayout();
 }
 private void hide(View view){visibility.put(view,view.getVisibility());view.setVisibility(View.GONE);}
 void restore(){
  watch.pictureInPicture=false;watch.setLayoutParams(layout);watch.setTranslationX(translationX);watch.setTranslationY(translationY);
  for(Map.Entry<View,Integer> entry:visibility.entrySet())entry.getKey().setVisibility(entry.getValue());
  watch.requestLayout();
 }
}
