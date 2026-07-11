import React, { useState, useEffect, useRef, useCallback } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { API_BASE_URL, getErrorMessage } from '../utils/api';

const DOCUMENT_STATUS = {
  IDLE: 'idle',
  LOADING: 'loading',
  SUCCESS: 'success',
  ERROR: 'error'
};

function formatFileSize(bytes) {
  if (!Number.isFinite(Number(bytes))) return 'Unknown size';

  const size = Number(bytes);
  if (size === 0) return '0 B';

  const units = ['B', 'KB', 'MB', 'GB'];
  const unitIndex = Math.min(Math.floor(Math.log(size) / Math.log(1024)), units.length - 1);
  const value = size / 1024 ** unitIndex;

  return `${value.toFixed(value >= 10 || unitIndex === 0 ? 0 : 1)} ${units[unitIndex]}`;
}

function formatUploadedDate(value) {
  if (!value) return 'Unknown date';

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return 'Unknown date';

  return new Intl.DateTimeFormat(undefined, {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: 'numeric',
    minute: '2-digit'
  }).format(date);
}

function getDocumentVisual(fileType = '', fileName = '') {
  const normalizedType = fileType.toLowerCase();
  const extension = fileName.split('.').pop()?.toLowerCase() || '';

  if (normalizedType.includes('pdf') || extension === 'pdf') {
    return { label: 'PDF', className: 'pdf', path: 'M7 3h7l5 5v13H7z M14 3v5h5 M9 14h6 M9 17h4' };
  }

  if (normalizedType.includes('word') || ['doc', 'docx'].includes(extension)) {
    return { label: 'DOC', className: 'doc', path: 'M7 3h7l5 5v13H7z M14 3v5h5 M9 13l1.2 5 1.4-4 1.4 4 1.2-5' };
  }

  if (normalizedType.includes('image') || ['jpg', 'jpeg', 'png'].includes(extension)) {
    return { label: 'IMG', className: 'image', path: 'M6 5h12v14H6z M9 10.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z M7.5 17l3.5-4 2.5 2.8 1.5-1.8 2.5 3' };
  }

  return { label: 'FILE', className: 'generic', path: 'M7 3h7l5 5v13H7z M14 3v5h5 M9 14h6 M9 17h6' };
}

