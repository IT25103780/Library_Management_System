const fs=require('fs'),vm=require('vm'),assert=require('assert');
const listeners={};const form={addEventListener(){},reportValidity(){return true;}};
const number={value:'',selectionStart:0,selectionEnd:0,form,addEventListener(k,f){listeners[k]=f},setCustomValidity(v){this.error=v},setSelectionRange(a,b){this.selectionStart=a;this.selectionEnd=b},setRangeText(v,a,b){this.value=this.value.slice(0,a)+v+this.value.slice(b);this.setSelectionRange(a+v.length,a+v.length)}};
const hint={};const expiry={addEventListener(){}};
vm.runInNewContext(fs.readFileSync(require('path').join(__dirname,'../src/main/webapp/assets/card-input.js'),'utf8'),{document:{querySelector:s=>s.includes('number')?number:expiry,getElementById:()=>hint,body:{classList:{contains:()=>false}}}});
function input(v,pos=v.length){number.value=v;number.setSelectionRange(pos,pos);listeners.input();}
input('1234567890123456');assert.equal(number.value,'1234 5678 9012 3456');assert.equal(number.error,'');
input('1234 5678 9012 3456');assert.equal(number.value,'1234 5678 9012 3456');
input('12ab34-5678');assert.equal(number.value,'1234 5678');assert(number.error);
input('1234 5678');number.setSelectionRange(5,5);listeners.beforeinput({inputType:'deleteContentBackward',preventDefault(){}});assert.equal(number.value,'1235 678');assert.equal(number.selectionStart,3);
input('1234 5678',4);listeners.beforeinput({inputType:'deleteContentForward',preventDefault(){}});assert.equal(number.value,'1234 678');
input('');assert.equal(number.value,'');
console.log('PASS: formatting, paste, non-digits, validation, deletion at both space boundaries, empty input');


input('4605591800357864');assert.equal(number.value,'4605 5918 0035 7864');assert.equal(hint.textContent,'16 / 16 digits');assert.equal(number.error,'');
input('4605 5918 0035 7864');assert.equal(hint.textContent,'16 / 16 digits');assert.equal(number.error,'');
input('1111111111111111');assert.equal(number.error,'');
input('123');assert.equal(hint.textContent,'3 / 16 digits');assert.equal(number.error,'Enter all 16 digits.');
console.log('PASS: screenshot regression cases and arbitrary 16-digit demo values');
