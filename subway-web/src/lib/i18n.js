import { writable, derived } from 'svelte/store';
import { browser } from '$app/environment';

const ko = {
  nav: {
    congestion: '혼잡도',
    line: '노선도',
    route: '경로',
  },
  auth: {
    login: '로그인',
    logout: '로그아웃',
    register: '회원가입',
    email: '이메일',
    password: '비밀번호',
    nickname: '닉네임',
    loginBtn: '로그인',
    registerBtn: '회원가입',
    noAccount: '계정이 없으신가요?',
    hasAccount: '이미 계정이 있으신가요?',
  },
  community: {
    title: '커뮤니티',
    write: '글쓰기',
    comment: '댓글',
    like: '좋아요',
    submitComment: '등록',
    noPost: '아직 게시글이 없어요',
    noComment: '아직 댓글이 없어요',
    loginToComment: '로그인 후 댓글을 작성할 수 있습니다',
  },
  complaint: {
    title: '민원 접수',
    myComplaints: '내 민원',
    category: '카테고리',
    station: '역명',
    content: '내용',
    submit: '접수하기',
    status: {
      RECEIVED: '접수완료',
      IN_PROGRESS: '처리중',
      COMPLETED: '처리완료',
      REJECTED: '반려',
    },
  },
  congestion: {
    title: '실시간 혼잡도',
    search: '역 검색',
    favorite: '즐겨찾기',
    addFavorite: '즐겨찾기 추가',
    noData: '운행 정보가 없습니다',
    level: {
      low: '여유',
      mid: '보통',
      high: '혼잡',
      full: '매우혼잡',
      unknown: '정보없음',
    },
  },
  route: {
    title: '경로 탐색',
    from: '출발역',
    to: '도착역',
    add: '경로 추가',
    transfer: '환승',
    arrive: '도착',
    realtime: '실시간',
    noRoute: '경로를 찾을 수 없어요',
  },
  common: {
    loading: '로딩 중...',
    error: '오류가 발생했습니다',
    cancel: '취소',
    confirm: '확인',
    delete: '삭제',
    save: '저장',
    back: '뒤로',
  },
};

const en = {
  nav: {
    congestion: 'Congestion',
    line: 'Line Map',
    route: 'Route',
  },
  auth: {
    login: 'Login',
    logout: 'Logout',
    register: 'Register',
    email: 'Email',
    password: 'Password',
    nickname: 'Nickname',
    loginBtn: 'Login',
    registerBtn: 'Register',
    noAccount: "Don't have an account?",
    hasAccount: 'Already have an account?',
  },
  community: {
    title: 'Community',
    write: 'Write',
    comment: 'Comment',
    like: 'Like',
    submitComment: 'Post',
    noPost: 'No posts yet',
    noComment: 'No comments yet',
    loginToComment: 'Login to write a comment',
  },
  complaint: {
    title: 'Report Issue',
    myComplaints: 'My Reports',
    category: 'Category',
    station: 'Station',
    content: 'Content',
    submit: 'Submit',
    status: {
      RECEIVED: 'Received',
      IN_PROGRESS: 'In Progress',
      COMPLETED: 'Completed',
      REJECTED: 'Rejected',
    },
  },
  congestion: {
    title: 'Live Congestion',
    search: 'Search station',
    favorite: 'Favorites',
    addFavorite: 'Add favorite',
    noData: 'No service info',
    level: {
      low: 'Comfortable',
      mid: 'Moderate',
      high: 'Crowded',
      full: 'Very Crowded',
      unknown: 'Unknown',
    },
  },
  route: {
    title: 'Route Finder',
    from: 'From',
    to: 'To',
    add: 'Add Route',
    transfer: 'Transfer',
    arrive: 'Arrive',
    realtime: 'Live',
    noRoute: 'No route found',
  },
  common: {
    loading: 'Loading...',
    error: 'An error occurred',
    cancel: 'Cancel',
    confirm: 'Confirm',
    delete: 'Delete',
    save: 'Save',
    back: 'Back',
  },
};

const MESSAGES = { ko, en };

const initialLocale = browser
  ? (localStorage.getItem('metrohub_locale') || 'ko')
  : 'ko';

export const locale = writable(initialLocale);

locale.subscribe(l => {
  if (browser) localStorage.setItem('metrohub_locale', l);
});

export const t = derived(locale, $locale => {
  const msgs = MESSAGES[$locale] || MESSAGES.ko;
  return (key) => {
    const parts = key.split('.');
    let val = msgs;
    for (const p of parts) {
      val = val?.[p];
      if (val === undefined) return key;
    }
    return val ?? key;
  };
});

export function toggleLocale() {
  locale.update(l => l === 'ko' ? 'en' : 'ko');
}
