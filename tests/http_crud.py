"""Run against a disposable seeded database; creates test records through the website."""
from pathlib import Path
exec(Path(__file__).with_name('http_smoke.py').read_text(encoding='utf-8').split('guest=Client()')[0])
import html,datetime
def rows(page):
 headers=re.findall(r'<th>(.*?)</th>',page,re.S)
 result=[]
 for tr in re.findall(r'<tbody>(.*?)</tbody>',page,re.S):
  for row in re.findall(r'<tr>(.*?)</tr>',tr,re.S):
   vals=[html.unescape(re.sub('<[^>]+>','',v)).strip() for v in re.findall(r'<td[^>]*>(.*?)</td>',row,re.S)]
   result.append(dict(zip(headers,vals)))
 return result
def find(client,entity,key,value):
 result=client.get('/manage?entity='+entity);ok(result,'list '+entity)
 return next(r for r in rows(result[1]) if r.get(key)==str(value))
def post(action,label=None,**data):
 result=admin.post('/action',{'action':action,**data});ok(result,label or action);return result
admin=Client();admin.login('admin');stamp=str(time.time_ns())[-12:]
for route in ['/audit','/member?id=5','/reports?type=members','/reports?type=overdue']:
 ok(admin.get(route),route)
for report in ['inventory','physical','digital','payments','fines','reservations','suppliers','purchases','categories','popular','reading','monthly','monthly-borrowing','members','overdue']:
 ok(admin.get('/reports?type='+report+'&branch=1&category=1&from=2026-01-01&to=2026-12-31'),'filtered '+report)
assert admin.get('/reports?type=physical&from=2026-12-31&to=2026-01-01')[0]==400
for entity in ['branches','categories','authors','publishers','suppliers']:
 name='CRUD '+entity+' '+stamp
 data={'entity':entity,'id':'0','name':name,'address':'Test address','phone':'0771234567','description':'Test description','email':'test@example.test','hours':'9-5'}
 ok(admin.post('/manage',data),'create '+entity);record=find(admin,entity,'name',name)
 data['id']=record['id'];data['name']=name+' edited';ok(admin.post('/manage',data),'update '+entity)
 post('toggle',entity=entity,id=record['id']);post('toggle',entity=entity,id=record['id'])
 post('delete-record',entity=entity,id=record['id'])
name='member'+stamp
data={'entity':'users','name':name,'username':name,'email':name+'@example.test','password':'CrudMember@2026','role':'READER','member_type':'STAFF','phone':'0775551234','address':'Testing','branch_id':'1'}
ok(admin.post('/manage',data),'create member');member=find(admin,'users','username',name)
data.update(id=member['id'],name=name+' updated');ok(admin.post('/manage',data),'edit member contact and type')
assert find(admin,'users','username',name)['member type']=='STAFF'
# Atomic multiline purchase and staged stock receipt.
invoice='INV-'+stamp
post('purchase-batch',supplier_id=1,branch_id=1,invoice_ref=invoice,notes='Test delivery',book0=1,quantity0=3,cost0=100,book1=2,quantity1=2,cost1=150)
ps=[r for r in rows(admin.get('/manage?entity=purchases')[1]) if r.get('invoice ref')==invoice]
assert len(ps)==2
purchase=next(r for r in ps if r['book id']=='1')
post('receive',id=purchase['id'],quantity=1)
assert find(admin,'purchases','id',purchase['id'])['status']=='PARTIALLY_RECEIVED'
assert admin.post('/action',{'action':'receive','id':purchase['id'],'quantity':9})[0]==400
post('receive',id=purchase['id'],quantity=2)
assert find(admin,'purchases','id',purchase['id'])['status']=='RECEIVED'
assert admin.post('/action',{'action':'receive','id':purchase['id'],'quantity':1})[0]==400
# Issue a newly received copy, change due date, return, protect history.
copies=rows(admin.get('/manage?entity=copies')[1]);copy=next(r for r in copies if r['book id']=='1' and r['branch id']=='1' and r['status']=='AVAILABLE')
post('copy-edit',id=copy['id'],branch_id=1,shelf='QA shelf')
post('issue',user_id=member['id'],copy_id=copy['id'])
loans=rows(admin.get('/circulation')[1]);loan=next(r for r in loans if r['copy id']==copy['id'] and r['status']=='ACTIVE')
due=(datetime.datetime.now()+datetime.timedelta(days=20)).strftime('%Y-%m-%dT%H:%M')
post('due-date',id=loan['id'],due_at=due,reason='Approved test extension')
post('return',id=loan['id']);assert admin.post('/action',{'action':'return','id':loan['id']})[0]==400
assert admin.post('/action',{'action':'copy-delete','id':copy['id']})[0]==400
post('void-loan',id=loan['id'],reason='Test transaction correction')
post('reserve-member',user_id=member['id'],book_id=4,branch_id=1)
page=admin.get('/reservations');ok(page,'reservation edit forms')
cards=re.findall(r'<article[^>]*>(.*?)</article>',page[1],re.S)
card=next(c for c in cards if name+' updated' in c and ('WAITING' in c or 'READY' in c))
res=re.search(r'name="id" value="(\d+)"',card).group(1)
post('reservation-edit',id=res,branch_id=2)
post('cancel-reservation',id=res)
post('assess-fines')
ok(admin.get('/member?id='+member['id']),'member activity history')
reader=Client();reader.get('/login');ok(reader.post('/login',{'username':name,'password':'CrudMember@2026'}),'new member login')
for action in ['assess-fines','due-date','copy-delete','purchase-batch','void-fine']:
 assert reader.post('/action',{'action':action,'id':1})[0]==400,action
