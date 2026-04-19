// frontend/src/app/test-connection/page.tsx
'use client';

import { useState } from 'react';

export default function TestConnectionPage() {
  const [status, setStatus] = useState<'idle' | 'loading' | 'success' | 'error'>('idle');
  const [response, setResponse] = useState<any>(null);
  const [error, setError] = useState<string | null>(null);

  const testConnection = async () => {
    setStatus('loading');
    setError(null);
    
    try {
      const res = await fetch('http://localhost:8080/api/issues', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
        },
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }

      const data = await res.json();
      setResponse(data);
      setStatus('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setStatus('error');
    }
  };

  const testLogin = async () => {
    setStatus('loading');
    setError(null);
    
    try {
      const res = await fetch('http://localhost:8080/auth/login', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          username: 'user',
          password: 'password123'
        }),
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }

      const data = await res.json();
      setResponse(data);
      setStatus('success');
      
      // Store token if returned
      if (data.token) {
        localStorage.setItem('token', data.token);
        console.log('Token stored:', data.token);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setStatus('error');
    }
  };

  const testAuthenticatedRequest = async () => {
    setStatus('loading');
    setError(null);
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError('No token found. Please login first.');
      setStatus('error');
      return;
    }

    try {
      const res = await fetch('http://localhost:8080/api/issues', {
        method: 'GET',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
      });

      if (!res.ok) {
        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
      }

      const data = await res.json();
      setResponse(data);
      setStatus('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setStatus('error');
    }
  };

  const testCreateIssue = async () => {
    setStatus('loading');
    setError(null);
    
    const token = localStorage.getItem('token');
    if (!token) {
      setError('No token found. Please login first.');
      setStatus('error');
      return;
    }

    try {
      const res = await fetch('http://localhost:8080/api/issues', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`,
        },
        body: JSON.stringify({
          title: 'Test Vulnerability from Frontend',
          description: 'This is a test vulnerability created from Next.js',
          severity: 3
        }),
      });

      if (!res.ok) {
        const errorText = await res.text();
        throw new Error(`HTTP ${res.status}: ${errorText}`);
      }

      const data = await res.json();
      setResponse(data);
      setStatus('success');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setStatus('error');
    }
  };

  return (
    <div className="min-h-screen bg-gray-50 py-8 px-4">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">
          Backend Connection Test
        </h1>

        {/* Connection Status */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4 text-gray-900">Connection Status</h2>
          <div className="flex items-center gap-3">
            <div className={`w-4 h-4 rounded-full ${
              status === 'idle' ? 'bg-gray-300' :
              status === 'loading' ? 'bg-yellow-400 animate-pulse' :
              status === 'success' ? 'bg-green-500' :
              'bg-red-500'
            }`} />
            <span className="text-sm font-medium">
              {status === 'idle' ? 'Not tested yet' :
               status === 'loading' ? 'Testing...' :
               status === 'success' ? 'Connected successfully!' :
               'Connection failed'}
            </span>
          </div>
        </div>

        {/* Test Buttons */}
        <div className="bg-white rounded-lg shadow p-6 mb-6">
          <h2 className="text-xl font-semibold mb-4">Test Actions</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <button
              onClick={testConnection}
              disabled={status === 'loading'}
              className="bg-blue-600 text-white px-4 py-2 rounded-md hover:bg-blue-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              1. Test Basic Connection (No Auth)
            </button>

            <button
              onClick={testLogin}
              disabled={status === 'loading'}
              className="bg-green-600 text-white px-4 py-2 rounded-md hover:bg-green-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              2. Test Login & Get Token
            </button>

            <button
              onClick={testAuthenticatedRequest}
              disabled={status === 'loading'}
              className="bg-purple-600 text-white px-4 py-2 rounded-md hover:bg-purple-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              3. Test Authenticated Request
            </button>

            <button
              onClick={testCreateIssue}
              disabled={status === 'loading'}
              className="bg-orange-600 text-white px-4 py-2 rounded-md hover:bg-orange-700 disabled:bg-gray-400 disabled:cursor-not-allowed transition"
            >
              4. Test Create Issue (POST)
            </button>
          </div>
        </div>

        {/* Error Display */}
        {error && (
          <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6">
            <h3 className="text-red-800 font-semibold mb-2">Error</h3>
            <p className="text-red-700 text-sm font-mono">{error}</p>
            <div className="mt-4 text-sm text-red-600">
              <p className="font-semibold mb-2">Troubleshooting:</p>
              <ul className="list-disc list-inside space-y-1">
                <li>Is the backend running on port 8080?</li>
                <li>Check CORS configuration in SecurityConfig.java</li>
                <li>Verify the endpoint exists in your controller</li>
                <li>Check browser console for detailed error</li>
              </ul>
            </div>
          </div>
        )}

        {/* Response Display */}
        {response && (
          <div className="bg-white rounded-lg shadow p-6">
            <h3 className="text-lg font-semibold mb-3">Response</h3>
            <pre className="bg-gray-100 rounded p-4 overflow-x-auto text-sm">
              {JSON.stringify(response, null, 2)}
            </pre>
          </div>
        )}

        {/* Info Panel */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-4 mt-6">
          <h3 className="text-blue-900 font-semibold mb-2">Testing Instructions</h3>
          <ol className="list-decimal list-inside space-y-2 text-sm text-blue-800">
            <li>Make sure your Spring Boot backend is running on <code className="bg-blue-100 px-1 rounded">http://localhost:8080</code></li>
            <li>Click "Test Basic Connection" - this will likely fail due to authentication</li>
            <li>Click "Test Login & Get Token" - this should return a JWT token</li>
            <li>Click "Test Authenticated Request" - this should return your issues</li>
            <li>Click "Test Create Issue" - this should create a new vulnerability</li>
          </ol>
        </div>

        {/* Backend Info */}
        <div className="bg-gray-100 rounded-lg p-4 mt-6">
          <h3 className="font-semibold mb-2 text-sm text-gray-700">Expected Backend Configuration</h3>
          <div className="text-xs text-gray-600 space-y-1 font-mono">
            <p>• Backend URL: http://localhost:8080</p>
            <p>• Login endpoint: POST /auth/login</p>
            <p>• Issues endpoint: GET/POST /api/issues</p>
            <p>• CORS: Allow origin http://localhost:3000</p>
          </div>
        </div>
      </div>
    </div>
  );
}