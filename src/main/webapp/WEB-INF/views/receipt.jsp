<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>
<%var payment=(Map<String,Object>)request.getAttribute("payment");boolean demo="DEMO".equals(payment.get("provider"));%>
<section class="section receipt-section">
<div class="receipt-caption"><span class="eyebrow">A LITTLE MOMENT. A NEW CHAPTER.</span><span class="paid-badge">&#10003; Paid</span></div>
<article class="receipt" aria-label="Payment receipt">
<div class="receipt-top"><div><span class="brand-text">LUMINA</span><small>THE LIBRARY</small></div><div><span class="eyebrow">PAYMENT RECEIPT</span><strong>LUM-<%=payment.get("id")%></strong></div></div>
<div class="receipt-greeting"><div><h1>Thank you.</h1><p>Your payment has been recorded.</p></div><span class="receipt-seal" aria-hidden="true">&#10003;</span></div>
<%if(demo){%><div class="notice">DEMONSTRATION RECEIPT — NO MONEY CHARGED</div><%}%>
<dl class="order-lines"><div><dt>Receipt</dt><dd>LUM-<%=payment.get("id")%></dd></div><div><dt>Paid by</dt><dd><%=Web.e(payment.get("name"))%></dd></div><div><dt>Payment date</dt><dd><%=Web.date(payment.get("paid_at"))%><small>Sri Lanka time</small></dd></div><div><dt>Description</dt><dd><%=payment.get("title")==null?"Library fine":Web.e(payment.get("title"))%></dd></div>

<div><dt>Provider / method</dt><dd><%=Web.e(payment.get("provider"))%> / <%=Web.e(payment.get("method"))%><%if(payment.get("card_last4")!=null){%><small>Card ending <%=Web.e(payment.get("card_last4"))%></small><%}%></dd></div><div><dt>Transaction reference</dt><dd><%=Web.e(payment.get("provider_ref"))%></dd></div><div class="order-total"><dt>Paid</dt><dd><%=Web.money(payment.get("amount"))%></dd></div></dl>
<p class="receipt-signoff">Lumina Library · A world within reach</p></article>
<div class="receipt-actions"><a class="button ghost" href="<%=ctx%>/receipt?id=<%=payment.get("id")%>&amp;format=pdf">Download Receipt</a><button class="button ghost print-button" data-print>Print / Save as PDF</button></div><a class="form-link" href="<%=ctx%>/payments">Back to payments</a></section>
<%@ include file="footer.jspf" %>
