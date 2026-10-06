import urllib.request,urllib.parse,http.cookiejar,re,json,time,sys
BASE=sys.argv[1] if len(sys.argv)>1 else 'http://localhost:8080/lumina'
class Client:
 def __init__(self):self.opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()));self.csrf=''
 def get(self,path):
  try:r=self.opener.open(BASE+path,timeout=30)
  except urllib.error.HTTPError as e:r=e
  data=r.read();text=data.decode('utf8','replace');m=re.search(r'name="csrf" value="([^"]+)"',text)
  if m:self.csrf=m.group(1)
  return r.status,text,r.geturl(),data
 def post(self,path,data):
  data={'csrf':self.csrf,**data}
  try:r=self.opener.open(BASE+path,urllib.parse.urlencode(data).encode(),timeout=40)
  except urllib.error.HTTPError as e:r=e
  content=r.read();text=content.decode('utf8','replace');m=re.search(r'name="csrf" value="([^"]+)"',text)
  if m:self.csrf=m.group(1)
  return r.status,text,r.geturl(),content
 def login(self,user):
  self.get('/login');response=self.post('/login',{'username':user,'password':'Lumina@2026!'})
  assert response[0]==200 and ('My Books' in response[1]),response[:3]
def ok(result,label):
 assert result[0]==200,(label,result[0],re.findall(r'<p>(.*?)</p>',result[1])[-3:])
 assert 'A small interruption.' not in result[1],(label,result[1][-2500:])
 print('PASS',label)
guest=Client()
for path in ['/home','/catalog','/catalog?format=DIGITAL&category=1&q=Art','/book?id=1','/login','/signup','/forgot','/assets/style.css','/assets/opening-book.gif','/assets/reader.mjs']:
 ok(guest.get(path),'public '+path)
admin=Client();admin.login('admin')
for path in ['/dashboard','/my-books','/payments','/notifications','/reservations','/fines','/profile','/circulation','/book-edit','/book-edit?id=1','/settings']:
 ok(admin.get(path),'admin '+path)
for entity in ['books','copies','categories','authors','publishers','suppliers','purchases','users','branches']:
 ok(admin.get('/manage?entity='+entity),'manage '+entity)
for report in ['inventory','physical','digital','payments','fines','reservations','suppliers','purchases','categories','popular','reading','monthly']:
 ok(admin.get('/reports?type='+report),'report '+report)
for fmt in ['xlsx','pdf']:
 response=admin.get('/export?type=inventory&format='+fmt);ok(response,'export '+fmt)
 assert response[3].startswith(b'PK' if fmt=='xlsx' else b'%PDF')
reader=Client();reader.get('/signup');stamp=str(int(time.time()));username='qa'+stamp
ok(reader.post('/signup',{'name':'QA Reader','email':username+'@example.test','username':username,'phone':'0771234567','address':'Colombo','password':'TestReader@2026','confirm_password':'TestReader@2026'}),'signup')
response=reader.post('/action',{'action':'borrow','book_id':1,'days':7});ok(response,'create digital order');pid=urllib.parse.parse_qs(urllib.parse.urlparse(response[2]).query)['id'][0]
ok(reader.post('/checkout',{'id':pid,'outcome':'success'}),'payment activation')
response=reader.get('/my-books');ok(response,'my books');loan=re.search(r'/reader\?id=(\d+)',response[1]).group(1)
ok(reader.get('/reader?id='+loan),'PDF reader page')
pdf=reader.get('/pdf?id='+loan);assert pdf[0]==200 and pdf[3].startswith(b'%PDF');print('PASS authorized PDF')
assert admin.get('/pdf?id='+loan)[0]==400;print('PASS other account denied PDF')
ok(reader.post('/action',{'action':'progress','id':loan,'page':2}),'automatic progress save')
assert 'data-start-page="2"' in reader.get('/reader?id='+loan)[1]
ok(reader.get('/receipt?id='+pid),'receipt')
ok(reader.post('/action',{'action':'reserve','book_id':2,'branch_id':1}),'reservation')
notification=reader.get('/api/notifications');assert json.loads(notification[1])['unread']>=3;print('PASS notifications generated')
ok(reader.post('/action',{'action':'read-notifications'}),'mark notifications read')
assert json.loads(reader.get('/api/notifications')[1])['unread']==0
assert reader.get('/manage?entity=users')[0]==400;print('PASS reader denied staff workspace')
bad=reader.post('/action',{'csrf':'invalid','action':'borrow','book_id':2,'days':7});assert bad[0]==400;print('PASS CSRF rejected')
print('HTTP integration checks passed on the target Tomcat deployment')
