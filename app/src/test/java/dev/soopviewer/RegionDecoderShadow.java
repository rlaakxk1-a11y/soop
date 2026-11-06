package dev.soopviewer;

import android.graphics.*;
import java.io.*;
import java.util.Arrays;
import javax.imageio.*;
import javax.imageio.stream.ImageInputStream;
import org.robolectric.annotation.*;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBitmapRegionDecoder;
import org.robolectric.util.ReflectionHelpers;

/** Robolectric 4.14's default region decoder returns empty pixels, including in native graphics mode.
 * This test-only adapter implements the Android region contract with the independent JDK decoder. */
@Implements(BitmapRegionDecoder.class)
public class RegionDecoderShadow extends ShadowBitmapRegionDecoder {
 private byte[] bytes;private int width,height;
 @Implementation protected static BitmapRegionDecoder newInstance(byte[] data,int offset,int length,boolean shared)throws IOException{
  BitmapRegionDecoder decoder=ReflectionHelpers.callConstructor(BitmapRegionDecoder.class,ReflectionHelpers.ClassParameter.from(long.class,0L));
  RegionDecoderShadow shadow=Shadow.extract(decoder);shadow.bytes=Arrays.copyOfRange(data,offset,offset+length);
  try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(shadow.bytes))){ImageReader reader=ImageIO.getImageReaders(in).next();try{reader.setInput(in);shadow.width=reader.getWidth(0);shadow.height=reader.getHeight(0);}finally{reader.dispose();}}
  return decoder;
 }
 @Implementation protected int getWidth(){return width;}
 @Implementation protected int getHeight(){return height;}
 @Implementation protected Bitmap decodeRegion(Rect region,BitmapFactory.Options options){
  try(ImageInputStream in=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))){ImageReader reader=ImageIO.getImageReaders(in).next();try{reader.setInput(in);ImageReadParam param=reader.getDefaultReadParam();param.setSourceRegion(new java.awt.Rectangle(region.left,region.top,region.width(),region.height()));int sample=Math.max(1,options.inSampleSize);param.setSourceSubsampling(sample,sample,0,0);java.awt.image.BufferedImage decoded=reader.read(0,param);int w=decoded.getWidth(),h=decoded.getHeight();return Bitmap.createBitmap(decoded.getRGB(0,0,w,h,null,0,w),w,h,Bitmap.Config.ARGB_8888);}finally{reader.dispose();}}
  catch(IOException e){throw new IllegalStateException(e);}
 }
}
