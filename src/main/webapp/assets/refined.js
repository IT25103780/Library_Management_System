/* Lumina enhancements. Payment success is rendered only by the verified server route. */
(() => {
 'use strict';
 const reduced = matchMedia('(prefers-reduced-motion: reduce)').matches;
 const common = new Set(['password123','password1234','qwerty12345','welcome123','letmein1234','1234567890a']);
 const rules = [
  ['10–128 characters', v => v.length >= 10 && v.length <= 128],
  ['One uppercase letter', v => /[A-Z]/.test(v)],
  ['One lowercase letter', v => /[a-z]/.test(v)],
  ['One number', v => /[0-9]/.test(v)],
  ['One special character', v => /[^A-Za-z0-9\s]/.test(v)],
  ['Not a common password', v => !!v && !common.has(v.toLowerCase().replace(/[^a-z0-9]/g,''))]
 ];
 document.querySelectorAll('[data-strong-password]').forEach((input, index) => {
  input.setAttribute('aria-label',input.closest('label')?.firstChild?.textContent?.trim() || 'New password');
  const wrap = document.createElement('div'); wrap.className = 'password-wrap';
  input.parentNode.insertBefore(wrap,input); wrap.append(input);
  const toggle=document.createElement('button');toggle.type='button';toggle.textContent='Show';toggle.setAttribute('aria-label','Show password');
  toggle.addEventListener('click',()=>{const show=input.type==='password';input.type=show?'text':'password';toggle.textContent=show?'Hide':'Show';toggle.setAttribute('aria-label',show?'Hide password':'Show password');});wrap.append(toggle);
  const help=document.createElement('div');help.className='password-guidance';help.id='password-help-'+index;
  help.innerHTML='<div class="password-strength" aria-hidden="true"><span></span></div><small class="password-status" role="status" aria-live="polite">Choose a strong password</small><ul class="password-rules"></ul>';
  wrap.after(help);input.setAttribute('aria-describedby',help.id);
  const list=help.querySelector('ul');rules.forEach(([label])=>{const li=document.createElement('li');li.textContent=label;list.append(li);});
  const confirm=input.form.querySelector('[name="confirm_password"]');
  let confirmation;
  if(confirm){confirm.setAttribute('aria-label',confirm.closest('label')?.firstChild?.textContent?.trim() || 'Confirm password');confirmation=document.createElement('small');confirmation.className='password-match';confirmation.id='password-match-'+index;confirmation.setAttribute('aria-live','polite');confirm.after(confirmation);confirm.setAttribute('aria-describedby',confirmation.id);}
  const match=()=>{if(!confirm)return;const mismatch=!!confirm.value&&confirm.value!==input.value;confirm.setCustomValidity(mismatch?'Passwords do not match.':'');confirmation.textContent=confirm.value?(mismatch?'Passwords do not match.':'Passwords match.') : '';confirmation.classList.toggle('matched',!!confirm.value&&!mismatch);};
  const update=()=>{const v=input.value;let count=0;rules.forEach(([,test],i)=>{const pass=test(v);list.children[i].classList.toggle('met',pass);if(pass)count++;});const strong=count===rules.length;help.dataset.state=!v?'empty':strong?'strong':'weak';help.querySelector('.password-strength span').style.width=v?Math.max(12,count/rules.length*100)+'%':'0%';help.querySelector('.password-status').textContent=!v?'Choose a strong password':strong?'Strong · Requirements met':'Not strong yet · Complete the checklist';input.setCustomValidity(v&&!strong?'Use 10–128 characters with uppercase, lowercase, a number and a special character. Avoid common passwords.':'');match();};
  input.addEventListener('input',update);input.addEventListener('change',update);confirm?.addEventListener('input',match);input.form.addEventListener('submit',e=>{update();if(!input.form.reportValidity())e.preventDefault();});update();
 });
 document.querySelectorAll('.workspace-nav a').forEach(a=>{const url=new URL(a.href);if(url.pathname===location.pathname&&url.search===location.search){a.classList.add('active');a.setAttribute('aria-current','page');}});
 document.querySelectorAll('[data-checkout-form]').forEach(form=>form.addEventListener('submit',e=>{
  if(form.dataset.submitting==='true'){e.preventDefault();return;}
  if(!form.checkValidity())return;
  form.dataset.submitting='true';
  // Preserve the clicked demo outcome in the POST when disabling its button.
  if(e.submitter?.name){const h=document.createElement('input');h.type='hidden';h.name=e.submitter.name;h.value=e.submitter.value;form.append(h);}
  form.querySelectorAll('button').forEach(b=>{b.disabled=true;});form.setAttribute('aria-busy','true');if(e.submitter)e.submitter.textContent='Processing…';
 }));
 addEventListener('pageshow',e=>{if(e.persisted)location.reload();});
 const dialog=document.querySelector('[data-payment-success]');
 if(dialog){
  const marker='lumina-success-'+dialog.dataset.paymentSuccess;
  let seen=false;try{seen=sessionStorage.getItem(marker)==='seen';sessionStorage.setItem(marker,'seen');}catch{}
  if(!seen){
   let timer;let closing=false;const previous=document.activeElement;
   const finish=()=>{dialog.close();dialog.remove();if(previous&&previous!==document.body)previous.focus();else document.querySelector('.receipt-actions a, .receipt-actions button')?.focus({preventScroll:true});};
   const close=()=>{if(closing)return;closing=true;clearTimeout(timer);dialog.classList.add('leaving');setTimeout(finish,reduced?80:380);};
   dialog.querySelector('[data-close-success]').addEventListener('click',close);dialog.addEventListener('cancel',e=>{e.preventDefault();close();});
   dialog.addEventListener('click',e=>{if(e.target===dialog){const r=dialog.getBoundingClientRect();if(e.clientX<r.left||e.clientX>r.right||e.clientY<r.top||e.clientY>r.bottom)close();}});
   dialog.showModal();timer=setTimeout(close,3900);
   // If the tab is hidden during the confirmation, let the user see it when returning.
   document.addEventListener('visibilitychange',()=>{if(closing)return;if(document.hidden)clearTimeout(timer);else timer=setTimeout(close,3900);});
  }else dialog.remove();
 }
})();
