(() => {
 'use strict';
 const number=document.querySelector('[data-demo-number]');
 if(number){
  const expiry=document.querySelector('[data-demo-expiry]');
  const validateExpiry=()=>{
   const match=/^(0[1-9]|1[0-2])\/([0-9]{2})$/.exec(expiry.value);
   const now=new Date();
   const valid=match&&new Date(2000+Number(match[2]),Number(match[1]),1)>now;
   expiry.setCustomValidity(expiry.value&&!valid?'Enter a current or future expiry date in MM/YY format.':'');
  };
  expiry.addEventListener('input',()=>{
   if(/^\d{2}$/.test(expiry.value)&&expiry.value.length>Number(expiry.dataset.previousLength||0))expiry.value+='/';
   expiry.dataset.previousLength=expiry.value.length;validateExpiry();
  });
  // These controls intentionally have no name: card data is never in the POST.
  number.form.addEventListener('submit',event=>{
   validateExpiry();
   if(!number.form.reportValidity()){event.preventDefault();event.stopImmediatePropagation();}
  },true);
 }
 if(!document.body.classList.contains('refined-workspace')||matchMedia('(prefers-reduced-motion: reduce)').matches)return;
 const elements=document.querySelectorAll('.workspace-title,.shelf-heading,.section-heading,.stat-card,.panel,.book-card,.shelf-card,.notification,.reservation-card,.detail-stage,.detail-copy,.borrow-panel');
 if('IntersectionObserver' in window){
  const observer=new IntersectionObserver(entries=>{
   let i=0;entries.forEach(entry=>{if(entry.isIntersecting){entry.target.style.setProperty('--arrival',Math.min(i++*55,220)+'ms');entry.target.classList.add('lumina-enter');observer.unobserve(entry.target);}});
  },{threshold:.05});
  elements.forEach(element=>observer.observe(element));
 }
})();
