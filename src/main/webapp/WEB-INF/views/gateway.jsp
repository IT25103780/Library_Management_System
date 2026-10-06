<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>

<section class="section narrow"><div class="panel"><span class="eyebrow">SECURE CARD PAYMENT</span><h1>Continue to checkout.</h1><p>PayHere will collect your card details securely.</p><form method="post" action="<%=AppConfig.get("payment.mode","demo").equals("payhere-live")?"https://www.payhere.lk/pay/checkout":"https://sandbox.payhere.lk/pay/checkout"%>"><%for(var f:((Map<String,String>)request.getAttribute("fields")).entrySet()){%><input type="hidden" name="<%=ViewUtils.e(f.getKey())%>" value="<%=ViewUtils.e(f.getValue())%>"><%}%><button class="button">Open secure card checkout &#8594;</button></form></div></section>

<%@ include file="footer.jspf" %>
