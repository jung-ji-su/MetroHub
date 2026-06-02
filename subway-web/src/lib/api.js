const API_BASE           = import.meta.env.VITE_API_BASE           || '';
const NOTIFICATION_BASE  = import.meta.env.VITE_NOTIFICATION_BASE  || '';

// 순환 의존 방지: store는 동적으로 import
let _authStore = null;
async function getAuthStore() {
  if (!_authStore) _authStore = (await import('./stores')).auth;
  return _authStore;
}

async function tryRefresh() {
  try {
    const { get } = await import('svelte/store');
    const authStore = await getAuthStore();
    const current = get(authStore);
    if (!current?.refreshToken) return null;
    const res = await fetch(`${API_BASE}/api/users/refresh`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken: current.refreshToken })
    });
    if (!res.ok) { authStore.logout(); return null; }
    const data = await res.json();
    authStore.login({ ...current, token: data.token, refreshToken: data.refreshToken });
    return data.token;
  } catch (_) {
    return null;
  }
}

async function request(method, path, body = null, token = null, baseUrl = API_BASE, _retried = false) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  let res;
  try {
    res = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body ? JSON.stringify(body) : undefined
    });
  } catch (_) {
    throw new Error('서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.');
  }

  // 액세스 토큰 만료 시 자동 갱신 후 재시도
  if (res.status === 401 && !_retried && token) {
    const newToken = await tryRefresh();
    if (newToken) return request(method, path, body, newToken, baseUrl, true);
  }

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
  refresh:  (refreshToken) => request('POST', '/api/users/refresh', { refreshToken }),
  logout:   (refreshToken) => request('POST', '/api/users/logout', { refreshToken }),

  // 혼잡도
  congestionByStation: (station) => request('GET', `/api/congestion/station/${encodeURIComponent(station)}`),
  congestionByLine:    (line)    => request('GET', `/api/congestion/line/${encodeURIComponent(line)}`),

  // 실시간 노선도
  lineTrains: (lineCode) => request('GET', `/api/line/${lineCode}/trains`),

  // 커뮤니티
  getPosts:      (line, page = 0, size = 10) => request('GET', `/api/community/line/${line}/posts?page=${page}&size=${size}`),
  getPost:       (id)             => request('GET', `/api/community/posts/${id}`),
  createPost:    (data, token)    => request('POST', '/api/community/posts', data, token),
  deletePost:    (postId, token)  => request('DELETE', `/api/community/posts/${postId}`, null, token),
  getComments:   (postId)              => request('GET',    `/api/community/posts/${postId}/comments`),
  createComment: (postId, data, token) => request('POST',   `/api/community/posts/${postId}/comments`, data, token),
  updateComment: (postId, commentId, data, token) => request('PATCH', `/api/community/posts/${postId}/comments/${commentId}`, data, token),
  deleteComment: (postId, commentId, token) => request('DELETE', `/api/community/posts/${postId}/comments/${commentId}`, null, token),
  myPosts:       (token, page = 0)     => request('GET',    `/api/community/posts/my?page=${page}&size=20`, null, token),
  toggleLike:    (postId, token)       => request('POST',   `/api/community/posts/${postId}/like`, null, token),
  getLikeStatus: (postId, token)       => request('GET',    `/api/community/posts/${postId}/like`, null, token),

  // 민원
  createComplaint: (data, token)  => request('POST',   '/api/complaints', data, token),
  myComplaints:    (token)        => request('GET',    '/api/complaints/my', null, token),
  getComplaint:    (id, token)    => request('GET',    `/api/complaints/${id}`, null, token),
  deleteComplaint: (id, token)    => request('DELETE', `/api/complaints/${id}`, null, token),

  // 시간대별 혼잡도
  congestionHourly: (station) => request('GET', `/api/congestion/station/${encodeURIComponent(station)}/hourly`),

  // 알림 구독 (notification service)
  getSubscriptions:         (token)         => request('GET',    '/api/notifications/subscriptions', null, token, NOTIFICATION_BASE),
  addSubscription:          (data, token)   => request('POST',   '/api/notifications/subscriptions', data, token, NOTIFICATION_BASE),
  deleteSubscription:       (id, token)     => request('DELETE', `/api/notifications/subscriptions/${id}`, null, token, NOTIFICATION_BASE),
  getMyNotifications:       (token, page=0) => request('GET',    `/api/notifications/my?page=${page}&size=20`, null, token, NOTIFICATION_BASE),
  markAllNotificationsRead: (token)         => request('POST',   '/api/notifications/my/read-all', null, token, NOTIFICATION_BASE),

  // 관리자
  adminGetComplaints:  (token, status='', page=0, size=20) =>
    request('GET', `/api/admin/complaints?status=${status}&page=${page}&size=${size}`, null, token),
  adminUpdateComplaintStatus: (id, status, token) =>
    request('PATCH', `/api/admin/complaints/${id}/status`, { status }, token),
};
