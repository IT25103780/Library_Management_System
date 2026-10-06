"""Additional restructuring regression tests. Use only a disposable demo-payment deployment.

Run: python tests/http_structure.py http://localhost:18082/lumina
Creates an account and a book, uploads generated files, and records a demo refund.
"""
from pathlib import Path
exec(Path(__file__).with_name('http_smoke.py').read_text(encoding='utf-8').split('guest=Client()')[0])
import base64
import html
import uuid


def request(client, path, method='GET', data=None, headers=None):
    req = urllib.request.Request(BASE + path, data=data, method=method, headers=headers or {})
    try:
        result = client.opener.open(req, timeout=40)
    except urllib.error.HTTPError as error:
        result = error
    return result.status, result.headers, result.read()


def multipart(client, fields, files):
    boundary = 'LuminaTest' + uuid.uuid4().hex
    chunks = []
    for name, value in {'csrf': client.csrf, **fields}.items():
        chunks.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n').encode())
    for name, filename, mime, content in files:
        chunks.append((f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"; filename="{filename}"\r\nContent-Type: {mime}\r\n\r\n').encode() + content + b'\r\n')
    chunks.append(f'--{boundary}--\r\n'.encode())
    return request(client, '/book-edit', 'POST', b''.join(chunks), {'Content-Type': 'multipart/form-data; boundary=' + boundary})


def sample_pdf():
    objects = [
        b'<< /Type /Catalog /Pages 2 0 R >>',
        b'<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
        b'<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Contents 4 0 R >>',
        b'<< /Length 0 >>\nstream\n\nendstream',
    ]
    data = b'%PDF-1.4\n'; offsets = [0]
    for i, obj in enumerate(objects, 1):
        offsets.append(len(data)); data += f'{i} 0 obj\n'.encode() + obj + b'\nendobj\n'
    xref = len(data)
    data += b'xref\n0 5\n0000000000 65535 f \n'
    data += b''.join(f'{offset:010d} 00000 n \n'.encode() for offset in offsets[1:])
    return data + f'trailer\n<< /Size 5 /Root 1 0 R >>\nstartxref\n{xref}\n%%EOF\n'.encode()


admin = Client(); admin.login('admin')
title = 'Structure upload ' + str(time.time_ns())
fields = dict(id=0, title=title, edition='First edition', publication_year=2026,
              author_id=1, category_id=1, publisher_id=1, format='DIGITAL',
              description='Disposable upload regression fixture', fee='125.00',
              duration_days=7, copies=0, branch_id=1)
pdf = sample_pdf()
png = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+jRZkAAAAASUVORK5CYII=')
admin.get('/book-edit')
assert multipart(admin, fields, [('pdf','bad.pdf','application/pdf',b'not a pdf')])[0] == 400
assert multipart(admin, fields, [('pdf','book.pdf','application/pdf',pdf), ('cover','cover.png','image/png',png)])[0] == 200
page = admin.get('/catalog?q=' + urllib.parse.quote(title))[1]
book_id = re.search(r'/book\?id=(\d+)',page).group(1)
book = admin.get('/book?id=' + book_id)[1]
assert title in book
editor = admin.get('/book-edit?id=' + book_id)[1]
reference = re.search(r'name="isbn" value="([^"]+)"', editor).group(1)
assert re.fullmatch(r'LUM-\d+',reference)
fields.update(id=book_id, title=title+' edited', isbn='SHOULD-NOT-REPLACE-REFERENCE')
assert multipart(admin, fields, [])[0] == 200
editor = admin.get('/book-edit?id=' + book_id)[1]
assert f'name="isbn" value="{reference}"' in editor
status, headers, cover = request(admin, '/cover?id='+book_id)
assert status == 200 and cover.startswith(b'\xff\xd8')
assert request(admin, '/cover?id='+book_id, headers={'If-None-Match':headers['ETag']})[0] == 304
print('PASS uploads, invalid PDF rejection, cover conversion/cache, editing and fixed catalog reference')

reader = Client(); reader.get('/signup'); username='structure'+str(time.time_ns())
password='Structure!Reader2026'
ok(reader.post('/signup',dict(name=username,email=username+'@example.test',username=username,
    phone='0771234567',address='Test address',password=password,confirm_password=password)), 'fixture signup')
response = reader.post('/action',dict(action='borrow',book_id=book_id,days=7))
ok(response,'uploaded book order')
payment_id=urllib.parse.parse_qs(urllib.parse.urlparse(response[2]).query)['id'][0]
assert 'No money is charged' in response[1] or 'DEMO' in response[1] or 'demo' in response[1].lower()
ok(reader.post('/action',dict(action='cancel-payment',id=payment_id)), 'cancel pending order')
response=reader.post('/action',dict(action='retry',id=payment_id));ok(response,'retry cancelled order')
payment_id=urllib.parse.parse_qs(urllib.parse.urlparse(response[2]).query)['id'][0]
ok(reader.post('/checkout',dict(id=payment_id,outcome='success')),'activate uploaded PDF')
loan_id=re.search(r'/reader\?id=(\d+)',reader.get('/my-books')[1]).group(1)
status, headers, content=request(reader,'/pdf?id='+loan_id,headers={'Range':'bytes=0-19'})
assert status==206 and content==pdf[:20] and headers['Content-Range'].startswith('bytes 0-19/')
status, headers, content=request(reader,'/pdf?id='+loan_id,'HEAD')
assert status==200 and content==b'' and int(headers['Content-Length'])==len(pdf)
assert request(reader,'/pdf?id='+loan_id,headers={'Range':'bytes=999999-'})[0]==416
assert request(admin,'/pdf?id='+loan_id)[0]==400
assert request(reader,'/payment-notify')[0]==400
assert reader.post('/payment-notify',{'order_id':'not-a-real-order'})[0]==400
print('PASS uploaded PDF access, range requests, HEAD, invalid range, ownership and invalid callbacks')

ok(admin.post('/action',dict(action='record-refund',id=payment_id,reference='STRUCTURE-TEST-DEMO')),'record demo refund')
assert reader.get('/pdf?id='+loan_id)[0]==400
assert reader.get('/receipt?id='+payment_id)[0]==400
assert reader.get('/payment-status?id='+payment_id+'&poll=1')[1]=='{"status":"REFUNDED"}'
print('PASS refund updates payment and revokes digital access')

new_password='Structure!Changed2026'
ok(reader.post('/profile',dict(operation='password',current_password=password,password=new_password,
                             confirm_password=new_password)), 'change test account password')
ok(reader.post('/logout',{}),'logout')
reader.get('/login')
assert reader.post('/login',dict(username=username,password=password))[0]==400
ok(reader.post('/login',dict(username=username,password=new_password)),'login with new password')
ok(reader.post('/profile',dict(name='Updated Test Reader',phone='0779999999',address='Updated test address')),'profile update')
assert 'Updated Test Reader' in reader.get('/profile')[1]
assert reader.get('/missing-structure-page')[0]==404
assert request(reader,'/profile','PUT')[0]==405
assert request(reader,'/assets/style.css','HEAD')[0]==200
print('PASS account changes, logout, HTTP method handling and static HEAD requests')
print('Additional structure regression checks passed.')
