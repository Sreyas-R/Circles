import React, { useState, useEffect, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';

const SLIDES = [
  {
    image: '/feature-secure-storage.jpg',
    tag: 'Security',
    title: 'Encrypted cloud storage',
    body: 'Every file is encrypted at rest with AWS KMS. Your documents stay private — only your circle can access them.',
  },
  {
    image: '/feature-invite-friends.jpg',
    tag: 'Collaboration',
    title: 'Invite your people',
    body: 'Create a circle, share a link, and start collaborating in seconds. No accounts needed for invites.',
  },
  {
    image: '/feature-instant-share.jpg',
    tag: 'Access',
    title: 'Instant access anywhere',
    body: 'Download or preview documents from any device. Secure, time-limited links mean you always stay in control.',
  },
];

const FEATURES = [
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
      </svg>
    ),
    title: 'End-to-end encryption',
    body: 'SSE-KMS encryption on every object. Keys are managed by AWS — never exposed to application code.',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
        <circle cx="9" cy="7" r="4" />
        <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
        <path d="M16 3.13a4 4 0 0 1 0 7.75" />
      </svg>
    ),
    title: 'Circle-based sharing',
    body: 'Organize files into private circles. Only members you invite can view, upload, or download.',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
        <polyline points="17 8 12 3 7 8" />
        <line x1="12" y1="3" x2="12" y2="15" />
      </svg>
    ),
    title: 'Drag-and-drop uploads',
    body: 'Upload images, PDFs, and documents up to 10 MB. Thumbnails are generated automatically.',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <circle cx="12" cy="12" r="10" />
        <polyline points="12 6 12 12 16 14" />
      </svg>
    ),
    title: 'Expiring download links',
    body: 'Presigned URLs expire in 15 minutes. No permanent links, no leaked credentials.',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <path d="M22 12h-4l-3 9L9 3l-3 9H2" />
      </svg>
    ),
    title: 'Real-time observability',
    body: 'CloudWatch integration provides audit trails and alerting on every upload and download event.',
  },
  {
    icon: (
      <svg viewBox="0 0 24 24" width="28" height="28" fill="none" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round">
        <rect x="2" y="3" width="20" height="14" rx="2" ry="2" />
        <line x1="8" y1="21" x2="16" y2="21" />
        <line x1="12" y1="17" x2="12" y2="21" />
      </svg>
    ),
    title: 'Works everywhere',
    body: 'Responsive web app that works seamlessly on desktop, tablet, and mobile browsers.',
  },
];

export default function Landing() {
  const navigate = useNavigate();
  const [activeSlide, setActiveSlide] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  const nextSlide = useCallback(() => {
    setActiveSlide(prev => (prev + 1) % SLIDES.length);
  }, []);

  useEffect(() => {
    if (isPaused) return;
    const timer = setInterval(nextSlide, 5000);
    return () => clearInterval(timer);
  }, [isPaused, nextSlide]);

  return (
    <div className="landing">
      {/* ── Header ── */}
      <header className="landing-header">
        <div className="landing-header-inner">
          <div className="landing-logo" onClick={() => window.scrollTo({ top: 0, behavior: 'smooth' })}>
            <div className="landing-logo-mark">
              <svg viewBox="0 0 32 32" width="26" height="26" fill="none">
                <circle cx="16" cy="16" r="14" stroke="currentColor" strokeWidth="2.2" />
                <circle cx="16" cy="16" r="6" stroke="currentColor" strokeWidth="2" opacity="0.45" />
              </svg>
            </div>
            Circles
          </div>
          <nav className="landing-nav">
            <button className="landing-nav-link" onClick={() => navigate('/login')}>Sign in</button>
            <button className="landing-cta-sm" onClick={() => navigate('/register')}>Get started</button>
          </nav>
        </div>
      </header>

      {/* ── Hero ── */}
      <section className="landing-hero">
        <div className="landing-hero-inner">
          <p className="landing-eyebrow">Secure document sharing for teams</p>
          <h1 className="landing-h1">
            Share files with the people<br className="landing-br" /> who matter most
          </h1>
          <p className="landing-subtitle">
            Circles gives your group a private, encrypted space to store and access
            documents — no complicated setup, no exposed links, no compromises.
          </p>
          <div className="landing-hero-actions">
            <button className="landing-cta-primary" onClick={() => navigate('/register')}>
              Create free account
              <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <line x1="4" y1="10" x2="16" y2="10" />
                <polyline points="11 5 16 10 11 15" />
              </svg>
            </button>
            <button className="landing-cta-ghost" onClick={() => navigate('/login')}>Sign in instead</button>
          </div>
        </div>
      </section>

      {/* ── Feature slideshow ── */}
      <section className="landing-slideshow-section">
        <div className="landing-slideshow-inner">
          <div className="landing-section-label">How it works</div>

          <div
            className="landing-slideshow"
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
          >
            <div className="landing-slide-media">
              {SLIDES.map((slide, i) => (
                <img
                  key={i}
                  src={slide.image}
                  alt={slide.title}
                  className={`landing-slide-img ${i === activeSlide ? 'active' : ''}`}
                />
              ))}
            </div>

            <div className="landing-slide-content">
              {SLIDES.map((slide, i) => (
                <div
                  key={i}
                  className={`landing-slide-text ${i === activeSlide ? 'active' : ''}`}
                  onClick={() => setActiveSlide(i)}
                  role="button"
                  tabIndex={0}
                  onKeyDown={e => { if (e.key === 'Enter') setActiveSlide(i); }}
                >
                  <span className="landing-slide-tag">{slide.tag}</span>
                  <h3>{slide.title}</h3>
                  <p>{slide.body}</p>
                  <div className="landing-slide-progress">
                    <div
                      className={`landing-slide-bar ${i === activeSlide && !isPaused ? 'running' : ''}`}
                    />
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ── Feature grid ── */}
      <section className="landing-features-section">
        <div className="landing-features-inner">
          <div className="landing-section-label">Features</div>
          <h2 className="landing-h2">Built for privacy from the ground up</h2>
          <div className="landing-features-grid">
            {FEATURES.map((f, i) => (
              <div className="landing-feature-card" key={i}>
                <div className="landing-feature-icon">{f.icon}</div>
                <h4>{f.title}</h4>
                <p>{f.body}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── CTA banner ── */}
      <section className="landing-cta-section">
        <div className="landing-cta-inner">
          <h2>Ready to get started?</h2>
          <p>Create a circle, invite your team, and start sharing securely — all in under a minute.</p>
          <button className="landing-cta-primary" onClick={() => navigate('/register')}>
            Create your circle
            <svg viewBox="0 0 20 20" width="18" height="18" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <line x1="4" y1="10" x2="16" y2="10" />
              <polyline points="11 5 16 10 11 15" />
            </svg>
          </button>
        </div>
      </section>

      {/* ── Footer ── */}
      <footer className="landing-footer">
        <span>© {new Date().getFullYear()} Circles</span>
        <span className="landing-footer-dot">·</span>
        <span>Private by design</span>
      </footer>
    </div>
  );
}