export default function Dashboard() {
  const navigate = useNavigate();
  const { id } = useParams(); // Current open circle ID from URL

  const [circles, setCircles] = useState([]);
  const [loading, setLoading] = useState(true);

  // Modals state
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [createError, setCreateError] = useState('');
  
  const [showInviteModal, setShowInviteModal] = useState(false);
  const [inviteError, setInviteError] = useState('');
  const [inviteLink, setInviteLink] = useState('Loading...');
  const [copied, setCopied] = useState(false);

  // Circle View state
  const [documents, setDocuments] = useState([]);
  const [documentsStatus, setDocumentsStatus] = useState(DOCUMENT_STATUS.IDLE);
  const [documentsError, setDocumentsError] = useState('');
  const [downloadingDocumentId, setDownloadingDocumentId] = useState(null);
  const [failedThumbnails, setFailedThumbnails] = useState(new Set());
  const [localPreviews, setLocalPreviews] = useState({});

  // Active circle detail
  const activeCircle = circles.find(c => c.id.toString() === id);
  const activeCircleId = activeCircle?.id;

  // File Upload and Toast States
  const fileInputRef = useRef(null);
  const [isUploading, setIsUploading] = useState(false);
  const [toasts, setToasts] = useState([]);

  const showToast = (message, type = 'success') => {
    const toastId = Date.now();
    setToasts(prev => [...prev, { id: toastId, message, type }]);
    setTimeout(() => {
      setToasts(prev => prev.filter(t => t.id !== toastId));
    }, 4000);
  };

  const handleTriggerUpload = () => {
    if (fileInputRef.current) {
      fileInputRef.current.click();
    }
  };

  const loadCircles = useCallback(async () => {
    try {
      const response = await fetch(`${API_BASE_URL}/GetCircles`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.status === 401 || response.status === 403) {
        navigate('/login');
        return;
      }

      const result = await response.json();
      if (response.ok && result.succMessage === 'SUCCESS') {
        setCircles(result.data || []);
      }
    } catch (error) {
      console.error("Failed to load circles", error);
    } finally {
      setLoading(false);
    }
  }, [navigate]);

  const loadDocuments = useCallback(async (circleId, signal) => {
    if (!circleId) return;

    setDocumentsStatus(DOCUMENT_STATUS.LOADING);
    setDocumentsError('');

    try {
      const response = await fetch(`${API_BASE_URL}/file/${circleId}/view/documents`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        signal
      });

      if (response.status === 401 || response.status === 403) {
        navigate('/login');
        return;
      }

      const result = await response.json();

      if (!response.ok) {
        throw new Error(getErrorMessage(result) || 'Failed to load documents');
      }

      const nextDocuments = Array.isArray(result) ? result : result.data;
      setDocuments(Array.isArray(nextDocuments) ? nextDocuments : []);
      setDocumentsStatus(DOCUMENT_STATUS.SUCCESS);
    } catch (error) {
      if (error.name === 'AbortError') return;

      console.error('Failed to load documents', error);
      setDocuments([]);
      setDocumentsError(error.message || 'Server connection failed while loading documents');
      setDocumentsStatus(DOCUMENT_STATUS.ERROR);
    }
  }, [navigate]);

  const handleFileChange = async (e) => {
    const file = e.target.files[0];
    if (!file || !activeCircleId) return;

    // Reset input value so same file can be selected again
    e.target.value = '';

    // Validate size (10 MB limit)
    const maxSize = 10 * 1024 * 1024;
    if (file.size > maxSize) {
      showToast('File is too large (max limit is 10MB)', 'error');
      return;
    }

    // Validate type (matching backend supported types)
    const allowedTypes = new Set([
      'image/jpeg',
      'image/png',
      'application/pdf',
      'application/msword',
      'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
    ]);
    
    if (!allowedTypes.has(file.type)) {
      showToast('Unsupported file format (allowed: JPEG, PNG, PDF, DOC/DOCX)', 'error');
      return;
    }

    setIsUploading(true);
    showToast(`Uploading ${file.name}...`, 'success');

    if (file.type.startsWith('image/')) {
      const localUrl = URL.createObjectURL(file);
      const fileKey = `${file.name}-${file.size}`;
      setLocalPreviews(prev => ({
        ...prev,
        [fileKey]: localUrl
      }));
    }

    const formData = new FormData();
    formData.append('file', file);

    try {
      const response = await fetch(`${API_BASE_URL}/file/${activeCircleId}/upload`, {
        method: 'POST',
        body: formData,
        credentials: 'include'
      });

      const result = await response.json();

      if (response.ok && result.succMessage) {
        showToast('File uploaded successfully!', 'success');
        await loadDocuments(activeCircleId);
      } else {
        const errorMsg = getErrorMessage(result) || result.errorMessage || 'Failed to upload file';
        showToast(errorMsg, 'error');
      }
    } catch {
      showToast('Server connection failed during upload', 'error');
    } finally {
      setIsUploading(false);
    }
  };

  const handleDocumentDownload = async (document) => {
    if (!activeCircleId || !document?.id || downloadingDocumentId) return;

    setDownloadingDocumentId(document.id);

    try {
      const response = await fetch(`${API_BASE_URL}/file/${activeCircleId}/${document.id}/download`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      if (response.status === 401) {
        navigate('/login');
        return;
      }

      const result = await response.json();

      if (!response.ok || result.errorMessage) {
        showToast(getErrorMessage(result) || 'Failed to download file', 'error');
        return;
      }

      const downloadUrl = result.data?.downloadUrl;
      if (!downloadUrl) {
        showToast('Download link was not returned by the server', 'error');
        return;
      }

      const link = window.document.createElement('a');
      link.href = downloadUrl;
      link.download = result.data?.fileName || document.fileName || '';
      link.rel = 'noopener noreferrer';
      window.document.body.appendChild(link);
      link.click();
      link.remove();
      showToast('Download started', 'success');
    } catch {
      showToast('Server connection failed while preparing download', 'error');
    } finally {
      setDownloadingDocumentId(null);
    }
  };

  useEffect(() => {
    loadCircles();
  }, [loadCircles]);

  useEffect(() => {
    if (!activeCircleId) {
      setDocuments([]);
      setDocumentsStatus(DOCUMENT_STATUS.IDLE);
      setDocumentsError('');
      setFailedThumbnails(new Set());
      setLocalPreviews(prev => {
        Object.values(prev).forEach(url => {
          try {
            URL.revokeObjectURL(url);
          } catch (e) {
            console.error("Failed to revoke object URL", e);
          }
        });
        return {};
      });
      return;
    }

    const controller = new AbortController();
    setFailedThumbnails(new Set());
    setLocalPreviews(prev => {
      Object.values(prev).forEach(url => {
        try {
          URL.revokeObjectURL(url);
        } catch (e) {
          console.error("Failed to revoke object URL", e);
        }
      });
      return {};
    });
    loadDocuments(activeCircleId, controller.signal);

    return () => controller.abort();
  }, [activeCircleId, loadDocuments]);

  const handleLogout = async () => {
    try {
      await fetch(`${API_BASE_URL}/logout`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });
    } catch (e) {
      console.error('Logout failed', e);
    } finally {
      navigate('/login');
    }
  };

  const handleCreateCircle = async (e) => {
    e.preventDefault();
    setCreateError('');
    const formData = new FormData(e.target);
    const data = Object.fromEntries(formData.entries());

    try {
      const response = await fetch(`${API_BASE_URL}/CreateCircle`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(data),
        credentials: 'include'
      });

      const result = await response.json();
      if (response.ok && result.succMessage) {
        setShowCreateModal(false);
        e.target.reset();
        await loadCircles();
      } else {
        setCreateError(getErrorMessage(result) || 'Failed to create circle');
      }
    } catch {
      setCreateError('Server connection failed');
    }
  };

  const handleOpenInvite = async () => {
    if (!activeCircle) return;
    setShowInviteModal(true);
    setInviteLink('Loading...');
    setInviteError('');
    setCopied(false);

    try {
      const response = await fetch(`${API_BASE_URL}/InviteUser?circleId=${activeCircle.id}`, {
        method: 'GET',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include'
      });

      const result = await response.json();
      if (response.ok && result.succMessage) {
        setInviteLink(result.inviteLink || result.data || result.link || 'Link generated');
      } else {
        setInviteError(getErrorMessage(result) || 'Failed to generate invite link');
        setInviteLink('');
      }
    } catch {
      setInviteError('Server connection failed');
      setInviteLink('');
    }
  };

  const handleCopy = () => {
    if (inviteLink && inviteLink !== 'Loading...') {
      navigator.clipboard.writeText(inviteLink).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      }).catch(() => {
        setInviteError('Failed to copy');
      });
    }
  };

  return (
    <>
      <nav className="navbar">
        <div className="nav-content">
          <div className="nav-brand" onClick={() => navigate('/dashboard')} style={{cursor: 'pointer'}}>Circles</div>
          <button id="logout-btn" onClick={handleLogout}>Logout</button>
        </div>
      </nav>

      <main className="dashboard-main">
        {!activeCircle ? (
          <>
            <header className="dashboard-header">
              <h1>Your Circles</h1>
              <p>Select a circle or create a new one to get started.</p>
            </header>

            {loading ? (
              <p>Loading circles...</p>
            ) : (
              <section className="circles-grid">
                {circles.map(c => (
                  <div key={c.id} className="circle-card" onClick={() => navigate(`/circle/${c.id}`)}>
                    <div>
                      <h3>{c.name}</h3>
                      <p>ID: {c.id}</p>
                    </div>
                  </div>
                ))}
                
                <div className="circle-card create-card" onClick={() => setShowCreateModal(true)}>
                  <div className="create-icon">
                    <svg viewBox="0 0 24 24" width="32" height="32" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                      <line x1="12" y1="5" x2="12" y2="19"></line>
                      <line x1="5" y1="12" x2="19" y2="12"></line>
                    </svg>
                  </div>
                  <h3>Create Circle</h3>
                </div>
              </section>
            )}
          </>
        ) : (
          <section className="circle-view">
            <div className="circle-header-bar">
              <div className="circle-header-left">
                <button className="icon-btn back-btn" onClick={() => navigate('/dashboard')} title="Back to Dashboard">
                  <svg viewBox="0 0 24 24" width="24" height="24" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <line x1="19" y1="12" x2="5" y2="12"></line>
                    <polyline points="12 19 5 12 12 5"></polyline>
                  </svg>
                </button>
                <h2>{activeCircle.name}</h2>
              </div>
              <div className="circle-header-right">
                <input 
                  type="file" 
                  ref={fileInputRef} 
                  onChange={handleFileChange} 
                  style={{ display: 'none' }} 
                  accept=".jpg,.jpeg,.png,.pdf,.doc,.docx,image/jpeg,image/png,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                />
                <button 
                  className={`icon-btn upload-btn ${isUploading ? 'pulse' : ''}`} 
                  onClick={handleTriggerUpload} 
                  disabled={isUploading} 
                  title="Upload File"
                >
                  <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
                    <polyline points="17 8 12 3 7 8"></polyline>
                    <line x1="12" y1="3" x2="12" y2="15"></line>
                  </svg>
                  <span>{isUploading ? 'Uploading...' : 'Upload'}</span>
                </button>
                <button className="icon-btn invite-btn" onClick={handleOpenInvite} title="Invite User" disabled={isUploading}>
                  <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path>
                    <circle cx="8.5" cy="7" r="4"></circle>
                    <line x1="20" y1="8" x2="20" y2="14"></line>
                    <line x1="23" y1="11" x2="17" y2="11"></line>
                  </svg>
                  <span>Invite</span>
                </button>
              </div>
            </div>
            
            <div className="documents-container">
              <div className="documents-title-row">
                <div>
                  <h3>Documents</h3>
                  <p>{documents.length} {documents.length === 1 ? 'file' : 'files'} in this circle</p>
                </div>
                <button className="refresh-documents-btn" onClick={() => loadDocuments(activeCircle.id)} disabled={documentsStatus === DOCUMENT_STATUS.LOADING}>
                  <svg viewBox="0 0 24 24" width="18" height="18" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="23 4 23 10 17 10"></polyline>
                    <polyline points="1 20 1 14 7 14"></polyline>
                    <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10"></path>
                    <path d="M20.49 15a9 9 0 0 1-14.85 3.36L1 14"></path>
                  </svg>
                </button>
              </div>

              {documentsStatus === DOCUMENT_STATUS.LOADING && (
                <div className="documents-grid">
                  {[1, 2, 3].map(item => (
                    <div key={item} className="document-card document-card-skeleton">
                      <div className="document-icon skeleton-block"></div>
                      <div className="document-skeleton-lines">
                        <span className="skeleton-line wide"></span>
                        <span className="skeleton-line narrow"></span>
                      </div>
                    </div>
                  ))}
                </div>
              )}

              {documentsStatus === DOCUMENT_STATUS.ERROR && (
                <div className="empty-state error-state">
                  <p>{documentsError}</p>
                  <button onClick={() => loadDocuments(activeCircle.id)}>Try again</button>
                </div>
              )}

              {documentsStatus === DOCUMENT_STATUS.SUCCESS && documents.length === 0 && (
                <div className="empty-state">
                  <p>No documents found.</p>
                </div>
              )}

              {documentsStatus === DOCUMENT_STATUS.SUCCESS && documents.length > 0 && (
                <div className="documents-grid">
                  {documents.map(document => {
                    const visual = getDocumentVisual(document.fileType, document.fileName);
                    const fileKey = `${document.fileName}-${document.fileSize}`;
                    const previewURL = (document.fileType?.startsWith('image/') && localPreviews[fileKey]) 
                      ? localPreviews[fileKey] 
                      : document.fileURL;

                    return (
                      <article
                        key={document.id || `${document.fileName}-${document.uploadedAt}`}
                        className={`document-card ${downloadingDocumentId === document.id ? 'document-card-downloading' : ''}`}
                        onClick={() => handleDocumentDownload(document)}
                        onKeyDown={(event) => {
                          if (event.key === 'Enter' || event.key === ' ') {
                            event.preventDefault();
                            handleDocumentDownload(document);
                          }
                        }}
                        role="button"
                        tabIndex={0}
                        aria-label={`Download ${document.fileName || 'document'}`}
                      >
                        {previewURL && !failedThumbnails.has(document.id) ? (
                          <div className="document-thumbnail-container">
                            <img 
                              src={previewURL} 
                              alt={document.fileName} 
                              className="document-thumbnail-img" 
                              loading="lazy" 
                              onError={() => {
                                setFailedThumbnails(prev => {
                                  const next = new Set(prev);
                                  next.add(document.id);
                                  return next;
                                });
                              }}
                            />
                          </div>
                        ) : (
                          <div className={`document-icon document-icon-${visual.className}`}>
                            <svg viewBox="0 0 24 24" width="34" height="34" stroke="currentColor" strokeWidth="1.8" fill="none" strokeLinecap="round" strokeLinejoin="round">
                              <path d={visual.path}></path>
                            </svg>
                            <span>{visual.label}</span>
                          </div>
                        )}
                        {downloadingDocumentId === document.id && (
                          <div className="document-download-badge">Preparing...</div>
                        )}
                        <div className="document-summary">
                          <h4 title={document.fileName}>{document.fileName || 'Untitled document'}</h4>
                          <p>{document.fileType || 'Unknown type'}</p>
                        </div>
                        <dl className="document-details">
                          <div>
                            <dt>Size</dt>
                            <dd>{formatFileSize(document.fileSize)}</dd>
                          </div>
                          <div>
                            <dt>Uploaded</dt>
                            <dd>{formatUploadedDate(document.uploadedAt)}</dd>
                          </div>
                          <div>
                            <dt>By</dt>
                            <dd>{document.uploadedBy || 'Unknown'}</dd>
                          </div>
                        </dl>
                      </article>
                    );
                  })}
                </div>
              )}
            </div>
          </section>
        )}
      </main>

      {/* Create Modal */}
      <div className={`modal-overlay ${showCreateModal ? 'active' : ''}`} onClick={(e) => { if (e.target.classList.contains('modal-overlay')) setShowCreateModal(false); }}>
        <div className="modal-content">
          <div className="modal-header">
            <h2>Create New Circle</h2>
            <button className="close-modal" onClick={() => setShowCreateModal(false)}>&times;</button>
          </div>
          
          {createError && <div className="error-message" style={{ display: 'block' }}>{createError}</div>}
          
          <form onSubmit={handleCreateCircle}>
            <div className="form-group">
              <label htmlFor="circle-name">Circle Name</label>
              <input type="text" id="circle-name" name="name" placeholder="e.g. Project Alpha" required />
            </div>
            <button type="submit" className="submit-btn">Create</button>
          </form>
        </div>
      </div>

      {/* Invite Modal */}
      <div className={`modal-overlay ${showInviteModal ? 'active' : ''}`} onClick={(e) => { if (e.target.classList.contains('modal-overlay')) setShowInviteModal(false); }}>
        <div className="modal-content invite-content">
          <div className="modal-header">
            <h2>Invite to Circle</h2>
            <button className="close-modal" onClick={() => setShowInviteModal(false)}>&times;</button>
          </div>
          
          {inviteError && <div className="error-message" style={{ display: 'block' }}>{inviteError}</div>}
          
          <div className="invite-body">
            <p>Share this link to invite others to <span style={{fontWeight: 600, color: 'var(--primary)'}}>{activeCircle?.name}</span>.</p>
            <div className="link-container">
              <input type="text" readOnly value={inviteLink} />
              <button className={`copy-btn ${copied ? 'copied' : ''}`} onClick={handleCopy} title="Copy to clipboard">
                {copied ? (
                  <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <polyline points="20 6 9 17 4 12"></polyline>
                  </svg>
                ) : (
                  <svg viewBox="0 0 24 24" width="20" height="20" stroke="currentColor" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round">
                    <rect x="9" y="9" width="13" height="13" rx="2" ry="2"></rect>
                    <path d="M5 15H4a2 2 0 0 1-2-2V4a2 2 0 0 1 2-2h9a2 2 0 0 1 2 2v1"></path>
                  </svg>
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div className="toast-container">
        {toasts.map(t => (
          <div key={t.id} className={`toast toast-${t.type}`}>
            <span className="toast-message">{t.message}</span>
            <button className="toast-close" onClick={() => setToasts(prev => prev.filter(item => item.id !== t.id))}>&times;</button>
          </div>
        ))}
      </div>
    </>
  );
}
