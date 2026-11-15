document.querySelectorAll('.transaction').forEach((row)=>row.addEventListener('click',()=>{
  document.querySelectorAll('.transaction').forEach((item)=>{item.classList.remove('selected');item.setAttribute('aria-pressed','false')});
  row.classList.add('selected');row.setAttribute('aria-pressed','true');
  const d=row.dataset;
  document.querySelector('.detail-main h3').textContent=d.merchant;
  document.querySelector('.detail-main p').textContent=`${d.category} · ${d.type}`;
  document.querySelector('.detail-amount').textContent=d.amount;
  document.querySelector('[data-detail="date"]').textContent=d.date;
  document.querySelector('[data-detail="method"]').textContent=d.method;
  document.querySelector('[data-detail="category"]').textContent=d.category;
  document.querySelector('[data-detail="source"]').textContent=d.source;
  const detail=document.querySelector('.detail-main');
  if(!matchMedia('(prefers-reduced-motion: reduce)').matches&&detail?.animate){
    detail.__changeAnimation?.cancel();
    detail.__changeAnimation=detail.animate([{opacity:.45,transform:'translateY(3px)'},{opacity:1,transform:'translateY(0)'}],{duration:220,easing:'cubic-bezier(.16,1,.3,1)'});
  }
}));

(()=>{
  const root=document.documentElement;
  const targets=[...document.querySelectorAll('[data-reveal],.section-head,.flow-step')];
  const hero=document.querySelector('.spendly-art');
  const reduced=matchMedia('(prefers-reduced-motion: reduce)').matches;
  const visible=el=>{const r=el.getBoundingClientRect();return r.bottom>0&&r.top<innerHeight*.92};
  if(reduced||!('IntersectionObserver'in window)){
    targets.forEach(el=>el.classList.add('is-revealed'));
    if(hero)hero.classList.add('is-arriving');
    return;
  }
  targets.forEach(el=>{if(visible(el))el.classList.add('is-revealed')});
  root.classList.add('site-motion-ready');
  const observer=new IntersectionObserver(entries=>entries.forEach(entry=>{
    if(entry.isIntersecting){entry.target.classList.add('is-revealed');observer.unobserve(entry.target)}
  }),{threshold:.12,rootMargin:'0px 0px -6% 0px'});
  targets.filter(el=>!el.classList.contains('is-revealed')).forEach(el=>observer.observe(el));
  if(hero)requestAnimationFrame(()=>requestAnimationFrame(()=>hero.classList.add('is-arriving')));
})();
