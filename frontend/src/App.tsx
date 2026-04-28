import React, { useEffect, useState, useCallback, useRef } from 'react';
import BankingUserApp from './BankingUserApp';
import OsMonitor from './OsMonitor';
import { connectWebSocket } from './websocket';
import { useBankStore } from './store';

function App() {
  const [monitorWidth, setMonitorWidth] = useState(450);
  const [isResizing, setIsResizing] = useState(false);
  const resizeRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const client = connectWebSocket();
    useBankStore.getState().addLog("Booting MiniBankOS...");
    useBankStore.getState().addLog("Kernel loaded. Awaiting system events.");
    
    return () => {
      client.deactivate();
    };
  }, []);

  const startResizing = useCallback(() => {
    setIsResizing(true);
  }, []);

  const stopResizing = useCallback(() => {
    setIsResizing(false);
  }, []);

  const resize = useCallback((mouseMoveEvent: MouseEvent) => {
    if (isResizing) {
      // Calculate new width: window width minus mouse X position
      const newWidth = window.innerWidth - mouseMoveEvent.clientX;
      // Clamp between 300px and 800px
      if (newWidth > 300 && newWidth < 800) {
        setMonitorWidth(newWidth);
      }
    }
  }, [isResizing]);

  useEffect(() => {
    window.addEventListener("mousemove", resize);
    window.addEventListener("mouseup", stopResizing);
    return () => {
      window.removeEventListener("mousemove", resize);
      window.removeEventListener("mouseup", stopResizing);
    };
  }, [resize, stopResizing]);

  return (
    <div className={`flex h-screen w-full bg-slate-100 overflow-hidden ${isResizing ? 'cursor-col-resize select-none' : ''}`}>
      {/* Left Pane: Banking App */}
      <div className="flex-1 h-full z-10 relative overflow-hidden">
        <BankingUserApp />
      </div>
      
      {/* Resize Handle */}
      <div 
        onMouseDown={startResizing}
        className={`w-1.5 h-full hover:bg-blue-500/50 cursor-col-resize transition-colors z-20 flex-shrink-0 ${isResizing ? 'bg-blue-500' : 'bg-slate-300'}`}
      />

      {/* Right Pane: OS Monitor */}
      <div 
        style={{ width: `${monitorWidth}px` }} 
        className="h-full z-0 flex-shrink-0"
      >
        <OsMonitor />
      </div>
    </div>
  );
}

export default App;
