package dev.soopviewer;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.*;

public final class ChatParser {
 public static List<ChatMessage> xml(String source)throws Exception{
  if(source.toUpperCase(Locale.ROOT).contains("<!DOCTYPE")||source.toUpperCase(Locale.ROOT).contains("<!ENTITY"))throw new IOException("허용하지 않는 XML 형식입니다");
  DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();factory.setExpandEntityReferences(false);
  Document doc=factory.newDocumentBuilder().parse(new ByteArrayInputStream(source.getBytes(StandardCharsets.UTF_8)));
  List<ChatMessage> out=new ArrayList<>();NodeList nodes=doc.getDocumentElement().getChildNodes();
  for(int i=0;i<nodes.getLength();i++)if(nodes.item(i) instanceof Element){Element e=(Element)nodes.item(i);if(!e.getTagName().equals("chat")&&!e.getTagName().equals("ogq"))continue;
   try {double sec=Double.parseDouble(value(e,"t"));if(!Double.isFinite(sec)||sec<0)continue;String name=value(e,e.getTagName().equals("ogq")?"sn":"n"),text=value(e,"m");
    if(e.getTagName().equals("ogq"))text="[스티커] "+text;if(!text.isEmpty())out.add(new ChatMessage((long)(sec*1000),name,text));
   }catch(NumberFormatException ignored){}
  }out.sort(Comparator.comparingLong(m->m.timeMs));return out;
 }
 static String value(Element e,String tag){NodeList list=e.getElementsByTagName(tag);return list.getLength()==0?"":list.item(0).getTextContent();}
 public static List<ChatMessage> packets(byte[] bytes){
  List<ChatMessage> out=new ArrayList<>();int pos=0;
  while(pos+14<=bytes.length){
   if(bytes[pos]!=27||bytes[pos+1]!=9)break;
   int code,len;try{code=Integer.parseInt(new String(bytes,pos+2,4,StandardCharsets.US_ASCII));len=Integer.parseInt(new String(bytes,pos+6,6,StandardCharsets.US_ASCII));}catch(Exception e){break;}
   if(len<0||len>bytes.length-pos-14)break;
   if(code==5){String[] parts=new String(bytes,pos+14,len,StandardCharsets.UTF_8).split("\f",-1);if(parts.length>6&&!parts[1].isEmpty()&&!parts[6].isEmpty())out.add(new ChatMessage(-1,parts[6],parts[1]));}
   pos+=14+len;
  }return out;
 }
 public static String packet(int code,String body){int bytes=body.getBytes(StandardCharsets.UTF_8).length;if(bytes>999999)throw new IllegalArgumentException();return String.format(Locale.ROOT,"\u001b\t%04d%06d00%s",code,bytes,body);}
}
