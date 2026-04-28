import React, { useEffect, useRef } from 'react';
import { useBankStore } from './store';
import { TerminalSquare, Cpu, MemoryStick, Lock } from 'lucide-react';
import { motion } from 'framer-motion';

const OsMonitor: React.FC = () => {
  const { osLogs, clearLogs } = useBankStore();
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [osLogs]);

  // Very simple parsing of the logs to create some visual queues
  const isPageFault = (log: string) => log.includes('[PAGE FAULT]');
  const isBanker = (log: string) => log.includes('[BANKER') || log.includes('Safe Sequence');

  return (
    <div className="flex flex-col h-full bg-slate-950 text-green-400 p-4 border-l border-slate-800 relative overflow-hidden font-mono text-sm shadow-2xl">
      <div className="absolute top-0 right-0 p-4 opacity-10 pointer-events-none">
        <Cpu size={200} />
      </div>

      <div className="flex items-center justify-between mb-6 border-b border-slate-800 pb-2 z-10">
        <div className="flex items-center space-x-2">
          <TerminalSquare className="text-primary" />
          <h2 className="text-xl font-bold tracking-widest text-primary">KERNEL MONITOR</h2>
        </div>
        <button 
          onClick={clearLogs} 
          className="text-[10px] uppercase font-bold tracking-tighter bg-slate-800 hover:bg-slate-700 text-slate-400 px-2 py-1 rounded border border-slate-700 transition"
        >
          Clear Logs
        </button>
      </div>

      {/* Visual OS Stats Area */}
      <div className="grid grid-cols-2 gap-4 mb-6 z-10 h-32">
        <motion.div 
          className="border border-slate-800 bg-slate-900 rounded p-3 relative flex items-center justify-center text-center"
          animate={{ borderColor: osLogs.some(isPageFault) ? ["#ef4444", "#1e293b"] : "#1e293b" }}
          transition={{ duration: 1 }}
        >
           <MemoryStick className="absolute top-2 left-2 text-slate-500 opacity-50" size={16}/>
           <div>
             <div className="text-slate-400 text-xs mb-1">MMU PAGES</div>
             <div className="text-lg">LRU ACTIVE</div>
           </div>
        </motion.div>
        
        <motion.div 
          className="border border-slate-800 bg-slate-900 rounded p-3 relative flex items-center justify-center text-center"
          animate={{ borderColor: osLogs.some(isBanker) ? ["#3b82f6", "#1e293b"] : "#1e293b" }}
          transition={{ duration: 1 }}
        >
           <Lock className="absolute top-2 left-2 text-slate-500 opacity-50" size={16}/>
           <div>
             <div className="text-slate-400 text-xs mb-1">DEADLOCK MGR</div>
             <div className="text-lg">Bankers Algo</div>
           </div>
        </motion.div>
      </div>

      <div className="text-xs text-slate-500 mb-2 mt-2 font-bold uppercase tracking-widest">Live Kernel Logs</div>
      
      {/* Log Output Area */}
      <div className="flex-1 overflow-y-auto bg-black rounded border border-slate-800 p-4 custom-scrollbar z-10 relative shadow-inner">
        {osLogs.length === 0 ? (
          <div className="opacity-50 italic">Kernel initialized. Waiting for events...</div>
        ) : (
          osLogs.map((log, index) => (
            <motion.div 
              key={index}
              initial={{ opacity: 0, x: -10 }}
              animate={{ opacity: 1, x: 0 }}
              className={`mb-1 whitespace-pre-wrap ${
                log.includes('[PAGE FAULT]') ? 'text-red-400' :
                log.includes('[PAGE-OUT]') ? 'text-orange-400' :
                log.includes('[PAGE-IN]') ? 'text-blue-400' :
                log.includes('SAFE') ? 'text-emerald-400 font-bold' :
                log.includes('blocked') ? 'text-red-500 font-bold bg-red-950/50 p-1' :
                log.includes('lock') ? 'text-yellow-300' :
                'text-green-400'
              }`}
            >
              <span className="opacity-50 text-xs mr-2">{'>'}</span>{log}
            </motion.div>
          ))
        )}
        <div ref={bottomRef} />
      </div>
    </div>
  );
};

export default OsMonitor;
