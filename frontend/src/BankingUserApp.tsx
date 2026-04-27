import React, { useState, useEffect } from 'react';
import { useBankStore } from './store';
import { Send, DollarSign, ArrowRightLeft, User, LogOut, Loader2, ShieldCheck, UserPlus, Key, Zap, List, RefreshCw, CheckCircle, XCircle } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';

const BankingUserApp: React.FC = () => {
  const { currentUser, setCurrentUser } = useBankStore();
  
  const [loading, setLoading] = useState(false);
  const [formType, setFormType] = useState<string>('login');
  
  // Generic Form States
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [targetUser, setTargetUser] = useState('');
  const [targetUser2, setTargetUser2] = useState('');
  const [targetUser3, setTargetUser3] = useState('');
  const [amount, setAmount] = useState('');
  const [duration, setDuration] = useState('');

  // Roster State
  const [roster, setRoster] = useState<any[]>([]);

  // Balance & History State
  const [balance, setBalance] = useState<number | null>(null);
  const [history, setHistory] = useState<any[]>([]);

  // Loan State
  const [loanRates, setLoanRates] = useState<any>({});
  const [loans, setLoans] = useState<any[]>([]);

  // Toast notifications
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [toastType, setToastType] = useState<'success' | 'error'>('success');

  const showToast = (msg: string, type: 'success' | 'error' = 'success') => {
    setToastMessage(msg);
    setToastType(type);
    setTimeout(() => setToastMessage(null), 4000);
  };

  const fetchRoster = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/users');
      const data = await res.json();
      setRoster(data);
    } catch(err) {
      console.error('Failed to fetch roster', err);
    }
  };

  const fetchBalanceAndHistory = async () => {
    try {
      const balRes = await fetch(`http://localhost:8080/api/accounts/${currentUser}/balance`);
      if (balRes.ok) {
        const balData = await balRes.json();
        setBalance(balData.balance);
      }

      const histRes = await fetch(`http://localhost:8080/api/accounts/${currentUser}/history`);
      if (histRes.ok) {
        const histData = await histRes.json();
        setHistory(histData.reverse()); // latest first
      }
    } catch(e) {
      console.error('Failed to fetch balance/history', e);
    }
  };

  const fetchLoanData = async () => {
    try {
      const ratesRes = await fetch('http://localhost:8080/api/loans/rates');
      if (ratesRes.ok) {
        setLoanRates(await ratesRes.json());
      }
      const loansRes = await fetch(`http://localhost:8080/api/loans/${currentUser}`);
      if (loansRes.ok) {
        setLoans(await loansRes.json());
      }
    } catch(e) {
      console.error('Failed to fetch loan data', e);
    }
  };

  useEffect(() => {
    if (formType === 'admin-permissions') fetchRoster();
    if (formType === 'balance') fetchBalanceAndHistory();
    if (formType === 'loan') fetchLoanData();
  }, [formType, currentUser]);

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
      
      if (formType === 'admin-permissions') {
         fetchRoster(); // refresh table if permissions changed
      }

      return data;
    } catch (err) {
      console.error(err);
      return { status: 'error', message: 'Connection failed' };
    } finally {
      setTimeout(() => setLoading(false), 500); // minimum visual delay
    }
  };

  const handleLogin = (e: React.FormEvent) => {
    e.preventDefault();
    sendCommand(`login ${username} ${password}`, true);
  };

  const handleAction = async (cmdStr: string, stayOnPage: boolean = false) => {
    const data = await sendCommand(cmdStr);
    
    // Beautiful toasting
    if (data && data.status === 'success') {
      if (formType === 'deposit') showToast(`Successfully deposited ₹${amount}`);
      else if (formType === 'withdraw') showToast(`Successfully withdrew ₹${amount}`);
      else if (formType === 'transfer') showToast(`Successfully transferred ₹${amount} to ${targetUser}`);
      else if (formType === 'loan') {
         showToast(`Loan application submitted`);
         fetchLoanData();
      } else if (formType === 'admin-create') {
         showToast(`Successfully provisioned account for ${username}`);
      } else if (!isActionSilent(cmdStr)) {
         showToast(`Action executed successfully`);
      }
    } else if (data && data.status === 'error') {
      showToast(data.message || 'Action failed', 'error');
    } else {
      if (!isActionSilent(cmdStr)) showToast(data.message || 'Action executed', 'success');
    }

    if (!stayOnPage) {
       setFormType('dashboard');
    }
    setTargetUser('');
    setTargetUser2('');
    setTargetUser3('');
    setAmount('');
    setDuration('');
    setUsername('');
    setPassword('');
  };

  const isActionSilent = (cmdStr: string) => cmdStr.startsWith('grant') || cmdStr.startsWith('revoke') || cmdStr.startsWith('slow-mode');

  if (currentUser && formType === 'login') {
      setFormType('dashboard');
  }

  // Common Input styling for perfect contrast
  const inputStyle = "w-full px-4 py-2 bg-slate-800 border border-slate-600 rounded-lg focus:ring-2 focus:ring-primary outline-none text-slate-100 placeholder-slate-400";
  const labelStyle = "block text-sm font-medium text-slate-300 mb-1";

  return (
    <div className="h-full bg-slate-950 flex flex-col relative text-slate-100">
      
      {/* Header */}
      <div className="bg-slate-900 shadow-sm border-b border-slate-800 px-6 py-4 flex justify-between items-center z-10 sticky top-0">
        <div className="flex items-center space-x-2 text-primary font-bold text-xl cursor-pointer" onClick={() => currentUser ? setFormType('dashboard') : setFormType('login')}>
           <ShieldCheck className="text-blue-500" />
           <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-primary flex-shrink-0">MiniBankOS</span>
           {currentUser === 'root' && <span className="ml-2 text-xs bg-red-900/50 text-red-400 px-2 py-1 rounded border border-red-800">ADMIN MODE</span>}
        </div>
        {currentUser && (
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-slate-300 bg-slate-800 px-3 py-1.5 rounded-full border border-slate-700">
              <User size={16} className="text-blue-400"/>
              <span className="font-semibold text-sm">{currentUser}</span>
            </div>
            <button onClick={() => sendCommand('logout')} className="text-slate-500 hover:text-red-400 transition-colors p-2 rounded-full hover:bg-slate-800">
              <LogOut size={20} />
            </button>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      <div className="flex-1 p-6 flex flex-col items-center justify-start md:justify-center relative overflow-hidden overflow-y-auto">
        
        {/* Background Decoration */}
        <div className="absolute top-[20%] left-[10%] w-64 h-64 bg-blue-600 rounded-full mix-blend-screen filter blur-[100px] opacity-20 pointer-events-none"></div>
        <div className="absolute top-[30%] right-[10%] w-64 h-64 bg-purple-600 rounded-full mix-blend-screen filter blur-[100px] opacity-20 pointer-events-none"></div>

        {/* Global Toast */}
        <AnimatePresence>
          {toastMessage && (
            <motion.div 
              initial={{ opacity: 0, y: -50, scale: 0.95 }} 
              animate={{ opacity: 1, y: 0, scale: 1 }} 
              exit={{ opacity: 0, scale: 0.95 }}
              className={`fixed top-24 z-50 px-6 py-3 rounded-lg shadow-2xl flex items-center space-x-3 font-semibold border ${toastType === 'success' ? 'bg-emerald-900/90 text-emerald-100 border-emerald-700' : 'bg-red-900/90 text-red-100 border-red-700'}`}
            >
              {toastType === 'success' ? <CheckCircle size={20}/> : <XCircle size={20}/>}
              <span>{toastMessage}</span>
            </motion.div>
          )}
        </AnimatePresence>

        <AnimatePresence mode="wait">
          
          {/* LOGIN VIEW */}
          {formType === 'login' && (
            <motion.div 
              key="login"
              initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
              className="glass-panel rounded-2xl w-full max-w-md p-8 relative z-20 bg-slate-900/80 border-slate-700 shadow-2xl"
            >
              <h2 className="text-2xl font-bold mb-6 text-slate-100 text-center">Sign In</h2>
              <form onSubmit={handleLogin} className="space-y-4">
                <div>
                  <label className={labelStyle}>Username</label>
                  <input required value={username} onChange={e=>setUsername(e.target.value)} type="text" className={inputStyle} />
                </div>
                <div>
                  <label className={labelStyle}>Password</label>
                  <input required value={password} onChange={e=>setPassword(e.target.value)} type="password" className={inputStyle} />
                </div>
                <button disabled={loading} type="submit" className="w-full bg-blue-600 hover:bg-blue-500 text-white font-semibold py-3 rounded-lg transition-colors flex justify-center mt-4">
                  {loading ? <Loader2 className="animate-spin" /> : "Access Account"}
                </button>
              </form>
            </motion.div>
          )}

          {/* ADMIN DASHBOARD VIEW */}
          {formType === 'dashboard' && currentUser === 'root' && (
             <motion.div 
               key="admin-dashboard"
               initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
               className="w-full max-w-4xl grid grid-cols-1 md:grid-cols-2 gap-6 relative z-10"
             >
                <div onClick={() => setFormType('admin-create')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-emerald-900/30 text-emerald-400 rounded-full"><UserPlus size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Provision User Account</h3>
                  <p className="text-sm text-slate-400 text-center">Create bank account + login credentials</p>
                </div>
                <div onClick={() => setFormType('admin-permissions')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-amber-900/30 text-amber-400 rounded-full"><Key size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Manage Permissions</h3>
                  <p className="text-sm text-slate-400 text-center">Grant/Revoke transfer privileges</p>
                </div>
                <div onClick={() => setFormType('admin-os')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 md:col-span-2 bg-gradient-to-r from-red-900 to-rose-900 text-white border-none shadow-red-900/20">
                  <div className="p-4 bg-white/10 rounded-full"><Zap size={32}/></div>
                  <h3 className="font-bold text-2xl">OS Presentation Controls</h3>
                  <p className="text-sm text-red-200 text-center">Force Deadlock Arrays & Slow Mode settings</p>
                </div>
             </motion.div>
          )}

          {/* USER DASHBOARD VIEW */}
          {formType === 'dashboard' && currentUser && currentUser !== 'root' && (
             <motion.div 
               key="user-dashboard"
               initial={{ opacity: 0, scale: 0.95 }} animate={{ opacity: 1, scale: 1 }} exit={{ opacity: 0, scale: 0.95 }}
               className="w-full max-w-4xl grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 relative z-10"
             >
                <div onClick={() => setFormType('balance')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-emerald-900/30 text-emerald-400 rounded-full"><DollarSign size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Check Balance</h3>
                </div>
                <div onClick={() => setFormType('deposit')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-blue-900/30 text-blue-400 rounded-full"><ArrowRightLeft size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Deposit</h3>
                </div>
                <div onClick={() => setFormType('withdraw')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-orange-900/30 text-orange-400 rounded-full"><LogOut size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Withdraw</h3>
                </div>
                <div onClick={() => setFormType('transfer')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 lg:col-span-2 bg-gradient-to-r from-blue-900 to-indigo-900 text-white border-none shadow-blue-900/20">
                  <div className="p-4 bg-white/10 rounded-full"><Send size={32}/></div>
                  <h3 className="font-bold text-2xl">Transfer Funds</h3>
                  <p className="text-sm text-blue-200 text-center">Send targeting another process array</p>
                </div>
                <div onClick={() => setFormType('loan')} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
                  <div className="p-4 bg-purple-900/30 text-purple-400 rounded-full"><List size={32}/></div>
                  <h3 className="font-bold text-xl text-slate-100">Manage Loans</h3>
                </div>
             </motion.div>
          )}

          {/* DYNAMIC ACTION FORMS (Withdraw, Deposit, Transfer, Create) */}
          {(formType === 'admin-create' || formType === 'admin-os' || formType === 'transfer' || formType === 'deposit' || formType === 'withdraw') && (
            <motion.div 
               key="action-form"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel rounded-2xl w-full max-w-md p-8 relative z-10 bg-slate-900/90 border-slate-700 shadow-2xl"
             >
               <button onClick={() => setFormType('dashboard')} className="text-sm text-primary mb-4 flex items-center hover:text-blue-400">&larr; Back to Dashboard</button>
               <h2 className="text-2xl font-bold mb-6 text-slate-100 capitalize">
                 {formType.replace('admin-', '').replace('user-', '')}
               </h2>
               
               <div className="space-y-4">
                 
                 {/* Admin Create Form */}
                 {formType === 'admin-create' && (
                   <>
                     <div><label className={labelStyle}>Username</label><input value={username} onChange={e=>setUsername(e.target.value)} type="text" className={inputStyle} /></div>
                     <div><label className={labelStyle}>Password</label><input value={password} onChange={e=>setPassword(e.target.value)} type="password" className={inputStyle} /></div>
                     <div><label className={labelStyle}>Initial Balance</label><input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className={inputStyle} /></div>
                     <button disabled={loading} onClick={() => handleAction(`create ${username} ${password} ${amount}`)} className="w-full bg-blue-600 hover:bg-blue-500 text-white flex justify-center items-center py-3 rounded-lg mt-4 font-semibold">
                       {loading ? <><Loader2 className="animate-spin mr-2" /> Processing...</> : "Provision Account"}
                     </button>
                   </>
                 )}

                 {/* Admin OS Form */}
                 {formType === 'admin-os' && (
                   <>
                     <div className="space-y-2 border-b border-slate-700 pb-4 mb-4">
                       <h3 className="font-semibold text-slate-200">Trigger Deadlock Demo</h3>
                       <p className="text-xs text-slate-400 mb-2">Requires 3 target accounts to form a cyclic wait ring.</p>
                       <input placeholder="Account Name 1" value={targetUser} onChange={e=>setTargetUser(e.target.value)} type="text" className={inputStyle} />
                       <input placeholder="Account Name 2" value={targetUser2} onChange={e=>setTargetUser2(e.target.value)} type="text" className={inputStyle} />
                       <input placeholder="Account Name 3" value={targetUser3} onChange={e=>setTargetUser3(e.target.value)} type="text" className={inputStyle} />
                       <button onClick={() => handleAction(`deadlock-demo ${targetUser} ${targetUser2} ${targetUser3}`, true)} className="w-full bg-rose-600 hover:bg-rose-500 text-white py-2 rounded-lg mt-2 font-semibold shadow-lg">Run Simulator Routine</button>
                     </div>
                     <div className="flex space-x-4">
                       <button onClick={() => handleAction(`slow-mode on`, true)} className="flex-1 border border-amber-600/50 bg-amber-900/20 hover:bg-amber-800/40 text-amber-500 py-2 rounded-lg font-semibold">Slow Mode ON</button>
                       <button onClick={() => handleAction(`slow-mode off`, true)} className="flex-1 border border-slate-700 bg-slate-800 hover:bg-slate-700 text-slate-300 py-2 rounded-lg font-semibold">Slow Mode OFF</button>
                     </div>
                   </>
                 )}

                 {/* User Action Forms */}
                 {(formType === 'transfer' || formType === 'deposit' || formType === 'withdraw') && (
                   <>
                     {formType === 'transfer' && (
                       <div><label className={labelStyle}>Recipient Account Name</label><input value={targetUser} onChange={e=>setTargetUser(e.target.value)} type="text" className={inputStyle} /></div>
                     )}
                     <div><label className={labelStyle}>Execution Amount (₹)</label><input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className={inputStyle} /></div>
                     <button 
                      disabled={loading}
                      onClick={() => handleAction(`${formType === 'transfer' ? `transfer ${currentUser} ${targetUser}` : `${formType} ${currentUser}`} ${amount}`)}
                      className="w-full bg-blue-600 flex justify-center items-center hover:bg-blue-500 text-white py-3 rounded-lg mt-4 font-semibold shadow-lg capitalize">
                        {loading ? <><Loader2 className="animate-spin mr-2" /> Processing {formType}...</> : `Execute ${formType}`}
                      </button>
                   </>
                 )}
               </div>
             </motion.div>
          )}

          {/* ADMIN PERMISSION TABLE VIEW */}
          {formType === 'admin-permissions' && (
            <motion.div 
               key="admin-table"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel rounded-2xl w-[90%] max-w-4xl p-8 relative z-10 bg-slate-900/90 border-slate-700 shadow-2xl"
             >
               <div className="flex justify-between items-center mb-6">
                 <button onClick={() => setFormType('dashboard')} className="text-sm text-primary hover:text-blue-400">&larr; Back Dashboard</button>
                 <h2 className="text-xl font-bold text-slate-100 flex items-center space-x-2">
                    <Key size={20} className="text-amber-500"/>
                    <span>System Permissions Roster</span>
                 </h2>
               </div>

               <div className="overflow-hidden rounded-lg border border-slate-700 overflow-x-auto">
                 <table className="w-full text-left text-sm text-slate-300">
                   <thead className="bg-slate-800/80 text-xs uppercase text-slate-400">
                     <tr>
                       <th className="px-6 py-4 font-semibold">User Target</th>
                       <th className="px-6 py-4 font-semibold">Role</th>
                       <th className="px-6 py-4 font-semibold text-center">Transfer Auth</th>
                       <th className="px-6 py-4 font-semibold text-right">Actions</th>
                     </tr>
                   </thead>
                   <tbody className="divide-y divide-slate-700/50">
                     {roster.map((user, idx) => (
                       <tr key={idx} className="hover:bg-slate-800/50 transition-colors">
                         <td className="px-6 py-4 font-medium text-slate-100">{user.username}</td>
                         <td className="px-6 py-4">
                           <span className={`px-2 py-1 rounded text-xs border ${user.role === 'ADMIN' ? 'bg-red-900/30 text-red-400 border-red-800' : 'bg-slate-800 text-slate-300 border-slate-700'}`}>
                             {user.role}
                           </span>
                         </td>
                         <td className="px-6 py-4 text-center">
                           {user.transferPermission ? (
                             <span className="inline-flex items-center text-emerald-400 text-xs"><ShieldCheck size={14} className="mr-1"/> Granted</span>
                           ) : (
                             <span className="inline-flex items-center text-red-400 text-xs"><LogOut size={14} className="mr-1"/> Revoked</span>
                           )}
                         </td>
                         <td className="px-6 py-4 text-right space-x-2">
                           {user.role !== 'ADMIN' && (
                             <>
                               {user.transferPermission ? (
                                 <button onClick={() => handleAction(`revoke ${user.username}`, true)} className="px-3 py-1 bg-red-900/80 hover:bg-red-800 text-red-100 rounded text-xs font-semibold border border-red-700 transition">Revoke</button>
                               ) : (
                                 <button onClick={() => handleAction(`grant ${user.username}`, true)} className="px-3 py-1 bg-emerald-900/80 hover:bg-emerald-800 text-emerald-100 rounded text-xs font-semibold border border-emerald-700 transition">Grant</button>
                               )}
                             </>
                           )}
                         </td>
                       </tr>
                     ))}
                   </tbody>
                 </table>
               </div>
             </motion.div>
          )}

          {/* BALANCE & HISTORY VIEW */}
          {formType === 'balance' && (
            <motion.div 
               key="balance-view"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel flex flex-col gap-6 rounded-2xl w-[90%] max-w-4xl p-8 relative z-10 bg-slate-900/90 border-slate-700 shadow-2xl"
             >
               <div className="flex justify-between items-center w-full">
                 <button onClick={() => setFormType('dashboard')} className="text-sm text-primary hover:text-blue-400">&larr; Back to Dashboard</button>
                 <button onClick={() => fetchBalanceAndHistory()} className="text-slate-400 flex text-sm items-center hover:text-white transition">
                    <RefreshCw size={16} className="mr-2"/> Refresh
                 </button>
               </div>

               <div className="bg-gradient-to-r from-emerald-900/80 to-teal-900/80 border border-emerald-800 p-8 rounded-2xl flex flex-col items-center justify-center shadow-lg transform transition">
                  <h3 className="text-emerald-200 text-sm font-bold uppercase tracking-wider mb-2">Available Balance</h3>
                  <div className="text-5xl font-extrabold text-white">
                     ₹{balance !== null ? balance.toFixed(2) : '---'}
                  </div>
               </div>

               <div className="mt-4">
                  <h3 className="text-lg font-bold text-slate-200 mb-4 flex items-center"><List size={18} className="mr-2" /> Payment History</h3>
                  <div className="overflow-hidden rounded-lg border border-slate-700 focus:outline-none">
                     <table className="w-full text-left text-sm text-slate-300">
                        <thead className="bg-slate-800/80 text-xs uppercase text-slate-400">
                           <tr>
                              <th className="px-4 py-3 font-semibold">Timestamp</th>
                              <th className="px-4 py-3 font-semibold">Type</th>
                              <th className="px-4 py-3 font-semibold">Details</th>
                              <th className="px-4 py-3 font-semibold text-right">Amount</th>
                           </tr>
                        </thead>
                        <tbody className="divide-y divide-slate-700/50">
                           {history.map((txn, idx) => {
                             const isSender = txn.from === currentUser;
                             const color = isSender ? 'text-orange-400' : 'text-emerald-400';
                             const sign = isSender ? '-' : '+';
                             
                             let details = '';
                             if (txn.type === 'TRANSFER') details = isSender ? `To: ${txn.to}` : `From: ${txn.from}`;
                             else details = txn.type;

                             return (
                               <tr key={idx} className="hover:bg-slate-800/50 transition-colors">
                                  <td className="px-4 py-3 text-xs text-slate-400">{new Date(parseInt(txn.timestamp)).toLocaleString()}</td>
                                  <td className="px-4 py-3"><span className="px-2 py-0.5 rounded text-[10px] uppercase font-bold bg-slate-800 border border-slate-700">{txn.type}</span></td>
                                  <td className="px-4 py-3 text-slate-200">{details}</td>
                                  <td className={`px-4 py-3 text-right font-bold ${color}`}>
                                    {sign}₹{parseFloat(txn.amount).toFixed(2)}
                                  </td>
                               </tr>
                             );
                           })}
                           {history.length === 0 && (
                             <tr><td colSpan={4} className="text-center py-6 text-slate-500">No transactions found.</td></tr>
                           )}
                        </tbody>
                     </table>
                  </div>
               </div>
             </motion.div>
          )}

          {/* LOAN MANAGER VIEW */}
          {formType === 'loan' && (
             <motion.div 
               key="loan-view"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel flex flex-col gap-6 rounded-2xl w-[90%] max-w-4xl p-8 relative z-10 bg-slate-900/90 border-slate-700 shadow-2xl"
             >
               <div className="flex justify-between items-center w-full mb-4">
                 <button onClick={() => setFormType('dashboard')} className="text-sm text-primary hover:text-blue-400">&larr; Back to Dashboard</button>
                 <h2 className="text-2xl font-bold text-slate-100 flex items-center">
                   <List className="mr-2 text-purple-400" /> Loan Management Portal
                 </h2>
               </div>

               {/* Rates Row */}
               <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {Object.entries(loanRates).map(([category, rate]) => (
                     <div key={category} className="bg-slate-800/50 border border-slate-700 rounded-xl p-4 flex flex-col items-center">
                        <span className="text-slate-400 text-xs font-bold uppercase tracking-wider mb-1">{category} Loan</span>
                        <span className="text-2xl font-bold text-purple-300">{rate as React.ReactNode}% <span className="text-sm font-normal text-slate-500">APR</span></span>
                     </div>
                  ))}
               </div>

               <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mt-4">
                  
                  {/* Left: Application Form */}
                  <div className="bg-slate-800/30 border border-slate-700 p-6 rounded-xl flex flex-col space-y-4">
                     <h3 className="font-bold text-slate-200">New Application</h3>
                     <div>
                       <label className={labelStyle}>Category</label>
                       <select value={username} onChange={e=>setUsername(e.target.value)} className={inputStyle}>
                          <option value="">Select Category...</option>
                          <option value="house">House</option>
                          <option value="education">Education</option>
                          <option value="business">Business</option>
                       </select>
                     </div>
                     <div><label className={labelStyle}>Amount (₹)</label><input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className={inputStyle} /></div>
                     <div><label className={labelStyle}>Duration (Years)</label><input value={duration} onChange={e=>setDuration(e.target.value)} type="number" className={inputStyle} /></div>
                     <button disabled={loading} onClick={() => handleAction(`loan ${username} ${currentUser} ${amount} ${duration}`, true)} className="w-full bg-purple-600 hover:bg-purple-500 text-white flex justify-center items-center py-2.5 rounded-lg font-semibold transition mt-auto">
                        {loading ? <Loader2 className="animate-spin" /> : "Apply"}
                     </button>
                  </div>

                  {/* Right: Active Loans Table */}
                  <div className="md:col-span-2 bg-slate-800/30 border border-slate-700 rounded-xl overflow-hidden flex flex-col">
                     <div className="px-6 py-4 border-b border-slate-700/50 bg-slate-800/50 flex justify-between items-center">
                        <h3 className="font-bold text-slate-200">Existing Agreements</h3>
                        <button onClick={fetchLoanData} className="text-slate-400 hover:text-white"><RefreshCw size={14}/></button>
                     </div>
                     <div className="flex-1 overflow-auto">
                        <table className="w-full text-left text-xs text-slate-300">
                           <thead className="bg-slate-800/30 uppercase text-slate-500 border-b border-slate-700/50">
                              <tr>
                                 <th className="px-4 py-3">Category</th>
                                 <th className="px-4 py-3">Principal</th>
                                 <th className="px-4 py-3">Interest (Rate)</th>
                                 <th className="px-4 py-3">Remaining</th>
                                 <th className="px-4 py-3" align="right">Status</th>
                              </tr>
                           </thead>
                           <tbody className="divide-y divide-slate-700/30">
                              {loans.map((loan, idx) => {
                                 const interestAccrued = loan.principal - loan.originalAmount;
                                 return (
                                   <tr key={idx} className="hover:bg-slate-700/20">
                                      <td className="px-4 py-3 font-semibold uppercase">{loan.category}</td>
                                      <td className="px-4 py-3">
                                         <div className="font-bold text-slate-200">₹{parseFloat(loan.principal).toFixed(2)}</div>
                                         <div className="text-[10px] text-slate-500">Orig: ₹{loan.originalAmount}</div>
                                      </td>
                                      <td className="px-4 py-3">
                                         <div className="text-purple-400">+₹{interestAccrued.toFixed(2)}</div>
                                         <div className="text-[10px] text-slate-500">@ {loan.yearlyInterest}%</div>
                                      </td>
                                      <td className="px-4 py-3">{loan.remainingMonths} mo <span className="text-slate-500 ml-1">/ {loan.durationYears * 12}</span></td>
                                      <td className="px-4 py-3 text-right">
                                         <span className={`px-2 py-1 rounded-full text-[10px] font-bold ${loan.status === 'ACTIVE' ? 'bg-blue-900/50 text-blue-400 border border-blue-800' : 'bg-emerald-900/50 text-emerald-400 border border-emerald-800'}`}>
                                            {loan.status}
                                         </span>
                                      </td>
                                   </tr>
                                 )
                              })}
                              {loans.length === 0 && (
                                <tr><td colSpan={5} className="text-center py-6 text-slate-500">No active loans found.</td></tr>
                              )}
                           </tbody>
                        </table>
                     </div>
                  </div>
               </div>
             </motion.div>
          )}

        </AnimatePresence>
      </div>
    </div>
  );
};

export default BankingUserApp;
