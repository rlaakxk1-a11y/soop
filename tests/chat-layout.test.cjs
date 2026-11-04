const {test}=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const {JSDOM}=require('jsdom');
const script=fs.readFileSync('app/src/main/assets/chat-layout.js','utf8');
function fixture(found=true){
 const dom=new JSDOM('<!doctype html><head></head><body>'+(found?'<aside data-soop-viewer-chat>hello</aside>':'')+'</body>',{url:'https://m.sooplive.co.kr/',runScripts:'outside-only'});
 dom.window.eval(script);return dom;
}
test('missing chat is reported; late arriving chat detected',async()=>{
 const d=fixture(false);assert.equal(d.window.viewerApply(false,35),false);
 const chat=d.window.document.createElement('aside');chat.setAttribute('data-soop-viewer-chat','');d.window.document.body.append(chat);
 await new Promise(r=>setTimeout(r,300));assert.ok(chat.hasAttribute('data-viewer-chat'));d.window.close();
});
test('hide and restore keeps original message node',()=>{
 const d=fixture();const w=d.window,chat=w.document.querySelector('aside');
 w.viewerApply(true,35);assert.match(w.document.getElementById('viewer-chat-style').textContent,/display:none/);
 w.viewerApply(false,35);assert.equal(w.document.querySelector('aside'),chat);assert.equal(chat.textContent,'hello');
 assert.match(w.document.getElementById('viewer-chat-style').textContent,/display:block/);d.window.close();
});
test('all placements and range clamping',()=>{
 const d=fixture();const w=d.window;w.innerWidth=1000;w.innerHeight=800;
 w.viewerApply(false,100,'left');let css=w.document.getElementById('viewer-chat-style').textContent;assert.match(css,/width:650px/);
 w.viewerApply(false,-5,'right');css=w.document.getElementById('viewer-chat-style').textContent;assert.match(css,/left:850px/);
 w.viewerApply(false,50,'top');assert.match(w.document.getElementById('viewer-chat-style').textContent,/top:0px/);
 w.viewerApply(false,50,'bottom');assert.match(w.document.getElementById('viewer-chat-style').textContent,/top:400px/);d.window.close();
});
test('draggable controls are not duplicated on reinjection',()=>{
 const d=fixture();const w=d.window;for(let n=0;n<20;n++){w.eval(script);w.viewerApply(false,35);}
 assert.equal(w.document.querySelectorAll('#viewer-drag').length,1);assert.equal(w.document.querySelectorAll('#viewer-resize').length,1);d.window.close();
});
test('cosmetic ads toggle restores stylesheet',()=>{
 const d=fixture();const w=d.window;w.viewerApply(false,35,'auto',true);assert.match(w.document.getElementById('viewer-ad-style').textContent,/adsbygoogle/);
 w.viewerApply(false,35,'auto',false);assert.equal(w.document.getElementById('viewer-ad-style').textContent,'');d.window.close();
});
test('floating geometry is constrained to viewport',()=>{
 const d=fixture();const w=d.window;w.localStorage.setItem('viewer.geometry',JSON.stringify({x:9999,y:9999,w:9999,h:9999}));
 // Reload the controller to read persisted geometry.
 delete w.viewerController;w.eval(script);w.viewerApply(false,35,'floating');
 const css=w.document.getElementById('viewer-chat-style').textContent;assert.match(css,/left:0px/);assert.match(css,/top:0px/);assert.match(css,/width:1024px/);d.window.close();
});
