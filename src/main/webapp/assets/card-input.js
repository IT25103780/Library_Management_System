/* Demo card details stay in the browser and are never submitted. */
(() => {
 'use strict';
 const number=document.querySelector('[data-demo-number]');
 if(!number)return;
 const hint=document.getElementById('card-number-help');
 const digitsOf=value=>value.replace(/[^0-9]/g,'');
 const validate=()=>{
  const count=digitsOf(number.value).length;
  number.setCustomValidity(count>0&&count!==16?'Enter all 16 digits.':'');
  hint.textContent=count+' / 16 digits';
 };
 const format=()=>{
  const before=number.value;
  const start=number.selectionStart??before.length,end=number.selectionEnd??start;
  const startDigits=digitsOf(before.slice(0,start)).length,endDigits=digitsOf(before.slice(0,end)).length;
  const digits=digitsOf(before).slice(0,16);
  const formatted=digits.match(/.{1,4}/g)?.join(' ')||'';
  if(before!==formatted){
   number.value=formatted;
   const caret=(count,oldPos)=>Math.min(formatted.length,count+Math.floor(Math.max(0,count-1)/4)+(count>0&&count%4===0&&before[oldPos-1]===' '?1:0));
   number.setSelectionRange(caret(startDigits,start),caret(endDigits,end));
  }
  validate();
 };
 number.addEventListener('beforeinput',event=>{
  const pos=number.selectionStart;
  if(pos!==number.selectionEnd)return;
  if(event.inputType==='deleteContentBackward'&&pos>1&&number.value[pos-1]===' '){
   event.preventDefault();number.setRangeText('',pos-2,pos,'end');format();
  }else if(event.inputType==='deleteContentForward'&&number.value[pos]===' '){
   event.preventDefault();number.setRangeText('',pos,pos+2,'end');format();
  }
 });
 number.addEventListener('paste',event=>{
  if(!event.clipboardData)return;
  event.preventDefault();
  const start=number.selectionStart??number.value.length,end=number.selectionEnd??start;
  const remaining=16-digitsOf(number.value.slice(0,start)+number.value.slice(end)).length;
  const incoming=digitsOf(event.clipboardData.getData('text')).slice(0,Math.max(0,remaining));
  number.setRangeText(incoming,start,end,'end');format();
 });
 number.addEventListener('input',format);
 number.addEventListener('change',format);
 number.addEventListener('blur',format);
 number.form.addEventListener('submit',format,true);
 format();
})();
