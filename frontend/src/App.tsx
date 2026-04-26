import React, { useEffect } from 'react';
import BankingUserApp from './BankingUserApp';
import OsMonitor from './OsMonitor';
import { connectWebSocket } from './websocket';
import { useBankStore } from './store';

function App() {
  useEffect(() => {
    const client = connectWebSocket();
    useBankStore.getState().addLog("Booting MiniBankOS...");
    useBankStore.getState().addLog("Kernel loaded. Awaiting system events.");
    
    return () => {
      client.deactivate();
    };
  }, []);

  return (
    <div className="flex h-screen w-full bg-slate-100 overflow-hidden">
      {/* Left Pane: Banking App */}
      <div className="flex-1 h-full z-10 relative">
        <BankingUserApp />
      </div>
      
      {/* Right Pane: OS Monitor */}
      <div className="w-[500px] h-full z-0 flex-shrink-0">
        <OsMonitor />
      </div>
    </div>
  );
}

export default App;
