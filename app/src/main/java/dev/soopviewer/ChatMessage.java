package dev.soopviewer;
public final class ChatMessage {
 public final long timeMs;public final String nick,text;
 public ChatMessage(long timeMs,String nick,String text){this.timeMs=timeMs;this.nick=nick;this.text=text;}
}
