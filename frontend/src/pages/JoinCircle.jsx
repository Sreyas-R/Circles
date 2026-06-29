import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { API_BASE_URL, getErrorMessage } from '../utils/api';

export default function JoinCircle() {
  const { token } = useParams();
  const navigate = useNavigate();
  const [status, setStatus] = useState('Joining circle...');

  useEffect(() => {
    const join = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/joinCircle/${token}`, {
          method: 'GET',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include'
        });

        const result = await response.json();
        
        if (response.status === 401 || response.status === 403) {
          // Store token in session storage to retry after login if we wanted to
          sessionStorage.setItem('pendingJoinToken', token);
          setStatus('Please login to join this circle.');
          setTimeout(() => navigate('/login'), 2000);
          return;
        }

        if (response.ok && result.succMessage === 'SUCCESS') {
          setStatus('Successfully joined the circle! Redirecting...');
          setTimeout(() => navigate('/dashboard'), 1500);
        } else {
          setStatus(getErrorMessage(result) || 'Failed to join circle.');
          setTimeout(() => navigate('/dashboard'), 3000);
        }
      } catch {
        setStatus('Server connection failed. Could not join circle.');
        setTimeout(() => navigate('/dashboard'), 3000);
      }
    };

    if (token) {
      join();
    }
  }, [token, navigate]);

  return (
    <div className="auth-page">
      <main className="auth-container">
        <header className="auth-header">
          <h1>{status}</h1>
          <p>Please wait a moment...</p>
        </header>
      </main>
    </div>
  );
}
