<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>
<section class="section result-section"><span class="eyebrow">LET'S GET YOU BACK ON TRACK</span><h1>A small interruption.</h1><p><%=ViewUtils.e(Objects.toString(request.getAttribute("problem"),"We could not complete this request. Please try again."))%></p><button class="button" data-back>Go back and review</button><a class="button ghost" href="<%=ctx%>/home">Return home</a></section>
<%@ include file="footer.jspf" %>
