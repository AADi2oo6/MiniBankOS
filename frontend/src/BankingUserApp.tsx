import React, { useState, useEffect } from 'react';
import { useBankStore } from './store';
import { Send, DollarSign, ArrowRightLeft, User, LogOut, Loader2, ShieldCheck, UserPlus, Key, Zap, List } from 'lucide-react';
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

  const fetchRoster = async () => {
    try {
      const res = await fetch('http://localhost:8080/api/users');
      const data = await res.json();
      setRoster(data);
    } catch(err) {
      console.error('Failed to fetch roster', err);
    }
  };

  useEffect(() => {
    if (formType === 'admin-permissions') {
      fetchRoster();
    }
  }, [formType]);

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

  const handleAction = (cmdStr: string, stayOnPage: boolean = false) => {
    sendCommand(cmdStr);
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

  if (currentUser && formType === 'login') {
      setFormType('dashboard');
  }

  // Common Input styling for perfect contrast
  const inputStyle = "w-full px-4 py-2 bg-slate-800 border border-slate-600 rounded-lg focus:ring-2 focus:ring-primary outline-none text-slate-100 placeholder-slate-400";
  const labelStyle = "block text-sm font-medium text-slate-300 mb-1";

  return (
    <div className="h-full bg-slate-950 flex flex-col relative text-slate-100">
      
      {/* Header */}
      <div className="bg-slate-900 shadow-sm border-b border-slate-800 px-6 py-4 flex justify-between items-center z-10">
        <div className="flex items-center space-x-2 text-primary font-bold text-xl">
           <ShieldCheck className="text-blue-500" />
           <span className="bg-clip-text text-transparent bg-gradient-to-r from-blue-400 to-primary">MiniBankOS</span>
           {currentUser === 'root' && <span className="ml-2 text-xs bg-red-900/50 text-red-400 px-2 py-1 rounded border border-red-800">ADMIN MODE</span>}
        </div>
        {currentUser && (
          <div className="flex items-center space-x-4">
            <div className="flex items-center space-x-2 text-slate-300">
              <User size={18} />
              <span className="font-semibold">{currentUser}</span>
            </div>
            <button onClick={() => sendCommand('logout')} className="text-slate-500 hover:text-red-400 transition-colors">
              <LogOut size={20} />
            </button>
          </div>
        )}
      </div>

      {/* Main Content Area */}
      <div className="flex-1 p-6 flex flex-col items-center justify-center relative overflow-hidden overflow-y-auto">
        
        {/* Background Decoration */}
        <div className="absolute top-[20%] left-[10%] w-64 h-64 bg-blue-600 rounded-full mix-blend-screen filter blur-[100px] opacity-20 pointer-events-none"></div>
        <div className="absolute top-[30%] right-[10%] w-64 h-64 bg-purple-600 rounded-full mix-blend-screen filter blur-[100px] opacity-20 pointer-events-none"></div>

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
                <div onClick={() => handleAction(`balance ${currentUser}`, true)} className="glass-panel cursor-pointer hover:shadow-2xl hover:-translate-y-1 transition-all p-8 rounded-2xl flex flex-col items-center justify-center space-y-4 bg-slate-900/80 border-slate-700">
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
                  <h3 className="font-bold text-xl text-slate-100">Apply for Loan</h3>
                </div>
             </motion.div>
          )}

          {/* DYNAMIC FORMS */}
          {formType !== 'login' && formType !== 'dashboard' && formType !== 'admin-permissions' && (
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
                     <button onClick={() => handleAction(`create ${username} ${password} ${amount}`)} className="w-full bg-blue-600 hover:bg-blue-500 text-white py-3 rounded-lg mt-4 font-semibold">Provision Account</button>
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
                     <div><label className={labelStyle}>Execution Amount ($)</label><input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className={inputStyle} /></div>
                     <button 
                      onClick={() => handleAction(`${formType === 'transfer' ? `transfer ${currentUser} ${targetUser}` : `${formType} ${currentUser}`} ${amount}`)}
                      className="w-full bg-blue-600 hover:bg-blue-500 text-white py-3 rounded-lg mt-4 font-semibold shadow-lg capitalize">
                        Execute {formType}
                      </button>
                   </>
                 )}

                 {/* Loan Form */}
                 {formType === 'loan' && (
                   <>
                     <div><label className={labelStyle}>Loan Category (e.g. loan-home)</label><input value={username} onChange={e=>setUsername(e.target.value)} type="text" className={inputStyle} /></div>
                     <div><label className={labelStyle}>Requested Amount ($)</label><input value={amount} onChange={e=>setAmount(e.target.value)} type="number" className={inputStyle} /></div>
                     <div><label className={labelStyle}>Amortization (Years)</label><input value={duration} onChange={e=>setDuration(e.target.value)} type="number" className={inputStyle} /></div>
                     <button onClick={() => handleAction(`loan ${currentUser} ${username} ${amount} ${duration}`)} className="w-full bg-purple-600 hover:bg-purple-500 text-white py-3 rounded-lg mt-4 font-semibold shadow-lg">Submit Application</button>
                   </>
                 )}

               </div>
             </motion.div>
          )}

          {/* ADMIN DATA TABLE VIEW (Wide width) */}
          {formType === 'admin-permissions' && (
            <motion.div 
               key="admin-table"
               initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }}
               className="glass-panel rounded-2xl w-full max-w-2xl p-8 relative z-10 bg-slate-900/90 border-slate-700 shadow-2xl"
             >
               <div className="flex justify-between items-center mb-6">
                 <button onClick={() => setFormType('dashboard')} className="text-sm text-primary hover:text-blue-400">&larr; Back</button>
                 <h2 className="text-xl font-bold text-slate-100 flex items-center space-x-2">
                    <Key size={20} className="text-amber-500"/>
                    <span>System Permissions Roster</span>
                 </h2>
               </div>

               <div className="overflow-hidden rounded-lg border border-slate-700">
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
                     {roster.length === 0 && (
                       <tr><td colSpan={4} className="text-center py-8 text-slate-500">Loading OS directory...</td></tr>
                     )}
                   </tbody>
                 </table>
               </div>
               
             </motion.div>
          )}

        </AnimatePresence>
      </div>
    </div>
  );
};

export default BankingUserApp;
