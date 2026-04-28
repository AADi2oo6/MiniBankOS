import { create } from 'zustand';

interface BankState {
  osLogs: string[];
  currentUser: string | null;
  addLog: (log: string) => void;
  clearLogs: () => void;
  setCurrentUser: (user: string | null) => void;
}

export const useBankStore = create<BankState>((set) => ({
  osLogs: [],
  currentUser: null,
  addLog: (log: string) => set((state) => ({ 
    osLogs: [...state.osLogs.slice(-100), log] // Keep last 100 logs
  })),
  clearLogs: () => set({ osLogs: [] }),
  setCurrentUser: (user) => set({ currentUser: user }),
})),
