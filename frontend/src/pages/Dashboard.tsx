import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  LayoutDashboard,
  CreditCard,
  ArrowUpRight,
  ArrowDownRight,
  Plus,
  RefreshCw,
  Loader2,
  LogOut,
  Bot,
} from 'lucide-react';
import toast from 'react-hot-toast';
import { bffService, handleApiError } from '../services/api';
import { useAuthStore } from '../store/authStore';
import type { DashboardResponse } from '../types/api';
import { formatCurrency } from '../utils/format';
import AIChat from '../components/AIChat';

export default function Dashboard() {
  const navigate = useNavigate();
  const { userId, username, logout } = useAuthStore();
  const [dashboard, setDashboard] = useState<DashboardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [showChat, setShowChat] = useState(false);

  const fetchDashboard = async (isRefresh = false) => {
    if (!userId) {
      navigate('/login');
      return;
    }

    if (isRefresh) {
      setRefreshing(true);
    } else {
      setLoading(true);
    }

    try {
      const data = await bffService.getDashboard(userId);
      setDashboard(data);
    } catch (error) {
      const apiError = handleApiError(error);
      toast.error(apiError.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchDashboard();
  }, [userId]);

  const handleLogout = () => {
    logout();
    toast.success('Logged out successfully');
    navigate('/login');
  };

  const totalBalance = dashboard?.accounts.reduce((sum, account) => sum + account.balance, 0) || 0;

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-gray-50">
        <div className="text-center">
          <Loader2 className="w-12 h-12 text-primary-600 animate-spin mx-auto mb-4" />
          <p className="text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50">
      {/* Header */}
      <header className="bg-white shadow-sm border-b border-gray-200">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-primary-600 rounded-lg flex items-center justify-center">
                <LayoutDashboard className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Virtual Bank</h1>
                <p className="text-sm text-gray-600">Welcome back, {username}</p>
              </div>
            </div>
            <div className="flex items-center gap-3">
              <button
                onClick={() => setShowChat(true)}
                className="bg-green-600 text-white px-4 py-2 rounded-lg hover:bg-green-700 transition-colors flex items-center gap-2"
              >
                <Bot className="w-4 h-4" />
                AI Assistant
              </button>
              <button
                onClick={() => fetchDashboard(true)}
                disabled={refreshing}
                className="btn-secondary flex items-center gap-2"
              >
                <RefreshCw className={`w-4 h-4 ${refreshing ? 'animate-spin' : ''}`} />
                Refresh
              </button>
              <button onClick={handleLogout} className="btn-secondary flex items-center gap-2">
                <LogOut className="w-4 h-4" />
                Logout
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Main Content */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* User Info Card */}
        <div className="card mb-6">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="text-xl font-semibold text-gray-900 mb-1">
                {dashboard?.firstName} {dashboard?.lastName}
              </h2>
              <p className="text-gray-600">{dashboard?.email}</p>
            </div>
            <div className="text-right">
              <p className="text-sm text-gray-600">Total Balance</p>
              <p className="text-3xl font-bold text-primary-600">{formatCurrency(totalBalance)}</p>
            </div>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <button
            onClick={() => navigate('/create-account')}
            className="card hover:shadow-lg transition-shadow cursor-pointer text-left"
          >
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-green-100 rounded-lg flex items-center justify-center">
                <Plus className="w-6 h-6 text-green-600" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900">Create New Account</h3>
                <p className="text-sm text-gray-600">Open a savings or checking account</p>
              </div>
            </div>
          </button>

          <button
            onClick={() => navigate('/transfer')}
            className="card hover:shadow-lg transition-shadow cursor-pointer text-left"
          >
            <div className="flex items-center gap-4">
              <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
                <ArrowUpRight className="w-6 h-6 text-blue-600" />
              </div>
              <div>
                <h3 className="font-semibold text-gray-900">Transfer Money</h3>
                <p className="text-sm text-gray-600">Send money between accounts</p>
              </div>
            </div>
          </button>
        </div>

        {/* Accounts */}
        <div className="mb-6">
          <h2 className="text-xl font-semibold text-gray-900 mb-4">Your Accounts</h2>
          {dashboard?.accounts.length === 0 ? (
            <div className="card text-center py-12">
              <CreditCard className="w-12 h-12 text-gray-400 mx-auto mb-4" />
              <p className="text-gray-600 mb-4">You don't have any accounts yet</p>
              <button onClick={() => navigate('/create-account')} className="btn-primary">
                Create Your First Account
              </button>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
              {dashboard?.accounts.map((account) => (
                <div key={account.accountId} className="card">
                  <div className="flex items-start justify-between mb-4">
                    <div>
                      <div className="flex items-center gap-2 mb-1">
                        <CreditCard className="w-5 h-5 text-primary-600" />
                        <h3 className="font-semibold text-gray-900">{account.accountType}</h3>
                      </div>
                      <p className="text-sm text-gray-600">{account.accountNumber}</p>
                    </div>
                    <div className="text-right">
                      <p className="text-2xl font-bold text-gray-900">
                        {formatCurrency(account.balance)}
                      </p>
                    </div>
                  </div>

                  {/* Recent Transactions */}
                  <div className="mt-4 pt-4 border-t border-gray-200">
                    <h4 className="text-sm font-medium text-gray-700 mb-3">Recent Transactions</h4>
                    {account.transactions.length === 0 ? (
                      <p className="text-sm text-gray-500">No recent transactions</p>
                    ) : (
                      <div className="space-y-2">
                        {account.transactions.slice(0, 3).map((transaction) => {
                          const isIncoming = transaction.toAccountId === account.accountId;
                          return (
                            <div
                              key={transaction.transactionId}
                              className="flex items-center justify-between py-2"
                            >
                              <div className="flex items-center gap-2">
                                {isIncoming ? (
                                  <ArrowDownRight className="w-4 h-4 text-green-600" />
                                ) : (
                                  <ArrowUpRight className="w-4 h-4 text-red-600" />
                                )}
                                <div>
                                  <p className="text-sm font-medium text-gray-900">
                                    {transaction.description || 'Transfer'}
                                  </p>
                                  <p className="text-xs text-gray-500">
                                    {new Date(transaction.timestamp).toLocaleDateString()}
                                  </p>
                                </div>
                              </div>
                              <span
                                className={`text-sm font-semibold ${
                                  isIncoming ? 'text-green-600' : 'text-red-600'
                                }`}
                              >
                                {isIncoming ? '+' : '-'}
                                {formatCurrency(transaction.amount)}
                              </span>
                            </div>
                          );
                        })}
                      </div>
                    )}
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </main>

      {/* AI Chat */}
      {showChat && <AIChat onClose={() => setShowChat(false)} />}
    </div>
  );
}
