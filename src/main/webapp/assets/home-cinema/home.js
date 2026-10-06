/* Home opening only. Scroll drives every pixel; there is no autonomous playback. */
(() => {
  'use strict';
  const root=document.querySelector('.lumina-home');if(!root)return;
  const journey=root.querySelector('.ld-journey'),stage=root.querySelector('.ld-stage'),canvas=root.querySelector('.ld-canvas');
  const ctx=canvas.getContext('2d',{alpha:false,desynchronized:true});
  const loader=root.querySelector('.ld-loader'),retry=root.querySelector('.ld-retry');
  const buffer=root.querySelector('.ld-buffer');buffer.hidden=true;
  if(!ctx){loader.querySelector('p').textContent='The library is ready below. This browser cannot show the journey.';return;}
  journey.classList.add('ld-enhanced');
  const chapters=[...root.querySelectorAll('.ld-chapter')],states=chapters.map(()=>({active:null}));
  const layers=chapters.map(chapter=>{
    const title=[...chapter.querySelectorAll('.ld-title-ink')];
    const items=[{node:chapter.querySelector('.ld-line'),at:0,kind:'line'},
      {node:chapter.querySelector('.ld-eyebrow'),at:.025},
      ...title.map((node,i)=>({node,at:.065+i*.045,kind:'title'}))];
    let at=.065+title.length*.045;
    for(const selector of ['p','.ld-tags','.ld-actions','.ld-cta','.ld-enter']){
      const node=chapter.querySelector(selector);if(node){items.push({node,at});at+=.025;}
    }
    return items.filter(item=>item.node);
  });
  const rail=root.querySelector('.ld-rail'),fill=rail.querySelector('i'),bottom=root.querySelector('.ld-bottom'),sceneLabel=root.querySelector('.ld-scene');
  const total=1344,perScene=192,mobile=matchMedia('(max-width:700px)').matches;
  const reduced=matchMedia('(prefers-reduced-motion:reduce)').matches,capacity=mobile?28:44;
  const cache=new Map(),atlases=new Map(),pending=new Map(),failures=new Map(),downloaded=new Set();
  const lifetime=new AbortController();
  let queue=[],active=0,ready=false,disposed=false,current=-1,position=0,direction=1;
  let raf=0,startY=0,travel=1,width=0,height=0,lastPosition=-1,lastSignature='',inJourney=true,scene=-1;
  let atlasActive=0,atlasQueue=[0,1,2,3,4,5,6],warmIndex=0,warmTimer;
  const atlasFailures=new Set();
  const clamp=n=>Math.max(0,Math.min(1,n));
  const ease=n=>{const t=clamp(n);return t*t*(3-2*t);};
  const frameURL=i=>`${journey.dataset.frameRoot}/scene-${Math.floor(i/perScene)+1}/frame_${String(i%perScene+1).padStart(4,'0')}.webp`;
  const base=journey.dataset.frameRoot.replace(/\/frames\/?$/,'');
  const close=image=>{if(image&&typeof image.close==='function')image.close();};
  async function decode(blob,preview=false){
    if(typeof createImageBitmap==='function')return createImageBitmap(blob,!preview&&mobile?{resizeWidth:960,resizeQuality:'medium'}:undefined);
    const objectURL=URL.createObjectURL(blob);
    try{const image=new Image();image.decoding='async';await new Promise((resolve,reject)=>{image.onload=resolve;image.onerror=reject;image.src=objectURL;});return image;}finally{URL.revokeObjectURL(objectURL);}
  }
  function updateLoader(){
    if(ready||disposed)return;
    const hasOpening=cache.has(0)||(failures.get(0)||0)>=2;
    const amount=Math.round((atlases.size+(hasOpening?1:0))/8*100);
    loader.querySelector('.ld-load-track>span').style.width=amount+'%';loader.querySelector('small').textContent=amount+'%';
    if(atlases.size===7&&hasOpening){
      ready=true;loader.classList.add('ld-loaded');loader.setAttribute('aria-hidden','true');
      loader.querySelector('a').tabIndex=-1;retry.tabIndex=-1;trim();schedule();
    }
  }
  function loadAtlases(){
    while(atlasActive<3&&atlasQueue.length&&!disposed){
      const index=atlasQueue.shift();atlasActive++;
      (async()=>{
        try{
          const response=await fetch(`${base}/previews/scene-${index+1}.webp`,{cache:'force-cache',signal:lifetime.signal});
          if(!response.ok)throw Error('Preview unavailable');
          const image=await decode(await response.blob(),true);
          if(disposed){close(image);return;}
          atlases.set(index,image);atlasFailures.delete(index);updateLoader();schedule();
        }catch(error){if(!disposed){atlasFailures.add(index);retry.hidden=false;loader.querySelector('p').textContent='A scene could not load. Retry, or continue to the library below.';}}
        finally{atlasActive--;loadAtlases();}
      })();
    }
  }
  function protectedIndices(){
    const set=new Set([current,current+1]);
    const boundary=Math.round(position/perScene)*perScene;
    if(boundary>0&&boundary<total&&Math.abs(position-boundary)<6){set.add(boundary-1);set.add(boundary);}
    if(!ready)set.add(0);return set;
  }
  function trim(){
    const protectedSet=protectedIndices();
    while(cache.size>capacity){
      const candidates=[...cache.keys()].filter(i=>!protectedSet.has(i));
      if(!candidates.length)break;
      const farthest=candidates.sort((a,b)=>Math.abs(b-current)-Math.abs(a-current))[0];close(cache.get(farthest));cache.delete(farthest);
    }
    canvas.dataset.cachedFrames=String(cache.size);
  }
  function prioritize(){
    queue=[...protectedIndices()];
    if(!ready)for(let i=0;i<32;i++)queue.push(i);
    const ahead=mobile?20:32,behind=mobile?4:8;
    for(let i=1;i<=ahead;i++){queue.push(current+i*direction);if(i<=behind)queue.push(current-i*direction);}
    queue=[...new Set(queue)].filter(i=>i>=0&&i<total&&!cache.has(i)&&!pending.has(i)&&(failures.get(i)||0)<2);
  }
  function pump(){
    while(active<4&&queue.length&&!disposed){
      const index=queue.shift();if(cache.has(index)||pending.has(index))continue;
      const controller=new AbortController(),abort=()=>controller.abort();lifetime.signal.addEventListener('abort',abort,{once:true});
      pending.set(index,controller);active++;
      (async()=>{
        try{
          const response=await fetch(frameURL(index),{cache:'force-cache',signal:controller.signal});if(!response.ok)throw Error('Detail unavailable');
          const image=await decode(await response.blob());
          if(disposed){close(image);return;}
          cache.set(index,image);downloaded.add(index);updateLoader();if(ready)trim();
          if(protectedIndices().has(index))schedule();
        }catch(error){if(!disposed&&error.name!=='AbortError'){failures.set(index,(failures.get(index)||0)+1);updateLoader();}}
        finally{lifetime.signal.removeEventListener('abort',abort);pending.delete(index);active--;if(!disposed){prioritize();pump();}}
      })();
    }
  }
  // Every scene has 49 small preview frames resident in a single atlas. If a
  // detailed image is late, scrolling still renders the correct moment instantly.
  function samples(at,weight,layers){
    at=Math.max(0,Math.min(total-1,at));
    const index=Math.floor(at),next=Math.min(total-1,index+1),mix=at-index;
    const left=cache.get(index),right=cache.get(next);
    if(left&&(mix<.0001||right)){
      layers.push({image:left,weight:weight*(1-mix),key:'d'+index});
      if(mix>.0001)layers.push({image:right,weight:weight*mix,key:'d'+next});return;
    }
    const sceneIndex=Math.floor(at/perScene),local=at-sceneIndex*perScene,atlas=atlases.get(sceneIndex);
    if(!atlas)return;
    const a=Math.min(47,Math.floor(local/4)),b=a+1,from=a*4,to=b===48?191:b*4,t=clamp((local-from)/(to-from));
    for(const [slot,alpha] of [[a,1-t],[b,t]])if(alpha>.0001)layers.push({image:atlas,sx:slot%7*256,sy:Math.floor(slot/7)*144,sw:256,sh:144,weight:weight*alpha,key:'p'+sceneIndex+':'+slot});
  }
  function draw(){
    if(atlases.size!==7)return;
    const layers=[],boundary=Math.round(position/perScene)*perScene;
    if(!reduced&&boundary>0&&boundary<total&&Math.abs(position-boundary)<6){
      const dissolve=ease((position-boundary+6)/12);
      samples(Math.min(position,boundary-1),1-dissolve,layers);samples(Math.max(position,boundary),dissolve,layers);
    }else samples(position,1,layers);
    const w=stage.clientWidth,h=stage.clientHeight;
    // Keep high-DPI screens from demanding two 4K canvas paints per scroll tick.
    const dpr=Math.min(devicePixelRatio||1,2,Math.sqrt(2400000/(w*h)));
    const pixelW=Math.max(1,Math.round(w*dpr)),pixelH=Math.max(1,Math.round(h*dpr));
    const resized=canvas.width!==pixelW||canvas.height!==pixelH||w!==width||h!==height;
    const signature=layers.map(layer=>layer.key).join('|');
    if(position===lastPosition&&signature===lastSignature&&!resized)return;
    if(resized){canvas.width=pixelW;canvas.height=pixelH;width=w;height=h;}
    ctx.setTransform(dpr,0,0,dpr,0,0);ctx.globalCompositeOperation='source-over';ctx.globalAlpha=1;ctx.fillStyle='#07111f';ctx.fillRect(0,0,w,h);
    ctx.imageSmoothingEnabled=true;ctx.imageSmoothingQuality='medium';
    // Add weighted layers in one synchronous paint; no intermediate black canvas
    // is ever presented. Weights sum to one, including at scene dissolves.
    ctx.fillStyle='#000';ctx.fillRect(0,0,w,h);ctx.globalCompositeOperation='lighter';
    for(const layer of layers){
      const iw=layer.sw||layer.image.naturalWidth||layer.image.width,ih=layer.sh||layer.image.naturalHeight||layer.image.height;
      const scale=Math.max(w/iw,h/ih);ctx.globalAlpha=layer.weight;
      ctx.drawImage(layer.image,layer.sx||0,layer.sy||0,iw,ih,(w-iw*scale)/2,(h-ih*scale)/2,iw*scale,ih*scale);
    }
    ctx.globalAlpha=1;ctx.globalCompositeOperation='source-over';
    lastPosition=position;lastSignature=signature;canvas.dataset.frame=String(current);canvas.dataset.framePosition=position.toFixed(4);
    canvas.dataset.quality=layers.some(layer=>layer.key[0]==='p')?'preview':'detail';
  }
  function render(){
    raf=0;if(disposed)return;
    const progress=clamp((scrollY-startY)/travel);position=Math.min(total-1,progress*total);
    const next=Math.floor(position);inJourney=scrollY<startY+journey.offsetHeight&&scrollY+innerHeight>startY;
    if(next!==current){
      if(current>=0)direction=next>current?1:-1;current=next;canvas.dataset.targetFrame=String(current);
      if(ready)for(const [index,controller] of pending)if(Math.abs(index-current)>48)controller.abort();
      prioritize();pump();
    }
    chapters.forEach((chapter,index)=>{
      const local=progress*7-index,activeChapter=local>=0&&(local<1||(index===6&&progress===1));
      const state=states[index],exit=index===6?1:1-ease((local-.80)/.18);
      const visible=local>=0&&local<=1;
      chapter.style.visibility=visible?'visible':'hidden';
      chapter.style.opacity=visible?String(reduced?(index===0?1:ease(local/.16))*exit:1):'0';
      chapter.style.transform='none';
      if(visible)for(const layer of layers[index]){
        const enter=index===0?1:ease((local-layer.at)/.18);
        const alpha=reduced?1:enter*exit;
        layer.node.style.opacity=String(alpha);
        layer.node.style.transform=reduced?'none':layer.kind==='line'
          ?`scaleX(${enter})`
          :`translateY(${(1-enter)*(layer.kind==='title'?38:12)-(1-exit)*8}px)`;
      }
      if(state.active!==activeChapter){
        chapter.setAttribute('aria-hidden',String(!activeChapter));
        chapter.inert=!activeChapter;
        chapter.querySelectorAll('a').forEach(link=>link.tabIndex=activeChapter?0:-1);
        state.active=activeChapter;
      }
    });
    const activeIndex=Math.min(6,Math.floor(progress*7));
    root.style.setProperty('--ld-right-shade',chapters[activeIndex].classList.contains('ld-right')?'1':'0');
    const now=Math.min(7,Math.floor(progress*7)+1);if(now!==scene){scene=now;sceneLabel.textContent=String(now).padStart(2,'0');}
    fill.style.transform=`scaleY(${progress})`;rail.style.opacity=bottom.style.opacity=String(clamp((1-progress)/.03));draw();
  }
  function schedule(){if(!raf&&!disposed)raf=requestAnimationFrame(render);}
  function measure(){startY=journey.getBoundingClientRect().top+scrollY;travel=Math.max(1,journey.offsetHeight-stage.offsetHeight);schedule();}
  async function warm(){
    if(disposed||navigator.connection?.saveData||warmIndex>=total)return;
    if(ready&&!active&&!document.hidden&&inJourney){
      while((downloaded.has(warmIndex)||pending.has(warmIndex))&&warmIndex<total)warmIndex++;
      if(warmIndex<total){const index=warmIndex++;try{const response=await fetch(frameURL(index),{cache:'force-cache',signal:lifetime.signal});if(response.ok){await response.arrayBuffer();downloaded.add(index);}}catch{/* Detail requests can retry. */}}
    }
    if(!disposed)warmTimer=setTimeout(warm,80);
  }
  retry.addEventListener('click',()=>{atlasQueue=[...atlasFailures];atlasFailures.clear();failures.clear();retry.hidden=true;loader.querySelector('p').textContent='Preparing your experience';loadAtlases();prioritize();pump();});
  root.querySelectorAll('a[href="#home-content"]').forEach(link=>link.addEventListener('click',event=>{event.preventDefault();const content=document.querySelector('#home-content');scrollTo({top:content.getBoundingClientRect().top+scrollY,behavior:'instant'});content.focus({preventScroll:true});}));
  const resize=new ResizeObserver(measure);resize.observe(stage);
  for(const selector of ['.topbar','.flash']){const element=document.querySelector(selector);if(element)resize.observe(element);}
  window.addEventListener('scroll',schedule,{passive:true});window.addEventListener('resize',measure,{passive:true});window.addEventListener('pageshow',measure);
  window.addEventListener('pagehide',event=>{if(event.persisted)return;disposed=true;lifetime.abort();resize.disconnect();cancelAnimationFrame(raf);clearTimeout(warmTimer);cache.forEach(close);atlases.forEach(close);cache.clear();atlases.clear();window.removeEventListener('scroll',schedule);window.removeEventListener('resize',measure);window.removeEventListener('pageshow',measure);});
  document.fonts?.ready.then(()=>{if(!disposed)measure();});
  loadAtlases();measure();warmTimer=setTimeout(warm,1000);
})();
