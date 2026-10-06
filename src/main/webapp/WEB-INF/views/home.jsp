<%@ page pageEncoding="UTF-8" %>
<%@ include file="header.jspf" %>

<link rel="stylesheet" href="<%=ctx%>/assets/home-cinema/home.css">
<script src="<%=ctx%>/assets/home-cinema/home.js" defer></script>
<div class="lumina-home">
  <section class="ld-journey" aria-label="The Lumina Library cinematic journey" data-frame-root="<%=ctx%>/assets/home-cinema/frames">
    <div class="ld-stage">
      <img class="ld-poster" src="<%=ctx%>/assets/home-cinema/frames/scene-1/frame_0001.webp" alt="The entrance to Lumina Library" width="1920" height="1080" fetchpriority="high">
      <canvas class="ld-canvas" role="img" aria-label="A scroll-controlled journey through the seven scenes of Lumina Library"></canvas>
      <div class="ld-shade" aria-hidden="true"></div>
      <a class="ld-workspace-link" href="#home-content">EXPLORE THE COLLECTION <span aria-hidden="true">↗</span></a>
      <div class="ld-story">
        <article class="ld-chapter ld-left" data-chapter="0" aria-hidden="false">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">THE LUMINA EXPERIENCE</span>
          <h1><span class="ld-title-line"><span class="ld-title-ink">Some doors open</span></span><span class="ld-title-line"><span class="ld-title-ink">to <em>entire worlds.</em></span></span></h1><p>Find your next chapter in a place made for curiosity.<br> Extraordinary books. Endless possibilities.</p><div class="ld-actions"><a href="<%=ctx%>/catalog">Explore the collection &#8599;</a><a href="<%=ctx%>/catalog?format=DIGITAL">Read from anywhere &#8594;</a></div><div class="ld-tags">A LITTLE WONDER. A LIFETIME OF DISCOVERY.</div>
          <span class="ld-enter">SCROLL TO ENTER <span aria-hidden="true">↓</span></span>
        </article>
        <article class="ld-chapter ld-left" data-chapter="1" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">01 / DISCOVER</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">Find What</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>Inspires You.</em></span></span></h2><p>Explore thousands of books across knowledge, technology, science, literature and more.</p><div class="ld-tags">SMART SEARCH &nbsp; · &nbsp; DIGITAL CATALOGUE</div>
        </article>
        <article class="ld-chapter ld-left" data-chapter="2" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">02 / EXPLORE</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">Your Next Book.</span></span><span class="ld-title-line"><span class="ld-title-ink">Closer Than</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>You Think.</em></span></span></h2><p>Search. Discover. Choose.</p><div class="ld-tags">CATALOGUE / ACTIVE &nbsp; · &nbsp; ACCESS / AVAILABLE</div>
        </article>
        <article class="ld-chapter ld-right" data-chapter="3" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">03 / EXPERIENCE</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">A Space Built</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>For Focus.</em></span></span></h2><p>Quiet spaces. Comfortable reading. Unlimited ideas.</p><div class="ld-tags">MAKE ROOM FOR POSSIBILITY</div>
        </article>
        <article class="ld-chapter ld-left" data-chapter="4" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">04 / LEARN</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">Every Page Opens</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>Another World.</em></span></span></h2><p>Read. Learn. Imagine. Grow.</p><div class="ld-tags">CURIOSITY HAS NO LIMITS</div>
        </article>
        <article class="ld-chapter ld-right" data-chapter="5" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">05 / BORROW</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">Simple. Fast.</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>Connected.</em></span></span></h2><p>Borrow books seamlessly with the Lumina Library Management System.</p><div class="ld-tags">YOUR LIBRARY &nbsp; · &nbsp; THOUGHTFULLY CONNECTED</div>
        </article>
        <article class="ld-chapter ld-left" data-chapter="6" aria-hidden="true">
          <span class="ld-line" aria-hidden="true"></span><span class="ld-eyebrow">WELCOME TO LUMINA</span>
          <h2><span class="ld-title-line"><span class="ld-title-ink">Your Journey</span></span><span class="ld-title-line"><span class="ld-title-ink">Through Knowledge</span></span><span class="ld-title-line"><span class="ld-title-ink"><em>Starts Here.</em></span></span></h2><p>Discover · Read · Learn · Grow</p><div class="ld-tags">THE NEXT CHAPTER IS YOURS</div>
          <a class="ld-cta" href="#home-content" tabindex="-1">EXPLORE THE COLLECTION <span aria-hidden="true">↗</span></a>
        </article>
      </div>
      <div class="ld-rail" aria-hidden="true"><span>01</span><div><i></i></div><span>07</span></div>
      <div class="ld-bottom" aria-hidden="true"><div><b class="ld-scene">01</b><span>/ 07</span><i></i><span>THE LUMINA EXPERIENCE</span></div><span>DISCOVER. READ. BECOME.</span></div>
      <div class="ld-loader" role="status"><span class="ld-loader-brand">LUMINA</span><p>Preparing your experience</p><div class="ld-load-track"><span></span></div><small>0%</small><button type="button" class="ld-retry" hidden>Try again</button><a href="#home-content">Continue to the library ↓</a></div>
      <span class="ld-buffer" role="status" hidden>Bringing your next chapter into focus…</span>
      <noscript><p class="ld-nojs">Enable JavaScript for the cinematic journey, or <a href="#home-content">continue to the library</a>.</p></noscript>
    </div>
  </section>
