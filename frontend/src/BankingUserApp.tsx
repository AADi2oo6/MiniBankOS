import React, { useState } from 'react';
import { useBankStore } from './store';
import { Send, DollarSign, ArrowRightLeft, User, LogOut, Loader2, ShieldCheck } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const BankingUserApp: React.FC = () => {
  const { currentUser, setCurrentUser } = useBankStore();
  
  const [loading, setLoading] = useState(false);
  const [formType, setFormType] = useState<'login' | 'dashboard' | 'transfer' | 'deposit'>('login');
  
  // Form States
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [targetUser, setTargetUser] = useState('');
  const [amount, setAmount] = useState('');

  const sendCommand = async (commandStr: string, isLogin: boolean = false) => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/execute', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ command: commandStr })
      });
      const data = await response.json();
      
      if (isLogin && data.status === 'success') {
         setCurrentUser(data.user);
         setFormType('dashboard');
      } else if (commandStr === 'logout') {
         setCurrentUser(null);
         setFormType('login');
      }
      
      // Wait a tiny bit for the OS logs to arrive via websocket before ending load state
      setTimeout(() => setLoading(false), 500);
      return data;
    } catch (err) {
      console.error(err);
      setLoading(false);
    }
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    sendCommand(`login ${username} ${password}`, true);
  };

  const handleAction = (cmdStr: string) => {
    sendCommand(cmdStr);
    setFormType('dashboard'); // return to dash
    setTargetUser('');
    setAmount('');
  };

  if (currentUser && formType === 'login') {
      setFormType('dashboard');
  }

  return (
    <div className="h-full bg-slate-100 flex flex-col relative">
      
      {/* Header */}
      <div className="bg-white shadow-sm border-b px-6 py-4 flex justify-between items-center z-10">
        <div className="flex items-center space-x-2 text-primary font-bold text-xl">
           <ShieldCheck className="text-blue-600" />
           <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-700 to-primary">MiniBankOS</span>
        </div>
        {currentUser && (
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-slate-600">
              <User size={18} />
              <span className="font-semibold">{currentUser}</span>
            </div>
            <button onClick={() => sendCommand('logout')} className="text-slate-400 hover:text-red-500 transition-colors">
              <LogOut size={20} />
            </button>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      <div className="flex-1 p-6 flex flex-col items-center justify-center relative overflow-hidden">
        
        {/* Background Decoration */}
        <div className="absolute top-[20%] left-[10%] w-64 h-64 bg-blue-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob"></div>
        <div className="absolute top-[30%] right-[10%] w-64 h-64 bg-purple-400 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-blob animation-delay-2000"></div>

        <AnimatePresence mode="wait">
          
          {/* LOGIN VIEW */}
          {formType === 'login' && (
            <motion.div 
              key="login"
              initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
              className="glass-panel rounded-2xl w-full max-w-md p-8 relative z-10 bg-white"
            >
              <h2 className="text-2xl font-bold mb-6 text-slate-800 text-center">Sign In</h2>
              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Username</label>
                  <input required value={username} onChange={e=>setUsername(e.target.value)} type="text" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none" />
                </div>
                <div>
                  <label className="block text-sm font-medium text-slate-700 mb-1">Password</label>
                  <input required value={password} onChange={e=>setPassword(e.target.value)} type="password" className="w-full px-4 py-2 border rounded-lg focus:ring-2 focus:ring-primary outline-none" />
                </div>
                <button disabled={loading} type="submit" className="w-full bg-primary hover:bg-blue-600 text-white font-semibold py-3 rounded-lg transition-colors flex justify-center mt-4">
                  {loading ? <Loader2 className="animate-spin" /> : "Access Account"}
                </button>
              </form>
            </motion.div>
          )}

          {/* DASHBOARD VIEW */}
          {formType === 'dashboard' && currentUser && (
             <motion.div 
               key="dashboard"
               initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }}
               className="w-full max-w-4xl grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10"
             >
                <div 
                  onClick={() => handleAction(`balance ${currentUser}`)}
                  className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-white/80"
                >
                  <div className="p-4 bg-emerald-100 text-emerald-600 rounded-full"><DollarSign size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-800">Check Balance</h3>
                  <p className="text-sm text-slate-500 text-center">Triggers Reader-Writer Lock (Read Mode)</p>
                </div>

                <div 
                  onClick={() => setFormType('deposit')}
                  className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-white/80"
                >
                  <div className="p-4 bg-blue-100 text-blue-600 rounded-full"><ArrowRightLeft size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-800">Deposit / Withdraw</h3>
                  <p className="text-sm text-slate-500 text-center">Triggers Reader-Writer Lock (Write Mode)</p>
                </div>

                <div 
                  onClick={() => setFormType('transfer')}
                  className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 md:col-span-2 bg-gradient-to-r from-blue-600 to-indigo-700 text-white border-none"
                >
                  <div className="p-4 bg-white/20 rounded-full"><Send size={32}/></div>
                  <h3 className="font-bold text-2xl">Transfer Funds</h3>
                  <p className="text-sm text-blue-100 text-center">Triggers Banker's Algorithm & Scheduler</p>
                </div>
             </motion.div>
          )}

          {/* DEPOSIT/TRANSFER FORMS */}
          {(formType === 'transfer' || formType === 'deposit') && (
            <motion.div 
               key="action-form"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel rounded-2xl w-full max-w-md p-8 relative z-10 bg-white"
             >
               <button onClick={() => setFormType('dashboard')} className="text-sm text-primary mb-4 block">&larr; Back to Dashboard</button>
               <h2 className="text-2xl font-bold mb-6 text-slate-800 capitalize">{formType}</h2>
               <div className="space-y-4">
                 {formType === 'transfer' && (
                   <div>
                     <label className="block text-sm font-medium text-slate-700 mb-1">Recipient Account</label>
                     <input value={targetUser} onChange={e=>setTargetUser(e.target.value)} type="text" className="w-full px-4 py-2 border rounded-lg focus:ring-2 outline-none" />
                   </div>
                 )}
                 <div>
                   <label className="block text-sm font-medium text-slate-700 mb-1">Amount</label>
                   <input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className="w-full px-4 py-2 border rounded-lg focus:ring-2 outline-none" />
                 </div>
                 <button 
                  onClick={() => formType === 'transfer' ? handleAction(`transfer ${currentUser} ${targetUser} ${amount}`) : handleAction(`deposit ${currentUser} ${amount}`)}
                  disabled={loading} 
                  className="w-full bg-slate-900 hover:bg-black text-white font-semibold py-3 rounded-lg transition-colors flex justify-center mt-4">
                   {loading ? <Loader2 className="animate-spin" /> : "Execute Transaction"}
                 </button>
               </div>
             </motion.div>
          )}

        </AnimatePresence>

      </div>
    </div>
  );
};

export default BankingUserApp;
