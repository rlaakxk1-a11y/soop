(() => {
 if (window.viewerController) return;
 const selectors = ['[data-soop-viewer-chat]', '#chat_area', '#chatting_area', '.chatting_area', '.chat_area', '#chatting', '.vod_chatting'];
 const attr = 'data-viewer-chat';
 let state = {hidden:false, percent:35, position:'auto', ads:true};
 let panel, style, handle, grip, observer, timer, dragging=false;
 let geometry;
 try {geometry=JSON.parse(localStorage.getItem('viewer.geometry') || 'null');} catch (_) {}
 const clamp=(n,min,max)=>Math.max(min,Math.min(max,n));
 function save(){try{localStorage.setItem('viewer.geometry',JSON.stringify(geometry));}catch(_) {}}
 function removeControls(){handle?.remove();grip?.remove();handle=grip=null;}
 function controls(){
  if(handle?.isConnected)return;
  removeControls();
  handle=document.createElement('button'); handle.id='viewer-drag';handle.textContent='⠿ 채팅 이동';handle.setAttribute('aria-label','드래그해서 채팅창 이동');
  grip=document.createElement('button');grip.id='viewer-resize';grip.textContent='◢';grip.setAttribute('aria-label','드래그해서 채팅창 크기 조절');
  document.body.append(handle,grip);
  for(const [element,resize] of [[handle,false],[grip,true]]){
   let start;
   element.addEventListener('pointerdown',e=>{
    const r=panel.getBoundingClientRect();start={x:e.clientX,y:e.clientY,left:r.left,top:r.top,width:r.width,height:r.height};
    dragging=true;element.setPointerCapture(e.pointerId);e.preventDefault();
   });
   element.addEventListener('pointermove',e=>{
    if(!dragging || !start)return;
    const dx=e.clientX-start.x,dy=e.clientY-start.y;
    geometry=resize ? {x:start.left,y:start.top,w:start.width+dx,h:start.height+dy} : {x:start.left+dx,y:start.top+dy,w:start.width,h:start.height};
    state.position='floating';render();e.preventDefault();
   });
   const end=()=>{start=null;dragging=false;save();};
   element.addEventListener('pointerup',end);element.addEventListener('pointercancel',end);
  }
 }
 function render(){
  if(!panel?.isConnected)return false;
  if(!style){style=document.createElement('style');style.id='viewer-chat-style';document.head.appendChild(style);}
  const W=innerWidth,H=innerHeight,p=clamp(Number(state.percent)||35,15,65)/100;
  let pos=state.position==='auto'?(W>H?'right':'bottom'):state.position;
  let x=0,y=0,w=W,h=H;
  if(pos==='left'||pos==='right'){w=Math.round(W*p);x=pos==='right'?W-w:0;}
  else if(pos==='top'||pos==='bottom'){h=Math.round(H*p);y=pos==='bottom'?H-h:0;}
  else {
   const g=geometry||{x:W*.1,y:H*.3,w:W*.7,h:H*.4};
   w=clamp(g.w,Math.min(180,W),W);h=clamp(g.h,Math.min(120,H),H);x=clamp(g.x,0,W-w);y=clamp(g.y,0,H-h);
   geometry={x,y,w,h};
  }
  const css=`[${attr}]{display:${state.hidden?'none':'block'}!important;position:fixed!important;inset:auto!important;left:${x}px!important;top:${y}px!important;width:${w}px!important;height:${h}px!important;max-width:none!important;max-height:none!important;min-width:0!important;min-height:0!important;transform:none!important;z-index:2147483645!important;box-sizing:border-box!important;overflow:auto!important;background:#141c2b!important;padding-top:32px!important;border:1px solid #5e819f!important;border-radius:10px!important}
  #viewer-drag,#viewer-resize{position:fixed!important;z-index:2147483646!important;display:${state.hidden?'none':'block'}!important;background:#25445d!important;color:white!important;border:0!important;touch-action:none!important;user-select:none!important;margin:0!important;padding:0!important;box-sizing:border-box!important;font:14px sans-serif!important}
  #viewer-drag{left:${x+4}px!important;top:${y+2}px!important;width:${Math.min(140,w-8)}px!important;height:28px!important;border-radius:6px!important;cursor:move!important}
  #viewer-resize{left:${x+w-32}px!important;top:${y+h-32}px!important;width:30px!important;height:30px!important;cursor:nwse-resize!important}`;
  if(style.textContent!==css)style.textContent=css;
  return true;
 }
 function detect(){
  if(panel?.isConnected)return render();
  panel=selectors.map(s=>document.querySelector(s)).find(Boolean);
  if(!panel){removeControls();return false;}
  panel.setAttribute(attr,'');controls();return render();
 }
 window.viewerController={get state(){return {...state};}};
 window.viewerApply=(hidden,percent,position='auto',ads=true)=>{
  state={hidden,percent,position:['auto','left','right','top','bottom','floating'].includes(position)?position:'auto',ads};
  let adStyle=document.getElementById('viewer-ad-style');
  if(!adStyle){adStyle=document.createElement('style');adStyle.id='viewer-ad-style';document.head.appendChild(adStyle);}
  const css=ads?'ins.adsbygoogle,iframe[src*="doubleclick.net"],iframe[src*="googlesyndication.com"],[data-ad-slot]{display:none!important}':'';
  if(adStyle.textContent!==css)adStyle.textContent=css;
  return detect();
 };
 observer=new MutationObserver(records=>{
  if(dragging)return;
  const external=records.some(r=>![style,handle,grip,document.getElementById('viewer-ad-style')].includes(r.target));
  if(!external)return;
  clearTimeout(timer);timer=setTimeout(detect,200);
 });
 observer.observe(document.documentElement,{childList:true,subtree:true});
 window.addEventListener('resize',()=>{render();});
})();