</div>
<div id="home-content" tabindex="-1"></div>
<div class="promise-strip"><span><i>01</i> A thoughtfully curated collection</span><span><i>02</i> Your reading room, anywhere</span><span><i>03</i> More than a library</span></div>
<section class="section"><div class="section-heading"><div><span class="eyebrow">HANDPICKED FOR YOUR NEXT CHAPTER</span><h2>On the reading table.</h2></div><a class="link-arrow" href="<%=ctx%>/catalog">View the collection &#8594;</a></div><div class="book-grid home-books"><%for(Map<String,Object> book:(List<Map<String,Object>>)request.getAttribute("books")){%><article class="book-card"><a class="book-object" href="<%=ctx%>/book?id=<%=book.get("id")%>"><img loading="lazy" src="<%=ctx%>/cover?id=<%=book.get("id")%>" alt="<%=ViewUtils.e(book.get("title"))%> cover" width="300" height="420"><span class="book-spine"></span></a><div class="book-meta"><span class="eyebrow"><%=ViewUtils.e(book.get("category"))%></span><h3><a href="<%=ctx%>/book?id=<%=book.get("id")%>"><%=ViewUtils.e(book.get("title"))%></a></h3><p><%=ViewUtils.e(book.get("author"))%></p><div class="book-bottom"><span class="pill"><%=ViewUtils.e(book.get("format"))%></span><a href="<%=ctx%>/book?id=<%=book.get("id")%>" aria-label="View book">&#8599;</a></div></div></article><%}%></div></section>
<section class="digital-feature section"><div class="digital-art"><div class="orbit orbit-one"></div><div class="orbit orbit-two"></div><div class="floating-book"><img src="<%=ctx%>/assets/cover-1.svg" alt="The Art of Paying Attention" loading="lazy"></div><span class="digital-stamp">YOUR NEXT READ<br><b>ANYWHERE.</b></span></div><div><span class="eyebrow">THE DIGITAL READING ROOM</span><h2>Your favourite corner.<br><em>Wherever you are.</em></h2><p>Borrow a digital book, settle in, and pick up where you left off. Your personal bookshelf is always within reach.</p><a class="button" href="<%=ctx%>/catalog?format=DIGITAL">Enter the digital library &#8599;</a><div class="mini-steps"><span>01 <b>Discover</b></span><span>02 <b>Borrow</b></span><span>03 <b>Get lost in a book</b></span></div></div></section>
<section class="section branch-section" id="branches"><div class="section-heading"><div><span class="eyebrow">A PLACE TO BELONG</span><h2>Find your quiet corner.</h2></div><p>Good books. Warm spaces.<br>A welcome at every branch.</p></div><div class="branch-grid"><%for(var br:(List<Map<String,Object>>)request.getAttribute("branches")){%><article class="branch-card"><span class="eyebrow">LUMINA LIBRARY</span><h3><%=ViewUtils.e(br.get("name"))%></h3><p><%=ViewUtils.e(br.get("address"))%></p><div><span><%=ViewUtils.e(br.get("hours"))%></span><span><%=ViewUtils.e(br.get("phone"))%></span></div></article><%}%></div></section>

<%@ include file="footer.jspf" %>
