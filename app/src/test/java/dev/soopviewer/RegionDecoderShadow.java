package dev.soopviewer;

import android.graphics.*;
import java.io.IOException;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBitmapRegionDecoder;
import org.robolectric.util.ReflectionHelpers;

/** Robolectric's region decoder returns empty pixels even in native graphics mode.
 * Test-only adapter: decode the fixture with BitmapFactory and apply the requested region.
 * The app uses the real Android region decoder, without allocating the full contact sheet. */
@Implements(BitmapRegionDecoder.class)
public class RegionDecoderShadow extends ShadowBitmapRegionDecoder {
 private Bitmap source;
 @Implementation protected static BitmapRegionDecoder newInstance(byte[] data,int offset,int length,boolean shared)throws IOException{
  BitmapRegionDecoder decoder=ReflectionHelpers.callConstructor(BitmapRegionDecoder.class,ReflectionHelpers.ClassParameter.from(long.class,0L));
  RegionDecoderShadow shadow=Shadow.extract(decoder);shadow.source=BitmapFactory.decodeByteArray(data,offset,length);
  if(shadow.source==null)throw new IOException("Invalid image fixture");return decoder;
 }
 @Implementation protected int getWidth(){return source.getWidth();}
 @Implementation protected int getHeight(){return source.getHeight();}
 @Implementation protected Bitmap decodeRegion(Rect region,BitmapFactory.Options options){
  Bitmap result=Bitmap.createBitmap(source,region.left,region.top,region.width(),region.height());int sample=Math.max(1,options.inSampleSize);
  return sample==1?result:Bitmap.createScaledBitmap(result,Math.max(1,result.getWidth()/sample),Math.max(1,result.getHeight()/sample),false);
 }
}
