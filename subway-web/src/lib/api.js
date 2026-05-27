const API_BASE = 'http://localhost:8080';

async function request(method, path, body = null, token = null) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_BASE}${path}`, {
    method,
    headers,
    body: body ? JSON.stringify(body) : undefined
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.error || `요청 실패 (${res.status})`);
  }

  return res.status === 204 ? null : res.json();
}

export const api = {
  // 인증
  login:    (data)        => request('POST', '/api/users/login', data),
  register: (data)        => request('POST', '/api/users/register', data),
  me:       (token)       => request('GET',  '/api/users/me', null, token),

  // 혼잡도
  congestionByStation: (station) => request('GET', `/api/congestion/station/${encodeURIComponent(station)}`),
  congestionByLine:    (line)    => request('GET', `/api/congestion/line/${encodeURIComponent(line)}`),

  // 커뮤니티
  getPosts:      (line, page = 0) => request('GET', `/api/community/line/${line}/posts?page=${page}&size=10`),
  getPost:       (id)             => request('GET', `/api/community/posts/${id}`),
  createPost:    (data, token)    => request('POST', '/api/community/posts', data, token),
  getComments:   (postId)         => request('GET', `/api/community/posts/${postId}/comments`),
  createComment: (postId, data, token) => request('POST', `/api/community/posts/${postId}/comments`, data, token),

  // 민원
  createComplaint: (data, token)  => request('POST', '/api/complaints', data, token),
  myComplaints:    (token)        => request('GET',  '/api/complaints/my', null, token),
  getComplaint:    (id, token)    => request('GET',  `/api/complaints/${id}`, null, token),
};
