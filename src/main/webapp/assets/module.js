(()=>{'use strict';const ctx=document.body.dataset.context;const $=s=>document.querySelector(s),$$=s=>document.querySelectorAll(s);
$('.menu-toggle')?.addEventListener('click',e=>{const open=$('.public-nav').classList.toggle('open');e.currentTarget.setAttribute('aria-expanded',String(open));});
$$('[data-dismiss]').forEach(b=>b.addEventListener('click',()=>b.parentElement.remove()));
$$('[data-print]').forEach(b=>b.addEventListener('click',()=>window.print()));
$$('[data-back]').forEach(b=>b.addEventListener('click',()=>history.length>1?history.back():location.assign(ctx+'/home')));
$$('form[data-confirm]').forEach(f=>f.addEventListener('submit',e=>{if(!confirm(f.dataset.confirm))e.preventDefault();}));
$$('.table-search').forEach(input=>input.addEventListener('input',()=>{const query=input.value.toLocaleLowerCase();input.closest('.panel').querySelectorAll('tbody tr').forEach(row=>{row.hidden=!row.textContent.toLocaleLowerCase().includes(query);});}));
const pagination=$('.pagination');if(pagination){const page=Number(pagination.dataset.page),total=Number(pagination.dataset.total);$$('[data-page-step]').forEach(button=>{const next=page+Number(button.dataset.pageStep);button.disabled=next<1||next>Math.max(1,Math.ceil(total/12));button.addEventListener('click',()=>{const url=new URL(location.href);url.searchParams.set('page',next);location.assign(url);});});}
$$('.export-link').forEach(a=>{const params=new URLSearchParams(location.search);params.set('format',a.dataset.format);a.href=ctx+'/export?'+params.toString();});
const cover=$('#cover-input');cover?.addEventListener('change',()=>{const file=cover.files[0];if(file&&['image/jpeg','image/png'].includes(file.type)){const url=URL.createObjectURL(file);const img=$('#cover-preview');img.onload=()=>URL.revokeObjectURL(url);img.src=url;}});
async function notifications(){if(document.hidden||document.body.dataset.signedIn!=='true')return;try{const res=await fetch(ctx+'/api/notifications');if(!res.ok||!res.headers.get('content-type')?.includes('application/json'))return;const data=await res.json(),badge=$('#notification-count');if(badge){badge.textContent=data.unread;badge.hidden=!data.unread;}}catch(e){}}notifications();setInterval(notifications,30000);
})();
