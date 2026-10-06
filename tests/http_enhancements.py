"""Run against a disposable demo deployment. Tests the new HTTP behavior."""
from pathlib import Path
exec(Path(__file__).with_name('http_smoke.py').read_text(encoding='utf-8').split('guest=Client()')[0])
reader=Client();reader.get('/signup');stamp=str(time.time_ns());username='enh'+stamp
base={'name':'Receipt Tester','email':username+'@example.test','username':username,'phone':'0771234567','address':'Colombo'}
for password in ['weakpassword','NoNumbers!!','nouppercase123!','NOLOWERCASE123!','NoSpecials123','Password123!']:
 assert reader.post('/signup',{**base,'password':password,'confirm_password':password})[0]==400,password
ok(reader.post('/signup',{**base,'password':'River!Quiet2026','confirm_password':'River!Quiet2026'}),'strong password signup')
res=reader.post('/action',{'action':'borrow','book_id':2,'days':7});ok(res,'new checkout')
pid=urllib.parse.parse_qs(urllib.parse.urlparse(res[2]).query)['id'][0]
assert 'name="card_method" value="VISA"' in res[1] and 'name="card_method" value="MASTER"' in res[1]
assert reader.get('/receipt?id='+pid)[0]==400
status=reader.get('/payment-status?id='+pid+'&poll=1');assert json.loads(status[1])['status']=='PENDING'
paid=reader.post('/checkout',{'id':pid,'outcome':'success'});ok(paid,'verified success and receipt')
assert 'data-payment-success="'+pid+'"' in paid[1]
assert 'Download Receipt' in paid[1] and 'isbn-bars' not in paid[1] and 'book-qr' not in paid[1]
status=reader.get('/payment-status?id='+pid+'&poll=1');assert json.loads(status[1])['status']=='SUCCESSFUL'
again=reader.get('/payment-status?id='+pid);assert 'data-payment-success=' not in again[1]
pdf=reader.get('/receipt?id='+pid+'&format=pdf');assert pdf[0]==200 and pdf[3].startswith(b'%PDF')
other=Client();other.login('reader');assert other.get('/receipt?id='+pid+'&format=pdf')[0]==400
res=reader.post('/action',{'action':'borrow','book_id':3,'days':7});failedid=urllib.parse.parse_qs(urllib.parse.urlparse(res[2]).query)['id'][0]
failed=reader.post('/checkout',{'id':failedid,'outcome':'fail'});assert 'data-payment-success=' not in failed[1];assert reader.get('/receipt?id='+failedid)[0]==400
res=reader.post('/action',{'action':'borrow','book_id':4,'days':7});delayedid=urllib.parse.parse_qs(urllib.parse.urlparse(res[2]).query)['id'][0]
class NoRedirect(urllib.request.HTTPRedirectHandler):
 def redirect_request(self,*args): return None
jar=next(h.cookiejar for h in reader.opener.handlers if isinstance(h,urllib.request.HTTPCookieProcessor))
direct=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(jar),NoRedirect())
try: direct.open(BASE+'/checkout',urllib.parse.urlencode({'csrf':reader.csrf,'id':delayedid,'outcome':'success'}).encode())
except urllib.error.HTTPError as e: assert e.code==302
assert json.loads(reader.get('/payment-status?id='+delayedid+'&poll=1')[1])['status']=='SUCCESSFUL'
assert 'data-payment-success=' in reader.get('/payment-status?id='+delayedid)[1]
print('PASS weak passwords rejected, Visa/Mastercard choices, polling, success once, PDF download, receipt permissions and failed payment')