for role in ['librarian','branch_manager','management']:
 client=Client();client.login(role);ok(client.get('/dashboard'),role+' dashboard');ok(client.get('/reports?type=monthly-borrowing'),role+' report')
 assert client.get('/audit')[0]==400
 assert client.get('/manage?entity=users')[0]==400
print('Extended CRUD, report-filter and permission checks passed.')

# Optional SQL fixture: deliberately make ONLY this test member's new loan overdue.
# Example: python tests/http_crud.py URL LuminaVerification_20260926
if len(sys.argv)>2:
 import subprocess
 database=sys.argv[2]
 assert re.fullmatch(r'LuminaVerification_[A-Za-z0-9_]+',database),'Disposable verification database required'
 post('issue',user_id=member['id'],copy_id=copy['id'])
 loan2=next(r for r in rows(admin.get('/circulation')[1]) if r['copy id']==copy['id'] and r['status']=='ACTIVE')
 subprocess.run(['sqlcmd','-S',r'.\SQLEXPRESS','-E','-C','-b','-d',database,'-Q',f"UPDATE loans SET due_at=DATEADD(hour,-25,GETDATE()) WHERE id={int(loan2['id'])} AND user_id={int(member['id'])}"],check=True,capture_output=True)
 post('assess-fines')
 f=next(r for r in rows(admin.get('/fines')[1]) if r['loan id']==loan2['id'])
 post('offline-fine',id=f['id'],amount='10',method='CASH',reference='QA-RECEIPT')
 f=next(r for r in rows(admin.get('/fines')[1]) if r['id']==f['id']);assert f['status']=='PARTIALLY_PAID'
 history=admin.get('/fine?id='+f['id']);ok(history,'fine detail and payment history')
 pay=re.search(r'/receipt\?id=(\d+)',history[1]).group(1)
 ok(admin.get('/receipt?id='+pay),'staff receipt')
 post('payment-correct',id=pay,method='BANK_TRANSFER',reference='QA-BANK',reason='Corrected method')
 post('payment-void',id=pay,reason='Mistaken receipt')
 post('return',id=loan2['id'])
 post('adjust-fine',id=f['id'],amount='25',reason='Authorised correction')
 post('offline-fine',id=f['id'],amount='25',method='CASH',reference='QA-SETTLED')
 assert next(r for r in rows(admin.get('/fines')[1]) if r['id']==f['id'])['status']=='PAID'
 print('Fine payment, correction, reversal and settlement HTTP checks passed.')
