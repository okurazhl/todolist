import { create } from 'zustand';
import { getAccessToken, saveAuth, clearAuth } from '../../shared/api/client';

interface AuthState {
  isLoggedIn: boolean;
  username: string | null;
  login: (accessToken: string, refreshToken: string, username: string) => void;
  logout: () => void;
  checkAuth: () => boolean;
}

export const useAuthStore = create<AuthState>((set) => ({
  isLoggedIn: !!getAccessToken(),
  username: null,

  login: (accessToken, refreshToken, username) => {
    saveAuth(accessToken, refreshToken);
    set({ isLoggedIn: true, username });
  },

  logout: () => {
    clearAuth();
    set({ isLoggedIn: false, username: null });
  },

  checkAuth: () => {
    const hasToken = !!getAccessToken();
    set({ isLoggedIn: hasToken });
    return hasToken;
  },
}));
